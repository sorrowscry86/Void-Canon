package org.voidcat.voidcanon

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.IdlingPolicies
import org.junit.Before
import org.junit.Assert.*
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import org.voidcat.voidcanon.ai.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.runBlocking

/** End-to-end smoke test against Android, with full-screen evidence including the IME. */
class StageOneUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun allowSoftwareEmulatorStartup() {
        IdlingPolicies.setMasterPolicyTimeout(120, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(120, TimeUnit.SECONDS)
    }

    @Test fun worldCharacterGalleryPlayAndKeyboard() {
        try { verifyFlow() }
        catch (error: Throwable) {
            runCatching { capture("99-failure") }
            throw AssertionError("${error.message}\n${runCatching { compose.onRoot().printToString() }.getOrDefault("No semantics tree available")}", error)
        }
    }

    private fun verifyFlow() {
        compose.waitUntil(120000) { compose.onAllNodesWithText("The Glass Harbor").fetchSemanticsNodes().isNotEmpty() }
        // Decode the real bundled assets before visual evidence; Compose idleness
        // alone does not include Coil's background image requests.
        val artContext = compose.activity
        val repo = (artContext.application as VoidCanonApplication).repository
        runBlocking {
            repo.snapshot().assets.forEach { asset ->
                val result = artContext.imageLoader.execute(ImageRequest.Builder(artContext)
                    .data(File(repo.images, asset.fileName)).size(Size.ORIGINAL).build())
                check(result is SuccessResult) { "Demo artwork must decode on Android: ${asset.label}" }
            }
        }
        capture("01-world")
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Mira"))
        compose.onNodeWithText("Mira").performClick()
        compose.onNodeWithText("Appearance & identity").assertExists()
        capture("02-character")
        compose.onNodeWithContentDescription("Edit character").performClick()
        compose.onNode(hasSetTextAction() and hasText("Mira")).performTextReplacement("Mira the Traveler")
        compose.onNodeWithText("Save changes").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Mira the Traveler").fetchSemanticsNodes().isNotEmpty() }
        // Dismiss transient feedback explicitly: test clocks/accessibility can prolong its timeout.
        val dismissible = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        compose.waitUntil(15000) { compose.onAllNodes(dismissible).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(dismissible).performSemanticsAction(SemanticsActions.Dismiss) { it() }
        compose.onNodeWithText("Images", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Open gallery").performScrollTo().performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Import image"))
        compose.onNodeWithText("Import image").assertExists()
        capture("03-gallery")
        compose.onNodeWithText("Play", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Scripted demo · offline").assertExists()
        compose.onNodeWithText("The harbor is quiet. Someone got here before us.").assertExists()
        capture("04-play")
        compose.onNode(hasSetTextAction()).performClick().performTextInput("Let's follow the lanterns and find the ship.")
        compose.waitForIdle()
        Thread.sleep(1500) // Allow Android's IME animation to finish before capturing the whole display.
        capture("05-play-keyboard")
        compose.onNodeWithContentDescription("Send demo message").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Then let's take the lantern road. If there is a trail, it starts where the ships came in.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Scene history").performClick()
        compose.onNodeWithText("Let's follow the lanterns and find the ship.").assertExists()
        capture("06-history")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue("Stage 3 needs explicit network capability", permissions.contains("android.permission.INTERNET"))
        val app = compose.activity.application as VoidCanonApplication
        assertFalse(app.aiSettings.configuration().consent)
        assertFalse(app.aiSettings.hasKey())
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Gallery", useUnmergedTree = true).performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Describe / review image"))
        compose.onNodeWithText("Describe / review image").performClick()
        compose.onNodeWithText("Visible appearance or setting").performTextInput("She wears a teal travel coat and has amber eyes.")
        compose.onNodeWithText("Approve", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { runBlocking { repo.snapshot().descriptions.any { it.approved } } }
        dismissFeedback()
        capture("07-approved-description")
        compose.onNodeWithText("Chapters", useUnmergedTree = true).performClick()
        // A failed preflight remains readable after transient feedback disappears.
        compose.onNodeWithText("Propose summary with OpenRouter").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Retry summary with OpenRouter").fetchSemanticsNodes().isNotEmpty() }
        dismissFeedback()
        compose.onNodeWithText("Summary wasn't saved. Your transcript is unchanged.").assertExists()
        compose.onNodeWithText("Write chapter summary").performClick()
        compose.onNodeWithText("Chapter title").performTextReplacement("Harbor chapter")
        compose.onNodeWithText("Editable memory summary").performTextInput("We followed the lanterns to investigate the arriving ship.")
        compose.onNodeWithText("Approve chapter").performClick()
        compose.waitUntil(15000) { runBlocking { repo.snapshot().chapters.any { it.approved } } }
        dismissFeedback()
        // Repository publication can precede the observed UI update on a software emulator.
        compose.waitUntil(15000) { compose.onAllNodesWithText("APPROVED MEMORY", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("APPROVED MEMORY", useUnmergedTree = true).assertExists()
        capture("08-approved-chapter")
        compose.onNodeWithText("Play", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Scene context").performClick()
        compose.onNodeWithText("Scene context").assertExists()
        capture("09-context")
        Espresso.pressBack()
        compose.waitForIdle()
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        compose.runOnIdle {
            vm.models.value = listOf(
                RouterModel("fixture/text:free", "[TEST] Free text", 8192, false, true, "0", "0"),
                RouterModel("fixture/vision:free", "[TEST] Free vision", 8192, true, true, "0", "0"),
                RouterModel("fixture/paid", "[TEST] Paid model", 8192, true, false, "0.01", "0.02"))
        }
        compose.onNodeWithContentDescription("AI settings").performClick()
        compose.onNodeWithText("Free only").assertExists()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("[TEST] Free text"))
        compose.onNodeWithText("[TEST] Paid model").assertDoesNotExist()
        capture("10-free-model-picker-fixture")
        Espresso.pressBack()
        // Exercise Android Keystore using a conspicuous non-secret test value, never a live key.
        val marker = "fixture-non-secret-openrouter-key"
        runBlocking {
            app.aiSettings.save(AiConfiguration(), marker)
            try {
                assertEquals(marker, app.aiSettings.readKey())
                val privatePrefs = context.getSharedPreferences("openrouter-private", android.content.Context.MODE_PRIVATE)
                assertFalse(privatePrefs.all.values.any { it.toString().contains(marker) })
                val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
                ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
                    var manifest = ""
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name == "manifest.json") manifest = zip.readBytes().toString(Charsets.UTF_8)
                        zip.closeEntry()
                    }
                    assertFalse(manifest.contains(marker))
                    assertFalse(manifest.contains("fixture/text:free"))
                }
            } finally { app.aiSettings.clearKey() }
        }
        verifyWorldLibrary()
    }

    private fun verifyWorldLibrary() {
        val app = compose.activity.application as VoidCanonApplication
        val repo = app.repository
        val vm = ViewModelProvider(compose.activity)[CanonViewModel::class.java]
        val sampleMessages = runBlocking { repo.snapshot().messages.map { it.id } }
        compose.onNodeWithText("World", useUnmergedTree = true).performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(0)
        compose.onNodeWithText("Worlds", useUnmergedTree = true).performClick()
        compose.onNodeWithText("New world").performClick()
        compose.onNodeWithText("World name").performTextInput("Moon Garden")
        compose.onNodeWithText("World description").performTextInput("A moonlit sanctuary of silver trees.")
        compose.onNode(hasText("Create world") and hasClickAction()).performClick()
        compose.waitUntil(15000) { vm.snapshot.value.world?.name == "Moon Garden" && !vm.busy.value }
        dismissFeedback()
        assertNull(runBlocking { repo.snapshot().scene })
        capture("11-own-blank-world")
        compose.onNodeWithText("Gallery", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Your visual library starts here").assertExists()
        compose.onNodeWithText("Play", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Your scene starts here").assertExists()
        compose.onNodeWithText("World", useUnmergedTree = true).performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Add character"))
        compose.onNodeWithText("Add character").performClick()
        compose.onNodeWithText("Name").performTextInput("Kuro")
        compose.onNodeWithText("Description").performTextInput("Red eyes, dark curls, and a violet dress.")
        compose.onNodeWithText("Author-only notes / secrets").performScrollTo().performTextInput("PRIVATE MOON SECRET")
        compose.onNode(hasText("Create character") and hasClickAction()).performClick()
        compose.waitUntil(15000) { vm.snapshot.value.entries.size == 1 && !vm.busy.value }
        dismissFeedback()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Add location"))
        compose.onNodeWithText("Add location").performClick()
        compose.onNodeWithText("Name").performTextInput("Lunar Atrium")
        compose.onNodeWithText("Description").performTextInput("Silver branches beneath an open sky.")
        compose.onNode(hasText("Create location") and hasClickAction()).performClick()
        compose.waitUntil(15000) { vm.snapshot.value.entries.size == 2 && !vm.busy.value }
        dismissFeedback()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToNode(hasText("Kuro"))
        capture("12-own-people-and-places")
        compose.onNodeWithText("Kuro").performClick()
        compose.onNodeWithText("Use in scene").performScrollTo().performClick()
        compose.onNodeWithText("Choose scene").assertExists()
        compose.onNodeWithText("Open scene").performClick()
        compose.waitUntil(15000) { vm.snapshot.value.scene != null && !vm.busy.value }
        dismissFeedback()
        compose.onNodeWithText("OpenRouter · setup needed").assertExists()
        compose.onNodeWithText("Demo", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Your scene is waiting.").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("I step into the lunar garden.")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        capture("13-own-scene")
        val ownWorld = vm.snapshot.value.world!!.id
        val ownScene = vm.snapshot.value.scene!!.id
        compose.onNodeWithText("World", useUnmergedTree = true).performClick()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(0)
        compose.onNodeWithText("Worlds", useUnmergedTree = true).performClick()
        capture("14-world-library")
        compose.onNodeWithText("The Glass Harbor", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { vm.snapshot.value.world?.id == "glass-harbor" && !vm.busy.value }
        dismissFeedback()
        assertEquals(sampleMessages, vm.snapshot.value.messages.map { it.id })
        compose.onNodeWithText("Worlds", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Moon Garden", useUnmergedTree = true).performClick()
        compose.waitUntil(15000) { vm.snapshot.value.world?.id == ownWorld && !vm.busy.value }
        dismissFeedback()
        assertEquals(ownScene, vm.snapshot.value.scene!!.id)
        assertEquals("I step into the lunar garden.", vm.draft.value)
        assertTrue(vm.snapshot.value.messages.isEmpty())
        val prompt = vm.contextPreview().messages.joinToString { it.content }
        assertTrue(prompt.contains("silver trees"))
        assertFalse(prompt.contains("PRIVATE MOON SECRET"))
        assertFalse(prompt.contains("followed the lanterns"))
        // Preserve the sample as the next smoke-test entry point.
        runBlocking { repo.selectWorld("glass-harbor") }
    }

    private fun dismissFeedback() {
        val dismissible = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        compose.waitUntil(15000) { compose.onAllNodes(dismissible).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(dismissible).performSemanticsAction(SemanticsActions.Dismiss) { it() }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(1000) // Let decoded images and the native display reach the captured frame.
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        requireNotNull(bitmap) { "Android did not provide a screenshot." }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
