package org.voidcat.voidcanon

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.voidcat.voidcanon.ai.*
import org.voidcat.voidcanon.data.*
import java.io.File
import java.util.concurrent.TimeUnit

/** Native UI + HTTPS fixtures: both providers can publish prose without accepting invalid state. */
class StoryCompatibilityUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun bothProvidersKeepUsableStoryWhenOptionalStateIsMissingOrInvalid() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        val app = compose.activity.application as VoidCanonApplication
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.waitUntil(120000) { vm.ready.value }
        assertFalse("Disposable fixture only", app.aiSettings.hasKey())
        assertFalse("Disposable fixture only", app.deepSeek.settings.hasKey())
        val original = runBlocking { app.repository.snapshot() }
        val config = app.aiSettings.configuration(); val directConfig = app.deepSeek.settings.configuration()
        val router = app.aiClient; val direct = app.deepSeekClient
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val trust = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val http = OkHttpClient.Builder().sslSocketFactory(trust.sslSocketFactory(), trust.trustManager).retryOnConnectionFailure(false).build()
        val server = MockWebServer().apply {
            useHttps(HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory(), false); start()
        }
        val progress = File(compose.activity.filesDir, "story-compatibility-progress.txt").apply { writeText("ready\n") }
        try {
            val scene = runBlocking {
                val w = app.repository.createWorld("Response compatibility fixture", "A quiet harbor")
                val c = app.repository.createEntry(w, "character", "Traveler", "Traveler", "", "PRIVATE NOTE")
                val l = app.repository.createEntry(w, "location", "Quay", "Quay", "", "PRIVATE NOTE")
                app.repository.selectWorld(w)
                app.repository.useInScene(c, l).also { app.repository.saveNarrativeSettings(it, NarrativeSettings(mode = "freeform")) }
            }
            compose.waitUntil(30000) { vm.snapshot.value.scene?.id == scene }
            val before = runBlocking { app.repository.snapshot() }
            app.aiClient = OpenRouterClient(server.url("/router/").toString(), http)
            app.deepSeekClient = OpenRouterClient(server.url("/direct/").toString(), http, "DeepSeek", false)
            compose.onNodeWithText("Play").performClick()
            var expectedMessages = 0
            for (provider in listOf("DeepSeek", "OpenRouter")) {
                if (provider == "DeepSeek") assertNull(runBlocking { vm.saveDeepSeek("fixture/text", "fixture-direct-key", true, true) })
                else {
                    assertNull(runBlocking { vm.saveDeepSeek("fixture/text", "", false, false) })
                    compose.runOnIdle {
                        vm.models.value = listOf(RouterModel("fixture/text:free", "Fixture", 32768, false, true, "0", "0", 4096))
                        vm.saveAiSettings(config.copy(rpModel = "fixture/text:free", freeOnly = true, consent = true), "fixture-router-key")
                    }
                    compose.waitUntil(30000) { vm.hasKey.value && !vm.busy.value }
                }
                for (envelope in listOf(false, true)) {
                    val story = "The traveler answers $provider ${if (envelope) "again" else "quietly"}."
                    val content = if (envelope) buildJsonObject {
                        put(if (provider == "DeepSeek") "story" else "narrative", story)
                        put("baseRevision", 999); put("actors", "invalid optional state")
                    }.toString() else story
                    server.enqueue(MockResponse().setBody(buildJsonObject {
                        put("model", "fixture/text")
                        putJsonArray("choices") { addJsonObject {
                            put("finish_reason", "stop"); putJsonObject("message") { put("content", content) }
                        } }
                    }.toString()))
                    val action = "I ask $provider ${if (envelope) "again" else "a question"}."
                    compose.runOnIdle { vm.updateDraft(action) }
                    compose.waitUntil(30000) { !vm.busy.value && vm.draft.value == action }
                    compose.onNodeWithContentDescription("Send roleplay message").assertIsEnabled()
                        .performSemanticsAction(SemanticsActions.OnClick) { it() }
                    expectedMessages += 2
                    compose.waitUntil(60000) { vm.generating.value == null && (vm.snapshot.value.messages.size == expectedMessages || vm.generationError.value != null) }
                    assertNull(vm.generationError.value)
                    compose.onNodeWithText(story).assertIsDisplayed()
                    compose.onAllNodesWithText(StoryResponse.WARNING).assertCountEquals(1)
                    compose.onNodeWithText(StoryResponse.WARNING).assertIsDisplayed()
                    assertEquals("", vm.draft.value)
                    val request = server.takeRequest(5, TimeUnit.SECONDS)!!
                    assertEquals(if (provider == "DeepSeek") "/direct/chat/completions" else "/router/chat/completions", request.path)
                    val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                    assertFalse(body.containsKey("response_format"))
                    assertEquals(provider == "OpenRouter", body.containsKey("provider"))
                    assertFalse(body.toString().contains("PRIVATE NOTE"))
                    val saved = runBlocking { app.repository.snapshot() }
                    assertEquals(before.scene!!.continuityJson, saved.scene!!.continuityJson)
                    assertEquals(before.scene!!.aurpgJson, saved.scene!!.aurpgJson)
                    assertEquals(before.entries, saved.entries); assertEquals(before.continuityEvents, saved.continuityEvents)
                    assertTrue(saved.generations.all { it.status == "completed" && it.error == StoryResponse.WARNING })
                    assertTrue(vm.contextPreview().messages.first().content.contains(StoryResponse.CONTEXT_NOTE))
                    progress.appendText("$provider ${if (envelope) "wrapper" else "prose"} passed\n")
                }
            }
            assertEquals(4, server.requestCount)
            compose.activityRule.scenario.recreate()
            val reopened = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
            compose.waitUntil(60000) { reopened.ready.value && reopened.snapshot.value.scene?.id == scene }
            // Reopening the app returns to World; explicitly resume the preserved scene.
            compose.onNodeWithText("Play").performClick()
            compose.waitUntil(60000) { compose.onAllNodesWithText("The traveler answers OpenRouter again.").fetchSemanticsNodes().isNotEmpty() }
            compose.onAllNodesWithText(StoryResponse.WARNING).onFirst().assertIsDisplayed()
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.filesDir, "screenshots/22-story-compatibility.png").apply { parentFile!!.mkdirs() }
                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle(); progress.appendText("restart warning and story passed\n")
            compose.onNode(hasSetTextAction()).performClick()
            compose.waitUntil(15000) { compose.onAllNodesWithText("Gallery").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("The traveler answers OpenRouter again.").assertIsDisplayed()
            compose.onNode(hasSetTextAction()).assertIsDisplayed()
            val keyboard = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(compose.activity.filesDir, "screenshots/23-story-compatibility-keyboard.png")
                .outputStream().use { keyboard.compress(Bitmap.CompressFormat.PNG, 100, it) }
            keyboard.recycle(); progress.appendText("keyboard story visibility passed\n")
        } finally {
            app.deepSeek.save(directConfig.rpModel, "", directConfig.consent, false); app.deepSeek.settings.clearKey()
            app.aiSettings.clearKey(); app.aiSettings.save(config, "")
            app.aiClient = router; app.deepSeekClient = direct
            compose.activityRule.scenario.close()
            runBlocking { original.world?.id?.let { app.repository.selectWorld(it) }; original.scene?.id?.let { app.repository.selectScene(it) } }
            server.shutdown()
        }
    }
}
