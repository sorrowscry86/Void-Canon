package org.voidcat.voidcanon

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.IdlingPolicies
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
import java.io.File
import java.util.concurrent.TimeUnit

class DeepSeekUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun isolatedEncryptedBackupMakesOneTextRequestWithoutAnOpenRouterKey() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        val app = compose.activity.application as VoidCanonApplication
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.waitUntil(120000) { vm.ready.value }
        assertFalse("Disposable fixture only", app.aiSettings.hasKey())
        assertFalse("Disposable fixture only", app.deepSeek.settings.hasKey())
        val progress = File(compose.activity.filesDir, "deepseek-ui-progress.txt")
        fun phase(value: String) { progress.appendText(value + "\n") }
        progress.writeText("ready\n")
        val original = runBlocking { app.repository.snapshot() }
        val originalClient = app.deepSeekClient
        val originalConfig = app.deepSeek.settings.configuration()
        val routerPrefs = File(app.applicationInfo.dataDir, "shared_prefs/openrouter-private.xml").readBytes()
        val world = runBlocking { app.repository.createWorld("DeepSeek TLS fixture", "Public setting") }
        val scene = runBlocking {
            val c = app.repository.createEntry(world, "character", "Test traveler", "Traveler", "", "PRIVATE CHARACTER NOTE")
            val l = app.repository.createEntry(world, "location", "Test quay", "Quiet quay", "", "PRIVATE LOCATION NOTE")
            app.repository.selectWorld(world); app.repository.useInScene(c, l)
        }
        val rules = runBlocking {
            app.repository.saveNarrativeSettings(scene, NarrativeSettings(mode = "aurpg"))
            app.repository.setMomentum(scene, 3)
            app.repository.rollDice(scene, "Gate check", 1, 6, 0, 4, 1)
            app.repository.addClock(scene, "Gate", 6)
            val id = Aurpg.decode(app.repository.snapshot().scene!!.aurpgJson).clocks.single().id
            app.repository.tickClock(scene, id, 1)
            Aurpg.decode(app.repository.snapshot().scene!!.aurpgJson)
        }
        phase("rules-ready")
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val server = MockWebServer().apply {
            useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory(), false); start()
        }
        val secret = "fixture-direct-deepseek-key"
        try {
            app.deepSeekClient = OpenRouterClient(server.url("/v1/").toString(), OkHttpClient.Builder()
                .sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).retryOnConnectionFailure(false).build(), "DeepSeek", false)
            compose.waitUntil(30000) { vm.snapshot.value.scene?.id == scene }
            assertNotNull(runBlocking { vm.saveDeepSeek("fixture/text", secret, false, true) })
            assertFalse(app.deepSeek.active()); assertFalse(app.deepSeek.settings.hasKey())
            assertNull(runBlocking { vm.saveDeepSeek("fixture/text", secret, true, true) })
            assertEquals(secret, app.deepSeek.settings.readKey())
            val reopened = DeepSeekBackup(app)
            assertTrue(reopened.active()); assertEquals("fixture/text", reopened.settings.configuration().rpModel)
            assertEquals(secret, reopened.settings.readKey())
            assertFalse(File(app.applicationInfo.dataDir, "shared_prefs/deepseek-private.xml").readText().contains(secret))
            assertArrayEquals(routerPrefs, File(app.applicationInfo.dataDir, "shared_prefs/openrouter-private.xml").readBytes())
            phase("key-isolation-verified")
            compose.onNodeWithText("Play").performClick()
            compose.onNodeWithContentDescription("AI settings").performClick()
            compose.onNodeWithText("DeepSeek direct backup").performScrollTo().performClick()
            phase("deepseek-sheet-opened")
            compose.onNodeWithText("DeepSeek text model ID").assertTextContains("fixture/text")
            compose.onAllNodes(isToggleable()).onLast().assertIsOn()
            server.enqueue(MockResponse().setBody("""{"data":[{"id":"fixture/text"}]}"""))
            compose.onNodeWithText("Load DeepSeek model IDs").performScrollTo().performClick()
            compose.waitUntil(30000) { compose.onAllNodesWithText("fixture/text").fetchSemanticsNodes().size >= 2 }
            phase("catalog-loaded")
            val catalog = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("/v1/models", catalog.path)
            assertEquals("Bearer $secret", catalog.getHeader("Authorization"))
            assertEquals(0L, catalog.bodySize)
            compose.onNodeWithText("Close").performClick(); compose.onNodeWithText("Close").performClick()
            phase("sheets-closed")
            val reply = Continuity.json.encodeToString(ContinuityReply(1, 0, "[TLS FIXTURE] The traveler watches the quay."))
            server.enqueue(MockResponse().setBody("""{"model":"fixture/text","choices":[{"finish_reason":"stop","message":{"content":${JsonString(reply)}}}],"usage":{"total_tokens":50}}"""))
            compose.onNode(hasSetTextAction()).performTextInput("I wait by the quay.")
            compose.waitUntil(30000) { !vm.busy.value && vm.draft.value == "I wait by the quay." }
            compose.onNodeWithContentDescription("Send roleplay message").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
            phase("send-invoked")
            compose.waitUntil(60000) { vm.generating.value == null && (vm.snapshot.value.messages.size == 2 || vm.generationError.value != null) }
            assertNull("DeepSeek fixture generation failed: ${vm.generationError.value}", vm.generationError.value)
            phase("reply-completed")
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            assertEquals("Bearer $secret", request.getHeader("Authorization"))
            assertEquals("/v1/chat/completions", request.path)
            val body = request.body.readUtf8()
            assertEquals("json_object", Json.parseToJsonElement(body).jsonObject.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
            assertTrue(body.contains(rules.checks.single().id)); assertFalse(body.contains(secret))
            assertTrue(body.contains("mechanics revision is NOT the continuity baseRevision"))
            assertTrue(body.contains("AURPG mechanics")); assertTrue(body.contains("Gate 1/6"))
            assertTrue(body.contains("total=${rules.checks.single().total}"))
            assertEquals(rules, Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson))
            assertFalse(body.contains("PRIVATE")); assertFalse(body.contains("\"provider\""))
            assertEquals(2, server.requestCount)
            assertTrue(vm.snapshot.value.generations.single().contextJson.contains("DeepSeek"))
            assertEquals("[TLS FIXTURE] The traveler watches the quay.", vm.snapshot.value.messages.last().text)
            // Exercise the reported Freeform path as well as AURPG, using the real VM route.
            assertNull(runBlocking { vm.saveNarrativeSettings(scene, NarrativeSettings(mode = "freeform")) })
            compose.waitUntil(30000) { vm.snapshot.value.scene?.narrative?.mode == "freeform" }
            val freeformReply = Continuity.json.encodeToString(ContinuityReply(1, 1, "[TLS FIXTURE] The traveler answers quietly."))
            server.enqueue(MockResponse().setBody("""{"model":"fixture/text","choices":[{"finish_reason":"stop","message":{"content":${JsonString(freeformReply)}}}]}"""))
            compose.runOnIdle { vm.updateDraft("I ask about the harbor.") }
            compose.waitUntil(30000) { !vm.busy.value && vm.draft.value == "I ask about the harbor." }
            compose.onNodeWithContentDescription("Send roleplay message").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(60000) { vm.generating.value == null && (vm.snapshot.value.messages.size == 4 || vm.generationError.value != null) }
            assertNull(vm.generationError.value)
            val freeformRequest = Json.parseToJsonElement(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()).jsonObject
            assertEquals("json_object", freeformRequest.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
            assertEquals(2L, Continuity.current(vm.snapshot.value).revision)
            assertEquals(rules, Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson))
            assertEquals("[TLS FIXTURE] The traveler answers quietly.", vm.snapshot.value.messages.last().text)
            assertTrue(vm.snapshot.value.generations.last().contextJson.contains("json_object"))
            phase("freeform-json-reply-completed")
            server.enqueue(MockResponse().setBody("""{"model":"fixture/text","choices":[{"finish_reason":"stop","message":{"content":"A plain chapter summary."}}]}"""))
            val messagesBeforeSummary = vm.snapshot.value.messages
            compose.runOnIdle { vm.proposeChapter() }
            compose.waitUntil(60000) { vm.generating.value == null && (vm.snapshot.value.chapters.isNotEmpty() || vm.summaryError.value != null) }
            assertNull(vm.summaryError.value)
            val summaryRequest = Json.parseToJsonElement(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()).jsonObject
            assertFalse(summaryRequest.containsKey("response_format"))
            assertEquals("A plain chapter summary.", vm.snapshot.value.chapters.single().summary)
            assertEquals(messagesBeforeSummary, vm.snapshot.value.messages)
            assertEquals(4, server.requestCount) // Catalog, two RP requests, one summary; no automatic retry.
            phase("plain-summary-completed")
            assertNull(runBlocking { vm.saveDeepSeek("fixture/text", "", false, false) })
            assertFalse(app.deepSeek.active()); assertFalse(vm.deepSeekActive.value)
            assertArrayEquals(routerPrefs, File(app.applicationInfo.dataDir, "shared_prefs/openrouter-private.xml").readBytes())
        } finally {
            phase("cleanup")
            app.deepSeek.save(originalConfig.rpModel, "", originalConfig.consent, false)
            app.deepSeek.settings.clearKey(); app.deepSeekClient = originalClient
            compose.activityRule.scenario.close()
            runBlocking { original.world?.id?.let { app.repository.selectWorld(it) }; original.scene?.id?.let { app.repository.selectScene(it) } }
            server.shutdown()
        }
    }
    private fun JsonString(value: String) = kotlinx.serialization.json.Json.encodeToString(value)
}
