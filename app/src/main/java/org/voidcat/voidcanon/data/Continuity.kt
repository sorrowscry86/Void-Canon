package org.voidcat.voidcanon.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

/** Scene-local observations, never world canon or a replacement for source messages. */
@Serializable
data class ContinuityState(
    val revision: Long = 0,
    val timeAnchor: String = "",
    val elapsedSeconds: Long = 0,
    val actors: List<ActorState> = emptyList(),
    val objects: List<ObjectState> = emptyList(),
)

@Serializable
data class ActorState(
    val actorId: String,
    val locationId: String,
    val present: Boolean = true,
    val position: String = "",
    val activity: String = "",
    val condition: String = "",
    val appearance: String = "",
    val knownFacts: String = "",
)

@Serializable
data class ObjectState(
    val id: String,
    val name: String,
    val locationId: String,
    val holderId: String = "",
    val hand: String = "",
    val position: String = "",
)

@Serializable
data class ActorChange(val state: ActorState, val evidence: String)
@Serializable
data class ObjectChange(val state: ObjectState, val evidence: String)

@Serializable
data class ContinuityReply(
    val protocol: Int,
    val baseRevision: Long,
    val narrative: String,
    val elapsedSeconds: Int = 0,
    val timeEvidence: String = "",
    val actors: List<ActorChange> = emptyList(),
    val objects: List<ObjectChange> = emptyList(),
    val visual: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
@androidx.room.Entity(tableName = "continuity_events")
data class ContinuityEvent(
    @androidx.room.PrimaryKey val id: String,
    val sceneId: String,
    val sourceMessageId: String?,
    val beforeJson: String,
    val afterJson: String,
    val createdAt: Long,
)

data class ContinuityProjection(val text: String, val actorIds: Set<String>, val objectIds: Set<String>, val omissions: List<String>)

object Continuity {
    const val PLAYER = "@player"
    const val CONTEXT_LIMIT = 4000
    val json = Json { encodeDefaults = true }

    fun current(s: WorldSnapshot): ContinuityState {
        val scene = requireNotNull(s.scene)
        return if (scene.continuityJson.isBlank()) ContinuityState(actors = listOf(
            ActorState(PLAYER, scene.locationId), ActorState(scene.characterId, scene.locationId),
        )) else decode(scene.continuityJson)
    }

    fun decode(text: String): ContinuityState {
        require(text.length <= 24000) { "Scene state is too large." }
        return json.decodeFromString<ContinuityState>(text)
    }

    fun validate(state: ContinuityState, s: WorldSnapshot, scene: SceneRecord = requireNotNull(s.scene)) {
        require(state.revision >= 0 && state.elapsedSeconds in 0..315360000L && state.timeAnchor.length <= 120) { "Invalid scene time or revision." }
        require(state.actors.size in 2..8 && state.objects.size <= 16) { "Track 2–8 participants and at most 16 objects per scene." }
        val actors = state.actors.associateBy { it.actorId }
        require(actors.size == state.actors.size && PLAYER in actors && scene.characterId in actors) { "Scene state needs unique participants, including you and the scene character." }
        fun location(id: String) = s.entries.any { it.id == id && it.kind == "location" && it.worldId == scene.worldId }
        state.actors.forEach { actor ->
            require(actor.actorId == PLAYER || s.entries.any { it.id == actor.actorId && it.kind == "character" && it.worldId == scene.worldId }) { "A participant belongs to another world or is unavailable." }
            require(location(actor.locationId)) { "Choose a location from this world." }
            require(listOf(actor.position, actor.activity, actor.condition, actor.appearance, actor.knownFacts).all { it.length <= 240 }) { "Keep each state detail within 240 characters." }
        }
        require(state.objects.map { it.id }.distinct().size == state.objects.size) { "Duplicate scene objects." }
        val hands = mutableSetOf<Pair<String, String>>()
        state.objects.forEach { item ->
            require(item.id.matches(Regex("[a-zA-Z0-9_-]{1,100}")) && item.name.isNotBlank() && item.name.length <= 80 && item.position.length <= 240) { "Invalid scene object." }
            require(location(item.locationId) && item.hand in setOf("", "left", "right", "both")) { "Invalid object location or hand." }
            if (item.holderId.isBlank()) require(item.hand.isBlank()) { "An unheld object cannot occupy a hand." }
            else {
                val holder = requireNotNull(actors[item.holderId]) { "The object holder is not a scene participant." }
                require(holder.locationId == item.locationId) { "Held objects must be at their holder's location." }
                val occupied = if (item.hand == "both") listOf("left", "right") else listOf(item.hand).filter { it.isNotBlank() }
                occupied.forEach { require(hands.add(item.holderId to it)) { "Two objects occupy the same hand. Correct the scene state." } }
            }
        }
        require(json.encodeToString(state).length <= 24000) { "Scene state is too large." }
    }

    /** Whole records only; omit offscreen state rather than slice facts into misleading fragments. */
    fun project(s: WorldSnapshot, state: ContinuityState = current(s)): ContinuityProjection {
        val scene = requireNotNull(s.scene)
        val names = state.actors.associate { it.actorId to if (it.actorId == PLAYER) scene.userRole else s.entry(it.actorId)?.name.orEmpty() }
        val lines = mutableListOf("CURRENT STATE revision=${state.revision}; time=${state.timeAnchor.ifBlank { "unknown" }}; elapsed=${state.elapsedSeconds}s since tracking began. Unknown fields are not established facts.")
        val actors = mutableSetOf<String>()
        val objects = mutableSetOf<String>()
        val omissions = mutableListOf<String>()
        fun include(line: String): Boolean {
            if (lines.sumOf { it.length + 1 } + line.length > CONTEXT_LIMIT) return false
            lines += line; return true
        }
        state.actors.sortedBy { if (it.actorId == PLAYER || it.actorId == scene.characterId) 0 else 1 }.forEach { actor ->
            if ((actor.actorId == PLAYER || actor.actorId == scene.characterId || actor.present) && include("ACTOR ${names[actor.actorId]} " + json.encodeToString(actor))) actors += actor.actorId
            else omissions += "State participant omitted (offscreen or budget): ${actor.actorId}"
        }
        state.objects.sortedBy { if (it.holderId in actors) 0 else 1 }.forEach { item ->
            val relevant = item.holderId in actors || (item.holderId.isBlank() && item.locationId == scene.locationId)
            if (relevant && include("OBJECT " + json.encodeToString(item))) objects += item.id
            else omissions += "State object omitted (offscreen or budget): ${item.id}"
        }
        require(PLAYER in actors && scene.characterId in actors) { "Core scene state exceeds its context allowance. Shorten state details." }
        return ContinuityProjection(lines.joinToString("\n"), actors, objects, omissions)
    }

    fun instruction() = """
        Reply with ONLY a JSON object, no markdown fences, using this protocol:
        {"protocol":1,"baseRevision":0,"narrative":"Your story response","elapsedSeconds":0,"timeEvidence":"","actors":[],"objects":[]}
        Copy baseRevision from CURRENT STATE. Each actors change is {"state":{"actorId":"existing id","locationId":"existing location id","present":true,"position":"","activity":"","condition":"","appearance":"","knownFacts":""},"evidence":"exact quote"}.
        Each objects change is {"state":{"id":"stable-id","name":"name","locationId":"existing location id","holderId":"actor id or empty","hand":"left/right/both/or empty","position":""},"evidence":"exact quote"}.
        Return only changed records, preserving their unchanged fields. Evidence must quote the current user action or your completed narrative verbatim. Track only established observable changes; do not infer motives, feelings, secret knowledge, or success of failed actions. Never invent the player's decisions. Known facts require explicit learning. Do not update omitted actors/objects; new objects may be introduced with new stable IDs. Held objects move with holders and cannot share hands. Use existing location IDs. Keep each actor detail within 240 characters. elapsedSeconds is 0 unless elapsed time is explicit in the text (quote it in timeEvidence); maximum 86400 per turn. Empty changes are valid. The narrative is the only player-facing story text. CURRENT STATE outranks older transcript for current physical facts; flag conflicts rather than silently teleporting characters. These records are provisional scene observations, never world canon.
    """.trimIndent()

    fun resolve(raw: String, input: String, s: WorldSnapshot): Pair<String, ContinuityState> {
        val payload = raw.trim().let { if (it.startsWith("```json\n") && it.endsWith("\n```")) it.removePrefix("```json\n").removeSuffix("\n```") else it }
        val reply = try { json.decodeFromString<ContinuityReply>(payload) } catch (e: Exception) {
            throw IllegalArgumentException("The model did not return a valid story/state response. No turn or state was applied; try another model.", e)
        }
        // Model omissions must not silently clear an existing injury, possession, or learned fact.
        val root = Json.parseToJsonElement(payload).jsonObject
        val actorFields = setOf("actorId", "locationId", "present", "position", "activity", "condition", "appearance", "knownFacts")
        val objectFields = setOf("id", "name", "locationId", "holderId", "hand", "position")
        root["actors"]?.jsonArray?.forEach { require(it.jsonObject.getValue("state").jsonObject.keys.containsAll(actorFields)) { "The model omitted participant fields. No state or turn was applied." } }
        root["objects"]?.jsonArray?.forEach { require(it.jsonObject.getValue("state").jsonObject.keys.containsAll(objectFields)) { "The model omitted object fields. No state or turn was applied." } }
        val before = current(s)
        require(reply.protocol == 1 && reply.baseRevision == before.revision) { "The model used an outdated state revision." }
        require(reply.narrative.isNotBlank() && reply.narrative.length <= 12000) { "The model returned no usable story." }
        require(reply.elapsedSeconds in 0..86400 && reply.actors.size <= 8 && reply.objects.size <= 16) { "Invalid state change size or elapsed time." }
        fun evidence(quote: String) { require(quote.isNotBlank() && quote.length <= 500 && (input.contains(quote) || reply.narrative.contains(quote))) { "A state change has no matching source passage." } }
        if (reply.elapsedSeconds > 0) evidence(reply.timeEvidence)
        val projection = project(s, before)
        require(reply.actors.map { it.state.actorId }.distinct().size == reply.actors.size && reply.objects.map { it.state.id }.distinct().size == reply.objects.size) { "Duplicate state updates." }
        reply.actors.forEach { evidence(it.evidence); require(it.state.actorId in projection.actorIds) { "The model changed an unprovided participant." } }
        reply.objects.forEach { change ->
            evidence(change.evidence)
            require(change.state.id in projection.objectIds || before.objects.none { it.id == change.state.id }) { "The model changed an omitted object." }
            require(change.state.holderId.isBlank() || change.state.holderId in projection.actorIds) { "The model used an unprovided holder." }
        }
        val actors = before.actors.associateBy { it.actorId }.toMutableMap()
        reply.actors.forEach { actors[it.state.actorId] = it.state }
        val objects = before.objects.associateBy { it.id }.toMutableMap()
        reply.objects.forEach { objects[it.state.id] = it.state }
        val after = before.copy(revision = before.revision + 1, elapsedSeconds = before.elapsedSeconds + reply.elapsedSeconds,
            actors = actors.values.toList(), objects = objects.values.toList())
        validate(after, s)
        project(s, after) // Fail before publication if core facts cannot fit next turn.
        return reply.narrative to after
    }
}
