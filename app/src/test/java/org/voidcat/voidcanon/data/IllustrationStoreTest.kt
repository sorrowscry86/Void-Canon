package org.voidcat.voidcanon.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.voidcat.voidcanon.ai.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IllustrationStoreTest {
    private lateinit var app: Application
    private lateinit var db: CanonDatabase
    private lateinit var repo: CanonRepository
    private lateinit var store: IllustrationStore
    private val name = "art-${UUID.randomUUID()}.db"
    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        open(); repo.initialize(); repo.artworkPreferences.resetPermissions()
    }
    private fun open() {
        db = Room.databaseBuilder(app, CanonDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        repo = CanonRepository(app, db)
        store = IllustrationStore(db, app.filesDir, app.cacheDir)
    }
    @After fun close() { db.close(); app.deleteDatabase(name) }
    private suspend fun enable() {
        val s = repo.snapshot(); repo.artworkPreferences.configure(s.scene!!.id, s.world!!.id, true, "", "")
    }
    private suspend fun appearance(s: WorldSnapshot, value: String): WorldSnapshot {
        val current = repo.snapshot()
        require(current.scene!!.id == s.scene!!.id)
        val state = Continuity.current(current)
        repo.correctContinuity(current.scene!!.id, state.revision, state.copy(actors = state.actors.map {
            if (it.actorId == current.scene!!.characterId) it.copy(appearance = value) else it
        }))
        return repo.snapshot()
    }
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
    }

    @Test fun pendingCandidatesCoalesceAndStaleClaimsCannotDispatch() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val original = store.jobs().single()
        store.plan(appearance(s, "Red coat"), repo.artworkPreferences)
        val current = store.plan(appearance(s, "Blue coat"), repo.artworkPreferences)
        assertEquals(1, store.jobs().count { it.status == "pending" })
        assertEquals(2, store.jobs().count { it.status == "superseded" })
        assertNull(store.claim(original.id, "2026-11-08", 100000))
        assertEquals(current.visualKey, store.jobs().last { it.status == "pending" }.visualKey)
    }

    @Test fun quotaAndCooldownCountDispatchedFailuresAcrossScenesAndSurviveRestore() = runBlocking {
        enable(); val s = repo.snapshot(); val base = 1700000000000L
        repeat(30) { n ->
            store.plan(appearance(s, "Coat $n"), repo.artworkPreferences)
            val pending = store.jobs().last { it.status == "pending" }
            val row = store.claim(pending.id, "2026-11-08", base + n * 30001L)!!
            store.fail(row.id, "failed", "Fixture failure")
        }
        store.plan(appearance(s, "Another coat"), repo.artworkPreferences)
        val pending = store.jobs().last { it.status == "pending" }
        assertNull(store.claim(pending.id, "2026-11-08", base + 31 * 30001))
        assertEquals(30, db.canonDao().illustrationAllowances().single().attempts)
        val backup = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(ByteArrayInputStream(backup))
        assertTrue(store.jobs().isEmpty())
        assertEquals(30, db.canonDao().illustrationAllowances().single().attempts)
        assertFalse(repo.artworkPreferences.enabled(s.scene!!.id))
        enable(); store.plan(repo.snapshot(), repo.artworkPreferences)
        assertNotNull(store.claim(store.jobs().single().id, "2026-11-09", base + 86400000))
    }

    @Test fun cooldownIsEnforcedAndManualRegenerationDoesNotConsumeAutomaticAllowance() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val first = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        store.fail(first.id, "failed", "test")
        store.plan(appearance(s, "Raincoat"), repo.artworkPreferences)
        val next = store.jobs().last { it.status == "pending" }
        assertNull(store.claim(next.id, "2026-11-08", 110000))
        store.plan(repo.snapshot(), repo.artworkPreferences, force = true)
        val manual = store.jobs().last { it.status == "pending" }
        assertFalse(manual.automatic)
        assertNotNull(store.claim(manual.id, "2026-11-08", 110000))
        assertEquals(1, db.canonDao().illustrationAllowances().single().attempts)
    }

    @Test fun restartNeverAutomaticallyReplaysASubmittedOrInterruptedRequest() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        store.claim(store.jobs().single().id, "2026-11-08", 100000)
        db.close(); open(); store.recover()
        assertEquals("interrupted", store.jobs().single().status)
        store.plan(repo.snapshot(), repo.artworkPreferences)
        assertEquals(1, store.jobs().size)
        assertNull(store.claim(store.jobs().single().id, "2026-11-08", 200000))
    }

    @Test fun cachedImagesDoNotAlterStoryFingerprintsAndEvictionNeverDeletesGalleryOriginals() = runBlocking {
        enable(); val s = repo.snapshot(); val fingerprint = SceneContext.fingerprint(s)
        store.plan(s, repo.artworkPreferences)
        val row = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        val complete = store.finish(row, GeneratedImage(png(), CloudflareImageClient.MODEL, row.id, 32))
        assertNotNull(store.file(complete))
        assertEquals(fingerprint, SceneContext.fingerprint(repo.snapshot()))
        assertEquals(s.assets, repo.snapshot().assets)
        store.prune(protect = complete.id, maxBytes = 0)
        assertNotNull(store.file(complete))
        store.prune(maxBytes = 0)
        assertNull(store.file(store.jobs().single()))
        s.assets.forEach { assertTrue(File(repo.images, it.fileName).isFile) }
    }

    @Test fun acceptedStoryAndImageIntentCommitOnceButMalformedOptionalArtDoesNotRejectStory() = runBlocking {
        enable(); val s = repo.snapshot()
        val record = GenerationRecord("fixture-turn", s.scene!!.id, "roleplay", "fixture/free",
            Json.encodeToString(SceneContext.build(s, "Continue", artwork = true)), SceneContext.fingerprint(s), "Continue")
        repo.beginGeneration(record)
        val reply = Continuity.json.encodeToString(ContinuityReply(1, 0, "Mira raises a lantern.", visual = JsonPrimitive(42)))
        val result = RouterResult(reply, "fixture/free", "{}")
        repo.finishGeneration(record.id, result); repo.finishGeneration(record.id, result)
        assertEquals(s.messages.size + 2, repo.snapshot().messages.size)
        assertEquals(1, store.jobs().size)
        assertEquals("reply-fixture-turn", store.jobs().single().plan().sourceMessageId)
        assertEquals("pending", store.jobs().single().status)
        val frozen = store.frame(s.scene!!.id)
        assertTrue(runCatching { store.plan(s, repo.artworkPreferences) }.isFailure)
        assertEquals(frozen, store.frame(s.scene!!.id)) // An older observer cannot overwrite the committed visual intent.
        assertEquals(s.assets, repo.snapshot().assets)
    }

    @Test fun evictionUsesRecentAccessRatherThanCreationOrder() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val first = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        val one = store.finish(first, GeneratedImage(png(), CloudflareImageClient.MODEL, first.id, 32))
        store.plan(appearance(s, "Raincoat"), repo.artworkPreferences)
        val second = store.claim(store.jobs().last { it.status == "pending" }.id, "2026-11-08", 200000)!!
        val two = store.finish(second, GeneratedImage(png(), CloudflareImageClient.MODEL, second.id, 32))
        store.touch(one.id, System.currentTimeMillis() + 120000)
        store.prune(maxBytes = store.file(one)!!.length())
        val rows = store.jobs().associateBy { it.id }
        assertNotNull(store.file(rows[one.id]))
        assertNull(store.file(rows[two.id]))
    }

    @Test fun realSchemaThreeUpgradePreservesEveryDomainRowAndId() = runBlocking {
        val before = repo.workspaceSnapshot()
        db.close(); app.deleteDatabase(name)
        val relative = "schemas/org.voidcat.voidcanon.data.CanonDatabase/3.json"
        val schema = Json.parseToJsonElement(listOf(File(relative), File("app/$relative")).first { it.isFile }.readText()).jsonObject.getValue("database").jsonObject
        val encoded = Continuity.json.encodeToJsonElement(before).jsonObject
        val keys = mapOf("worlds" to "worlds", "entries" to "entries", "assets" to "assets", "scenes" to "scenes", "messages" to "messages", "visual_descriptions" to "descriptions", "chapters" to "chapters", "generations" to "generations", "continuity_events" to "continuityEvents")
        SQLiteDatabase.openOrCreateDatabase(app.getDatabasePath(name), null).use { old ->
            schema.getValue("entities").jsonArray.forEach { raw ->
                val entity = raw.jsonObject; val table = entity.getValue("tableName").jsonPrimitive.content
                old.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("${'$'}{TABLE_NAME}", table))
                encoded[keys.getValue(table)]!!.jsonArray.forEach { row ->
                    val values = android.content.ContentValues()
                    row.jsonObject.forEach { (key, value) ->
                        val p = value.jsonPrimitive
                        when {
                            value == JsonNull -> values.putNull(key)
                            p.booleanOrNull != null -> values.put(key, if (p.boolean) 1 else 0)
                            p.longOrNull != null && !p.isString -> values.put(key, p.long)
                            else -> values.put(key, p.content)
                        }
                    }
                    old.insertOrThrow(table, null, values)
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.version = 3
        }
        open(); repo.initialize()
        assertEquals(before, repo.workspaceSnapshot())
        assertTrue(store.jobs().isEmpty())
        assertEquals(4, db.openHelper.writableDatabase.version)
    }
}
