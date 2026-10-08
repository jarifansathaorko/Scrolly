package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ScrollyDatabase
import com.example.data.local.dao.DayTotals
import com.example.data.local.dao.ScrollyDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.FriendBattleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.Locale

/**
 * The user's own stats.
 *
 * Every field is derived from tracked data. The previous version returned hardcoded
 * literals (14,832 lifetime scrolls, an 11-day best streak, a 23W/17L record), so the
 * Profile screen showed numbers that could never change regardless of what the user did
 * and could not be reconciled with the Stats tab.
 */
data class UserProfile(
    val username: String,
    val totalScrollsAllTime: Int,
    val streakDays: Int,
    val bestStreak: Int,
    val activeDays: Int,
    val battlesWon: Int,
    val battlesLost: Int,
    val battlesTied: Int,
    val currentLevel: Int,
    val currentXp: Int,
    val xpForNextLevel: Int
) {
    /** e.g. `"23 / 17"` — omits a zero-loss tail so `12 / 0` does not read as a loss. */
    val battleRecord: String
        get() = if (battlesLost > 0) "$battlesWon / $battlesLost" else battlesWon.toString()

    val hasBattles: Boolean
        get() = battlesWon + battlesLost + battlesTied > 0
}

data class HealthyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val rewardScrolls: Int,
    val rewardXp: Int,
    val iconName: String,
    val durationMinutes: Int,
    val isCompleted: Boolean = false
)

