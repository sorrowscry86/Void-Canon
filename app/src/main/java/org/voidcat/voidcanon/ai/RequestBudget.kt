package org.voidcat.voidcanon.ai

/** Conservative character estimate, not a tokenizer; never silently truncate chapter sources. */
fun summaryOutputTokens(model: RouterModel, messages: List<PromptMessage>): Int = generationOutputTokens(model, messages, "summary")

fun generationOutputTokens(model: RouterModel, messages: List<PromptMessage>, purpose: String): Int {
    val estimatedInput = messages.sumOf { it.content.length.toLong() + 32 } / 2 + 512
    val available = model.contextLength.toLong() - estimatedInput
    val output = minOf(4096L, model.maxCompletionTokens?.toLong() ?: 4096L, available)
    require(output >= if (purpose == "roleplay") 1024 else 512) {
        "The selected model leaves too little output room for this $purpose request. Choose a model with a larger context/output allowance in AI settings. Your transcript is unchanged; scene state is also preserved."
    }
    return output.toInt()
}
