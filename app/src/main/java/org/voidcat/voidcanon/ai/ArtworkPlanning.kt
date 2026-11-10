package org.voidcat.voidcanon.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.voidcat.voidcanon.data.*

/** Optional observations are isolated from the story/state protocol. No free-form director instructions. */
@Serializable
data class VisualBeat(val kind: String, val action: String = "", val environment: String = "", val evidence: String = "") {
    companion object {
        fun parse(raw: String, input: String, narrative: String): VisualBeat? = runCatching {
            val payload = raw.trim().removePrefix("```json\n").removeSuffix("\n```")
            val value = Json.parseToJsonElement(payload).jsonObject["visual"] ?: return null
            val beat = Json.decodeFromJsonElement<VisualBeat>(value)
            require(beat.kind in setOf("action", "environment"))
            require(beat.evidence.length in 1..500 && (input.contains(beat.evidence) || narrative.contains(beat.evidence)))
            require(beat.action.length <= 240 && beat.environment.length <= 300)
            require(beat.action.isNotBlank() || beat.environment.isNotBlank())
            listOf(beat.action, beat.environment).filter { it.isNotBlank() }.forEach { require(beat.evidence.contains(it)) }
            beat
        }.getOrNull()

        fun instruction() = """
            Optional artwork observation: add a top-level "visual" value, null for ordinary dialogue or minor gestures.
            For a major visible action or a changed visible environment, use {"kind":"action" or "environment","action":"","environment":"","evidence":"exact quote"}.
            action (max 240 characters) and environment (max 300) must each be exact excerpts of evidence; evidence (max 500) must quote this user input or your completed narrative.
            Describe only what a camera could see at the player's current location. No thoughts, knowledge, motives, speculation, hypothetical/failed actions, new identity traits, art instructions, or offscreen events. Minor talking, looking and nodding do not warrant artwork. Never invent an event for an image. This optional field does not change the story/state rules.
        """.trimIndent()
    }
}

@Serializable
data class VisualSnapshot(
    val worldId: String, val sceneId: String, val ownerId: String, val ownerName: String,
    val locationId: String, val locationName: String, val identity: String, val appearance: String,
    val position: String, val objects: List<String>, val setting: String, val action: String,
    val environment: String, val referenceId: String?, val referenceHash: String?, val style: String,
)

@Serializable
data class ImagePromptPlan(
    val snapshot: VisualSnapshot, val prompt: String, val visualKey: String, val reason: String,
    val sourceMessageId: String?, val sourceIds: List<String>, val omissions: List<String>,
    val actionSourceId: String? = null, val environmentSourceId: String? = null,
    val actionEvidence: String = "", val environmentEvidence: String = "",
) {
    /** A local scene-context caption, never a claim to have interpreted the generated pixels. */
    fun caption() = listOf(if (snapshot.ownerId == snapshot.locationId) snapshot.locationName else "${snapshot.ownerName} · ${snapshot.locationName}",
        snapshot.action.ifBlank { snapshot.appearance }, snapshot.environment)
        .filter { it.isNotBlank() }.joinToString(" — ").replace(Regex("\\s+"), " ").take(240).trim()
    val visualEvidence get() = listOf(actionEvidence, environmentEvidence).filter { it.isNotBlank() }.distinct().joinToString("\n")
    val estimatedNeurons get() = if (snapshot.referenceId == null) 27 else 32
}

