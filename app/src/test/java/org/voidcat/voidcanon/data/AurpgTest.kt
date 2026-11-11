package org.voidcat.voidcanon.data

import org.junit.Assert.*
import org.junit.Test

class AurpgTest {
    @Test fun recordedMathAndMomentumAreDeterministicAndDoNotMutatePriorState() {
        val before = AurpgState(momentum = 5)
        val after = Aurpg.roll(before, "Cross the quay", 2, 6, 2, 10, 2, { 3 }, 42)
        val roll = after.checks.single()
        assertEquals(listOf(3, 3), roll.dice); assertEquals(10, roll.total)
        assertEquals(true, roll.passed); assertEquals(3, after.momentum)
        assertEquals(1L, after.revision); assertTrue(before.checks.isEmpty())
        assertEquals(after, Aurpg.decode(Aurpg.encode(after)))
    }
    @Test fun unopposedRollDoesNotInventAResultAndNaturalMaximumHasNoAutomaticCritical() {
        val roll = Aurpg.roll(AurpgState(), "", 1, 20, 0, null, 0, { 20 }).checks.single()
        assertNull(roll.passed)
        val fail = Aurpg.roll(AurpgState(), "", 1, 20, 0, 21, 0, { 20 }).checks.single()
        assertEquals(false, fail.passed)
    }
    @Test fun invalidSpendingOrNativeResultDoesNotProduceState() {
        assertTrue(runCatching { Aurpg.roll(AurpgState(), "", 1, 20, 0, null, 1, { 10 }) }.isFailure)
        assertTrue(runCatching { Aurpg.roll(AurpgState(), "", 1, 20, 0, null, 0, { 0 }) }.isFailure)
        assertTrue(runCatching { Aurpg.roll(AurpgState(), "", 11, 20, 0, null, 0, { 10 }) }.isFailure)
    }
    @Test fun corruptedTotalsDuplicateIdsAndUnsupportedVersionsAreRejected() {
        val state = Aurpg.roll(AurpgState(), "", 1, 20, 0, null, 0, { 10 })
        assertTrue(runCatching { Aurpg.validate(state.copy(checks = state.checks.map { it.copy(total = 999) })) }.isFailure)
        assertTrue(runCatching { Aurpg.validate(state.copy(checks = state.checks + state.checks)) }.isFailure)
        assertTrue(runCatching { Aurpg.validate(state.copy(version = 99)) }.isFailure)
    }
    @Test fun currentCountersAndOnlyRecentRollsEnterProjection() {
        var state = AurpgState(clocks = listOf(ProgressClock("clock", "Harbor watch", 6, 2)))
        repeat(5) { state = Aurpg.roll(state, "roll-$it", 1, 20, 0, null, 0, { 7 }) }
        val text = Aurpg.project(state)
        assertTrue(text.contains("Harbor watch 2/6")); assertTrue(text.contains("roll-4"))
        assertFalse(text.contains("roll-0")); assertFalse(text.contains("roll-1"))
        assertEquals(5, state.checks.size)
    }
}
