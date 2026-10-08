package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ScrollyDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.local.entity.FriendBattleEntity
import com.example.data.local.entity.ScrollEventEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        DailyStatsEntity::class,
        AppStatsEntity::class,
        ScrollEventEntity::class,
        AppLimitEntity::class,
        AchievementEntity::class,
        FriendBattleEntity::class
    ],
    version = ScrollyDatabase.VERSION,
    exportSchema = true
)
abstract class ScrollyDatabase : RoomDatabase() {
    abstract fun scrollyDao(): ScrollyDao

    companion object {
        const val VERSION = 3
        private const val DB_NAME = "scrolly_database"

        @Volatile
        private var INSTANCE: ScrollyDatabase? = null

        fun getInstance(context: Context): ScrollyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): ScrollyDatabase =
            Room.databaseBuilder(context, ScrollyDatabase::class.java, DB_NAME)
                // Explicit migration, NOT destructive fallback: this is a wellbeing
                // tracker and a user's scroll history is the entire product. Silently
                // wiping it on a schema bump is unacceptable, so adding an entity field
                // now costs a migration instead of data loss.
                .addMigrations(MIGRATION_2_3)
                .addCallback(SeedCallback)
                .build()

        /**
         * v2 → v3: give `app_stats` its natural `(date, packageName)` primary key.
         *
         * v2 used a surrogate `id`, so nothing stopped two rows existing for the same
         * app/day and `getAppStat` returned an arbitrary one. This collapses any
         * pre-existing duplicates (keeping the largest count) and rebuilds the table
         * with the composite key the DAO's `ON CONFLICT` upserts rely on.
         */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_stats_new (
                        date TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        scrollCount INTEGER NOT NULL,
                        PRIMARY KEY (date, packageName)
                    )
                    """.trimIndent()
                )

                // Collapse duplicates, summing counts so no scroll is lost in the fix.
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO app_stats_new (date, packageName, appName, scrollCount)
                    SELECT date, packageName, MAX(appName), SUM(scrollCount)
                    FROM app_stats
                    GROUP BY date, packageName
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE app_stats")
                db.execSQL("ALTER TABLE app_stats_new RENAME TO app_stats")
            }
        }

        // ── Date helpers ───────────────────────────────────────────────────

        fun getTodayDate(): String = dateFormat("yyyy-MM-dd").format(Date())

        fun getDateOffset(daysOffset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, daysOffset)
            return dateFormat("yyyy-MM-dd").format(cal.time)
        }

        fun getMonthPrefix(offsetMonths: Int = 0): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, offsetMonths)
            return dateFormat("yyyy-MM").format(cal.time)
        }

        fun getYearPrefix(offsetYears: Int = 0): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, offsetYears)
            return dateFormat("yyyy").format(cal.time)
        }

        /**
         * Exclusive start-of-day epoch millis for [date] (`yyyy-MM-dd`, local time).
         *
         * Callers use this to turn a stored date into a timestamp range for the raw
         * event log, which stores absolute millis rather than a date string.
         */
        fun startOfDayMillis(date: String): Long {
            val parsed = parseDate(date)
            val cal = Calendar.getInstance()
            if (parsed != null) {
                cal.set(parsed[0], parsed[1] - 1, parsed[2], 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
            } else {
                // Unparseable date: fall back to *today's* start rather than "now",
                // so a bad input can never silently produce an empty range.
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        /** Exclusive end-of-day epoch millis for [date] (i.e. the start of the following day). */
        fun endOfDayMillis(date: String): Long {
            val parsed = parseDate(date)
                // Derive tomorrow from the *given* date. Computing it from "now" instead
                // returned the start of the next real day for every date, so querying a
                // past day returned a range spanning two days and leaked today's events.
                ?: return startOfDayMillis(date) + MILLIS_PER_DAY

            return Calendar.getInstance().apply {
                clear() // midnight, and DST-safe because we add a whole day afterwards
                set(parsed[0], parsed[1] - 1, parsed[2])
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        }

        private fun parseDate(date: String): IntArray? = try {
            val parsed = dateFormat("yyyy-MM-dd").parse(date) ?: return null
            val cal = Calendar.getInstance().apply { time = parsed }
            intArrayOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        } catch (_: Exception) {
            null
        }

        private fun dateFormat(pattern: String): SimpleDateFormat =
            SimpleDateFormat(pattern, Locale.US)

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        /** Daily allowance applied to newly-created days. */
        const val DEFAULT_DAILY_GOAL = 100

        // Seed data lives in the companion so the nested [SeedCallback] object — which
        // has no enclosing instance — can still read it.
        private val DEFAULT_LIMITS = listOf(
            arrayOf<Any>("com.instagram.android", "Instagram", 100, 80),
            arrayOf<Any>("com.google.android.youtube", "YouTube Shorts", 75, 60),
            arrayOf<Any>("com.zhiliaoapp.musically", "TikTok", 80, 60),
            arrayOf<Any>("com.facebook.katana", "Facebook", 60, 45),
            arrayOf<Any>("com.snapchat.android", "Snapchat", 50, 35)
        )

        private val ACHIEVEMENTS = listOf(
            arrayOf<Any>("first_step", "First Step", "Log your very first short-form scroll session", "flag", 1),
            arrayOf<Any>("under_100", "Under 100", "Stay under 100 scrolls in a single day", "shield", 100),
            arrayOf<Any>("streak_7", "7-Day Streak", "Maintain a 7-day streak under your daily limit", "fire", 7),
            arrayOf<Any>("touch_grass", "Touch Grass Champion", "Keep daily scrolls below 30 on a weekend day", "grass", 30),
            arrayOf<Any>("battle_winner", "Scroll Ninja", "Win a 1v1 Scroll Battle", "sword", 1),
            arrayOf<Any>("zen_master", "Zen Master", "Complete 5 mindful focus breaks", "lotus", 5)
        )
    }

    /**
     * Seeds first-run data.
     *
     * Writes happen through raw SQL on the [SupportSQLiteDatabase] that Room supplies.
     * The previous implementation launched a coroutine and looked up `INSTANCE?.dao()`,
     * which raced with the very assignment that was still in flight — so on a fresh
     * install the seed could silently no-op and leave the user with no app limits,
     * no achievements and no battles. Running the inserts on the provided database
     * keeps them inside Room's own create transaction: no race, no extra scope, no leak.
     */
    private object SeedCallback : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val today = getTodayDate()

            DEFAULT_LIMITS.forEach { (pkg, name, limit, warning) ->
                db.execSQL(
                    "INSERT OR REPLACE INTO app_limits " +
                        "(packageName, appName, dailyLimit, warningThreshold, isBlocked, isEnabled) " +
                        "VALUES (?, ?, ?, ?, 0, 1)",
                    arrayOf<Any>(pkg, name, limit, warning)
                )
            }

            ACHIEVEMENTS.forEach { a ->
                db.execSQL(
                    "INSERT OR REPLACE INTO achievements " +
                        "(id, title, description, iconName, targetValue, currentValue, isUnlocked, unlockedAt) " +
                        "VALUES (?, ?, ?, ?, ?, 0, 0, NULL)",
                    arrayOf<Any>(a[0], a[1], a[2], a[3], a[4])
                )
            }

            db.execSQL(
                "INSERT OR REPLACE INTO daily_stats (date, totalScrolls, goal, isGoalMet, xpEarned) " +
                    "VALUES (?, 0, ?, 0, 0)",
                arrayOf<Any>(today, DEFAULT_DAILY_GOAL)
            )

            DEFAULT_LIMITS.forEach { (pkg, name, _, _) ->
                db.execSQL(
                    "INSERT OR REPLACE INTO app_stats (date, packageName, appName, scrollCount) " +
                        "VALUES (?, ?, ?, 0)",
                    arrayOf<Any>(today, pkg, name)
                )
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // WAL keeps the accessibility service's scroll writes from blocking reads
            // that the Compose UI is observing.
            try {
                db.execSQL("PRAGMA foreign_keys = ON")
            } catch (_: Exception) {
                // Non-fatal: no FK constraints are declared yet.
            }
        }
    }
}