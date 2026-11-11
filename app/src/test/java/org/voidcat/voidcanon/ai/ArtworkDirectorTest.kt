package org.voidcat.voidcanon.ai

import android.app.Application
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.voidcat.voidcanon.data.*
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtworkDirectorTest {
    private lateinit var app: Application
    private lateinit var db: CanonDatabase
    private lateinit var repo: CanonRepository
    private lateinit var director: ArtworkDirector
    private lateinit var scope: CoroutineScope
    private lateinit var server: MockWebServer
    private lateinit var lane: ImageLane
    private val clock = AtomicLong(Instant.parse("2026-11-08T12:00:00Z").toEpochMilli())
    private val posts = AtomicInteger()
    private val name = "director-${UUID.randomUUID()}.db"
    private var gate: CountDownLatch? = null
    private var failure = 0
    private var statusFailure = 0
    private var dispatchAllowed = true

    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        db = Room.databaseBuilder(app, CanonDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
        repo = CanonRepository(app, db); repo.initialize(); repo.artworkPreferences.resetPermissions()
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
        server = MockWebServer().apply { useHttps(serverTls.sslSocketFactory(), false); start() }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.method == "GET") {
                    if (statusFailure != 0) return MockResponse().setResponseCode(statusFailure)
                    val day = Instant.ofEpochMilli(clock.get()).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    return MockResponse().setBody("""{"day":"$day","limit":5000,"reserved":0,"model":"${CloudflareImageClient.MODEL}"}""")
                }
                posts.incrementAndGet()
                gate?.await(15, TimeUnit.SECONDS)
                if (failure != 0) return MockResponse().setResponseCode(failure)
                val id = Json.parseToJsonElement(request.body.readUtf8()).jsonObject.getValue("requestId")
                return MockResponse().setBody(buildJsonObject {
                    put("requestId", id); put("model", CloudflareImageClient.MODEL)
                    put("image", Base64.getEncoder().encodeToString(bytes)); put("reservedNeurons", 32)
                }.toString())
            }
        }
        val client = CloudflareImageClient(OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
            .retryOnConnectionFailure(false).build())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
        lane = ImageLane()
        director = ArtworkDirector(db, IllustrationStore(db, app.filesDir, app.cacheDir), repo.artworkPreferences,
            { ImageConnection(server.url("/").toString(), "fixture-non-secret-device-code") }, { true }, { client }, repo.images, lane, scope, clock::get, canDispatch = { dispatchAllowed }, retain = repo::retainIllustration)
        director.source(repo.snapshot())
        scope.launch { repo.observe().collect { director.source(it.selected) } }
        Unit
    }
    @After fun close() = runBlocking {
        gate?.countDown(); scope.cancel(); scope.coroutineContext[Job]!!.join()
        server.shutdown(); db.close(); app.deleteDatabase(name); Unit
    }
    private suspend fun ready() = withTimeout(20000) { director.ui.first { it.plan != null } }
    private suspend fun enable() {
        val s = repo.snapshot()
        repo.artworkPreferences.configure(s.scene!!.id, s.world!!.id, true, "", "")
        director.refresh()
    }
    private suspend fun image() = withTimeout(20000) { director.ui.first { it.image != null } }

    @Test fun permissionAndForegroundAreRequiredAndImagesDoNotMutateTheStory() = runBlocking {
        ready(); director.visible(true); delay(100)
        assertEquals(0, server.requestCount)
        director.visible(false); enable(); delay(100)
        assertEquals(0, server.requestCount)
        val before = repo.snapshot()
        dispatchAllowed = false; director.visible(true); delay(100)
        assertEquals(0, server.requestCount)
        dispatchAllowed = true; director.refresh()
        val rendered = image()
        assertTrue(rendered.image!!.isFile)
        val after = repo.snapshot()
        assertEquals(before, after.copy(assets = before.assets))
        assertEquals(before.assets.size + 1, after.assets.size)
        assertEquals(rendered.job!!.plan().caption(), after.assets.single { it.id == rendered.job.id }.label)
        assertEquals("exploratory", after.assets.single { it.id == rendered.job.id }.referenceKind)
        assertEquals(1, posts.get())
        assertEquals(1, rendered.used)
        repeat(3) { director.source(repo.snapshot()); director.refresh() }
        delay(100)
        assertEquals(1, posts.get())
    }

    @Test fun aLateResultCannotReplaceAnotherScenesArtwork() = runBlocking {
        ready(); gate = CountDownLatch(1); enable(); director.visible(true)
        withTimeout(10000) { while (posts.get() == 0) delay(10) }
        val oldScene = repo.snapshot().scene!!.id
        val location = repo.createEntry("glass-harbor", "location", "Garden", "A quiet garden", "", "")
        repo.useInScene("mira", location)
        val next = repo.snapshot()
        director.source(next)
        withTimeout(10000) { director.ui.first { it.plan?.snapshot?.sceneId == next.scene!!.id } }
        gate!!.countDown()
        withTimeout(20000) { while (db.canonDao().illustrationJobs().none { it.sceneId == oldScene && it.status == "complete" }) delay(20) }
        delay(100)
        assertEquals(next.scene!!.id, director.ui.value.plan!!.snapshot.sceneId)
        assertNull(director.ui.value.image)
        assertEquals(1, posts.get())
        assertTrue(repo.snapshot().messages.isEmpty())
    }

    @Test fun aSubmittedFailureIsNotAutomaticallyRetriedOrRefunded() = runBlocking {
        ready(); failure = 502; enable(); director.visible(true)
        withTimeout(10000) { director.ui.first { it.job?.status == "failed" } }
        repeat(3) { director.refresh() }; delay(100)
        assertEquals(1, posts.get())
        assertEquals(1, db.canonDao().illustrationAllowances().single().attempts)
        assertEquals(1, repo.snapshot().messages.size)
    }

    @Test fun manualPriorityDefersAutomaticDispatchAndAuthenticationRequiresReconnection() = runBlocking {
        ready(); lane.manualWaiting = true; enable(); director.visible(true); delay(100)
        assertEquals(0, server.requestCount)
        statusFailure = 401; lane.manualWaiting = false; director.refresh()
        withTimeout(10000) { director.ui.first { it.job?.status == "failed" } }
        assertEquals(0, posts.get())
        assertTrue(db.canonDao().illustrationAllowances().isEmpty())
        val s = repo.snapshot(); val old = Continuity.current(s)
        repo.correctContinuity(s.scene!!.id, old.revision, old.copy(actors = old.actors.map { if (it.actorId == "mira") it.copy(appearance = "Raincoat") else it }))
        director.source(repo.snapshot())
        withTimeout(10000) { director.ui.first { it.plan?.snapshot?.appearance == "Raincoat" } }
        delay(100); assertEquals(1, server.requestCount)
        statusFailure = 0; director.reconnected()
        image()
        assertEquals(1, posts.get())
    }
}
