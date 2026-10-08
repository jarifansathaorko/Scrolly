package com.example

import com.example.data.local.ScrollyDatabase
import com.example.data.repository.AppRecognitionNames
import com.example.data.repository.NotchShape
import com.example.tracking.detector.AppRecognitionEngine
import com.example.tracking.detector.DisplayCutoutDetectionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Host-side tests for the pure logic sitting between the detector and the UI: app-name
 * resolution, notch classification, and the date arithmetic the whole statistics layer is
 * keyed on.
 *
 * This file previously asserted `2 + 2 == 4`, which gave no signal and concealed the fact
 * that none of the above had any coverage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRecognitionAndCutoutTest {

    // ── App recognition ───────────────────────────────────────────────────

    @Test
    fun `supported packages resolve to their feed accurate names`() {
        assertEquals("Instagram Reels", AppRecognitionEngine.getAppName("com.instagram.android"))
        assertEquals("YouTube Shorts", AppRecognitionEngine.getAppName("com.google.android.youtube"))
        assertEquals("TikTok", AppRecognitionEngine.getAppName("com.zhiliaoapp.musically"))
        assertEquals("Facebook Reels", AppRecognitionEngine.getAppName("com.facebook.katana"))
        assertEquals("Snapchat Spotlight", AppRecognitionEngine.getAppName("com.snapchat.android"))
        assertEquals("Spotify Video", AppRecognitionEngine.getAppName("com.spotify.music"))
    }

    @Test
    fun `regional and lite variants resolve through the detector`() {
        assertEquals("Instagram Reels", AppRecognitionEngine.getAppName("com.instagram.lite"))
        assertEquals("TikTok", AppRecognitionEngine.getAppName("com.ss.android.ugc.trill"))
        assertEquals("TikTok", AppRecognitionEngine.getAppName("com.ss.android.ugc.aweme"))
        assertEquals("TikTok", AppRecognitionEngine.getAppName("com.zhiliaoapp.musically.go"))
        assertEquals("Facebook Reels", AppRecognitionEngine.getAppName("com.facebook.lite"))
        assertEquals("Spotify Video", AppRecognitionEngine.getAppName("com.spotify.lite"))
    }

    @Test
    fun `an untracked package still resolves to a readable name`() {
        // The Block screen lets a user set a limit for an app the detector has never
        // seen, and the Battle screen stores names it receives, so a useful fallback must
        // exist rather than "Unknown App" everywhere.
        assertEquals("Instagram", AppRecognitionEngine.getAppName("com.instagram.beta"))
        assertEquals("YouTube Shorts", AppRecognitionEngine.getAppName("com.google.android.youtube.tv"))
        assertEquals("Facebook", AppRecognitionEngine.getAppName("com.facebook.orca"))
        assertEquals("App", AppRecognitionEngine.getAppName("com.example.unknown"))
        assertEquals("App", AppRecognitionEngine.getAppName(""))
    }

    @Test
    fun `supported check tolerates null and blank`() {
        assertFalse(AppRecognitionEngine.isSupported(null))
        assertFalse(AppRecognitionEngine.isSupported(""))
        assertFalse(AppRecognitionEngine.isSupported("com.example.unknown"))
        assertTrue(AppRecognitionEngine.isSupported("com.instagram.android"))
    }

    @Test
    fun `every recognised package maps to a name that contains its short form`() {
        // The database seed and the detector must never disagree about what an app is
        // called, or the same app shows up under two different labels.
        AppRecognitionEngine.SUPPORTED_APPS.keys.forEach { pkg ->
            val short = AppRecognitionNames.displayNameFor(pkg)
            assertTrue("$pkg resolved to a blank name", short.isNotBlank())
            assertTrue(
                "detector name for $pkg ('" + AppRecognitionEngine.getAppName(pkg) +
                    "') should mention '$short'",
                AppRecognitionEngine.getAppName(pkg).contains(short, ignoreCase = true)
            )
        }
    }

    // ── Notch classification ──────────────────────────────────────────────

    @Test
    fun `a wide notch is classified as wide`() {
        assertEquals(
            NotchShape.WIDE_NOTCH,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = 0, widthDp = 120, heightDp = 30)
        )
    }

    @Test
    fun `a small round cutout near the centre is a centre punch hole`() {
        assertEquals(
            NotchShape.PUNCH_HOLE_CENTER,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = 0, widthDp = 20, heightDp = 20)
        )
    }

    @Test
    fun `a small cutout biased left or right is classified by its offset`() {
        assertEquals(
            NotchShape.PUNCH_HOLE_LEFT,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = -60, widthDp = 20, heightDp = 20)
        )
        assertEquals(
            NotchShape.PUNCH_HOLE_RIGHT,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = 60, widthDp = 20, heightDp = 20)
        )
    }

    @Test
    fun `a cutout taller than wide is a waterdrop`() {
        assertEquals(
            NotchShape.WATER_DROP,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = 0, widthDp = 20, heightDp = 30)
        )
    }

    @Test
    fun `an oversized round cutout still resolves to a punch hole`() {
        // Falls through the small-cutout branch but must not fall off the end.
        assertEquals(
            NotchShape.PUNCH_HOLE_CENTER,
            DisplayCutoutDetectionService.classifyNotchShape(centerXDp = 0, widthDp = 50, heightDp = 40)
        )
    }

    // ── Date arithmetic ───────────────────────────────────────────────────

    @Test
    fun `today is a well formed ISO date`() {
        assertTrue(ScrollyDatabase.getTodayDate().matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun `offsetting by a day moves forwards and backwards correctly`() {
        val today = ScrollyDatabase.getTodayDate()
        assertEquals("offset zero must be today", today, ScrollyDatabase.getDateOffset(0))
        assertTrue(ScrollyDatabase.getDateOffset(-1) < today)
        assertTrue(ScrollyDatabase.getDateOffset(1) > today)
    }

    @Test
    fun `month and year prefixes have the expected shape`() {
        assertTrue(ScrollyDatabase.getMonthPrefix().matches(Regex("""\d{4}-\d{2}""")))
        assertTrue(ScrollyDatabase.getYearPrefix().matches(Regex("""\d{4}""")))
    }
}