package org.voidcat.voidcanon.ai

/** Conservative character estimate, not a tokenizer; never silently truncate chapter sources. */
fun summaryOutputTokens(model: RouterModel, messages: List<PromptMessage>): Int {
    val estimatedInput = messages.sumOf { it.content.length.toLong() + 32 } / 2 + 512
    val available = model.contextLength.toLong() - estimatedInput
    val output = minOf(4096L, model.maxCompletionTokens?.toLong() ?: 4096L, available)
    require(output >= 512) {
        "This transcript leaves too little room for a summary with the selected model. Choose a model with a larger context in AI settings, or write a chapter summary manually. Your transcript is unchanged."
    }
    return output.toInt()
}