/** The image builder cannot access notes, knowledge, lore, chapter memory or the transcript. */
object ArtworkPlanning {
    private val json = Json { encodeDefaults = true }
    fun build(s: WorldSnapshot, brief: String, style: String, previous: ImagePromptPlan? = null, beat: VisualBeat? = null): ImagePromptPlan {
        val scene = requireNotNull(s.scene) { "Choose a scene first." }
        val state = Continuity.current(s)
        val player = state.actors.first { it.actorId == Continuity.PLAYER }
        val focus = state.actors.firstOrNull { it.actorId == scene.characterId && it.present && it.locationId == player.locationId }
        val location = requireNotNull(s.entry(player.locationId))
        val owner = if (focus != null) requireNotNull(s.entry(scene.characterId)) else location
        val selected = if (focus != null) scene.cutoutAssetId else scene.backgroundAssetId
        val reference = (s.asset(selected)?.takeIf { it.entityId == owner.id } ?: s.asset(owner.primaryAssetId))
            ?.takeIf { it.entityId == owner.id && it.referenceKind != "exploratory" && it.contentHash.isNotBlank() }
        fun description(asset: AssetRecord?) = s.descriptions.firstOrNull {
            it.assetId == asset?.id && it.approved && it.sourceHash == asset?.contentHash
        }
        val identity = if (focus != null) brief.trim().ifBlank { description(reference)?.text.orEmpty() } else ""
        require(focus == null || reference != null || identity.isNotBlank()) { "Choose a character reference or write a visual brief before generating artwork." }
        require(brief.length <= 800 && style.length <= 300) { "Keep the visual brief within 800 characters and style within 300." }
        val background = (s.asset(scene.backgroundAssetId)?.takeIf { it.entityId == location.id } ?: s.asset(location.primaryAssetId))
            ?.takeIf { it.referenceKind != "exploratory" }
        val settingDescription = description(background)
        val samePlace = previous?.snapshot?.locationId == location.id && previous.snapshot.ownerId == owner.id
        val environment = beat?.environment?.takeIf { it.isNotBlank() } ?: previous?.snapshot?.environment?.takeIf { samePlace }.orEmpty()
        val objects = state.objects.filter { it.locationId == player.locationId && (it.holderId == focus?.actorId || it.holderId.isBlank()) }
            .sortedBy { it.id }.map { item ->
                item.name + if (item.holderId == focus?.actorId) " held${item.hand.takeIf { it.isNotBlank() }?.let { " in $it hand(s)" }.orEmpty()}" else item.position.takeIf { it.isNotBlank() }?.let { "; $it" }.orEmpty()
            }
        val retainAction = samePlace && previous?.snapshot?.objects == objects && previous.snapshot.appearance == focus?.appearance.orEmpty()
        val action = beat?.action?.takeIf { it.isNotBlank() } ?: previous?.snapshot?.action?.takeIf { retainAction }.orEmpty()
        val visual = VisualSnapshot(scene.worldId, scene.id, owner.id, owner.name, location.id, location.name,
            identity, focus?.appearance.orEmpty(), focus?.position.orEmpty(), objects, settingDescription?.text.orEmpty(),
            action, environment, reference?.id, reference?.contentHash, style.trim())
        val essential = buildList {
            add("Illustrate the current fictional moment. Player stays outside the frame; no lettering or captions.")
            if (focus != null) {
                add(if (reference != null) "Depict the same character as the reference; preserve facial identity and hair." else "Character visual identity:")
                if (identity.isNotBlank()) add(identity)
                if (visual.appearance.isNotBlank()) add("Current temporary appearance (overrides baseline clothing): ${visual.appearance}")
            } else add("Environment only; do not depict absent characters.")
            if (action.isNotBlank()) add("Visible moment: $action")
            add("Location: ${location.name}.")
        }
        require(essential.joinToString("\n").length <= 1850) { "The essential visual brief and action are too long. Shorten the brief; the story is unaffected." }
        val blocks = essential.toMutableList()
        val omissions = mutableListOf<String>()
        fun optional(label: String, text: String) {
            if (text.isBlank()) return
            val block = "$label: $text"
            if (blocks.joinToString("\n").length + block.length + 1 <= 2000) blocks += block else omissions += label
        }
        optional("Position", visual.position)
        optional("Relevant objects", objects.joinToString("; "))
        optional("Setting", visual.setting)
        optional("Visible environment", environment)
        optional("Composition and style", style.trim().ifBlank { if (reference != null) "Eye-level scene, match the reference's visual style." else "Eye-level illustrated scene, natural proportions." })
        val old = previous?.snapshot
        val reason = when {
            old == null -> "First illustration"
            old.locationId != visual.locationId -> "Location changed"
            old.ownerId != visual.ownerId -> "Visible character changed"
            old.referenceHash != visual.referenceHash || old.identity != visual.identity || old.style != visual.style -> "Visual reference or brief changed"
            old.appearance != visual.appearance -> "Appearance changed"
            old.objects != visual.objects -> "Visible objects changed"
            old.action != visual.action || old.environment != visual.environment -> "Major visible moment"
            else -> "Reuse current artwork"
        }
        // Position alone is minor; keep it in the prompt without creating a generation trigger.
        val key = sha256(("artwork-v1|${CloudflareImageClient.MODEL}|portrait|" + json.encodeToString(visual.copy(position = ""))).toByteArray())
        val quotedMessage = beat?.let { cue -> s.messages.takeLast(2).lastOrNull { it.text.contains(cue.evidence) }?.id }
        val actionSource = if (beat?.action?.isNotBlank() == true) quotedMessage else previous?.actionSourceId?.takeIf { retainAction }
        val environmentSource = if (beat?.environment?.isNotBlank() == true) quotedMessage else previous?.environmentSourceId?.takeIf { samePlace }
        val actionEvidence = if (beat?.action?.isNotBlank() == true) beat.evidence else previous?.actionEvidence?.takeIf { retainAction }.orEmpty()
        val environmentEvidence = if (beat?.environment?.isNotBlank() == true) beat.evidence else previous?.environmentEvidence?.takeIf { samePlace }.orEmpty()
        val sources = listOfNotNull(reference?.id, reference?.let { description(it)?.id }, settingDescription?.id,
            "state:${scene.id}:${state.revision}", actionSource, environmentSource).distinct()
        return ImagePromptPlan(visual, blocks.joinToString("\n"), key, reason, s.messages.lastOrNull()?.id, sources, omissions, actionSource, environmentSource, actionEvidence, environmentEvidence)
    }
}
