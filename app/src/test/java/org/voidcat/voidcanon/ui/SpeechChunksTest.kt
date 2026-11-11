package org.voidcat.voidcanon.ui

import org.junit.Assert.*
import org.junit.Test

class SpeechChunksTest {
    @Test fun longDialogueIsFullyRetainedWithinTheEngineLimit() {
        val text = (1..1200).joinToString(" ") { "Lantern $it lights the harbor." }
        val chunks = speechChunks(text, 4000)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.isNotEmpty() && it.length <= 4000 })
        assertEquals(text, chunks.joinToString(""))
    }
    @Test fun hardWordBoundariesAndSurrogatePairsRemainIntact() {
        val text = "A\uD83D\uDE00B".repeat(100)
        val chunks = speechChunks(text, 3)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.none { Character.isLowSurrogate(it.first()) || Character.isHighSurrogate(it.last()) })
    }
    @Test fun emptyTextDoesNotQueueSpeech() { assertTrue(speechChunks("", 4000).isEmpty()) }
}
