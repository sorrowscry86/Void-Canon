package org.voidcat.voidcanon.ai

import org.junit.Assert.*
import org.junit.Test
import org.voidcat.voidcanon.data.*

class NarrativePlayTest {
    private fun snapshot(settings: NarrativeSettings = NarrativeSettings()) = WorldSnapshot(
        worlds = listOf(WorldRecord("w", "World", "Public setting", null)),
        entries = listOf(EntryRecord("c", "w", "character", "Mira", "Traveler", authorNotes = "PRIVATE CHARACTER SECRET"),
            EntryRecord("l", "w", "location", "Docks", "Harbor")),
        scenes = listOf(SceneRecord("s", "w", "c", "l", null, null, authorNotes = "PRIVATE SCENE SECRET", narrative = settings)),
    )

    @Test fun allFreeformPreferencesEnterTheAuditedBoundedContextWithoutSecrets() {
        for (preset in NarrativePlay.presets) for (pacing in NarrativePlay.pacings) for (length in NarrativePlay.lengths) {
            val settings = NarrativeSettings("freeform", preset.id, pacing.id, length.id)
            val context = SceneContext.build(snapshot(settings), "I look around.")
            val system = context.messages.first().content
            assertTrue(system.contains(preset.description))
            assertTrue(system.contains(pacing.description))
            assertTrue(system.contains(length.description))
            assertTrue(system.contains("Never invent the user's choices"))
            assertTrue(system.contains("continuity JSON"))
            assertFalse(system.contains("PRIVATE"))
            assertEquals(settings.mode, context.settings["playMode"])
            assertEquals(settings.preset, context.settings["narrativePreset"])
            assertTrue(context.messages.sumOf { it.content.length } <= context.budgetCharacters)
        }
    }

    @Test fun classicDoesNotApplyDormantFreeformPreferences() {
        val context = SceneContext.build(snapshot(NarrativeSettings(preset = "exploration", length = "rich")), "Hello")
        assertFalse(context.messages.first().content.contains("MLRPE"))
        assertFalse(context.messages.first().content.contains("220–350"))
        assertEquals("classic", context.settings["playMode"])
    }

    @Test fun narrativeSettingsChangePendingRequestFingerprintButNotHistory() {
        val original = snapshot()
        val changed = original.copy(scenes = original.scenes.map { it.copy(narrative = NarrativeSettings("freeform")) })
        assertNotEquals(SceneContext.fingerprint(original), SceneContext.fingerprint(changed))
        assertEquals(original.messages, changed.messages)
    }

    @Test fun coreNarrativeInstructionsAreReservedInsteadOfSilentlyTruncated() {
        val s = snapshot(NarrativeSettings("freeform"))
        assertTrue(runCatching { SceneContext.build(s, "I wait.", budget = 1500) }.isFailure)
    }

    @Test fun unsupportedRulesModesAndPreferencesAreRejected() {
        listOf(NarrativeSettings(mode = "unsupported-rules"), NarrativeSettings(preset = "autonomous"),
            NarrativeSettings(pacing = "skip"), NarrativeSettings(length = "unlimited")).forEach {
            assertTrue(runCatching { SceneContext.build(snapshot(it), "Hello") }.isFailure)
        }
    }
}
