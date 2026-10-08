package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @param isGoalMet `true` once the day's scroll count has reached [goal] — i.e. the
 *   daily allowance is spent. It is **not** "the user succeeded"; it drives the
 *   block/warning UI in [com.example.data.repository.TrackingRepository]. Always kept
 *   equal to `totalScrolls >= goal` so the flag can never disagree with the counter.
 */
@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, // format: YYYY-MM-DD
    val totalScrolls: Int,
    val goal: Int = 100,
    val isGoalMet: Boolean = false,
    val xpEarned: Int = 0
)

/**
 * One row per (day, app).
 *
 * The primary key is the natural composite `(date, packageName)` rather than a
 * surrogate `id`. That matters because every scroll is written on the accessibility
 * thread, and a surrogate key forced the repository into a read-modify-write cycle
 * that could duplicate rows and lose counts. With a composite key the write is a
 * single atomic `INSERT … ON CONFLICT DO UPDATE` (see
 * [com.example.data.local.dao.ScrollyDao.incrementAppScrollCount]).
 */
@Entity(tableName = "app_stats", primaryKeys = ["date", "packageName"])
data class AppStatsEntity(
    val date: String, // YYYY-MM-DD
    val packageName: String,
    val appName: String,
    val scrollCount: Int
)

@Entity(tableName = "scroll_events")
data class ScrollEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val scrollDelta: Int = 1
)

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimit: Int,
    val warningThreshold: Int,
    val isBlocked: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "friend_battles")
data class FriendBattleEntity(
    @PrimaryKey
    val id: String,
    val friendName: String,
    val friendUsername: String,
    val userScrolls: Int,
    val friendScrolls: Int,
    val date: String, // YYYY-MM-DD
    val status: String // "WINNING", "LOSING", "TIED", "COMPLETED"
)