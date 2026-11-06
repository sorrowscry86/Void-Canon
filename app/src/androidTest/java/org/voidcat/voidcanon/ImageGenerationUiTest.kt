package org.voidcat.voidcanon

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.runner.RunWith
import org.voidcat.voidcanon.ai.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

/** TLS network fixture exercises Android consent, reference selection, preview and explicit save. */
@RunWith(AndroidJUnit4::class)
class ImageGenerationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun generatePreviewAndSavePreservesTheBaseline() {
        val app = compose.activity.application as VoidCanonApplication
        compose.waitUntil(30000) { runCatching { compose.onAllNodesWithText("The Glass Harbor").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        val before = runBlocking { app.repository.snapshot() }
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        val server = MockWebServer().apply { useHttps(serverTls.sslSocketFactory(), false); start() }
        val oldClient = app.imageClient
        app.imageClient = CloudflareImageClient(OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build())
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(110, 80, 160)) }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        bitmap.recycle()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                assertEquals(setOf("requestId", "prompt", "size"), body.keys)
                return MockResponse().setBody(buildJsonObject {
                    put("requestId", body.getValue("requestId")); put("model", CloudflareImageClient.MODEL)
                    put("image", Base64.getEncoder().encodeToString(bytes)); put("reservedNeurons", 27)
                }.toString())
            }
        }
        val connection = File(app.cacheDir, "fixture-image-connection.json").apply {
            writeText(Json.encodeToString(ImageConnection(server.url("/").toString(), "fixture-only-scoped-device-access-token")))
        }
        try {
            compose.runOnIdle { vm.importImageConnection(Uri.fromFile(connection)) }
            compose.waitUntil(10000) { vm.imageConnected.value && !vm.busy.value }
            compose.onNodeWithText("Gallery").performClick()
            compose.onNodeWithText("Generate artwork").performScrollTo().assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(30000) { compose.onAllNodesWithText("Image prompt").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Image prompt").performScrollTo().performTextInput("A new portrait for a test")
            compose.onNodeWithText("Generate image").performScrollTo().assertIsNotEnabled()
            compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
            compose.onNodeWithText("Generate image").performScrollTo().performClick()
            compose.waitUntil(30000) { vm.imagePreview.value != null }
            assertEquals(before.assets.size, runBlocking { app.repository.snapshot() }.assets.size)
            compose.onNodeWithText("Save to Gallery").performScrollTo().performClick()
            compose.waitUntil(10000) { vm.imagePreview.value == null && !vm.busy.value }
            val after = runBlocking { app.repository.snapshot() }
            assertEquals(before.assets.size + 1, after.assets.size)
            assertEquals(before.entry(before.scene!!.characterId)!!.primaryAssetId, after.entry(before.scene!!.characterId)!!.primaryAssetId)
            assertEquals(before.messages, after.messages)
            assertEquals(1, server.requestCount)
            assertEquals("exploratory", after.assets.last { it.provenance.contains("A new portrait for a test") }.referenceKind)
            compose.onNodeWithText("Close").assertIsDisplayed().performClick()
            val capture = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val screenshot = File(app.filesDir, "screenshots/17-image-generation-fixture.png").apply { parentFile!!.mkdirs() }
            screenshot.outputStream().use { capture.compress(Bitmap.CompressFormat.PNG, 100, it) }; capture.recycle()
        } finally {
            app.imageClient = oldClient; app.imageSettings.clear(); connection.delete(); server.shutdown()
        }
    }
}
