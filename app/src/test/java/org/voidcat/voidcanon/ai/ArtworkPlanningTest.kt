package org.voidcat.voidcanon.ai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import org.voidcat.voidcanon.data.*

class ArtworkPlanningTest {
    private fun scene(state: ContinuityState? = null): WorldSnapshot {
        val base = WorldSnapshot(
            worlds = listOf(WorldRecord("world", "Harbor", "SECRET_WORLD_LORE", null)),
            entries = listOf(EntryRecord("mira", "world", "character", "Mira", "SECRET_DESCRIPTION", "SECRET_LORE", "portrait", "SECRET_AUTHOR"),
                EntryRecord("docks", "world", "location", "Docks", "SECRET_LOCATION", primaryAssetId = "background"),
                EntryRecord("garden", "world", "location", "Garden", "")),
            assets = listOf(AssetRecord("portrait", "mira", "portrait.png", "portrait", "Mira", "", false, "character-hash"),
                AssetRecord("background", "docks", "background.png", "background", "Docks", "", false, "location-hash")),
            scenes = listOf(SceneRecord("scene", "world", "mira", "docks", "background", "portrait", characterKnowledge = "SECRET_KNOWLEDGE", authorNotes = "SECRET_SCENE")),
            descriptions = listOf(VisualDescription("identity", "portrait", "character-hash", "Amber eyes and a teal coat", true, "approved", 0),
                VisualDescription("setting", "background", "location-hash", "Still water and wooden piers", true, "approved", 0)),
            messages = listOf(MessageRecord("reply", "scene", "character", "SECRET_TRANSCRIPT", 0)),
        )
        return if (state == null) base else base.copy(scenes = base.scenes.map { it.copy(continuityJson = Continuity.json.encodeToString(state)) })
    }
    private fun plan(s: WorldSnapshot, previous: ImagePromptPlan? = null, beat: VisualBeat? = null) = ArtworkPlanning.build(s, "", "", previous, beat)

    @Test fun captionsUseVisibleContextWithoutPrivateFieldsAndRemainBounded() {
        val s = scene()
        val p = plan(s, beat = VisualBeat("action", "Mira watches the ships.", "Fog on the water.", "Mira watches the ships. Fog on the water."))
        assertEquals("Mira · Docks — Mira watches the ships. — Fog on the water.", p.caption())
        assertFalse(p.caption().contains("SECRET"))
        assertEquals("Mira · Docks", plan(s).caption())
        assertTrue(p.copy(snapshot = p.snapshot.copy(action = "x".repeat(500))).caption().length <= 240)
    }

    @Test fun compilesOnlyAllowlistedVisualMaterialWithExplicitTemporaryAppearance() {
        val s = scene()
        val state = Continuity.current(s).let { it.copy(actors = it.actors.map { a -> if (a.actorId == "mira") a.copy(appearance = "Red raincoat", knownFacts = "SECRET_FACT") else a }) }
        val p = plan(scene(state))
        assertTrue(p.prompt.contains("Amber eyes"))
        assertTrue(p.prompt.contains("overrides baseline clothing): Red raincoat"))
        assertTrue(p.prompt.contains("Still water"))
        assertFalse(p.prompt.contains("SECRET"))
        assertEquals("portrait", p.snapshot.referenceId)
        assertEquals(32, p.estimatedNeurons)
        assertTrue(p.prompt.length <= 2000)
    }

    @Test fun dialogueKnowledgeElapsedTimeAndMinorPositionDoNotInvalidateArt() {
        val s = scene(); val before = plan(s)
        val state = Continuity.current(s).copy(revision = 12, elapsedSeconds = 900,
            actors = Continuity.current(s).actors.map { it.copy(position = "a little to the left", activity = "talking", knownFacts = "A secret was learned", condition = "hungry") })
        val after = plan(scene(state).copy(messages = listOf(MessageRecord("new", "scene", "user", "New dialogue", 1))), before)
        assertEquals(before.visualKey, after.visualKey)
        assertFalse(after.prompt.contains("hungry"))
    }

    @Test fun appearanceAndHeldObjectsChangeTheVisualFingerprint() {
        val s = scene(); val before = plan(s)
        val changed = Continuity.current(s).let { it.copy(actors = it.actors.map { a -> if (a.actorId == "mira") a.copy(appearance = "wet coat") else a },
            objects = listOf(ObjectState("lantern", "Brass lantern", "docks", "mira", "left"))) }
        val after = plan(scene(changed), before)
        assertNotEquals(before.visualKey, after.visualKey)
        assertTrue(after.prompt.contains("Brass lantern held in left hand"))
    }

    @Test fun offscreenFocusUsesLocationReferenceAndExcludesCharacter() {
        val s = scene()
        val changed = Continuity.current(s).let { it.copy(actors = it.actors.map { a -> if (a.actorId == "mira") a.copy(present = false) else a }) }
        val p = plan(scene(changed))
        assertEquals("background", p.snapshot.referenceId)
        assertTrue(p.prompt.contains("Environment only"))
        assertFalse(p.prompt.contains("Amber eyes"))
    }

