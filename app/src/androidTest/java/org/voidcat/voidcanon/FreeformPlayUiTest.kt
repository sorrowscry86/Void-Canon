package org.voidcat.voidcanon

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.voidcat.voidcanon.data.*
import java.io.File
import java.util.concurrent.TimeUnit

/** Offline author preferences and reading layout; no inference or real credential required. */
class FreeformPlayUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun scenePreferencesPersistAndCompactToolbarLeavesRoomForDialogue() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        val app = compose.activity.application as VoidCanonApplication
        var vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.waitUntil(120000) { vm.ready.value }
        val original = runBlocking { app.repository.snapshot() }
        val world = runBlocking { app.repository.createWorld("Freeform offline fixture", "Local setting") }
        val scene = runBlocking {
            val character = app.repository.createEntry(world, "character", "Test traveler", "A traveler", "", "")
            val location = app.repository.createEntry(world, "location", "Test quay", "A quiet quay", "", "")
            app.repository.selectWorld(world)
            val id = app.repository.useInScene(character, location)
            val library = app.repository.workspaceSnapshot()
            library.asset("mira-art")?.let { image -> File(app.repository.images, image.fileName).inputStream().use { app.repository.importImage(character, "cutout", it) } }
            library.asset("harbor-art")?.let { image -> File(app.repository.images, image.fileName).inputStream().use { app.repository.importImage(location, "background", it) } }
            id
        }
        val longDialogue = "[OFFLINE LAYOUT FIXTURE] " + (1..18).joinToString("\n\n") { "Lantern $it lights the quay. The traveler pauses beside a stack of crates, watching the water while leaving your next move to you." }
        runBlocking { app.database.canonDao().putMessages(listOf(MessageRecord("freeform-layout-fixture", scene, "character", longDialogue, 0))) }
        try {
            compose.waitUntil(30000) { vm.snapshot.value.scene?.id == scene && vm.snapshot.value.messages.isNotEmpty() }
            compose.onNodeWithText("Play").performClick()
            val labels = listOf("Play mode", "Scene context", "Scene details", "AI settings", "Artwork")
            val tops = labels.map { label -> compose.onNodeWithContentDescription(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top }
            assertTrue("Toolbar must stay on one row", tops.max() - tops.min() < 2f)
            compose.onNodeWithText("AI", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Context", useUnmergedTree = true).assertDoesNotExist()
            assertFullInputHeight()
            capture("01-compact-play-long-dialogue")
            compose.onNodeWithContentDescription("Play mode").performClick()
            compose.onNodeWithText("Freeform Story").performScrollTo().performClick()
            compose.onNodeWithText("Character focus").performScrollTo().performClick()
            compose.onNodeWithText("Measured").performScrollTo().performClick()
            compose.onNodeWithText("Rich").performScrollTo().performClick()
            compose.onNodeWithText("AURPG · planned").performScrollTo().assertIsDisplayed()
            capture("02-freeform-settings")
            compose.onNodeWithText("Save play settings").performScrollTo().performClick()
            val expected = NarrativeSettings("freeform", "character", "measured", "rich")
            compose.waitUntil(30000) { vm.snapshot.value.scene?.narrative == expected }
            compose.onNodeWithContentDescription("Play mode").performClick()
            compose.onNodeWithText("Character focus").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Classic RP").performScrollTo().performClick()
            compose.onNodeWithText("Close").performClick()
            assertEquals(expected, runBlocking { app.repository.snapshot() }.scene!!.narrative)
            compose.activityRule.scenario.recreate()
            vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
            compose.waitUntil(120000) { vm.ready.value && vm.snapshot.value.scene?.id == scene }
            compose.onNodeWithText("Play").performClick()
            compose.onNodeWithContentDescription("Play mode").performClick()
            compose.onNodeWithText("Freeform Story").assertIsSelected()
            compose.onNodeWithText("Character focus").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Measured").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Rich").performScrollTo().assertIsSelected()
            compose.onNodeWithText("Close").performClick()
            capture("04-freeform-saved-play")
            compose.onNodeWithContentDescription("Scene context").performClick()
            compose.onNodeWithText("Scene context").assertIsDisplayed()
            assertEquals("character", vm.contextPreview().settings["narrativePreset"])
            assertTrue(vm.contextPreview().messages.first().content.contains("220–350"))
            compose.onNodeWithText("Close").performClick()
            compose.onNode(hasSetTextAction()).performClick().performTextReplacement("An unsent freeform action")
            compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Play mode").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithContentDescription("Send roleplay message").assertIsDisplayed()
            assertFullInputHeight()
            capture("03-freeform-keyboard")
            Espresso.closeSoftKeyboard()
            compose.onNode(hasSetTextAction()).assertTextContains("An unsent freeform action")
            assertEquals(longDialogue, runBlocking { app.repository.snapshot() }.messages.single().text)
            assertTrue(runBlocking { app.repository.snapshot() }.generations.isEmpty())
        } finally {
            runBlocking { original.world?.id?.let { app.repository.selectWorld(it) }; original.scene?.id?.let { app.repository.selectScene(it) } }
        }
    }

    private fun assertFullInputHeight() {
        val height = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.height
        val minimum = 56f * compose.activity.resources.displayMetrics.density
        assertTrue("Long dialogue must not squeeze the input below 56 dp: $height < $minimum", height >= minimum - 1f)
    }

    private fun capture(name: String) {
        // Capture the settled layout, after modal exit/IME animation and save feedback.
        compose.runOnIdle { ViewModelProvider(compose.activity)[CanonViewModel::class.java].notice.value = null }
        compose.waitForIdle(); Thread.sleep(750)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.filesDir, "screenshots/$name.png").apply { parentFile!!.mkdirs() }
            .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
