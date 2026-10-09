package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyCounterTest {

    private val d1 = "2026-10-09"
    private val d2 = "2026-10-10"

    @Test
    fun startsAtZero() {
        assertEquals(0, DailyCounter().value)
    }

    @Test
    fun add_isOptimisticAndCommitKeepsValueStable() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        assertEquals(11, c.add(d1, 1))
        assertEquals(11, c.commit(d1, 1, newTotal = 11))
        assertEquals(11, c.value)
    }

    @Test
    fun regression_failedWriteDoesNotInflateCounter() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        c.add(d1, 1)
        assertEquals(10, c.rollback(d1, 1))
        // And it stays correct for the next successful write.
        c.add(d1, 1)
        assertEquals(11, c.commit(d1, 1, 11))
    }

    @Test
    fun rollback_onlyUndoesItsOwnDelta() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        c.add(d1, 1)
        c.add(d1, 5)
        assertEquals(11, c.rollback(d1, 5))
    }

    @Test
    fun regression_counterResetsWhenDayChanges() {
        val c = DailyCounter()
        c.anchor(d1, 99)
        c.add(d1, 1)
        c.commit(d1, 1, 100)
        // First scroll of the new day must be 1, not 101.
        assertEquals(1, c.add(d2, 1))
        assertEquals(d2, c.currentDate)
        assertEquals(1, c.commit(d2, 1, 1))
    }

    @Test
    fun anchorForNewDayResetsToDatabaseTotal() {
        val c = DailyCounter()
        c.anchor(d1, 100)
        assertEquals(0, c.anchor(d2, 0))
        assertEquals(7, c.anchor(d2, 7)) // idle, so DB is authoritative
    }

    @Test
    fun olderDateNeverMovesLiveCounterBackwards() {
        val c = DailyCounter()
        c.anchor(d2, 3)
        assertEquals(3, c.add(d1, 1)) // straggler stamped before midnight
        assertEquals(3, c.anchor(d1, 99))
        assertEquals(3, c.commit(d1, 1, 100))
        assertEquals(3, c.rollback(d1, 1))
        assertEquals(d2, c.currentDate)
    }

    @Test
    fun anchorIgnoredWhileWriteInFlight_thenCommitIsAuthoritative() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        c.add(d1, 1) // value 11, write running
        // Database flow already emits the new row before commit() runs: must not show 12.
        assertEquals(11, c.anchor(d1, 11))
        assertEquals(11, c.commit(d1, 1, 11))
    }

    @Test
    fun anchorAppliesExternalChangesWhenIdle() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        assertEquals(25, c.anchor(d1, 25))
        assertEquals(0, c.anchor(d1, 0)) // e.g. data cleared: counter may go down when idle
    }

    @Test
    fun concurrentWritesCommitInOrder() {
        val c = DailyCounter()
        c.anchor(d1, 10)
        c.add(d1, 1)
        c.add(d1, 1)
        assertEquals(12, c.value)
        assertEquals(12, c.commit(d1, 1, 11)) // 11 stored + 1 still pending
        assertEquals(12, c.commit(d1, 1, 12))
    }

    @Test
    fun commitAndRollbackForStaleDateAreIgnored() {
        val c = DailyCounter()
        c.anchor(d1, 5)
        c.add(d1, 1) // write for d1 still running...
        c.add(d2, 1) // ...when the day rolls over
        assertEquals(1, c.commit(d1, 1, 6))
        assertEquals(1, c.rollback(d1, 1))
        assertEquals(d2, c.currentDate)
    }

    @Test
    fun nonPositiveDeltaIsIgnored() {
        val c = DailyCounter()
        c.anchor(d1, 4)
        assertEquals(4, c.add(d1, 0))
        assertEquals(4, c.add(d1, -3))
    }

    @Test
    fun pendingNeverGoesNegative() {
        val c = DailyCounter()
        c.anchor(d1, 4)
        assertEquals(4, c.rollback(d1, 10))
        assertEquals(4, c.commit(d1, 10, 4))
    }
}
