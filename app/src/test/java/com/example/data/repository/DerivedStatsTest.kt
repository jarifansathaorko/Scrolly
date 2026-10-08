package com.example.data.repository

import com.example.data.local.ScrollyDatabase
import com.example.data.local.dao.DayTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Pure-logic tests for the statistics Scrolly derives from tracked data.
 *
 * These cover values that used to be hardcoded literals on the Profile screen
 * (14,832 lifetime scrolls, an 11-day best streak, a 23W/17L record) and the Stats
 * screen's Morning/Afternoon/Evening/Night split, which used to be a fixed
 * 15/35/40% ratio applied to the day's total rather than real activity.
 *
 * No database is involved: everything here is a pure function of its inputs, so the
 * assertions are deterministic and independent of device state.
 */
class DerivedStatsTest {

    private fun totals(vararg rows: Triple<String, Int, Int>): List<DayTotals> =
        rows.map { (date, scrolls, goal) -> DayTotals(date, scrolls, goal) }

    // ── Streaks ───────────────────────────────────────────────────────────

    @Test
    fun `no history means no streak`() {
        assertEquals(0 to 0, GamificationRepository.streaksFrom(emptyList()))
    }

    @Test
    fun `today under goal counts as a one day streak`() {
        val result = GamificationRepository.streaksFrom(
            totals(Triple(ScrollyDatabase.getTodayDate(), 10, 100))
        )
        assertEquals(1, result.first)
        assertEquals(1, result.second)
    }

    @Test
    fun `streak survives midnight when today has no row yet`() {
        // Before the first scroll of a new day the counter must not report zero.
        val result = GamificationRepository.streaksFrom(
            totals(Triple(ScrollyDatabase.getDateOffset(-1), 40, 100))
        )
        assertEquals(1, result.first)
    }

    @Test
    fun `streak breaks on a day over goal`() {
        val result = GamificationRepository.streaksFrom(
            totals(
                Triple(ScrollyDatabase.getTodayDate(), 150, 100),
                Triple(ScrollyDatabase.getDateOffset(-1), 20, 100)
            )
        )
        assertEquals("today is over goal so the current streak is 0", 0, result.first)
        assertEquals("yesterday's run is still the best", 1, result.second)
    }

    @Test
    fun `a day exactly at the goal still counts as keeping control`() {
        val result = GamificationRepository.streaksFrom(
            totals(Triple(ScrollyDatabase.getTodayDate(), 100, 100))
        )
        assertEquals(1, result.first)
    }

    @Test
    fun `streak counts every consecutive day up to today`() {
        val rows = (0..4).map { offset ->
            Triple(ScrollyDatabase.getDateOffset(-offset), 30, 100)
        }
        assertEquals(5, GamificationRepository.streaksFrom(totals(*rows.toTypedArray())).first)
    }

    @Test
    fun `streak stops at the first gap in the data`() {
        val rows = listOf(
            Triple(ScrollyDatabase.getTodayDate(), 10, 100),
            Triple(ScrollyDatabase.getDateOffset(-1), 10, 100),
            // No row for -2, so the run cannot extend past the gap.
            Triple(ScrollyDatabase.getDateOffset(-3), 10, 100),
            Triple(ScrollyDatabase.getDateOffset(-4), 10, 100)
        )
        assertEquals(2, GamificationRepository.streaksFrom(totals(*rows.toTypedArray())).first)
    }

    @Test
    fun `best streak survives a current streak of zero`() {
        val rows = listOf(
            Triple(ScrollyDatabase.getTodayDate(), 500, 100),
            Triple(ScrollyDatabase.getDateOffset(-1), 10, 100),
            Triple(ScrollyDatabase.getDateOffset(-2), 10, 100),
            Triple(ScrollyDatabase.getDateOffset(-3), 10, 100)
        )
        val result = GamificationRepository.streaksFrom(totals(*rows.toTypedArray()))
        assertEquals(0, result.first)
        assertEquals(3, result.second)
    }

    // ── Levels ────────────────────────────────────────────────────────────

    @Test
    fun `zero lifetime scrolls is level one with no progress`() {
        val (level, xp, needed) = GamificationRepository.levelForXp(0)
        assertEquals(1, level)
        assertEquals(0, xp)
        assertTrue("the next level must require some XP", needed > 0)
    }

    @Test
    fun `level two starts exactly at the first threshold`() {
        val required = GamificationRepository.xpRequiredForLevel(2)
        assertEquals(
            "just below the threshold stays level 1",
            1,
            GamificationRepository.levelForXp(required - 1).first
        )
        assertEquals(
            "at the threshold becomes level 2",
            2,
            GamificationRepository.levelForXp(required).first
        )
    }

    @Test
    fun `level never goes backwards and progress always fits the bar`() {
        var previousLevel = 0
        listOf(0, 500, 999, 1_000, 3_000, 25_000, 500_000).forEach { xp ->
            val (level, into, needed) = GamificationRepository.levelForXp(xp)
            assertTrue("level must not go backwards at xp=$xp", level >= previousLevel)
            assertTrue("progress must not be negative at xp=$xp", into >= 0)
            assertTrue("progress must fit the bar at xp=$xp", into < needed)
            previousLevel = level
        }
    }

