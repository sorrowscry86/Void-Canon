package org.voidcat.voidcanon

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.voidcat.voidcanon.ai.ArtworkPreferences
import java.util.concurrent.TimeUnit

/** Settings must persist even before a new phone has a reference or provider connection. */
class ArtworkSettingsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun savesIncompleteSetupAndReopensAfterActivityRecreation() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        val app = compose.activity.application as VoidCanonApplication
        var vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.waitUntil(120000) { vm.ready.value }
        assertFalse("Use disposable data without a real image connection", app.imageSettings.connected())
        val original = runBlocking { app.repository.snapshot() }
        val world = runBlocking { app.repository.createWorld("Artwork settings fixture", "") }
        val scene = runBlocking {
            val character = app.repository.createEntry(world, "character", "New traveler", "", "", "")
            val location = app.repository.createEntry(world, "location", "New harbor", "", "", "")
            app.repository.selectWorld(world)
            app.repository.useInScene(character, location)
        }
        try {
            compose.waitUntil(30000) { vm.snapshot.value.scene?.id == scene }
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Automatic scene artwork").performScrollTo().performClick()
            compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
            compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
            compose.onNodeWithText("World artwork style (optional)").performScrollTo().performTextInput("Watercolor harbor")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Save artwork settings").performScrollTo().performClick()
            compose.onNodeWithText("Automatic scene artwork").assertIsDisplayed()
            assertTrue(ArtworkPreferences(app).enabled(scene))
            assertTrue(ArtworkPreferences(app).describeForRoleplay(scene))
            assertEquals("Watercolor harbor", ArtworkPreferences(app).style(world))
            compose.onNodeWithText("Automatic scene artwork").performScrollTo().performClick()
            compose.onAllNodes(isToggleable()).onFirst().assertIsOn()
            compose.onAllNodes(isToggleable()).onLast().assertIsOn()
            compose.onNodeWithText("World artwork style (optional)").assertTextContains("Watercolor harbor")
            compose.onNodeWithText("Character visual brief (optional)").performScrollTo().performClick().performTextInput("Teal coat and dark hair")
            compose.onNodeWithText("Scene artwork").assertIsDisplayed()
            Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Save artwork settings").performScrollTo().performClick()
            compose.activityRule.scenario.recreate()
            vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
            compose.waitUntil(120000) { vm.ready.value && vm.snapshot.value.scene?.id == scene }
            compose.onNodeWithText("Settings").performClick()
            compose.onNodeWithText("Automatic scene artwork").performScrollTo().performClick()
            compose.onAllNodes(isToggleable()).onFirst().assertIsOn()
            compose.onNodeWithText("Character visual brief (optional)").assertTextContains("Teal coat and dark hair")
            compose.onNodeWithText("World artwork style (optional)").performScrollTo().assertTextContains("Watercolor harbor")
            assertEquals("Teal coat and dark hair", ArtworkPreferences(app).brief(scene))
        } finally {
            app.repository.artworkPreferences.pause(scene)
            runBlocking { original.world?.id?.let { app.repository.selectWorld(it) }; original.scene?.id?.let { app.repository.selectScene(it) } }
        }
    }
}
