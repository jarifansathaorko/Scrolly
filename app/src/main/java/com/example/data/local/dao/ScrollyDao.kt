package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.FriendBattleEntity
import com.example.data.local.entity.ScrollEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Aggregation row for the app-breakdown chart.
 *
 * `totalScrolls` is aliased by SQLite as a `Long`; Room maps it because the declared
 * Kotlin type is `Int`.
 */
data class AppLifetimeTotal(
    val packageName: String,
    val appName: String,
    val totalScrolls: Int
)

/** Per-day (total, goal) pair used to derive the on-streak / best-streak counters. */
data class DayTotals(
    val date: String,
    val totalScrolls: Int,
    val goal: Int
)

/** Battle count grouped by outcome, used for the Profile screen's W/L record. */
data class BattleStatusCount(
    val status: String,
    val count: Int
)

@Dao
interface ScrollyDao {

    // ── Daily stats ───────────────────────────────────────────────────────

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun getDailyStatsFlow(date: String): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getDailyStats(date: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 7")
    fun getRecentDailyStatsFlow(): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY date ASC")
    fun getAllDailyStatsFlow(): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getDailyStatsBetweenFlow(startDate: String, endDate: String): Flow<List<DailyStatsEntity>>

    @Query("SELECT * FROM daily_stats WHERE date LIKE :pattern || '%' ORDER BY date ASC")
    fun getDailyStatsLikeFlow(pattern: String): Flow<List<DailyStatsEntity>>

    /** Lifetime total across every tracked day — drives the Profile screen. */
    @Query("SELECT COALESCE(SUM(totalScrolls), 0) FROM daily_stats")
    fun getLifetimeScrollTotalFlow(): Flow<Int>

    /**
     * Per-day totals for the most recent [limit] days, newest first.
     *
     * A "streak" means consecutive days spent at or under the daily goal, so it cannot
     * be derived from a precomputed column without a scheduled job — it is computed from
     * these rows instead (see `GamificationRepository.streakFrom`).
     */
    @Query("SELECT date, totalScrolls, goal FROM daily_stats ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDayTotals(limit: Int): List<DayTotals>

    /**
     * Atomically adds [delta] to a day's total, creating the row on first write.
     *
     * This replaces a read-modify-write (`getDailyStats` then `upsert`). Under the
     * accessibility thread's high write rate that pattern could interleave two scrolls
     * and persist only one, permanently desynchronising the counter from reality.
     */
    @Query(
        """
        INSERT INTO daily_stats (date, totalScrolls, goal, isGoalMet, xpEarned)
        VALUES (:date, :delta, :goal, 0, 0)
        ON CONFLICT(date) DO UPDATE SET
            totalScrolls = totalScrolls + :delta,
            isGoalMet    = CASE WHEN (totalScrolls + :delta) >= goal THEN 1 ELSE 0 END
        """
    )
    suspend fun incrementDailyScrollCount(date: String, delta: Int, goal: Int)

    @Query(
        """
        INSERT INTO daily_stats (date, totalScrolls, goal, isGoalMet, xpEarned)
        VALUES (:date, 0, :goal, 0, 0)
        ON CONFLICT(date) DO UPDATE SET goal = :goal
        """
    )
    suspend fun setDailyGoal(date: String, goal: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStats(stats: DailyStatsEntity)

    // ── App stats ─────────────────────────────────────────────────────────

    @Query("SELECT * FROM app_stats WHERE date = :date ORDER BY scrollCount DESC")
    fun getAppStatsForDateFlow(date: String): Flow<List<AppStatsEntity>>

    @Query(
        "SELECT packageName, MAX(appName) AS appName, SUM(scrollCount) AS totalScrolls " +
            "FROM app_stats WHERE date = :date GROUP BY packageName ORDER BY totalScrolls DESC"
    )
    fun getAppStatsSummaryForDateFlow(date: String): Flow<List<AppLifetimeTotal>>

    @Query(
        "SELECT packageName, MAX(appName) AS appName, SUM(scrollCount) AS totalScrolls " +
            "FROM app_stats WHERE date >= :startDate AND date <= :endDate " +
            "GROUP BY packageName ORDER BY totalScrolls DESC"
    )
    fun getAppStatsSummaryBetweenFlow(startDate: String, endDate: String): Flow<List<AppLifetimeTotal>>

    @Query(
        "SELECT packageName, MAX(appName) AS appName, SUM(scrollCount) AS totalScrolls " +
            "FROM app_stats WHERE date LIKE :pattern || '%' " +
            "GROUP BY packageName ORDER BY totalScrolls DESC"
    )
    fun getAppStatsSummaryLikeFlow(pattern: String): Flow<List<AppLifetimeTotal>>

    /**
     * Atomically adds [delta] to an app's daily total, creating the row on first write.
     * @see incrementDailyScrollCount for why this is not a read-modify-write.
     */
    @Query(
        """
        INSERT INTO app_stats (date, packageName, appName, scrollCount)
        VALUES (:date, :packageName, :appName, :delta)
        ON CONFLICT(date, packageName) DO UPDATE SET
            scrollCount = scrollCount + :delta,
            appName     = excluded.appName
        """
    )
    suspend fun incrementAppScrollCount(date: String, packageName: String, appName: String, delta: Int)

    @Query("SELECT * FROM app_stats WHERE date = :date AND packageName = :packageName LIMIT 1")
    suspend fun getAppStat(date: String, packageName: String): AppStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppStats(stats: AppStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppStatsList(stats: List<AppStatsEntity>)

    @Query(
        "SELECT packageName, MAX(appName) AS appName, SUM(scrollCount) AS totalScrolls " +
            "FROM app_stats GROUP BY packageName ORDER BY totalScrolls DESC"
    )
    fun getLifetimeAppTotalsFlow(): Flow<List<AppLifetimeTotal>>

    // ── Scroll events ─────────────────────────────────────────────────────

    @Insert
    suspend fun insertScrollEvent(event: ScrollEventEntity)

    @Query("SELECT COUNT(*) FROM scroll_events WHERE timestamp >= :sinceTimestamp")
    suspend fun getScrollCountSince(sinceTimestamp: Long): Int

    /**
     * Raw event timestamps for a single local day.
     *
     * The Stats screen used to *fabricate* a Morning/Afternoon/Evening/Night split by
     * applying fixed 15/35/40% ratios to the day's total. Timestamps are already stored,
     * so the split is now bucketed from real events (in
     * [com.example.data.repository.TrackingRepository.getScrollsByTimeOfDay]) instead of
     * being invented.
     *
     * The range is half-open (`[start, end)`) so the final millisecond of the day is
     * included without double-counting the first millisecond of the next.
     */
    @Query("SELECT timestamp FROM scroll_events WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    suspend fun getScrollTimestampsBetween(startMillis: Long, endMillis: Long): List<Long>

    /**
     * Reactive variant of [getScrollTimestampsBetween].
     *
     * Re-emits whenever `scroll_events` changes, which is what lets the Stats screen's
     * time-of-day chart stay live without polling.
     */
    @Query("SELECT timestamp FROM scroll_events WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun getScrollTimestampsBetweenFlow(startMillis: Long, endMillis: Long): Flow<List<Long>>

    // ── Limits ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM app_limits ORDER BY appName ASC")
    fun getAllLimitsFlow(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimit(packageName: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(limit: AppLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimits(limits: List<AppLimitEntity>)

    /** Clears every "limit reached" latch so a new day starts unblocked. */
    @Query("UPDATE app_limits SET isBlocked = 0")
    suspend fun clearAllBlocks()

    // ── Achievements ──────────────────────────────────────────────────────

    @Query("SELECT * FROM achievements")
    fun getAllAchievementsFlow(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    // ── Friend battles ────────────────────────────────────────────────────

    @Query("SELECT * FROM friend_battles ORDER BY date DESC")
    fun getAllBattlesFlow(): Flow<List<FriendBattleEntity>>

    /** One-shot read, used when recomputing outcomes in a write transaction. */
    @Query("SELECT * FROM friend_battles")
    suspend fun getAllBattlesOnce(): List<FriendBattleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBattles(battles: List<FriendBattleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBattle(battle: FriendBattleEntity)

    @Query("SELECT * FROM friend_battles WHERE id = :battleId LIMIT 1")
    suspend fun getBattle(battleId: String): FriendBattleEntity?

    /** Outcomes grouped by status, so the Profile screen can show a real W/L record. */
    @Query("SELECT status, COUNT(*) AS count FROM friend_battles GROUP BY status")
    fun getBattleRecordGroupedFlow(): Flow<List<BattleStatusCount>>

    @Query("UPDATE friend_battles SET userScrolls = :userScrolls, status = :status WHERE id = :battleId")
    suspend fun updateBattleProgress(battleId: String, userScrolls: Int, status: String)

    @Query("DELETE FROM friend_battles WHERE date < :cutoffDate")
    suspend fun deleteBattlesBefore(cutoffDate: String)

    /**
     * Records one scroll and updates the day's + app totals in a single transaction.
     *
     * Keeping all three writes atomic is what guarantees the dashboard numbers can
     * never diverge: previously a process death between the daily-stat write and the
     * event insert permanently lost the count from one aggregate but not the other.
     */
    @Transaction
    suspend fun recordScrollAtomic(
        date: String,
        packageName: String,
        appName: String,
        delta: Int,
        goal: Int
    ) {
        incrementDailyScrollCount(date, delta, goal)
        incrementAppScrollCount(date, packageName, appName, delta)
        insertScrollEvent(
            ScrollEventEntity(
                timestamp = System.currentTimeMillis(),
                packageName = packageName,
                appName = appName,
                scrollDelta = delta
            )
        )
    }
}