    @Test
    fun `lifetime scrolls increase the level`() {
        val low = GamificationRepository.levelForXp(100).first
        val high = GamificationRepository.levelForXp(120_000).first
        assertTrue("120k scrolls should outrank 100 scrolls (got $high vs $low)", high > low)
    }

    @Test
    fun `negative scroll totals are clamped instead of throwing`() {
        val (level, xp, _) = GamificationRepository.levelForXp(-50)
        assertEquals(1, level)
        assertEquals(0, xp)
    }

    // ── Time-of-day bucketing ─────────────────────────────────────────────

    @Test
    fun `an empty event log yields four zeroed periods`() {
        val buckets = TrackingRepository.bucketByTimeOfDay(emptyList())
        assertEquals(4, buckets.size)
        assertTrue(buckets.all { it.count == 0 })
    }

    @Test
    fun `every hour of the day is counted exactly once`() {
        val start = ScrollyDatabase.startOfDayMillis(ScrollyDatabase.getTodayDate())
        val stamps = (0..23).map { hour ->
            Calendar.getInstance().apply {
                timeInMillis = start
                set(Calendar.HOUR_OF_DAY, hour)
            }.timeInMillis
        }
        assertEquals("no event may be lost", 24, TrackingRepository.bucketByTimeOfDay(stamps).sumOf { it.count })
    }

    @Test
    fun `a single event lands in exactly one period`() {
        val start = ScrollyDatabase.startOfDayMillis(ScrollyDatabase.getTodayDate())
        val buckets = TrackingRepository.bucketByTimeOfDay(
            listOf(Calendar.getInstance().apply { timeInMillis = start }.timeInMillis)
        )
        assertEquals(1, buckets.sumOf { it.count })
    }

    @Test
    fun `bucketing is proportional to the events supplied`() {
        val now = System.currentTimeMillis()
        assertEquals(7, TrackingRepository.bucketByTimeOfDay(List(7) { now }).sumOf { it.count })
    }

    // ── Day boundaries ────────────────────────────────────────────────────

    @Test
    fun `day range is half open and end of today is start of tomorrow`() {
        val today = ScrollyDatabase.getTodayDate()
        val start = ScrollyDatabase.startOfDayMillis(today)
        val end = ScrollyDatabase.endOfDayMillis(today)

        assertTrue("start must precede end", start < end)
        assertEquals(
            "end of today must be the start of tomorrow",
            end,
            ScrollyDatabase.startOfDayMillis(ScrollyDatabase.getDateOffset(1))
        )
        assertTrue(
            "midday falls strictly inside the range",
            start < start + 43_200_000L && start + 43_200_000L < end
        )
    }

    @Test
    fun `an unparseable date falls back to today rather than now`() {
        assertEquals(
            ScrollyDatabase.startOfDayMillis(ScrollyDatabase.getTodayDate()),
            ScrollyDatabase.startOfDayMillis("not-a-date")
        )
    }

    @Test
    fun `a past day's range covers only that day`() {
        // Regression: `endOfDayMillis` used to ignore its `date` argument and always
        // return the start of the next *real* day, so querying yesterday returned a two
        // day window that leaked today's events into the past day's chart.
        val yesterday = ScrollyDatabase.getDateOffset(-1)
        val yStart = ScrollyDatabase.startOfDayMillis(yesterday)
        val yEnd = ScrollyDatabase.endOfDayMillis(yesterday)

        assertTrue(
            "yesterday must be roughly one day long",
            (yEnd - yStart) in (23 * 3_600_000L)..(25 * 3_600_000L)
        )
        assertTrue(
            "now must fall outside yesterday's range",
            System.currentTimeMillis() !in yStart until yEnd
        )
    }

    @Test
    fun `every date in a week has a distinct contiguous range`() {
        val ranges = (0..6).map { offset ->
            val date = ScrollyDatabase.getDateOffset(-offset)
            ScrollyDatabase.startOfDayMillis(date) to ScrollyDatabase.endOfDayMillis(date)
        }
        assertEquals("each day must start at a distinct time", 7, ranges.map { it.first }.toSet().size)
        ranges.zipWithNext { newer, older ->
            // A day is 23 or 25 hours across a DST boundary, so only contiguity is
            // guaranteed, not a fixed 24 hours.
            assertTrue(
                "${newer.first} must follow ${older.second}",
                newer.first == older.second
            )
            assertTrue(
                "a day must be roughly 24 hours",
                older.second - older.first in (23 * 3_600_000L)..(25 * 3_600_000L)
            )
        }
    }

    // ── Profile presentation ──────────────────────────────────────────────

    @Test
    fun `battle record omits a zero loss tail`() {
        assertEquals("12", profile(wins = 12, losses = 0).battleRecord)
        assertEquals("12 / 5", profile(wins = 12, losses = 5).battleRecord)
    }

    @Test
    fun `a profile with no battles reports none`() {
        assertTrue(!profile(wins = 0, losses = 0).hasBattles)
        assertTrue(profile(wins = 1, losses = 0).hasBattles)
    }

    private fun profile(wins: Int, losses: Int) = UserProfile(
        username = "tester",
        totalScrollsAllTime = 0,
        streakDays = 0,
        bestStreak = 0,
        activeDays = 0,
        battlesWon = wins,
        battlesLost = losses,
        battlesTied = 0,
        currentLevel = 1,
        currentXp = 0,
        xpForNextLevel = 100
    )
}