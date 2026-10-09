package com.example.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.local.DateKeys
import com.example.data.local.ScrollyDatabase
import com.example.data.local.dao.ScrollyDao
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.ScrollEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LimitReachedEvent(
    val packageName: String,
    val appName: String,
    val currentScrolls: Int,
    val limit: Int
)

/** Runs [block] atomically: either every write inside it is applied, or none is. */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

/** Runs [block] inside a Room transaction. This is what the app uses. */
class RoomTransactionRunner(private val db: RoomDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction { block() }
}

/** Runs [block] with no transaction. Only for tests/fakes; production code must pass [RoomTransactionRunner]. */
object DirectTransactionRunner : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
}

class TrackingRepository(
    private val dao: ScrollyDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val transactions: TransactionRunner = DirectTransactionRunner
) {
    private val mutex = Mutex()
    private val _limitReachedEvents = MutableSharedFlow<LimitReachedEvent>(extraBufferCapacity = 5)
    val limitReachedEvents = _limitReachedEvents.asSharedFlow()

    private val counter = DailyCounter()
    private val _todayTotalScrolls = MutableStateFlow(0)
    val todayTotalScrolls = _todayTotalScrolls.asStateFlow()

    /** The current local day as a `yyyy-MM-dd` key. Advances itself at midnight. */
    private val _todayKey = MutableStateFlow(DateKeys.day())
    val todayKey: StateFlow<String> = _todayKey.asStateFlow()

    init {
        // Advance the day key at local midnight. The sleep is capped so a clock or time-zone change,
        // or a late wake-up after Doze, is corrected within MAX_MIDNIGHT_POLL_MS.
        scope.launch {
            while (true) {
                delay(DateKeys.millisUntilNextDay().coerceAtMost(MAX_MIDNIGHT_POLL_MS))
                _todayKey.value = DateKeys.day()
            }
        }
        // Keep the live counter anchored to the database for whichever day is "today".
        scope.launch {
            todayTotals().collect { (date, total) -> publish { counter.anchor(date, total) } }
        }
    }

    /** `(date, total scrolls stored for that date)`, re-subscribing whenever the day changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun todayTotals(): Flow<Pair<String, Int>> =
        todayKey.flatMapLatest { date ->
            dao.getDailyStatsFlow(date).map { stats -> date to (stats?.totalScrolls ?: 0) }
        }

    /** Applies [change] to [counter] and publishes the result, atomically with respect to other callers. */
    private inline fun publish(change: () -> Int) {
        synchronized(counter) { _todayTotalScrolls.value = change() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayStatsFlow(): Flow<DailyStatsEntity?> =
        todayKey.flatMapLatest { date -> dao.getDailyStatsFlow(date) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayAppStatsFlow(): Flow<List<AppStatsEntity>> =
        todayKey.flatMapLatest { date -> dao.getAppStatsForDateFlow(date) }

    fun getDailyStatsFlow(date: String): Flow<DailyStatsEntity?> {
        return dao.getDailyStatsFlow(date)
    }

    fun getWeeklyStatsFlow(): Flow<List<DailyStatsEntity>> {
        return dao.getRecentDailyStatsFlow()
    }

    fun getAllDailyStatsFlow(): Flow<List<DailyStatsEntity>> {
        return dao.getAllDailyStatsFlow()
    }

    fun getDailyStatsBetweenFlow(startDate: String, endDate: String): Flow<List<DailyStatsEntity>> {
        return dao.getDailyStatsBetweenFlow(startDate, endDate)
    }

    fun getDailyStatsLikeFlow(pattern: String): Flow<List<DailyStatsEntity>> {
        return dao.getDailyStatsLikeFlow(pattern)
    }

    fun getAppStatsSummaryForDateFlow(date: String): Flow<List<com.example.data.local.dao.AppLifetimeTotal>> {
        return dao.getAppStatsSummaryForDateFlow(date)
    }

    fun getAppStatsSummaryBetweenFlow(startDate: String, endDate: String): Flow<List<com.example.data.local.dao.AppLifetimeTotal>> {
        return dao.getAppStatsSummaryBetweenFlow(startDate, endDate)
    }

    fun getAppStatsSummaryLikeFlow(pattern: String): Flow<List<com.example.data.local.dao.AppLifetimeTotal>> {
        return dao.getAppStatsSummaryLikeFlow(pattern)
    }

    fun getAppLimitsFlow(): Flow<List<AppLimitEntity>> {
        return dao.getAllLimitsFlow()
    }

    private class ScrollOutcome(val dailyTotal: Int, val limitEvent: LimitReachedEvent?)

    /**
     * Records [delta] scrolls for [packageName] against the day the scroll happened on.
     *
     * The live counter is bumped immediately so the UI and island feel instant, then the three
     * related rows (daily total, per-app total, raw event) plus the limit check are written in a
     * single transaction. If that write fails or is cancelled the optimistic bump is rolled back
     * and the exception is rethrown, so callers still see the failure.
     */
    suspend fun recordScroll(packageName: String, appName: String, delta: Int = 1) {
        if (delta <= 0) return
        val date = DateKeys.day()
        if (date > _todayKey.value) _todayKey.value = date // timer was late; don't show a stale day
        publish { counter.add(date, delta) }

        val outcome = try {
            mutex.withLock {
                transactions.inTransaction { persistScroll(date, packageName, appName, delta) }
            }
        } catch (t: Throwable) {
            publish { counter.rollback(date, delta) }
            throw t
        }

        publish { counter.commit(date, delta, outcome.dailyTotal) }
        // Emitted outside the lock so a slow collector can never stall scroll recording.
        outcome.limitEvent?.let { _limitReachedEvents.emit(it) }
    }

    private suspend fun persistScroll(
        date: String,
        packageName: String,
        appName: String,
        delta: Int
    ): ScrollOutcome {
        // 1. Daily total
        val currentDaily = dao.getDailyStats(date) ?: DailyStatsEntity(
            date = date,
            totalScrolls = 0,
            goal = 100,
            isGoalMet = false,
            xpEarned = 0
        )
        val newTotal = currentDaily.totalScrolls + delta
        dao.upsertDailyStats(
            currentDaily.copy(totalScrolls = newTotal, isGoalMet = newTotal <= currentDaily.goal)
        )

        // 2. Per-app total
        val currentAppStat = dao.getAppStat(date, packageName) ?: AppStatsEntity(
            date = date,
            packageName = packageName,
            appName = appName,
            scrollCount = 0
        )
        val newAppScrolls = currentAppStat.scrollCount + delta
        dao.upsertAppStats(currentAppStat.copy(scrollCount = newAppScrolls))

        // 3. Raw event
        dao.insertScrollEvent(
            ScrollEventEntity(packageName = packageName, appName = appName, scrollDelta = delta)
        )

        // 4. Limit state. `isBlocked` is a function of today's count versus the limit, so it is also
        //    cleared once the count is back under the limit (a new day, or a raised limit).
        var limitEvent: LimitReachedEvent? = null
        val limit = dao.getLimit(packageName)
        if (limit != null && limit.isEnabled) {
            val overLimit = newAppScrolls >= limit.dailyLimit
            if (overLimit && !limit.isBlocked) {
                dao.upsertLimit(limit.copy(isBlocked = true))
                limitEvent = LimitReachedEvent(
                    packageName = packageName,
                    appName = appName,
                    currentScrolls = newAppScrolls,
                    limit = limit.dailyLimit
                )
            } else if (!overLimit && limit.isBlocked) {
                dao.upsertLimit(limit.copy(isBlocked = false))
            }
        }
        return ScrollOutcome(newTotal, limitEvent)
    }

    suspend fun updateAppLimit(packageName: String, newLimit: Int, warningThreshold: Int, isEnabled: Boolean) {
        val existing = dao.getLimit(packageName) ?: AppLimitEntity(
            packageName = packageName,
            appName = when (packageName) {
                "com.instagram.android" -> "Instagram"
                "com.google.android.youtube" -> "YouTube Shorts"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme" -> "TikTok"
                "com.facebook.katana" -> "Facebook"
                "com.snapchat.android" -> "Snapchat"
                else -> "App"
            },
            dailyLimit = newLimit,
            warningThreshold = warningThreshold,
            isEnabled = isEnabled
        )
        dao.upsertLimit(
            existing.copy(
                dailyLimit = newLimit,
                warningThreshold = warningThreshold,
                isEnabled = isEnabled,
                isBlocked = false // Reset block when limit is adjusted
            )
        )
    }

    suspend fun unblockAppTemporarily(packageName: String, extraScrolls: Int) {
        val limit = dao.getLimit(packageName) ?: return
        dao.upsertLimit(
            limit.copy(
                dailyLimit = limit.dailyLimit + extraScrolls,
                isBlocked = false
            )
        )
    }

    suspend fun setDailyGoal(goal: Int) {
        val today = ScrollyDatabase.getTodayDate()
        val currentDaily = dao.getDailyStats(today) ?: DailyStatsEntity(
            date = today,
            totalScrolls = 0,
            goal = goal
        )
        dao.upsertDailyStats(currentDaily.copy(goal = goal))
    }

    private companion object {
        /** Upper bound on how long the midnight watcher sleeps between checks. */
        const val MAX_MIDNIGHT_POLL_MS = 15 * 60 * 1000L
    }
}
