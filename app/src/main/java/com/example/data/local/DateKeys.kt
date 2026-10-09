package com.example.data.local

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the `yyyy-MM-dd` / `yyyy-MM` / `yyyy` strings that are stored in, and compared against,
 * the `date` columns of `daily_stats` and `app_stats`.
 *
 * These strings are **storage keys**, not display text, so they must not depend on the device
 * locale. A formatter created with `Locale.getDefault()` emits non-ASCII digits under locales such
 * as `bn`, `ar`, `fa`, `ne` and `mr`, which would (a) orphan existing history when the language is
 * changed and (b) break the `date >= ?`, `date LIKE '2026-10%'` and lexical-ordering assumptions
 * that the DAO queries rely on. Everything here is pinned to [Locale.US] (ASCII digits, Gregorian).
 *
 * Use a locale-aware formatter only for text shown to the user (e.g. "Oct 9", "Mon").
 */
object DateKeys {

    const val DAY_PATTERN = "yyyy-MM-dd"
    const val MONTH_PATTERN = "yyyy-MM"
    const val YEAR_PATTERN = "yyyy"

    /**
     * A new formatter for [pattern] that is safe for storage keys. A new instance is returned on
     * every call because [SimpleDateFormat] is not thread-safe.
     */
    fun formatter(pattern: String, zone: TimeZone = TimeZone.getDefault()): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = zone }

    /** `yyyy-MM-dd` for the instant [timeMillis] in [zone]. */
    fun day(
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): String = formatter(DAY_PATTERN, zone).format(timeMillis)

    /** `yyyy-MM-dd` for [daysOffset] calendar days from the instant [timeMillis]. */
    fun dayOffset(
        daysOffset: Int,
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(zone, Locale.US).apply {
            this.timeInMillis = timeMillis
            add(Calendar.DAY_OF_YEAR, daysOffset)
        }
        return formatter(DAY_PATTERN, zone).format(cal.timeInMillis)
    }

    /** `yyyy-MM` for [offsetMonths] months from the instant [timeMillis]. */
    fun month(
        offsetMonths: Int = 0,
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(zone, Locale.US).apply {
            this.timeInMillis = timeMillis
            add(Calendar.MONTH, offsetMonths)
        }
        return formatter(MONTH_PATTERN, zone).format(cal.timeInMillis)
    }

    /** `yyyy` for [offsetYears] years from the instant [timeMillis]. */
    fun year(
        offsetYears: Int = 0,
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = Calendar.getInstance(zone, Locale.US).apply {
            this.timeInMillis = timeMillis
            add(Calendar.YEAR, offsetYears)
        }
        return formatter(YEAR_PATTERN, zone).format(cal.timeInMillis)
    }

    /** `"<prefix>-NN"` with a zero-padded ASCII number, e.g. `child("2026-10", 9) == "2026-10-09"`. */
    fun child(prefix: String, number: Int): String = String.format(Locale.US, "%s-%02d", prefix, number)

    /**
     * Milliseconds from [timeMillis] until the next local midnight in [zone]. Always in
     * `1..90_000_000` (DST days are 23–25 h long), so it is safe to use as a delay.
     */
    fun millisUntilNextDay(
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(zone, Locale.US).apply {
            this.timeInMillis = timeMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - timeMillis).coerceAtLeast(1L)
    }
}
