package com.example.data.repository

import android.util.Log
import com.example.data.local.ScrollyDatabase
import com.example.data.local.dao.AppLifetimeTotal
import com.example.data.local.dao.ScrollyDao
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

data class LimitReachedEvent(
    val packageName: String,
    val appName: String,
    val currentScrolls: Int,
    val limit: Int
)

/** Named part of the local day, used for the Stats screen's real time-of-day chart. */
enum class TimeOfDay(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

data class TimeOfDayCount(
    val period: TimeOfDay,
    val count: Int
)

/**
 * Owns every write to scroll statistics.
 *
 * Three invariants this class exists to protect:
 *
 *  1. **The day rolls over.** A phone left running overnight must start counting from
 *     zero on the new date. The previous implementation captured `today` once in `init`
 *     and collected that date's row forever, so after midnight the in-memory counter kept
 *     climbing from yesterday's total. [watchForDayChange] restarts the observer when the
 *     local date changes.
 *
 *  2. **Memory never outruns storage.** [todayTotalScrolls] updates optimistically so the
 *     overlay reacts with zero latency, but it is always recomputed as
 *     `storedTotal + pendingDelta` rather than being mutated blindly. The old code only
 *     ever accepted *increases* from the database, so a single failed write left the
 *     displayed count permanently too high with no way to recover.
 *
 *  3. **Writes are atomic.** Daily total, per-app total and the raw event log are written
 *     in one transaction ([ScrollyDao.recordScrollAtomic]) so the aggregates cannot
 *     disagree after a crash mid-write.
 */
class TrackingRepository(
    private val dao: ScrollyDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TrackingRepository"

        /** How often the local date is re-checked for a rollover. */
        private const val DAY_WATCH_INTERVAL_MS = 60_000L

        /** Pause after a change is noticed, so in-flight writes settle before swapping. */
        private const val DAY_ROLLOVER_GRACE_MS = 1_500L

        /**
         * Buckets absolute timestamps into the four named parts of the local day.
         *
         * Pure and clock-based, so it is unit-testable without a database. Night wraps
         * midnight, so it is the union of 21:00-23:59 and 00:00-05:59.
         */
        fun bucketByTimeOfDay(timestamps: List<Long>): List<TimeOfDayCount> {
            val buckets = IntArray(24)
            val calendar = Calendar.getInstance()
            timestamps.forEach { millis ->
                calendar.timeInMillis = millis
                buckets[calendar.get(Calendar.HOUR_OF_DAY)]++
            }

            fun sum(hours: IntRange) = hours.sumOf { buckets[it] }

            return listOf(
                TimeOfDayCount(TimeOfDay.MORNING, sum(6..11)),
                TimeOfDayCount(TimeOfDay.AFTERNOON, sum(12..16)),
                TimeOfDayCount(TimeOfDay.EVENING, sum(17..20)),
                TimeOfDayCount(TimeOfDay.NIGHT, sum(21..23) + sum(0..5))
            )
        }
    }

    private val _limitReachedEvents = MutableSharedFlow<LimitReachedEvent>(extraBufferCapacity = 8)
    val limitReachedEvents = _limitReachedEvents.asSharedFlow()

    private val _todayTotalScrolls = MutableStateFlow(0)
    val todayTotalScrolls = _todayTotalScrolls.asStateFlow()

    private val _trackedDate = MutableStateFlow(ScrollyDatabase.getTodayDate())
    val trackedDate = _trackedDate.asStateFlow()

    /** Last total actually read back from storage. */
    @Volatile
    private var storedTotal: Int = 0

    /** Scrolled but not yet visible to the observer, so the UI can lead the database. */
    private val pendingDelta = AtomicInteger(0)

    /** Session-only bonus allowances; deliberately never persisted (see [unblockAppTemporarily]). */
    private val bonusScrolls = ConcurrentHashMap<String, Int>()

    init {
        scope.launch { observeToday() }
        scope.launch { watchForDayChange() }
    }

    private val onNewDayListeners = CopyOnWriteArrayList<() -> Unit>()

    /**
     * Registers a callback invoked after each day rollover.
     *
     * Used to clear session-scoped state that is per-day by nature: a challenge bonus and
     * a "limit reached" block. Without it a user could claim each bonus exactly once ever,
     * and could stay blocked across an entire following day.
     */
    fun addOnNewDayListener(listener: () -> Unit) {
        onNewDayListeners += listener
    }

    // ── Count publishing ──────────────────────────────────────────────────

    private fun publish() {
        val value = (storedTotal + pendingDelta.get()).coerceAtLeast(0)
        if (_todayTotalScrolls.value != value) {
            _todayTotalScrolls.value = value
        }
    }

    /**
     * Mirrors the stored daily total, combined with any not-yet-persisted scrolls, into
     * [todayTotalScrolls].
     *
     * Driven by [trackedDate] through `flatMapLatest`, so a rollover re-subscribes to the
     * new day's row automatically instead of leaving a collector attached to yesterday.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeToday() {
        _trackedDate
            .flatMapLatest { date -> dao.getDailyStatsFlow(date) }
            .collect { stats ->
                storedTotal = stats?.totalScrolls ?: 0
                publish()
            }
    }

    /**
     * Re-establishes the storage-backed observer whenever the local date changes.
     *
     * The clock is re-read each tick rather than advanced by arithmetic, so manual
     * date/time changes and DST transitions are handled exactly like midnight.
     */
    private suspend fun watchForDayChange() {
        while (scope.isActive) {
            delay(DAY_WATCH_INTERVAL_MS)
            val today = ScrollyDatabase.getTodayDate()
            if (today != _trackedDate.value) {
                Log.i(TAG, "Day rolled over: ${_trackedDate.value} -> $today")
                onDayChanged(today)
            }
        }
    }

    private suspend fun onDayChanged(newDate: String) {
        // Let any in-flight write for the previous day settle before swapping observers.
        delay(DAY_ROLLOVER_GRACE_MS)

        pendingDelta.set(0)
        storedTotal = 0
        publish()

        // "Limit reached" latches are per-day. Clearing them here is what stops a user
        // from staying permanently blocked because of yesterday's overspend.
        runCatching { dao.clearAllBlocks() }
            .onFailure { Log.w(TAG, "Failed to clear daily blocks", it) }

        bonusScrolls.clear()
        _trackedDate.value = newDate

        // Notify session-scoped consumers (e.g. challenge claim state).
        onNewDayListeners.forEach { listener ->
            runCatching { listener() }
                .onFailure { Log.w(TAG, "onNewDay listener failed", it) }
        }
    }

    /**
     * Test hook: emulates the clock crossing midnight without waiting for real time.
     *
     * Exposed so the rollover path can be covered by tests instead of only at 00:00.
     */
    @androidx.annotation.VisibleForTesting
    internal suspend fun forceDayRollover(newDate: String) = onDayChanged(newDate)

    /** Forces an immediate re-read of the day's total from storage. */
    suspend fun refreshToday() {
        val today = ScrollyDatabase.getTodayDate()
        if (today != _trackedDate.value) {
            onDayChanged(today)
            return
        }
        runCatching { dao.getDailyStats(today) }
            .onSuccess { stats -> storedTotal = stats?.totalScrolls ?: 0 }
            .onFailure { Log.w(TAG, "refreshToday failed", it) }
        publish()
    }

    // ── Read flows ────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayStatsFlow(): Flow<DailyStatsEntity?> = trackedDate.flatMapLatest { date ->
        dao.getDailyStatsFlow(date)
    }

    fun getDailyStatsFlow(date: String): Flow<DailyStatsEntity?> = dao.getDailyStatsFlow(date)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayAppStatsFlow(): Flow<List<AppStatsEntity>> = trackedDate.flatMapLatest { date ->
        dao.getAppStatsForDateFlow(date)
    }

    fun getRecentDailyStatsFlow(): Flow<List<DailyStatsEntity>> = dao.getRecentDailyStatsFlow()

    fun getAllDailyStatsFlow(): Flow<List<DailyStatsEntity>> = dao.getAllDailyStatsFlow()

    fun getDailyStatsBetweenFlow(startDate: String, endDate: String): Flow<List<DailyStatsEntity>> =
        dao.getDailyStatsBetweenFlow(startDate, endDate)

    fun getDailyStatsLikeFlow(pattern: String): Flow<List<DailyStatsEntity>> =
        dao.getDailyStatsLikeFlow(pattern)

    fun getAppStatsSummaryForDateFlow(date: String): Flow<List<AppLifetimeTotal>> =
        dao.getAppStatsSummaryForDateFlow(date)

    fun getAppStatsSummaryBetweenFlow(startDate: String, endDate: String): Flow<List<AppLifetimeTotal>> =
        dao.getAppStatsSummaryBetweenFlow(startDate, endDate)

    fun getAppStatsSummaryLikeFlow(pattern: String): Flow<List<AppLifetimeTotal>> =
        dao.getAppStatsSummaryLikeFlow(pattern)

    fun getAppLimitsFlow(): Flow<List<AppLimitEntity>> = dao.getAllLimitsFlow()

    fun getLifetimeScrollTotalFlow(): Flow<Int> = dao.getLifetimeScrollTotalFlow()

    /**
     * Reactive per-period scroll counts for [date], bucketed from the raw event log.
     *
     * Replaces a hardcoded 15/35/40% split that invented a Morning/Afternoon/Evening/
     * Night breakdown unrelated to when the user actually scrolled. Reactive so the
     * chart updates live as scrolls are logged.
     */
    fun getScrollsByTimeOfDayFlow(date: String): Flow<List<TimeOfDayCount>> {
        val start = ScrollyDatabase.startOfDayMillis(date)
        val end = ScrollyDatabase.endOfDayMillis(date)
        return dao.getScrollTimestampsBetweenFlow(start, end).map { bucketByTimeOfDay(it) }
    }

    /** One-shot snapshot of [getScrollsByTimeOfDayFlow]. */
    suspend fun getScrollsByTimeOfDay(date: String): List<TimeOfDayCount> =
        bucketByTimeOfDay(
            runCatching { dao.getScrollTimestampsBetween(
                ScrollyDatabase.startOfDayMillis(date),
                ScrollyDatabase.endOfDayMillis(date)
            ) }.getOrElse {
                Log.w(TAG, "getScrollsByTimeOfDay failed for $date", it)
                emptyList()
            }
        )

    // ── Writes ────────────────────────────────────────────────────────────

    /**
     * Records [delta] scrolls for an app.
     *
     * Storage is updated through a single atomic transaction so the daily total, the
     * per-app total and the event log cannot diverge. Memory is updated first for
     * latency and re-anchored to the authoritative row once the write returns.
     */
    suspend fun recordScroll(packageName: String, appName: String, delta: Int = 1) {
        if (delta == 0) return

        val date = ScrollyDatabase.getTodayDate()
        if (date != _trackedDate.value) {
            // A scroll landed just after midnight: settle the rollover before counting.
            onDayChanged(date)
        }

        val goal = currentGoal(date)

        pendingDelta.addAndGet(delta)
        publish()

        try {
            dao.recordScrollAtomic(date, packageName, appName, delta, goal)
            pendingDelta.addAndGet(-delta)
            runCatching { dao.getDailyStats(date) }
                .onSuccess { storedTotal = it?.totalScrolls ?: 0 }
            publish()
            checkLimitReached(date, packageName, appName)
        } catch (e: Exception) {
            Log.e(TAG, "recordScroll failed for $packageName (+$delta)", e)
            // Re-anchor to storage rather than leaving an inflated counter on screen.
            pendingDelta.addAndGet(-delta)
            runCatching { dao.getDailyStats(date) }
                .onSuccess { storedTotal = it?.totalScrolls ?: 0 }
            publish()
        }
    }

    private suspend fun currentGoal(date: String): Int =
        runCatching { dao.getDailyStats(date)?.goal }
            .getOrNull()
            ?.takeIf { it > 0 }
            ?: ScrollyDatabase.DEFAULT_DAILY_GOAL

    /** Emits at most one [LimitReachedEvent] per app per day. */
    private suspend fun checkLimitReached(date: String, packageName: String, appName: String) {
        val limit = runCatching { dao.getLimit(packageName) }.getOrNull() ?: return
        if (!limit.isEnabled || limit.isBlocked || limit.dailyLimit <= 0) return

        val appScrolls = runCatching { dao.getAppStat(date, packageName)?.scrollCount }
            .getOrNull() ?: return
        val effective = effectiveLimit(packageName, limit.dailyLimit)
        if (appScrolls < effective) return

        dao.upsertLimit(limit.copy(isBlocked = true))
        _limitReachedEvents.emit(
            LimitReachedEvent(
                packageName = packageName,
                appName = appName,
                currentScrolls = appScrolls,
                limit = effective
            )
        )
    }

    suspend fun updateAppLimit(packageName: String, newLimit: Int, warningThreshold: Int, isEnabled: Boolean) {
        val existing = dao.getLimit(packageName) ?: AppLimitEntity(
            packageName = packageName,
            appName = AppRecognitionNames.displayNameFor(packageName),
            dailyLimit = newLimit,
            warningThreshold = warningThreshold,
            isEnabled = isEnabled
        )
        bonusScrolls.remove(packageName)
        dao.upsertLimit(
            existing.copy(
                dailyLimit = newLimit.coerceIn(1, 1_000),
                warningThreshold = warningThreshold.coerceIn(1, 1_000),
                isEnabled = isEnabled,
                // Editing a limit is an explicit user intent, so clear any block.
                isBlocked = false
            )
        )
    }

    /**
     * Grants a bonus of [extraScrolls] scrolls on top of the app's stored limit.
     *
     * The previous implementation added the bonus to `dailyLimit` *and persisted it*, so
     * every challenge claim permanently raised the user's own configured ceiling —
     * claiming five bonuses multiplied their allowance by five. The extra allowance now
     * lives only in [bonusScrolls], is cleared on day rollover, and never touches storage.
     */
    suspend fun unblockAppTemporarily(packageName: String, extraScrolls: Int) {
        if (extraScrolls <= 0) return
        val limit = dao.getLimit(packageName) ?: return

        bonusScrolls.merge(packageName, extraScrolls, Int::plus)
        dao.upsertLimit(limit.copy(isBlocked = false))
    }

    /** The allowance currently in force: the stored limit plus any unspent session bonus. */
    fun effectiveLimit(packageName: String, storedLimit: Int): Int =
        (storedLimit + (bonusScrolls[packageName] ?: 0)).coerceAtLeast(1)

    suspend fun setDailyGoal(goal: Int) {
        dao.setDailyGoal(_trackedDate.value, goal.coerceIn(1, 10_000))
    }
}

/** Canonical short app names, shared by every repository so labels stay consistent. */
object AppRecognitionNames {
    fun displayNameFor(packageName: String): String = when {
        packageName.contains("instagram") -> "Instagram"
        packageName.contains("youtube") -> "YouTube Shorts"
        packageName.contains("musically") ||
            packageName.contains("ugc.trill") ||
            packageName.contains("ugc.aweme") -> "TikTok"
        packageName.contains("facebook") -> "Facebook"
        packageName.contains("snapchat") -> "Snapchat"
        packageName.contains("spotify") -> "Spotify Video"
        else -> "App"
    }
}