    @Test fun losingAHeldObjectDoesNotRetainAnOldActionThatStillHoldsIt() {
        val s = scene()
        val held = Continuity.current(s).copy(objects = listOf(ObjectState("lantern", "Lantern", "docks", "mira", "left")))
        val before = plan(scene(held), beat = VisualBeat("action", "holds a lantern", evidence = "holds a lantern"))
        val after = plan(s, before)
        assertFalse(after.prompt.contains("holds a lantern"))
        assertNotEquals(before.visualKey, after.visualKey)
    }

    @Test fun movingThePlayerUsesTheTrackedLocationAndDropsOldActionAndWeather() {
        val s = scene(); val before = plan(s, beat = VisualBeat("action", "raises a lantern", "fog rolls in", "raises a lantern; fog rolls in"))
        val moved = Continuity.current(s).let { it.copy(actors = it.actors.map { a -> a.copy(locationId = "garden") }) }
        val after = plan(scene(moved), before)
        assertTrue(after.prompt.contains("Location: Garden"))
        assertFalse(after.prompt.contains("fog rolls"))
        assertFalse(after.prompt.contains("raises a lantern"))
        assertNotEquals(before.visualKey, after.visualKey)
    }

    @Test fun exploratoryArtworkIsNotAnIdentityReferenceAndStaleDescriptionsAreNotUsed() {
        val s = scene()
        val exploratory = s.copy(assets = s.assets.map { it.copy(referenceKind = "exploratory") }, descriptions = emptyList())
        assertThrows(IllegalArgumentException::class.java) { plan(exploratory) }
        val textOnly = ArtworkPlanning.build(exploratory, "A traveler with amber eyes", "ink drawing")
        assertNull(textOnly.snapshot.referenceId)
        assertEquals(27, textOnly.estimatedNeurons)
        val stale = s.copy(descriptions = s.descriptions.map { it.copy(sourceHash = "old-hash") })
        assertFalse(plan(stale).prompt.contains("Amber eyes"))
    }

    @Test fun entireOptionalBlocksAreOmittedAndEssentialIdentityNeverSilentlyClipped() {
        val s = scene().let { it.copy(descriptions = it.descriptions.map { d -> if (d.assetId == "background") d.copy(text = "Setting ".repeat(500)) else d }) }
        val p = plan(s)
        assertTrue(p.omissions.contains("Setting"))
        assertTrue(p.prompt.contains("Amber eyes"))
        assertTrue(p.prompt.length <= 2000)
        val hugeIdentity = s.copy(descriptions = s.descriptions.map { it.copy(text = "Identity ".repeat(500)) })
        assertThrows(IllegalArgumentException::class.java) { plan(hugeIdentity) }
    }

    @Test fun optionalVisualHintRequiresExactQuotedSupportAndCannotInvalidateTheStory() {
        val s = scene()
        val reply = ContinuityReply(1, 0, "Mira raises a lantern.", visual = JsonPrimitive("malformed optional value"))
        val raw = Continuity.json.encodeToString(reply)
        assertEquals(reply.narrative, Continuity.resolve(raw, "Continue", s).first)
        assertNull(VisualBeat.parse(raw, "Continue", reply.narrative))
        val good = raw.replace("\"malformed optional value\"", """{"kind":"action","action":"raises a lantern","evidence":"Mira raises a lantern."}""")
        val beat = VisualBeat.parse(good, "Continue", reply.narrative)!!
        assertEquals("raises a lantern", beat.action)
        assertNull(VisualBeat.parse(good.replace("raises a lantern\",", "grows wings\","), "Continue", reply.narrative))
        val before = plan(s); val next = plan(s, before, beat)
        assertNotEquals(before.visualKey, next.visualKey)
        assertEquals(next.visualKey, plan(s, next).visualKey)
    }

    @Test fun carriedVisualDetailsKeepTheirOriginalSourceMessages() {
        val s = scene().copy(messages = listOf(MessageRecord("weather-turn", "scene", "character", "Fog rolls across the pier.", 1)))
        val weather = plan(s, beat = VisualBeat("environment", environment = "Fog rolls across the pier.", evidence = "Fog rolls across the pier."))
        val next = s.copy(messages = s.messages + MessageRecord("action-turn", "scene", "character", "Mira raises a hand.", 2))
        val action = plan(next, weather, VisualBeat("action", action = "Mira raises a hand.", evidence = "Mira raises a hand."))
        val dialogue = next.copy(messages = next.messages + MessageRecord("dialogue-turn", "scene", "character", "We can talk here.", 3))
        val carried = plan(dialogue, action)
        assertEquals(action.visualKey, carried.visualKey)
        assertEquals("weather-turn", carried.environmentSourceId)
        assertEquals("action-turn", carried.actionSourceId)
        assertTrue(carried.sourceIds.containsAll(listOf("weather-turn", "action-turn")))
        assertTrue(carried.visualEvidence.contains("Fog rolls"))
    }
}
