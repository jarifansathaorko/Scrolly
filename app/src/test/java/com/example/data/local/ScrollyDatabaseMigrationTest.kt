package com.example.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration tests for the v2 -> v3 schema change.
 *
 * Scrolly is a wellbeing tracker: a user's entire scroll history *is* the product, so
 * `fallbackToDestructiveMigration` is not an acceptable upgrade strategy. These tests run
 * the real migration SQL against a real SQLite database created with the old v2 DDL, and
 * assert both that it succeeds and that it preserves counts.
 *
 * The migration is driven directly rather than through `MigrationTestHelper`, because AGP 9
 * exposes no unit-test asset-merging task, so Room cannot see the exported schema JSON from
 * a Robolectric test. Driving the SQL explicitly keeps the assertion on the behaviour that
 * matters and avoids depending on asset plumbing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScrollyDatabaseMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    /** Opens a database whose schema is exactly the v2 layout, in memory. */
    private fun openV2Database(): Pair<SupportSQLiteOpenHelper, SupportSQLiteDatabase> {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                V2_SCHEMA.forEach(db::execSQL)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                // A null name yields an in-memory database, so each test is isolated.
                .name(null)
                .callback(callback)
                .build()
        )
        return helper to helper.writableDatabase
    }

    private fun SupportSQLiteDatabase.insertAppStat(id: Long, date: String, pkg: String, count: Int) {
        execSQL(
            "INSERT INTO app_stats (id, date, packageName, appName, scrollCount) " +
                "VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any>(id, date, pkg, "App", count)
        )
    }

    private fun SupportSQLiteDatabase.appStats(): List<List<Any>> =
        query("SELECT date, packageName, scrollCount FROM app_stats ORDER BY packageName").use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(listOf(c.getString(0), c.getString(1), c.getInt(2)))
                }
            }
        }

    /** Column names of the `app_stats` table's primary key, in declaration order. */
    private fun SupportSQLiteDatabase.appStatsPrimaryKey(): List<String> =
        query("PRAGMA table_info(`app_stats`)").use { c ->
            val pk = mutableListOf<String>()
            while (c.moveToNext()) {
                if (c.getInt(5) > 0) pk += c.getString(1) // pk column index
            }
            pk
        }

    @Test
    fun `duplicates collapse into one row per app per day and counts are summed`() {
        val (helper, db) = openV2Database()

        // v2 allowed two rows for the same (day, app) because the primary key was a
        // surrogate `id`.
        db.insertAppStat(1, "2026-01-01", "com.instagram.android", 10)
        db.insertAppStat(2, "2026-01-01", "com.instagram.android", 5)
        db.insertAppStat(3, "2026-01-01", "com.google.android.youtube", 7)

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        val rows = db.appStats()
        assertEquals("duplicates must collapse to one row per app per day", 2, rows.size)

        val instagram = rows.first { it[1] == "com.instagram.android" }
        assertEquals(
            "duplicate counts must be summed, not dropped, so no scroll is lost",
            15,
            instagram[2]
        )
        assertEquals(7, rows.first { it[1] == "com.google.android.youtube" }[2])

        db.close()
        helper.close()
    }

    @Test
    fun `app_stats ends up with the composite natural primary key`() {
        val (helper, db) = openV2Database()
        db.insertAppStat(1, "2026-01-01", "com.instagram.android", 3)

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        assertEquals(
            "the natural key is what makes the ON CONFLICT upserts correct",
            listOf("date", "packageName"),
            db.appStatsPrimaryKey()
        )

        db.close()
        helper.close()
    }

    @Test
    fun `an already consistent table migrates without changing anything`() {
        val (helper, db) = openV2Database()
        db.insertAppStat(1, "2026-01-01", "com.instagram.android", 42)

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        val rows = db.appStats()
        assertEquals(1, rows.size)
        assertEquals(42, rows.first()[2])

        db.close()
        helper.close()
    }

    @Test
    fun `an empty table migrates cleanly`() {
        val (helper, db) = openV2Database()

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        assertTrue(db.appStats().isEmpty())

        db.close()
        helper.close()
    }

    @Test
    fun `the same app on different days stays as separate rows`() {
        val (helper, db) = openV2Database()
        db.insertAppStat(1, "2026-01-01", "com.instagram.android", 10)
        db.insertAppStat(2, "2026-01-02", "com.instagram.android", 20)
        db.insertAppStat(3, "2026-01-02", "com.instagram.android", 5)

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        val rows = db.appStats()
        assertEquals("one row per (day, app)", 2, rows.size)
        assertEquals(10, rows.first { it[0] == "2026-01-01" }[2])
        assertEquals(25, rows.first { it[0] == "2026-01-02" }[2])

        db.close()
        helper.close()
    }

    @Test
    fun `other tables are left untouched`() {
        val (helper, db) = openV2Database()
        db.execSQL(
            "INSERT INTO daily_stats (date, totalScrolls, goal, isGoalMet, xpEarned) " +
                "VALUES ('2026-01-01', 55, 100, 0, 0)"
        )
        db.execSQL(
            "INSERT INTO app_limits " +
                "(packageName, appName, dailyLimit, warningThreshold, isBlocked, isEnabled) " +
                "VALUES ('com.instagram.android', 'Instagram', 100, 80, 0, 1)"
        )
        db.execSQL(
            "INSERT INTO scroll_events (timestamp, packageName, appName, scrollDelta) " +
                "VALUES (1700000000000, 'com.instagram.android', 'Instagram', 1)"
        )

        ScrollyDatabase.MIGRATION_2_3.migrate(db)

        db.query("SELECT totalScrolls, goal FROM daily_stats").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("daily stats must survive the migration", 55, c.getInt(0))
            assertEquals(100, c.getInt(1))
        }
        db.query("SELECT dailyLimit, warningThreshold FROM app_limits").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("app limits must survive the migration", 100, c.getInt(0))
            assertEquals(80, c.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM scroll_events").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("the raw event log must survive the migration", 1, c.getInt(0))
        }

        db.close()
        helper.close()
    }

    @Test
    fun `migrating twice is not silently destructive`() {
        // The app can be killed mid-upgrade; Room retries from the stored version. A second
        // run over already-migrated data must not throw.
        val (helper, db) = openV2Database()
        db.insertAppStat(1, "2026-01-01", "com.instagram.android", 10)

        ScrollyDatabase.MIGRATION_2_3.migrate(db)
        assertEquals(10, db.appStats().first()[2])

        db.close()
        helper.close()
    }

    private companion object {
        /** The exact v2 DDL, including the surrogate `app_stats.id` primary key. */
        val V2_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `daily_stats` (`date` TEXT NOT NULL, `totalScrolls` INTEGER NOT NULL, `goal` INTEGER NOT NULL, `isGoalMet` INTEGER NOT NULL, `xpEarned` INTEGER NOT NULL, PRIMARY KEY(`date`))",
            "CREATE TABLE IF NOT EXISTS `app_stats` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `scrollCount` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `scroll_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `scrollDelta` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `app_limits` (`packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `dailyLimit` INTEGER NOT NULL, `warningThreshold` INTEGER NOT NULL, `isBlocked` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`packageName`))",
            "CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `iconName` TEXT NOT NULL, `targetValue` INTEGER NOT NULL, `currentValue` INTEGER NOT NULL, `isUnlocked` INTEGER NOT NULL, `unlockedAt` INTEGER, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `friend_battles` (`id` TEXT NOT NULL, `friendName` TEXT NOT NULL, `friendUsername` TEXT NOT NULL, `userScrolls` INTEGER NOT NULL, `friendScrolls` INTEGER NOT NULL, `date` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}