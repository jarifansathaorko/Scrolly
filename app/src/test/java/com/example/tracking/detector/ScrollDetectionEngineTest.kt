package com.example.tracking.detector

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behavioural tests for [ScrollDetectionEngine] with no service attached.
 *
 * With `service = null` the node tree cannot be inspected, so reel context can only come
 * from a tab click (`cachedIsReelContext`) or from TikTok's always-vertical feed. The
 * tests below drive those paths explicitly, which is how the engine behaves in practice:
 * a tab is tapped once, then the pager produces scrolls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScrollDetectionEngineTest {

    private class Harness {
        var scrolls = 0
        val visibilityChanges = mutableListOf<Triple<String, String, Boolean>>()
        var lastAppName: String? = null

        val engine = ScrollDetectionEngine(
            service = null,
            onScrollDetected = { _, appName ->
                scrolls++
                lastAppName = appName
            },
            onReelVisibilityChanged = { pkg, appName, visible ->
                visibilityChanges += Triple(pkg, appName, visible)
            }
        )

        val isVisible: Boolean get() = visibilityChanges.lastOrNull()?.third ?: false
    }

    private fun scroll(pkg: String, className: String? = null) =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED).apply {
            packageName = pkg
            className?.let { this.className = it }
        }

    private fun window(pkg: String) =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = pkg
        }

    private fun click(pkg: String, label: String) =
        AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED).apply {
            packageName = pkg
            this.className = "android.widget.Button"
            contentDescription = label
        }

    /** Enters a reel feed, which is what a user does by tapping the Reels/Shorts tab. */
    private fun Harness.enterReels(pkg: String, tabLabel: String) {
        engine.processAccessibilityEvent(click(pkg, tabLabel))
    }

    // ── Package filtering ─────────────────────────────────────────────────

    @Test
    fun `tiktok counts a scroll without any tab tap because its feed is vertical`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically"))
        assertEquals(1, h.scrolls)
        assertEquals("TikTok", h.lastAppName)
        assertTrue(h.isVisible)
    }

    @Test
    fun `an unsupported package is never counted`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.example.random.game"))
        assertEquals(0, h.scrolls)
        assertFalse(h.isVisible)
    }

    @Test
    fun `system packages never add scrolls nor dismiss the overlay`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically"))
        assertTrue(h.isVisible)

        listOf(
            "com.example",
            "com.android.systemui",
            "com.google.android.gms",
            "android",
            "com.google.android.webview",
            "com.android.vending"
        ).forEach { pkg ->
            h.engine.processAccessibilityEvent(window(pkg))
            h.engine.processAccessibilityEvent(
                AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
                    packageName = pkg
                }
            )
        }

        assertEquals("system packages must not add scrolls", 1, h.scrolls)
        assertTrue("the overlay must survive system ui events", h.isVisible)
    }

    @Test
    fun `leaving for an unsupported app hides the overlay`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically"))
        assertTrue(h.isVisible)

        h.engine.processAccessibilityEvent(window("com.google.android.apps.nexuslauncher"))

        assertFalse(h.isVisible)
    }

    @Test
    fun `a null package name is ignored`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_SCROLLED)
        )
        assertEquals(0, h.scrolls)
    }

    // ── Entering and leaving a feed ───────────────────────────────────────

    @Test
    fun `tapping the shorts tab makes subsequent scrolls count`() {
        val h = Harness()
        h.enterReels("com.google.android.youtube", "Shorts")
        assertTrue("tapping Shorts should reveal the counter", h.isVisible)

        h.engine.processAccessibilityEvent(scroll("com.google.android.youtube"))
        assertEquals(1, h.scrolls)
        assertEquals("YouTube Shorts", h.lastAppName)
    }

    @Test
    fun `scrolling before any tab is tapped is not counted`() {
        // Without a node tree the engine has no positive evidence of a reel context, so
        // it must stay silent rather than count scrolls anywhere.
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.google.android.youtube"))
        assertEquals(0, h.scrolls)
    }

    @Test
    fun `tapping away from the feed hides the overlay`() {
        val h = Harness()
        h.enterReels("com.google.android.youtube", "Shorts")
        assertTrue(h.isVisible)

        h.engine.processAccessibilityEvent(click("com.google.android.youtube", "Home"))

        assertFalse(h.isVisible)
    }

    @Test
    fun `an unambiguous non reel screen hides the overlay`() {
        val h = Harness()
        h.enterReels("com.instagram.android", "Reels")
        assertTrue(h.isVisible)

        // Instagram's DMs are an unambiguous "not reels" screen.
        h.engine.processAccessibilityEvent(
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
                packageName = "com.instagram.android"
                className = "com.instagram.direct.thread.DirectThreadActivity"
            }
        )

        assertFalse(h.isVisible)
    }

    // ── Scroll classification ─────────────────────────────────────────────

    @Test
    fun `horizontal carousels are rejected`() {
        listOf(
            "android.widget.HorizontalScrollView",
            "com.google.android.material.tabs.TabLayout",
            "androidx.recyclerview.widget.RecyclerView\$Carousel",
            "android.view.HorizontalScrollBar"
        ).forEach { className ->
            val h = Harness()
            h.enterReels("com.zhiliaoapp.musically", "Home")
            h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically", className))
            assertEquals("$className must not be counted as a reel swipe", 0, h.scrolls)
        }
    }

    @Test
    fun `vertical pagers are accepted once a reel context is established`() {
        listOf(
            "androidx.viewpager2.widget.ViewPager2",
            "android.widget.ScrollView",
            // TikTok's own feed is a RecyclerView, so this must count.
            "androidx.recyclerview.widget.RecyclerView"
        ).forEach { className ->
            val h = Harness()
            h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically", className))
            assertEquals("$className should count as a reel swipe", 1, h.scrolls)
        }
    }

    @Test
    fun `rapid scrolls inside the debounce window count once`() {
        val h = Harness()
        repeat(5) { h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically")) }
        assertEquals("a 400ms debounce must collapse a burst", 1, h.scrolls)
    }

    @Test
    fun `tiktok regional package variants are all recognised`() {
        listOf(
            "com.zhiliaoapp.musically",
            "com.zhiliaoapp.musically.go",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme"
        ).forEach { pkg ->
            val h = Harness()
            h.engine.processAccessibilityEvent(scroll(pkg))
            assertEquals("$pkg must be counted", 1, h.scrolls)
            assertEquals("TikTok", h.lastAppName)
        }
    }

    @Test
    fun `instagram lite and facebook variants are recognised`() {
        val instagramLite = Harness()
        instagramLite.enterReels("com.instagram.lite", "Reels")
        instagramLite.engine.processAccessibilityEvent(scroll("com.instagram.lite"))
        assertEquals(1, instagramLite.scrolls)

        val facebook = Harness()
        facebook.enterReels("com.facebook.lite", "Reels")
        facebook.engine.processAccessibilityEvent(scroll("com.facebook.lite"))
        assertEquals(1, facebook.scrolls)
    }

    // ── Instagram comment sheet ───────────────────────────────────────────

    @Test
    fun `opening the comment sheet must not count a scroll`() {
        val h = Harness()
        val pkg = "com.instagram.android"
        h.enterReels(pkg, "Reels")

        // Tapping the comment button fires a programmatic scroll as the sheet animates.
        h.engine.processAccessibilityEvent(click(pkg, "Comment"))
        h.engine.processAccessibilityEvent(scroll(pkg))

        assertEquals("the sheet animation must not be counted", 0, h.scrolls)
    }

    @Test
    fun `comment sheet suppression does not stop later reel swipes`() {
        val h = Harness()
        val pkg = "com.instagram.android"

        // After a reset the suppression window and debounce are cleared, so a genuine
        // swipe must count again. This guards against the suppression ever becoming
        // permanent, which would silently stop all Instagram tracking.
        h.enterReels(pkg, "Reels")
        h.engine.processAccessibilityEvent(click(pkg, "Comment"))
        h.engine.processAccessibilityEvent(scroll(pkg))
        assertEquals(0, h.scrolls)

        h.engine.resetState()

        h.enterReels(pkg, "Reels")
        h.engine.processAccessibilityEvent(scroll(pkg))
        assertEquals("a genuine reel swipe must still be counted", 1, h.scrolls)
    }

    @Test
    fun `a comment click does not itself count as a scroll`() {
        val h = Harness()
        val pkg = "com.instagram.android"
        h.enterReels(pkg, "Reels")

        h.engine.processAccessibilityEvent(click(pkg, "Comment"))

        assertEquals(0, h.scrolls)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Test
    fun `resetState hides an active overlay exactly once`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically"))
        assertTrue(h.isVisible)

        h.engine.resetState()

        assertFalse("reset must hide the overlay", h.isVisible)
        assertEquals(
            "reset must emit exactly one hide, not one per cleanup path",
            1,
            h.visibilityChanges.count { !it.third }
        )
    }

    @Test
    fun `resetState on an inactive engine emits nothing`() {
        val h = Harness()
        h.engine.resetState()
        assertTrue(h.visibilityChanges.isEmpty())
    }

    @Test
    fun `resetState is idempotent`() {
        val h = Harness()
        h.engine.processAccessibilityEvent(scroll("com.zhiliaoapp.musically"))

        h.engine.resetState()
        val afterFirst = h.visibilityChanges.size
        h.engine.resetState()

        assertEquals("a second reset must be a no-op", afterFirst, h.visibilityChanges.size)
    }
}