class GamificationRepository(
    private val dao: ScrollyDao,
    context: Context,
    private val firebaseManager: FirebaseManager,
    private val trackingRepository: TrackingRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scrolly_prefs", Context.MODE_PRIVATE)

    private val _challenges = MutableStateFlow(buildChallenges())

    /**
     * Healthy challenges and their completion state.
     *
     * Previously a plain `List` property, so completion could not be represented and the
     * Claim button could be pressed unlimited times for unlimited bonus scrolls.
     */
    val challenges: StateFlow<List<HealthyChallenge>> = _challenges.asStateFlow()

    /** Real, data-derived profile statistics. */
    val userProfile: Flow<UserProfile> = combine(
        trackingRepository.getLifetimeScrollTotalFlow(),
        trackingRepository.getAllDailyStatsFlow(),
        dao.getBattleRecordGroupedFlow()
    ) { lifetime, dailyStats, battleGroups ->
        val totals = dailyStats.map { DayTotals(it.date, it.totalScrolls, it.goal) }
        val (current, best) = streaksFrom(totals)
        val level = levelForXp(lifetime)

        UserProfile(
            username = storedUsername(),
            totalScrollsAllTime = lifetime,
            streakDays = current,
            bestStreak = best,
            activeDays = dailyStats.count { it.totalScrolls > 0 },
            battlesWon = battleGroups.firstOrNull { it.status == BATTLE_WINNING }?.count ?: 0,
            battlesLost = battleGroups.firstOrNull { it.status == BATTLE_LOSING }?.count ?: 0,
            battlesTied = battleGroups.firstOrNull { it.status == BATTLE_TIED }?.count ?: 0,
            currentLevel = level.first,
            currentXp = level.second,
            xpForNextLevel = level.third
        )
    }

    fun getAchievementsFlow(): Flow<List<AchievementEntity>> = dao.getAllAchievementsFlow()

    /**
     * Marks [challengeId] complete and grants its scroll bonus.
     *
     * Returns `false` when the challenge was already claimed, which is what stops the
     * Claim button from being spammable for unlimited extra scrolls.
     */
    suspend fun completeChallenge(challengeId: String, packageName: String): Boolean {
        val challenge = _challenges.value.firstOrNull { it.id == challengeId } ?: return false
        if (challenge.isCompleted) return false

        trackingRepository.unblockAppTemporarily(packageName, challenge.rewardScrolls)
        _challenges.value = _challenges.value.map {
            if (it.id == challengeId) it.copy(isCompleted = true) else it
        }
        return true
    }

    /** Clears claim state, e.g. on a new day. */
    fun resetChallenges() {
        _challenges.value = buildChallenges()
    }

    fun storedUsername(): String =
        prefs.getString(KEY_USERNAME, DEFAULT_USERNAME) ?: DEFAULT_USERNAME

    suspend fun updateUsername(newUsername: String) {
        val trimmed = newUsername.trim().removePrefix("@")
        if (trimmed.isEmpty()) return
        prefs.edit().putString(KEY_USERNAME, trimmed).apply()
        firebaseManager.updateUsername(trimmed)
    }

    private fun buildChallenges(): List<HealthyChallenge> = listOf(
        HealthyChallenge(
            id = CHALLENGE_WALK,
            title = "Touch Grass Walk",
            description = "Step outside or walk around for $WALK_MINUTES minutes without checking screens.",
            rewardScrolls = 15,
            rewardXp = 50,
            iconName = "walk",
            durationMinutes = WALK_MINUTES
        ),
        HealthyChallenge(
            id = CHALLENGE_PHONE_DOWN,
            title = "Phone-Down Reset",
            description = "Place your phone face down for 10 peaceful minutes.",
            rewardScrolls = 20,
            rewardXp = 75,
            iconName = "phone",
            durationMinutes = 10
        ),
        HealthyChallenge(
            id = CHALLENGE_BREATHS,
            title = "Box Breathing Reset",
            description = "Take 8 deep intentional breaths to calm attention and reset dopamine.",
            rewardScrolls = 10,
            rewardXp = 30,
            iconName = "wind",
            durationMinutes = 2
        ),
        HealthyChallenge(
            id = CHALLENGE_STRETCH,
            title = "Desk Posture Stretch",
            description = "Release neck and thumb strain with gentle shoulder rolls & wrist stretches.",
            rewardScrolls = 12,
            rewardXp = 40,
            iconName = "stretch",
            durationMinutes = 3
        )
    )

    companion object {
        const val DEFAULT_USERNAME = "you"
        private const val KEY_USERNAME = "username"

        const val CHALLENGE_WALK = "walk_5"
        const val CHALLENGE_PHONE_DOWN = "phone_down_10"
        const val CHALLENGE_BREATHS = "deep_breaths"
        const val CHALLENGE_STRETCH = "stretch_3"

        private const val WALK_MINUTES = 5

        const val BATTLE_WINNING = "WINNING"
        const val BATTLE_LOSING = "LOSING"
        const val BATTLE_TIED = "TIED"

        /** Cumulative XP required to *reach* [level]; level 1 starts at 0 XP. */
        fun xpRequiredForLevel(level: Int): Int {
            val n = level.coerceIn(1, 500)
            // Triangular numbers: 1_000, 3_000, 6_000, 10_000 … each level costs 1_000 more.
            return 1_000 * (n - 1) * n / 2
        }

        /**
         * Maps a lifetime scroll count onto `(level, xpIntoLevel, xpForNextLevel)`.
         *
         * Uses a real curve rather than the previous fixed "level 8 / 3,450 XP", which
         * was the same for every user regardless of their history.
         */
        fun levelForXp(totalXp: Int): Triple<Int, Int, Int> {
            val xp = totalXp.coerceAtLeast(0)
            var level = 1
            while (level < 500 && xp >= xpRequiredForLevel(level + 1)) level++
            val floorXp = xpRequiredForLevel(level)
            val nextXp = xpRequiredForLevel(level + 1)
            return Triple(level, xp - floorXp, (nextXp - floorXp).coerceAtLeast(1))
        }

        /**
         * Current and best run of consecutive days spent at or under the daily goal.
         *
         * A day qualifies when `totalScrolls <= goal`. Today not yet having a row (a
         * fresh install, or midnight before the first scroll) does not break a streak
         * that was intact through yesterday — otherwise every morning before the first
         * scroll would silently reset the user's streak.
         */
        fun streaksFrom(totals: List<DayTotals>): Pair<Int, Int> {
            val byDate = totals.associate { it.date to it }

            val today = ScrollyDatabase.getTodayDate()
            val yesterday = ScrollyDatabase.getDateOffset(-1)
            val cursor = when {
                byDate.containsKey(today) -> today
                byDate.containsKey(yesterday) -> yesterday
                else -> return 0 to 0
            }

            fun qualifies(date: String) = byDate[date]?.let { it.totalScrolls <= it.goal } == true

            var current = 0
            var cursorDate = cursor
            while (qualifies(cursorDate)) {
                current++
                val previous = previousDate(cursorDate)
                if (byDate[previous] == null) break
                cursorDate = previous
            }

            // Best streak: the longest consecutive qualifying run anywhere in history.
            var best = 0
            var run = 0
            totals.sortedBy { it.date }.forEach { row ->
                if (row.totalScrolls <= row.goal) {
                    run++
                    if (run > best) best = run
                } else {
                    run = 0
                }
            }

            return current to maxOf(best, current)
        }

        private fun previousDate(date: String): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val cal = Calendar.getInstance()
            return runCatching {
                cal.time = sdf.parse(date)!!
                cal.add(Calendar.DAY_OF_YEAR, -1)
                sdf.format(cal.time)
            }.getOrDefault(date)
        }
    }
}

