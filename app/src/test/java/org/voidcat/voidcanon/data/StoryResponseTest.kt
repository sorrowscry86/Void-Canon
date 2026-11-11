package org.voidcat.voidcanon.data

import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class StoryResponseTest {
    private fun world() = WorldSnapshot(
        worlds = listOf(WorldRecord("w", "World", "", null)),
        entries = listOf(EntryRecord("mira", "w", "character", "Mira", ""), EntryRecord("dock", "w", "location", "Dock", "")),
        scenes = listOf(SceneRecord("scene", "w", "mira", "dock", null, null)))

    @Test fun plainStoryDoesNotNeedAStateEnvelope() {
        val reply = StoryResponse.resolve("Mira smiles. \"Come along,\" she says.", "Hello", world())
        assertEquals("Mira smiles. \"Come along,\" she says.", reply.narrative)
        assertNull(reply.state); assertEquals(StoryResponse.WARNING, reply.warning)
        assertEquals("Yes.", StoryResponse.resolve("Yes.", "Will you come?", world()).narrative)
        assertEquals("[OOC: a pause] Mira waits.", StoryResponse.resolve("[OOC: a pause] Mira waits.", "Wait", world()).narrative)
    }

    @Test fun fencedValidProtocolRetainsFullValidation() {
        val raw = Continuity.json.encodeToString(ContinuityReply(1, 0, "Mira waits."))
        for (prefix in listOf("```JSON\r\n", "```\n", "```json\n")) {
            val reply = StoryResponse.resolve(prefix + raw + "\n```", "Wait", world())
            assertEquals("Mira waits.", reply.narrative); assertEquals(1L, reply.state!!.revision); assertNull(reply.warning)
        }
    }

    @Test fun commonWrappersProvideStoryWithoutApplyingTheirState() {
        for (key in listOf("narrative", "story", "text", "content", "response")) {
            val reply = StoryResponse.resolve("""{"$key":"Mira waits.","actors":"wrong shape","elapsedSeconds":100}""", "Wait", world())
            assertEquals("Mira waits.", reply.narrative); assertNull(reply.state)
        }
        assertEquals("Mira waits.", StoryResponse.resolve("""{"message":{"content":"Mira waits."}}""", "Wait", world()).narrative)
    }

    @Test fun staleOrUnsupportedMetadataNeverPreventsUsableProseOrChangesState() {
        for (reply in listOf(ContinuityReply(1, 99, "Mira waits."), ContinuityReply(99, 0, "Mira waits."),
            ContinuityReply(1, 0, "Mira waits.", elapsedSeconds = 100))) {
            val result = StoryResponse.resolve(Continuity.json.encodeToString(reply), "Wait", world())
            assertEquals("Mira waits.", result.narrative); assertNull(result.state); assertNotNull(result.warning)
        }
    }

    @Test fun missingTruncatedOrOversizedStoryStillFailsWithoutPublishingProtocolText() {
        for (raw in listOf("", "{}", "{\"actors\":[]}", "{\"narrative\":\"unfinished", "[{}", "[]", "null", "42", "```json\n{}", "x".repeat(12001))) {
            assertTrue(raw.take(80), runCatching { StoryResponse.resolve(raw, "Wait", world()) }.isFailure)
        }
    }

    @Test fun reasoningIsNeverUsedAsStory() {
        assertEquals("Mira waits.", StoryResponse.resolve("<think>Private reasoning</think>\nMira waits.", "Wait", world()).narrative)
        assertTrue(runCatching { StoryResponse.resolve("<think>Unfinished reasoning", "Wait", world()) }.isFailure)
        assertTrue(runCatching { StoryResponse.resolve("<think>Only reasoning</think>", "Wait", world()) }.isFailure)
        assertTrue(runCatching { StoryResponse.resolve(Continuity.json.encodeToString(ContinuityReply(1, 0, "<THINK>Reasoning</THINK>")), "Wait", world()) }.isFailure)
    }
}
