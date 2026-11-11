package org.voidcat.voidcanon.data

import kotlinx.serialization.json.*

data class ResolvedStory(val narrative: String, val state: ContinuityState?, val warning: String? = null)

/** Story publication is independent of optional, strictly validated continuity updates. */
object StoryResponse {
    const val WARNING = "Story saved; tracked state unchanged. Review scene continuity if needed."
    const val CONTEXT_NOTE = "The most recent story turn did not update tracked state. CURRENT STATE is the last validated snapshot, not proof that the recent story did not occur. Continue the recent narrative without undoing it to match older tracked values. Propose only explicitly supported state changes using the current state revision."
    private val fenced = Regex("\\A```(?:json|text)?[ \\t]*\\r?\\n([\\s\\S]*?)\\r?\\n```[ \\t]*\\z", RegexOption.IGNORE_CASE)
    private val arrayStart = Regex("""^\[\s*(?:[\[{"\d\-]|true\b|false\b|null\b|\])""")
    private fun hasThinking(text: String) = text.contains("<think>", ignoreCase = true) || text.contains("</think>", ignoreCase = true)

    fun resolve(raw: String, input: String, snapshot: WorldSnapshot): ResolvedStory {
        var payload = raw.trim()
        // Some providers include complete thinking blocks in content. Never publish them.
        while (payload.startsWith("<think>", ignoreCase = true)) {
            val end = payload.indexOf("</think>", ignoreCase = true)
            require(end >= 0) { "The model returned unfinished reasoning without a story. Your draft is preserved." }
            payload = payload.substring(end + "</think>".length).trim()
        }
        payload = fenced.matchEntire(payload)?.groupValues?.get(1)?.trim() ?: payload
        val tracked = runCatching { Continuity.resolve(payload, input, snapshot) }.getOrNull()
        if (tracked != null && !hasThinking(tracked.first)) return ResolvedStory(tracked.first, tracked.second)

        val element = runCatching { Json.parseToJsonElement(payload) }.getOrNull()
        fun text(value: JsonElement?): String? = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        fun story(obj: JsonObject): String? = listOf("narrative", "story", "response", "text", "content")
            .firstNotNullOfOrNull { key -> text(obj[key])?.takeIf { it.isNotBlank() } }
        val narrative = when (element) {
            is JsonObject -> story(element) ?: (element["message"] as? JsonObject)?.let(::story)
            is JsonPrimitive -> text(element) ?: payload.takeUnless {
                element is JsonNull || it == "true" || it == "false" || it.toBigDecimalOrNull() != null
            }
            is JsonArray -> null
            else -> payload.takeUnless { it.startsWith("{") || it.startsWith("```") || arrayStart.containsMatchIn(it) }
        }?.trim()
        require(!narrative.isNullOrBlank() && narrative.length <= 12000 &&
            !narrative.startsWith("{") && !narrative.startsWith("```") && !arrayStart.containsMatchIn(narrative) &&
            !hasThinking(narrative)) {
            "The model returned no usable story text. Your draft and history are preserved; no automatic retry was made."
        }
        return ResolvedStory(narrative, null, WARNING)
    }
}