/**
 * Friend battles.
 *
 * Outcomes are recomputed from both participants' real scroll counts rather than
 * trusted from a stored string, so a battle can never drift out of sync with the
 * numbers it summarises.
 */
class SocialRepository(
    private val dao: ScrollyDao,
    private val firebaseManager: FirebaseManager,
    private val trackingRepository: TrackingRepository
) {
    fun getBattlesFlow(): Flow<List<FriendBattleEntity>> = dao.getAllBattlesFlow()

    /** Creates or replaces a battle. */
    suspend fun upsertBattle(battle: FriendBattleEntity) = dao.upsertBattle(battle)

    /** Removes battles older than [cutoffDate], keeping the table from growing forever. */
    suspend fun pruneBattlesOlderThan(cutoffDate: String) = dao.deleteBattlesBefore(cutoffDate)

    /** Lower scroll count wins, so `LOSING` means the user scrolled more. */
    fun statusFor(userScrolls: Int, friendScrolls: Int): String = when {
        userScrolls < friendScrolls -> GamificationRepository.BATTLE_WINNING
        userScrolls > friendScrolls -> GamificationRepository.BATTLE_LOSING
        else -> GamificationRepository.BATTLE_TIED
    }

    /**
     * Recomputes and persists every battle's outcome from the user's live total.
     *
     * The previous implementation computed a `status` and then discarded it —
     * `val existing = dao.getAllBattlesFlow()` was dead code and no write ever happened,
     * so a battle's result never changed however many scrolls either side recorded.
     */
    suspend fun refreshBattleOutcomes() {
        val userScrolls = trackingRepository.todayTotalScrolls.value
        dao.getAllBattlesOnce().forEach { battle ->
            val status = statusFor(userScrolls, battle.friendScrolls)
            if (status != battle.status || battle.userScrolls != userScrolls) {
                dao.updateBattleProgress(
                    battleId = battle.id,
                    userScrolls = userScrolls,
                    status = status
                )
            }
        }
    }

    suspend fun recordBattleResult(battleId: String, userFinal: Int, friendFinal: Int) {
        if (dao.getBattle(battleId) == null) return
        dao.updateBattleProgress(
            battleId = battleId,
            userScrolls = userFinal,
            status = statusFor(userFinal, friendFinal)
        )
    }

    suspend fun connectFriend(friendUid: String): Boolean = firebaseManager.connectFriend(friendUid)

    suspend fun getFriends(): List<FriendData> = firebaseManager.getFriends()
}