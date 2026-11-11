package org.voidcat.voidcanon

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

/** Exercises visible exits, settings return paths, and draft preservation with the IME open. */
class NavigationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsAndExitsPreserveLocalWork() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS)
        compose.waitUntil(120000) { compose.onAllNodesWithText("The Glass Harbor").fetchSemanticsNodes().isNotEmpty() }
        val app = compose.activity.application as VoidCanonApplication
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        val before = runBlocking { app.repository.snapshot() }
        val configuration = app.aiSettings.configuration()
        compose.onNodeWithText("0.6.2 · build 15").assertIsDisplayed()
        for (destination in listOf("World", "Gallery", "Play", "Chapters")) {
            compose.onNode(hasText(destination) and hasClickAction()).performClick()
            compose.onNodeWithText("Settings").assertIsDisplayed().performClick()
            compose.onNodeWithText("Version 0.6.2 · build 15").assertIsDisplayed()
            compose.onNodeWithText("Regular Void Canon app").assertIsDisplayed()
            compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("AI & models").performClick()
        compose.onNodeWithText("OpenRouter").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and hasAnyDescendant(hasText("Choose your models")))
            .performScrollToIndex(5)
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithText("Artwork connection").performClick()
        compose.onNodeWithText("Cloudflare · FLUX.2 Klein 4B").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("Backups & data").performScrollTo().performClick()
        compose.onNodeWithText("Export backup").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Play").performClick()
        compose.onNodeWithContentDescription("Scene context").performClick()
        compose.onNodeWithText("Scene context").assertIsDisplayed()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Scene history").performClick()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Scene details").performClick()
        compose.onNodeWithText("Review scene continuity").performClick()
        compose.onNodeWithText("Save state correction").performScrollTo()
        compose.onNodeWithText("Close").assertIsDisplayed().performClick()
        compose.onNodeWithText("Cancel").performClick()
        val draft = vm.draft.value
        try {
            compose.onNode(hasSetTextAction()).performClick().performTextReplacement("Navigation fixture unsent draft")
            compose.waitUntil(15000) { compose.onAllNodesWithText("Gallery").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithText("Back").assertIsDisplayed()
            capture("18-play-keyboard-exit")
            compose.onNodeWithText("Back").performClick()
            compose.onNodeWithText("Play").performClick()
            compose.onNode(hasSetTextAction()).assertTextContains("Navigation fixture unsent draft")
        } finally { compose.runOnIdle { vm.updateDraft(draft) } }
        compose.onNodeWithText("Settings").performClick()
        capture("19-settings")
        compose.onNodeWithText("Close").performClick()
        val after = runBlocking { app.repository.snapshot() }
        assertEquals(before.messages, after.messages)
        assertEquals(before.entries, after.entries)
        assertEquals(before.assets, after.assets)
        assertEquals(before.continuityEvents, after.continuityEvents)
        assertEquals(configuration, app.aiSettings.configuration())
    }

    private fun capture(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.filesDir, "screenshots/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
