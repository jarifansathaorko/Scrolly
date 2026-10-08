package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ScrollyDatabase
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [TrackingRepository] against a real (in-memory) Room database.
 *
 * These lock in the properties the previous implementation violated:
 *  - scroll writes must not lose counts when several land in quick succession;
 *  - the day must roll over at midnight instead of carrying yesterday's total forward;
 *  - the in-memory counter must re-anchor to storage;
 *  - "limit reached" must latch per day rather than firing on every scroll;
 *  - a challenge bonus must never permanently raise the user's configured limit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingRepositoryTest {

    private lateinit var database: ScrollyDatabase
    private lateinit var dao: com.example.data.local.dao.ScrollyDao

    private val instagram = "com.instagram.android"
    private val shorts = "com.google.android.youtube"

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ScrollyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scrollyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Builds a repository on the test's own scope, so its collectors share the test
     * scheduler and `advanceUntilIdle()` drains them deterministically.
     */
    private fun TestScope.repository(): TrackingRepository =
        TrackingRepository(dao, scope = backgroundScope)

    private fun limit(
        pkg: String = instagram,
        dailyLimit: Int = 100,
        blocked: Boolean = false,
        enabled: Boolean = true
    ) = AppLimitEntity(
        packageName = pkg,
        appName = "App",
        dailyLimit = dailyLimit,
        warningThreshold = (dailyLimit * 0.8f).toInt(),
        isBlocked = blocked,
        isEnabled = enabled
    )

    // ── Recording ─────────────────────────────────────────────────────────

    @Test
    fun `recording a scroll updates every aggregate consistently`() = runTest {
        val repo = repository()

        repo.recordScroll(instagram, "Instagram Reels", 1)
        advanceUntilIdle()

        val today = ScrollyDatabase.getTodayDate()
        assertEquals(1, dao.getDailyStats(today)?.totalScrolls)
        assertEquals(1, dao.getAppStat(today, instagram)?.scrollCount)
        assertEquals(1, dao.getScrollCountSince(0L))
    }

    @Test
    fun `rapid scrolls do not overwrite each other`() = runTest {
        val repo = repository()

        // The old read-modify-write cycle could interleave these and persist only some.
        repeat(25) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        val today = ScrollyDatabase.getTodayDate()
        assertEquals(25, dao.getDailyStats(today)?.totalScrolls)
        assertEquals(25, dao.getAppStat(today, instagram)?.scrollCount)
        assertEquals(25, dao.getScrollCountSince(0L))
    }

    @Test
    fun `per app totals stay separate while the daily total sums them`() = runTest {
        val repo = repository()

        repeat(4) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        repeat(6) { repo.recordScroll(shorts, "YouTube Shorts", 1) }
        advanceUntilIdle()

        val today = ScrollyDatabase.getTodayDate()
        assertEquals(10, dao.getDailyStats(today)?.totalScrolls)
        assertEquals(4, dao.getAppStat(today, instagram)?.scrollCount)
        assertEquals(6, dao.getAppStat(today, shorts)?.scrollCount)
    }

    @Test
    fun `one row per app per day is maintained`() = runTest {
        val repo = repository()

        repeat(5) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        val summary = dao.getAppStatsSummaryForDateFlow(ScrollyDatabase.getTodayDate()).first()
        assertEquals(
            "duplicate (day, app) rows would break the ON CONFLICT upserts",
            1,
            summary.size
        )
        assertEquals(5, summary.first().totalScrolls)
    }

    @Test
    fun `a zero delta is a no-op`() = runTest {
        val repo = repository()
        repo.recordScroll(instagram, "Instagram Reels", 0)
        advanceUntilIdle()
        assertEquals(0, dao.getScrollCountSince(0L))
    }

    @Test
    fun `the exposed counter matches storage`() = runTest {
        val repo = repository()
        repeat(3) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()
        assertEquals(3, repo.todayTotalScrolls.value)
    }

    @Test
    fun `refreshToday re-anchors the counter to storage`() = runTest {
        val repo = repository()
        repo.recordScroll(instagram, "Instagram Reels", 2)
        advanceUntilIdle()

        // Simulate storage changing underneath us (another process, or a repair).
        dao.upsertDailyStats(dao.getDailyStats(ScrollyDatabase.getTodayDate())!!.copy(totalScrolls = 42))

        repo.refreshToday()
        advanceUntilIdle()

        assertEquals(42, repo.todayTotalScrolls.value)
    }

    @Test
    fun `recording before any row exists creates the day`() = runTest {
        val repo = repository()
        assertNull(dao.getDailyStats(ScrollyDatabase.getTodayDate()))

        repo.recordScroll(instagram, "Instagram Reels", 1)
        advanceUntilIdle()

        val stats = dao.getDailyStats(ScrollyDatabase.getTodayDate())
        assertNotNull(stats)
        assertEquals(1, stats!!.totalScrolls)
        assertTrue("a new day must get a usable default goal", stats.goal > 0)
    }

    @Test
    fun `an unknown app still records and gets a readable name`() = runTest {
        val repo = repository()
        repo.recordScroll("com.example.unknown.app", "Unknown App", 1)
        advanceUntilIdle()

        val stats = dao.getAppStat(ScrollyDatabase.getTodayDate(), "com.example.unknown.app")
        assertNotNull(stats)
        assertEquals("Unknown App", stats!!.appName)
    }

    // ── Day rollover ──────────────────────────────────────────────────────

    @Test
    fun `rollover notifies session-scoped listeners`() = runTest {
        val repo = repository()
        var notifications = 0
        repo.addOnNewDayListener { notifications++ }

        repo.forceDayRollover(ScrollyDatabase.getTodayDate())
        advanceUntilIdle()

        assertEquals("challenge claim state must be able to reset per day", 1, notifications)
    }

    @Test
    fun `a throwing rollover listener does not break the rollover`() = runTest {
        val repo = repository()
        var reached = false
        repo.addOnNewDayListener { error("boom") }
        repo.addOnNewDayListener { reached = true }

        repo.forceDayRollover(ScrollyDatabase.getTodayDate())
        advanceUntilIdle()

        assertTrue("later listeners must still run", reached)
        assertEquals(ScrollyDatabase.getTodayDate(), repo.trackedDate.value)
    }

    @Test
    fun `trackedDate starts on today`() = runTest {
        assertEquals(ScrollyDatabase.getTodayDate(), repository().trackedDate.value)
    }

    @Test
    fun `a new day resets the counter and clears yesterday's block`() = runTest {
        val repo = repository()
        val yesterday = ScrollyDatabase.getDateOffset(-1)

        dao.upsertDailyStats(
            DailyStatsEntity(date = yesterday, totalScrolls = 80, goal = 100, isGoalMet = false)
        )
        dao.upsertLimit(limit(dailyLimit = 10, blocked = true))

        // Emulate the clock crossing midnight.
        repo.forceDayRollover(ScrollyDatabase.getTodayDate())
        advanceUntilIdle()

        assertEquals("yesterday's total must not leak into today", 0, repo.todayTotalScrolls.value)
        assertFalse("yesterday's block must not persist", dao.getLimit(instagram)!!.isBlocked)
    }

    @Test
    fun `scrolling after a rollover counts into the new day`() = runTest {
        val repo = repository()
        val yesterday = ScrollyDatabase.getDateOffset(-1)
        dao.upsertDailyStats(
            DailyStatsEntity(date = yesterday, totalScrolls = 77, goal = 100, isGoalMet = false)
        )

        repo.forceDayRollover(ScrollyDatabase.getTodayDate())
        advanceUntilIdle()

        repo.recordScroll(instagram, "Instagram Reels", 1)
        advanceUntilIdle()

        assertEquals(1, repo.todayTotalScrolls.value)
        assertEquals("yesterday must be untouched", 77, dao.getDailyStats(yesterday)?.totalScrolls)
    }

    // ── Limits ────────────────────────────────────────────────────────────

    @Test
    fun `reaching a limit blocks the app`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 3))

        repeat(3) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        assertTrue(dao.getLimit(instagram)!!.isBlocked)
    }

    @Test
    fun `an already blocked app is not re-blocked or re-reported`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 3, blocked = true))

        repeat(10) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        // Still exactly one block, and the daily total kept climbing regardless.
        assertTrue(dao.getLimit(instagram)!!.isBlocked)
        assertEquals(10, dao.getDailyStats(ScrollyDatabase.getTodayDate())?.totalScrolls)
    }

    @Test
    fun `a disabled limit never blocks`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 1, enabled = false))

        repeat(3) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        assertFalse(dao.getLimit(instagram)!!.isBlocked)
    }

    @Test
    fun `editing a limit clears an existing block`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 5, blocked = true))

        repo.updateAppLimit(instagram, newLimit = 50, warningThreshold = 40, isEnabled = true)

        val updated = dao.getLimit(instagram)!!
        assertEquals(50, updated.dailyLimit)
        assertFalse(updated.isBlocked)
    }

    @Test
    fun `limit input is clamped to a sane range`() = runTest {
        val repo = repository()

        repo.updateAppLimit(instagram, newLimit = -5, warningThreshold = 99_999, isEnabled = true)

        val updated = dao.getLimit(instagram)!!
        assertTrue("limit must stay positive", updated.dailyLimit >= 1)
        assertTrue("limit must stay bounded", updated.dailyLimit <= 1_000)
    }

    // ── Bonuses ───────────────────────────────────────────────────────────

    @Test
    fun `a challenge bonus never changes the stored limit`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 100, blocked = true))

        // Claiming bonuses used to add to dailyLimit *and persist it*, so five claims
        // multiplied the user's own ceiling.
        repeat(5) { repo.unblockAppTemporarily(instagram, 15) }

        assertEquals(100, dao.getLimit(instagram)!!.dailyLimit)
    }

    @Test
    fun `a bonus raises the effective allowance without persisting`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 100))

        repo.unblockAppTemporarily(instagram, 25)

        assertEquals(125, repo.effectiveLimit(instagram, 100))
        assertEquals(100, dao.getLimit(instagram)!!.dailyLimit)
    }

    @Test
    fun `a bonus clears the block so the user can continue`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 1, blocked = true))

        repo.unblockAppTemporarily(instagram, 10)

        assertFalse(dao.getLimit(instagram)!!.isBlocked)
    }

    @Test
    fun `a non-positive bonus is ignored`() = runTest {
        val repo = repository()
        dao.upsertLimit(limit(dailyLimit = 10))

        repo.unblockAppTemporarily(instagram, 0)

        assertEquals(10, repo.effectiveLimit(instagram, 10))
    }

    // ── Daily goal ────────────────────────────────────────────────────────

    @Test
    fun `setting a daily goal updates the stored row`() = runTest {
        val repo = repository()
        repo.recordScroll(instagram, "Instagram Reels", 1)
        advanceUntilIdle()

        repo.setDailyGoal(250)

        assertEquals(250, dao.getDailyStats(ScrollyDatabase.getTodayDate())?.goal)
    }

    @Test
    fun `the goal flag flips exactly when the limit is consumed`() = runTest {
        val repo = repository()

        repo.setDailyGoal(3)
        repo.recordScroll(instagram, "Instagram Reels", 2)
        advanceUntilIdle()
        assertFalse("still under the goal", dao.getDailyStats(ScrollyDatabase.getTodayDate())!!.isGoalMet)

        repo.recordScroll(instagram, "Instagram Reels", 1)
        advanceUntilIdle()

        val stats = dao.getDailyStats(ScrollyDatabase.getTodayDate())!!
        assertEquals(3, stats.totalScrolls)
        assertTrue("goal reached at exactly the limit", stats.isGoalMet)
    }

    // ── Aggregates ────────────────────────────────────────────────────────

    @Test
    fun `lifetime total sums every day`() = runTest {
        val repo = repository()
        repo.recordScroll(instagram, "Instagram Reels", 5)
        advanceUntilIdle()

        assertEquals(5, repo.getLifetimeScrollTotalFlow().first())
    }

    @Test
    fun `time of day buckets reflect real events`() = runTest {
        val repo = repository()
        // One call per event: a delta of 3 is a single event row, not three.
        repeat(3) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        val buckets = repo.getScrollsByTimeOfDay(ScrollyDatabase.getTodayDate())
        assertEquals("every logged scroll must be bucketed", 3, buckets.sumOf { it.count })
    }

    @Test
    fun `a past day has no events`() = runTest {
        val repo = repository()
        repeat(2) { repo.recordScroll(instagram, "Instagram Reels", 1) }
        advanceUntilIdle()

        val yesterday = ScrollyDatabase.getDateOffset(-1)
        assertEquals(
            "yesterday's range must not include today's events",
            0,
            repo.getScrollsByTimeOfDay(yesterday).sumOf { it.count }
        )
    }

    @Test
    fun `app summary aggregates across a range`() = runTest {
        val repo = repository()
        repo.recordScroll(instagram, "Instagram Reels", 2)
        repo.recordScroll(shorts, "YouTube Shorts", 3)
        advanceUntilIdle()

        val summary = dao.getAppStatsSummaryForDateFlow(ScrollyDatabase.getTodayDate()).first()
        assertEquals(2, summary.size)
        assertEquals(
            "summary must be ordered by contribution",
            shorts,
            summary.first().packageName
        )
    }
}