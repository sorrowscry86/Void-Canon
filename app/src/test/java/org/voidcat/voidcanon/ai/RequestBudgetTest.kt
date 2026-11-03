package org.voidcat.voidcanon.ai

import org.junit.Assert.*
import org.junit.Test

class RequestBudgetTest {
    private val model = RouterModel("fixture/free", "Free", 32768, false, true, "0", "0")
    private val messages = listOf(PromptMessage("user", "A scene transcript"))

    @Test fun providerOutputMaximumIsRespectedAndOldCatalogsRemainUsable() {
        assertEquals(4096, summaryOutputTokens(model, messages))
        assertEquals(2048, summaryOutputTokens(model.copy(maxCompletionTokens = 2048), messages))
    }

    @Test fun inputAndOutputShareTheContextWindow() {
        val text = listOf(PromptMessage("user", "x".repeat(4000)))
        assertEquals(1568, summaryOutputTokens(model.copy(contextLength = 4096), text))
        assertEquals(4000, text.single().content.length)
    }

    @Test fun tooSmallContextFailsLocallyWithoutSilentlyDroppingSources() {
        val error = runCatching { summaryOutputTokens(model.copy(contextLength = 4096),
            listOf(PromptMessage("user", "x".repeat(24000)))) }.exceptionOrNull()
        assertTrue(error!!.message.orEmpty().contains("larger context"))
        assertTrue(error.message.orEmpty().contains("transcript is unchanged"))
    }
}
