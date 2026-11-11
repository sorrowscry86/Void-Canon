package org.voidcat.voidcanon

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.voidcat.voidcanon.ai.*
import org.voidcat.voidcanon.data.*
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream

/** Local HTTPS fixtures only: real UI, Keystore, RP commit, image lane and optional Gallery retention. */
class AutomaticArtworkUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun automaticArtDoesNotBlockRpAndPopulatesCaptionedGallery() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS)
        compose.waitUntil(120000) { compose.onAllNodesWithText("The Glass Harbor").fetchSemanticsNodes().isNotEmpty() }
        val app = compose.activity.application as VoidCanonApplication
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        assertFalse("Run this fixture on disposable data without a real provider key", app.aiSettings.hasKey())
        val originalAi = app.aiClient; val originalImages = app.imageClient
        val originalConfig = app.aiSettings.configuration()
        val allowanceBefore = runBlocking {
            // Isolate cache/job fixtures without changing worlds, story history or the consumed daily allowance.
            app.database.canonDao().clearIllustrations(); app.database.canonDao().clearIllustrationFrames()
            app.database.canonDao().illustrationAllowances().firstOrNull { it.day == LocalDate.now(ZoneOffset.UTC).toString() }?.attempts ?: 0
        }
        val before = runBlocking { app.repository.snapshot() }
        val gate = CountDownLatch(1)
        val imageCalls = AtomicInteger(); val rpCalls = AtomicInteger(); val visionCalls = AtomicInteger()
        val observedImage = "[FIXTURE] The image shows a teal coat, a brass clasp and fog beside a railing."
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val http = OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
            .retryOnConnectionFailure(false).readTimeout(360, TimeUnit.SECONDS).callTimeout(360, TimeUnit.SECONDS).build()
        val server = MockWebServer().apply { useHttps(serverTls.sslSocketFactory(), false); start() }
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(105, 70, 150)) }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/v1/status") return MockResponse().setBody("""{"day":"${LocalDate.now(ZoneOffset.UTC)}","limit":5000,"reserved":0,"model":"${CloudflareImageClient.MODEL}"}""")
                if (request.path == "/router/chat/completions") {
                    val payload = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                    if (payload.getValue("messages").toString().contains("image_url")) {
                        visionCalls.incrementAndGet()
                        assertTrue(payload.toString().contains("data:image/png;base64,"))
                        assertFalse(payload.toString().contains("PRIVATE"))
                        return MockResponse().setBody(buildJsonObject {
                            put("model", "fixture/vision:free")
                            putJsonArray("choices") { addJsonObject { put("finish_reason", "stop"); putJsonObject("message") { put("content", observedImage) } } }
                        }.toString())
                    }
                    val turn = rpCalls.incrementAndGet()
                    assertTrue(payload.getValue("messages").toString().contains("MLRPE-inspired narrative preferences"))
                    assertTrue(payload.getValue("messages").toString().contains("60–120 words"))
                    assertTrue(payload.getValue("messages").toString().contains("focus character's dialogue"))
                    if (turn > 1) assertTrue(payload.getValue("messages").toString().contains(observedImage))
                    if (turn == 1) assertTrue(payload.getValue("messages").toString().contains("Optional artwork observation"))
                    val state = runBlocking { Continuity.current(app.repository.snapshot()) }
                    val reply = Continuity.json.encodeToString(ContinuityReply(1, state.revision, "[FIXTURE] Mira waits beside the railing."))
                    return MockResponse().setBody(buildJsonObject {
                        put("model", "fixture/text:free")
                        putJsonArray("choices") { addJsonObject { put("finish_reason", "stop"); putJsonObject("message") { put("content", reply) } } }
                        putJsonObject("usage") { put("total_tokens", 50) }
                    }.toString())
                }
                val count = imageCalls.incrementAndGet()
                if (count == 1) gate.await(300, TimeUnit.SECONDS)
                if (count > 1) return MockResponse().setResponseCode(502)
                val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                assertEquals("portrait", body.getValue("size").jsonPrimitive.content)
                assertTrue(body.containsKey("reference"))
                assertFalse(body.getValue("prompt").jsonPrimitive.content.contains("PRIVATE"))
                return MockResponse().setBody(buildJsonObject {
                    put("requestId", body.getValue("requestId")); put("model", CloudflareImageClient.MODEL)
                    put("image", Base64.getEncoder().encodeToString(bytes)); put("reservedNeurons", 32)
                }.toString())
            }
        }
        app.imageClient = CloudflareImageClient(http)
        app.aiClient = OpenRouterClient(server.url("/router/").toString(), http)
        val connection = File(app.cacheDir, "auto-art-fixture.json").apply {
            writeText(Json.encodeToString(ImageConnection(server.url("/").toString(), "fixture-only-scoped-image-access-token")))
        }
        try {
            compose.runOnIdle {
                vm.importImageConnection(Uri.fromFile(connection))
                vm.models.value = listOf(RouterModel("fixture/text:free", "[FIXTURE] RP", 16000, false, true, "0", "0"),
                    RouterModel("fixture/vision:free", "[FIXTURE] vision", 16000, true, true, "0", "0"))
            }
            compose.waitUntil(30000) { vm.imageConnected.value && !vm.busy.value }
            compose.runOnIdle { vm.saveAiSettings(originalConfig.copy(rpModel = "fixture/text:free", visionModel = "fixture/vision:free", consent = true), "fixture-non-secret-openrouter-key") }
            compose.waitUntil(30000) { vm.hasKey.value && !vm.busy.value }
            dismissFeedback()
            compose.onNodeWithText("Play").performClick()
            compose.onNodeWithContentDescription("Play mode").performClick()
            compose.onNodeWithText("Freeform Story").performScrollTo().performClick()
            compose.onNodeWithText("Character focus").performScrollTo().performClick()
            compose.onNodeWithText("Brisk").performScrollTo().performClick()
            compose.onNodeWithText("Short").performScrollTo().performClick()
            compose.onNodeWithText("Save play settings").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Artwork").performClick()
            compose.onNodeWithText("Character visual brief (optional)").performScrollTo().performClick().performTextInput("[FIXTURE] Teal travel coat.")
            compose.waitForIdle(); Thread.sleep(750)
            compose.onNodeWithText("Scene artwork").assertIsDisplayed()
            compose.onNodeWithText("Close").assertIsDisplayed()
            Espresso.closeSoftKeyboard()
            compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
            compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
            compose.onNodeWithText("Save artwork settings").performScrollTo().performClick()
            compose.waitUntil(30000) { imageCalls.get() == 1 && vm.imageBusy.value }
            dismissFeedback()
            assertNull(vm.generating.value)
            compose.onNode(hasSetTextAction()).performClick().performTextReplacement("I wait beside the railing.")
            compose.onNodeWithContentDescription("Send roleplay message").assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(30000) { vm.snapshot.value.messages.any { it.text == "[FIXTURE] Mira waits beside the railing." } && vm.generating.value == null }
            assertEquals(1, rpCalls.get()); assertTrue("Image ended early: ${vm.artwork.value.status}", vm.imageBusy.value)
            assertEquals(before.assets, runBlocking { app.repository.snapshot() }.assets)
            gate.countDown()
            compose.waitUntil(30000) { vm.artwork.value.image != null && !vm.imageBusy.value }
            Espresso.closeSoftKeyboard(); dismissFeedback()
            compose.onNodeWithContentDescription("Current scene illustration").assertIsDisplayed()
            compose.onNodeWithText(vm.artwork.value.job!!.plan().caption()).assertIsDisplayed()
            capture("21-automatic-artwork-fixture")
            compose.onNodeWithContentDescription("Artwork").performClick()
            compose.onNodeWithText("View prompt").performClick()
            compose.onNodeWithText("Artwork prompt").assertIsDisplayed()
            compose.waitUntil(15000) { vm.snapshot.value.assets.size == before.assets.size + 1 && !vm.busy.value }
            compose.onNodeWithText("Saved to Gallery automatically").performScrollTo().assertIsDisplayed()
            val kept = runBlocking { app.repository.snapshot() }
            assertEquals(before.entry(before.scene!!.characterId)!!.primaryAssetId, kept.entry(before.scene!!.characterId)!!.primaryAssetId)
            assertEquals("exploratory", kept.assets.single { it.id !in before.assets.map(AssetRecord::id) }.referenceKind)
            assertEquals(vm.artwork.value.job!!.plan().caption(), kept.assets.single { it.id == vm.artwork.value.job!!.id }.label)
            assertEquals(1, imageCalls.get())
            compose.waitUntil(30000) { vm.snapshot.value.descriptions.any { it.assetId == vm.artwork.value.job!!.id && it.text == observedImage } }
            assertEquals(1, visionCalls.get())
            assertTrue(vm.contextPreview().messages.any { it.content.contains(observedImage) })
            compose.waitUntil(15000) { compose.onAllNodesWithText("Regenerate image").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Regenerate image").performScrollTo().performClick()
            compose.waitUntil(30000) { vm.artwork.value.status.contains("HTTP 502") && !vm.artwork.value.running }
            assertEquals(2, imageCalls.get())
            assertEquals(allowanceBefore + 1, vm.artwork.value.used)
            compose.onNodeWithText("Close").assertIsDisplayed().performClick()
            compose.onNodeWithText("Pause artwork").performClick()
            compose.waitUntil(10000) { !vm.artwork.value.enabled }
            compose.onNodeWithText("Layered view").performClick()
            compose.waitUntil(10000) { !vm.artwork.value.illustrated }
            compose.onNodeWithText("Illustrated view").performClick()
            compose.waitUntil(10000) { vm.artwork.value.illustrated }
            compose.onNodeWithText("Close").performClick()
            compose.onNode(hasSetTextAction()).performClick().performTextReplacement("I examine the clasp.")
            compose.onNodeWithContentDescription("Send roleplay message").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(30000) { vm.snapshot.value.messages.size == before.messages.size + 4 && vm.generating.value == null }
            assertEquals(2, rpCalls.get()); assertEquals(1, visionCalls.get())
            assertEquals(before.messages.size + 4, vm.snapshot.value.messages.size)
            assertEquals(before.entries, vm.snapshot.value.entries)
            val archive = ByteArrayOutputStream().also { runBlocking { app.repository.export(it) } }.toByteArray()
            ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
                var manifest = ""
                while (true) { val e = zip.nextEntry ?: break; if (e.name == "manifest.json") manifest = zip.readBytes().toString(Charsets.UTF_8) }
                assertFalse(manifest.contains("illustration_jobs"))
                assertFalse(manifest.contains("fixture-only-scoped-image-access-token"))
                assertFalse(manifest.contains(vm.artwork.value.image!!.name))
            }
        } finally {
            gate.countDown()
            compose.runOnIdle { vm.pauseArtwork() }
            app.repository.artworkPreferences.resetPermissions()
            app.aiSettings.clearKey(); app.aiSettings.save(originalConfig, "")
            app.imageSettings.clear(); app.imageClient = originalImages; app.aiClient = originalAi
            connection.delete(); server.shutdown()
        }
    }
    private fun dismissFeedback() {
        val dismiss = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        if (compose.onAllNodes(dismiss).fetchSemanticsNodes().isNotEmpty()) compose.onAllNodes(dismiss).onLast().performSemanticsAction(SemanticsActions.Dismiss) { it() }
    }
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(750)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.filesDir, "screenshots/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
