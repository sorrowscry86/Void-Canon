package org.voidcat.voidcanon

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
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

class AurpgSpeechUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun localRulesPersistAndSpeechIsExplicitWithoutChangingStory() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        val app = compose.activity.application as VoidCanonApplication
        var vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.waitUntil(120000) { vm.ready.value }
        val progress = File(compose.activity.filesDir, "rules-ui-progress.txt")
        fun phase(value: String) { progress.appendText(value + "\n") }
        progress.writeText("ready\n")
        val original = runBlocking { app.repository.snapshot() }
        val world = runBlocking { app.repository.createWorld("Rules and speech fixture", "") }
        val scene = runBlocking {
            val c = app.repository.createEntry(world, "character", "Traveler", "", "", "")
            val l = app.repository.createEntry(world, "location", "Quay", "", "", "")
            app.repository.selectWorld(world); app.repository.useInScene(c, l)
        }
        val text = "[OFFLINE FIXTURE] Lanterns flicker beside the harbor gate. The traveler waits for your next move."
        runBlocking { app.database.canonDao().putMessages(listOf(MessageRecord("rules-speech-$scene", scene, "character", text, 0))) }
        try {
            compose.waitUntil(30000) { vm.snapshot.value.scene?.id == scene && vm.snapshot.value.messages.size == 1 }
            compose.onNodeWithText("Play").performClick()
            compose.onNodeWithContentDescription("Play mode").performClick()
            compose.onNodeWithText("AURPG").performScrollTo().performClick()
            compose.onNodeWithText("Save AURPG mode & open tools").performScrollTo().performClick()
            phase("requested-tools")
            compose.onNodeWithText("AURPG tools").assertIsDisplayed()
            phase("tools-visible")
            repeat(3) { index ->
                phase("momentum-$index")
                compose.onNodeWithText("Momentum +1").performScrollTo().performClick()
                compose.waitUntil(30000) { Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson).momentum == index + 1 }
            }
            phase("momentum-set")
            compose.onNodeWithText("Check label (optional)").performScrollTo().performTextInput("Open gate")
            compose.onNodeWithText("Target (optional, 1–1000)").performScrollTo().performTextInput("10")
            compose.onNodeWithText("Momentum to spend").performScrollTo().performTextReplacement("1")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Roll and record").performScrollTo().performClick()
            compose.waitUntil(30000) { Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson).checks.size == 1 }
            phase("rolled")
            compose.onNodeWithText("New clock name").performScrollTo().performTextInput("Alarm")
            Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Add progress clock").performScrollTo().performClick()
            compose.waitUntil(30000) { Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson).clocks.size == 1 }
            compose.onNodeWithContentDescription("Increase Alarm").performScrollTo().performClick()
            compose.waitUntil(30000) { Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson).clocks.single().filled == 1 }
            compose.runOnIdle { vm.notice.value = null }; compose.waitForIdle()
            phase("clock-ticked")
            capture("01-aurpg-tools")
            val expected = Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson)
            assertEquals(2, expected.momentum)
            assertEquals(expected.checks.single().dice.sum() + 1, expected.checks.single().total)
            compose.onNodeWithText("Close").performClick(); compose.onNodeWithText("Close").performClick()
            compose.runOnIdle { vm.notice.value = null }
            phase("before-speech")
            compose.onNodeWithContentDescription("Read aloud").assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(30000) { compose.onAllNodesWithContentDescription("Stop reading").fetchSemanticsNodes().isNotEmpty() || vm.notice.value?.contains("voice", true) == true || vm.notice.value?.contains("Read Aloud") == true }
            if (compose.onAllNodesWithContentDescription("Stop reading").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithContentDescription("Stop reading").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithContentDescription("Read aloud").assertIsDisplayed()
            capture("02-read-aloud-play")
            compose.activityRule.scenario.recreate()
            vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
            compose.waitUntil(120000) { vm.ready.value && vm.snapshot.value.scene?.id == scene }
            assertEquals("aurpg", vm.snapshot.value.scene!!.narrative.mode)
            assertEquals(expected, Aurpg.decode(vm.snapshot.value.scene!!.aurpgJson))
            assertTrue(vm.contextPreview().messages.any { it.content.contains("Alarm 1/6") && it.content.contains("Recorded check") })
            assertEquals(text, vm.snapshot.value.messages.single().text)
            assertTrue(vm.snapshot.value.generations.isEmpty())
        } finally {
            runBlocking { original.world?.id?.let { app.repository.selectWorld(it) }; original.scene?.id?.let { app.repository.selectScene(it) } }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(750)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.filesDir, "screenshots/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
