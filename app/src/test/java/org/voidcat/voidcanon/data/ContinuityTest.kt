package org.voidcat.voidcanon.data

import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test
import org.voidcat.voidcanon.ai.SceneContext

class ContinuityTest {
    private fun world(): WorldSnapshot = WorldSnapshot(
        worlds = listOf(WorldRecord("w", "World", "", null)),
        entries = listOf(EntryRecord("mira", "w", "character", "Mira", ""),
            EntryRecord("dock", "w", "location", "Dock", ""),
            EntryRecord("tower", "w", "location", "Tower", "")),
        scenes = listOf(SceneRecord("scene", "w", "mira", "dock", null, null)),
    )
    private fun withState(state: ContinuityState): WorldSnapshot = world().let { s ->
        s.copy(scenes = listOf(s.scene!!.copy(continuityJson = Continuity.json.encodeToString(state))))
    }
    private fun encoded(reply: ContinuityReply) = Continuity.json.encodeToString(reply)

    @Test fun observedMovementAndObjectTransferProduceANewRevision() {
        val s = world()
        val mira = Continuity.current(s).actors.last().copy(position = "beside railing", activity = "watching entrance")
        val narrative = "Mira waits beside the railing, lantern in her right hand. Twenty seconds pass."
        val response = ContinuityReply(1, 0, narrative, 20, "Twenty seconds pass.",
            listOf(ActorChange(mira, "Mira waits beside the railing")),
            listOf(ObjectChange(ObjectState("lantern", "Lantern", "dock", "mira", "right"), "lantern in her right hand")))
        val (story, state) = Continuity.resolve(encoded(response), "I wait.", s)
        assertEquals(narrative, story); assertEquals(1L, state.revision); assertEquals(20L, state.elapsedSeconds)
        assertEquals("mira", state.objects.single().holderId)
        assertEquals("watching entrance", state.actors.last().activity)
    }

    @Test fun rejectsDuplicateHandsUnknownLocationsAndCrossWorldActors() {
        val s = world()
        val base = Continuity.current(s)
        val item = ObjectState("lamp", "Lamp", "dock", "mira", "right")
        assertTrue(runCatching { Continuity.validate(base.copy(objects = listOf(item, item.copy(id = "key"))), s) }.isFailure)
        assertTrue(runCatching { Continuity.validate(base.copy(objects = listOf(item.copy(hand = "both"), item.copy(id = "key", hand = "left"))), s) }.isFailure)
        assertTrue(runCatching { Continuity.validate(base.copy(objects = listOf(item.copy(locationId = "tower"))), s) }.isFailure)
        assertTrue(runCatching { Continuity.validate(base.copy(actors = base.actors + ActorState("outsider", "dock")), s) }.isFailure)
        assertTrue(runCatching { Continuity.validate(base.copy(actors = base.actors.map { it.copy(locationId = "missing") }), s) }.isFailure)
    }

    @Test fun malformedUnsupportedStaleAndUnsourcedRepliesFailClosed() {
        val s = world()
        val valid = ContinuityReply(1, 0, "Mira waits.")
        val invalid = listOf("Mira waits.", "{}",
            """{"protocol":1,"baseRevision":0,"narrative":"Mira waits.","actors":[{"state":{"actorId":"mira","locationId":"dock"},"evidence":"Mira waits."}]}""", encoded(valid.copy(protocol = 2)), encoded(valid.copy(baseRevision = 9)),
            encoded(valid.copy(actors = listOf(ActorChange(Continuity.current(s).actors.last().copy(position = "tower"), "not in the story")))),
            encoded(valid.copy(elapsedSeconds = 5)), encoded(valid.copy(elapsedSeconds = -1)))
        invalid.forEach { assertTrue(it, runCatching { Continuity.resolve(it, "Wait", s) }.isFailure) }
        assertEquals(0L, Continuity.current(s).revision)
    }

    @Test fun emptyChangesPreserveFactsRatherThanReconstructingFromMemory() {
        val base = Continuity.current(world()).copy(revision = 7, timeAnchor = "midnight", elapsedSeconds = 10,
            objects = listOf(ObjectState("key", "Key", "dock", "mira", "left")))
        val after = Continuity.resolve(encoded(ContinuityReply(1, 7, "Mira speaks.")), "Hello", withState(base)).second
        assertEquals(base.copy(revision = 8), after)
    }

    @Test fun offscreenFactsAreOmittedAndCannotBeMutatedByTheModel() {
        val s = world().let { it.copy(entries = it.entries + EntryRecord("npc", "w", "character", "NPC", "")) }
        val base = Continuity.current(s).copy(actors = Continuity.current(s).actors + ActorState("npc", "tower", false, activity = "OFFSCREEN-SECRET"))
        val scoped = s.copy(scenes = listOf(s.scene!!.copy(continuityJson = Continuity.json.encodeToString(base))))
        val projection = Continuity.project(scoped)
        assertFalse(projection.text.contains("OFFSCREEN-SECRET")); assertTrue(projection.omissions.isNotEmpty())
        val reply = ContinuityReply(1, 0, "NPC arrives.", actors = listOf(ActorChange(base.actors.last().copy(present = true), "NPC arrives.")))
        assertTrue(runCatching { Continuity.resolve(encoded(reply), "Wait", scoped) }.isFailure)
    }

    @Test fun stateContextIsBoundedAndNeverIncludesRevisionHistoryOrPrivateNotes() {
        val s = world()
        val base = Continuity.current(s).copy(actors = Continuity.current(s).actors.map {
            it.copy(position = "p".repeat(240), activity = "a".repeat(240), condition = "c".repeat(240), appearance = "d".repeat(240), knownFacts = "k".repeat(240))
        })
        val projected = withState(base).copy(continuityEvents = listOf(ContinuityEvent("event", "scene", null, "OLD-STATE-SECRET", "", 1)))
        val context = SceneContext.build(projected, "Go", 9000)
        assertTrue(Continuity.project(projected).text.length <= Continuity.CONTEXT_LIMIT)
        assertTrue(context.messages.sumOf { it.content.length } <= 9000)
        assertFalse(context.messages.toString().contains("OLD-STATE-SECRET"))
        assertTrue(runCatching { SceneContext.build(projected, "Go", 1000) }.isFailure)
    }
}
