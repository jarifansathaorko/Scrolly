package com.example.data.local

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateKeysTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val dhaka = TimeZone.getTimeZone("Asia/Dhaka")
    private val newYork = TimeZone.getTimeZone("America/New_York")

    private fun at(
        y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, s: Int = 0, ms: Int = 0,
        zone: TimeZone = utc
    ): Long = Calendar.getInstance(zone, Locale.US).apply {
        clear()
        set(y, m - 1, d, h, min, s)
        set(Calendar.MILLISECOND, ms)
    }.timeInMillis

    /** Runs [block] with [tag] as the JVM default locale, always restoring the original. */
    private fun <T> withDefaultLocale(tag: String, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(tag))
        try {
            return block()
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun day_formatsIsoKeyInRequestedZone() {
        val instant = at(2026, 10, 9, 18, 30, zone = utc) // 18:30 UTC
        assertEquals("2026-10-09", DateKeys.day(instant, utc))
        // Dhaka is UTC+6, so the same instant is already 00:30 on the next local day.
        assertEquals("2026-10-10", DateKeys.day(instant, dhaka))
    }

    @Test
    fun keys_areAsciiRegardlessOfDeviceLocale() {
        val instant = at(2026, 10, 9, 12)
        for (tag in listOf("bn-BD", "ar-EG", "fa-IR", "ne-NP", "mr-IN", "en-US")) {
            withDefaultLocale(tag) {
                assertEquals(tag, "2026-10-09", DateKeys.day(instant, utc))
                assertEquals(tag, "2026-10", DateKeys.month(0, instant, utc))
                assertEquals(tag, "2026", DateKeys.year(0, instant, utc))
                assertEquals(tag, "2026-10-09", DateKeys.child("2026-10", 9))
            }
        }
    }

    @Test
    fun regression_defaultLocaleFormatterProducesNonAsciiDigitsForBengali() {
        // Documents the original defect: the formatter the app used to build keys.
        val instant = at(2026, 10, 9, 12)
        val legacy = withDefaultLocale("bn-BD") {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = utc }.format(instant)
        }
        assertNotEquals("2026-10-09", legacy)
    }

    @Test
    fun dayOffset_crossesMonthAndYearBoundaries() {
        assertEquals("2026-02-28", DateKeys.dayOffset(-1, at(2026, 3, 1, 12), utc))
        assertEquals("2028-02-29", DateKeys.dayOffset(-1, at(2028, 3, 1, 12), utc)) // leap year
        assertEquals("2027-01-01", DateKeys.dayOffset(1, at(2026, 12, 31, 12), utc))
        assertEquals("2026-10-02", DateKeys.dayOffset(-7, at(2026, 10, 9, 12), utc))
        assertEquals("2026-10-09", DateKeys.dayOffset(0, at(2026, 10, 9, 12), utc))
    }

    @Test
    fun dayOffset_isCorrectAcrossDaylightSavingChange() {
        // US clocks spring forward on 2026-03-08 (a 23-hour day).
        assertEquals("2026-03-08", DateKeys.dayOffset(1, at(2026, 3, 7, 12, zone = newYork), newYork))
        assertEquals("2026-03-09", DateKeys.dayOffset(1, at(2026, 3, 8, 12, zone = newYork), newYork))
        assertEquals("2026-03-07", DateKeys.dayOffset(-1, at(2026, 3, 8, 12, zone = newYork), newYork))
    }

    @Test
    fun monthAndYearOffsets() {
        assertEquals("2025-12", DateKeys.month(-1, at(2026, 1, 15), utc))
        assertEquals("2027-01", DateKeys.month(1, at(2026, 12, 15), utc))
        assertEquals("2025", DateKeys.year(-1, at(2026, 6, 1), utc))
        // Jan 31 + 1 month must clamp to the last day of February, not overflow into March.
        assertEquals("2026-02", DateKeys.month(1, at(2026, 1, 31), utc))
    }

    @Test
    fun child_zeroPads() {
        assertEquals("2026-10-01", DateKeys.child("2026-10", 1))
        assertEquals("2026-10-31", DateKeys.child("2026-10", 31))
        assertEquals("2026-01", DateKeys.child("2026", 1))
    }

    @Test
    fun keys_sortChronologicallyAsStrings() {
        val keys = listOf(
            DateKeys.day(at(2026, 12, 31), utc),
            DateKeys.day(at(2026, 2, 3), utc),
            DateKeys.day(at(2025, 12, 31), utc),
            DateKeys.day(at(2026, 10, 9), utc)
        )
        assertEquals(
            listOf("2025-12-31", "2026-02-03", "2026-10-09", "2026-12-31"),
            keys.sorted()
        )
    }

    @Test
    fun millisUntilNextDay_nearMidnight() {
        assertEquals(500L, DateKeys.millisUntilNextDay(at(2026, 10, 9, 23, 59, 59, 500), utc))
        assertEquals(86_400_000L, DateKeys.millisUntilNextDay(at(2026, 10, 9, 0, 0, 0, 0), utc))
        assertEquals(1_000L, DateKeys.millisUntilNextDay(at(2026, 10, 9, 23, 59, 59, 0), utc))
    }

    @Test
    fun millisUntilNextDay_onDaylightSavingDayIs23Hours() {
        val midnight = at(2026, 3, 8, 0, 0, zone = newYork)
        assertEquals(23L * 3_600_000L, DateKeys.millisUntilNextDay(midnight, newYork))
        assertTrue(DateKeys.millisUntilNextDay(midnight, newYork) in 1L..90_000_000L)
    }
}
