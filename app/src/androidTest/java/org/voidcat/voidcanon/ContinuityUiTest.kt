package org.voidcat.voidcanon

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.voidcat.voidcanon.ai.SceneContext
import org.voidcat.voidcanon.data.Continuity
import java.io.File
import java.util.concurrent.TimeUnit

/** Run only on a disposable installation, like the existing full walkthrough. */
class ContinuityUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun correctStateAndRecoverItThroughTheEditor() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS)
        compose.waitUntil(120000) { compose.onAllNodesWithText("The Glass Harbor").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Play", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Scene details").performClick()
        compose.onNodeWithText("Review scene continuity").performClick()
        compose.onNodeWithText("Time at start of tracking (optional)").performTextInput("Harbor dusk")
        compose.onNodeWithText("Position").performScrollTo().performTextInput("beside the railing")
        compose.onNodeWithText("Current activity").performScrollTo().performTextInput("watching the quay")
        val app = compose.activity.application as VoidCanonApplication
        val transcript = runBlocking { app.repository.snapshot().messages }
        compose.onNodeWithText("Save state correction").performScrollTo().performClick()
        compose.waitUntil(15000) { runBlocking { Continuity.current(app.repository.snapshot()).revision > 0 } }
        val snapshot = runBlocking { app.repository.snapshot() }
        assertEquals(transcript, snapshot.messages)
        assertEquals("beside the railing", Continuity.current(snapshot).actors.first().position)
        assertTrue(SceneContext.build(snapshot, "Wait").messages.first().content.contains("watching the quay"))
        compose.onNodeWithText("Review scene continuity").performClick()
        compose.onNodeWithText("Position").performScrollTo().assertTextContains("beside the railing")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        val file = File(app.filesDir, "screenshots/15-scene-continuity.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
