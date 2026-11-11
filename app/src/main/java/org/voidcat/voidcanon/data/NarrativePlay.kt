package org.voidcat.voidcanon.data

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

/** Stable portable IDs. These are author preferences, never inferred story state. */
@Serializable
data class NarrativeSettings(
    @ColumnInfo(defaultValue = "'classic'") val mode: String = "classic",
    @ColumnInfo(defaultValue = "'collaborative'") val preset: String = "collaborative",
    @ColumnInfo(defaultValue = "'balanced'") val pacing: String = "balanced",
    @ColumnInfo(defaultValue = "'standard'") val length: String = "standard",
)

data class NarrativeOption(val id: String, val label: String, val description: String)

object NarrativePlay {
    val modes = listOf(
        NarrativeOption("classic", "Classic RP", "The existing character-and-narration style."),
        NarrativeOption("freeform", "Freeform Story", "MLRPE-inspired narrative preferences for this scene."),
    )
    val presets = listOf(
        NarrativeOption("collaborative", "Collaborative", "Balance character interaction and narration; leave openings for your next move."),
        NarrativeOption("character", "Character focus", "Emphasize the focus character's dialogue, mannerisms and observable reactions."),
        NarrativeOption("exploration", "World exploration", "Emphasize nearby sensory details, places and discoveries while keeping the character involved."),
    )
    val pacings = listOf(
        NarrativeOption("measured", "Measured", "Stay close to the current beat; avoid skipping time or resolving an encounter."),
        NarrativeOption("balanced", "Balanced", "Move the current beat forward with one natural development."),
        NarrativeOption("brisk", "Brisk", "Keep description focused and introduce one immediate development without skipping the user's decisions."),
    )
    val lengths = listOf(
        NarrativeOption("short", "Short", "Aim for 60–120 words of narrative."),
        NarrativeOption("standard", "Standard", "Aim for 120–220 words of narrative."),
        NarrativeOption("rich", "Rich", "Aim for 220–350 words of narrative."),
    )

    fun validate(settings: NarrativeSettings) {
        require(modes.any { it.id == settings.mode } && presets.any { it.id == settings.preset } &&
            pacings.any { it.id == settings.pacing } && lengths.any { it.id == settings.length }) {
            "Unsupported narrative settings. Choose the available play options."
        }
    }

    fun label(settings: NarrativeSettings): String = modes.firstOrNull { it.id == settings.mode }?.label ?: "Unknown mode"

    fun instruction(settings: NarrativeSettings): String {
        validate(settings)
        if (settings.mode == "classic") return ""
        return "\nFreeform Story (MLRPE-inspired narrative preferences):\n" +
            presets.first { it.id == settings.preset }.description + "\n" +
            pacings.first { it.id == settings.pacing }.description + "\n" +
            lengths.first { it.id == settings.length }.description +
            " These are narrative-length targets, not permission to truncate the required response protocol. " +
            "Respond to the user's actual contribution and end at a meaningful opening for their response. " +
            "Portray only the focus character and optional nearby environmental narration; no independent offscreen simulation. " +
            "Never invent the user's choices, speech, feelings or thoughts, force a major outcome, or invent dice, stats or rules results. " +
            "Respect supplied knowledge and continuity; discoveries remain provisional. " +
            "Return the required continuity JSON, with all story prose in its narrative field."
    }

    fun audit(settings: NarrativeSettings) = mapOf(
        "playMode" to settings.mode, "narrativePreset" to settings.preset,
        "narrativePacing" to settings.pacing, "narrativeLength" to settings.length,
    )
}
