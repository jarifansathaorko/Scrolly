package com.example.tracking.detector

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * ScrollDetectionEngine
 * Highly precise, battery-optimized reel and short-form video swipe detection engine.
 * - Strictly rejects horizontal swiping (left-right carousels, photo galleries, stories, tab switches).
 * - Distinguishes regular social media feeds (e.g. Facebook Newsfeed, Instagram Home, YouTube Home)
 *   from dedicated Reels / Shorts / TikTok / Spotify video tabs.
 * - Ultra-low battery consumption: Uses intelligent screen context caching and rate limits node hierarchy inspections
 *   to avoid CPU wake-locks and battery drain.
 * - Debounces swipe intervals to prevent double counts from fling decelerations.
 */
class ScrollDetectionEngine(
    private val service: AccessibilityService? = null,
    private val onScrollDetected: (packageName: String, appName: String) -> Unit,
    private val onReelVisibilityChanged: ((packageName: String, appName: String, isWatchingReels: Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "ScrollDetectionEngine"
        private const val MIN_SCROLL_INTERVAL_MS = 350L
        private const val CONTENT_CHANGE_EVAL_THROTTLE_MS = 1200L
        private const val CACHE_EXPIRY_MS = 3000L
    }

    private var lastScrollTimestamp: Long = 0
    private var lastContentChangeEvalTimestamp: Long = 0
    private var lastFullEvalTimestamp: Long = 0

    private var currentPackage: String? = null
    private var isCurrentlyWatchingReels: Boolean = false
    private var cachedIsReelContext: Boolean = false

    private fun isIgnoredSystemPackage(packageName: String): Boolean {
        if (packageName == service?.packageName) return true
        if (packageName == "com.example") return true
        if (packageName == "android") return true
        if (packageName.startsWith("com.android.systemui")) return true
        if (packageName.contains("inputmethod") || packageName.contains("keyboard")) return true
        if (packageName == "com.google.android.gms") return true
        if (packageName == "com.google.android.webview") return true
        if (packageName == "com.android.vending") return true
        return false
    }

    fun processAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // 1. Filter out ignored packages immediately (0 CPU cost)
        if (isIgnoredSystemPackage(packageName)) {
            return
        }

        val isSupported = AppRecognitionEngine.isSupported(packageName)

        // 2. Window / screen switch (Major lifecycle event)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (!isSupported) {
                if (isCurrentlyWatchingReels || currentPackage != packageName) {
                    currentPackage = packageName
                    isCurrentlyWatchingReels = false
                    cachedIsReelContext = false
                    onReelVisibilityChanged?.invoke(packageName, "", false)
                }
                return
            }

            val appName = AppRecognitionEngine.getAppName(packageName)
            currentPackage = packageName
            // Force evaluate new window
            evaluateScreenContext(packageName, appName, event, force = true)
            return
        }

        if (!isSupported) {
            return
        }

        val appName = AppRecognitionEngine.getAppName(packageName)

        // 3. Tab or Button clicks (Instant detection when user taps Reels or Shorts tab!)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            handleViewClicked(packageName, appName, event)
            return
        }

        // 4. Scroll event processing (Strictly vertical & debounced)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            handleScroll(packageName, appName, event)
            return
        }

        // 5. Tab / Pane transitions (Rate-limited to prevent battery drain from frequent updates)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val now = System.currentTimeMillis()
            if (now - lastContentChangeEvalTimestamp < CONTENT_CHANGE_EVAL_THROTTLE_MS) {
                return // Throttle high-frequency UI updates to save battery
            }
            lastContentChangeEvalTimestamp = now

            val contentChange = event.contentChangeTypes
            if (contentChange and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED != 0 ||
                contentChange and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED != 0 ||
                contentChange and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0
            ) {
                evaluateScreenContext(packageName, appName, event, force = false)
            }
        }
    }

    /**
     * Scrolless-inspired click detection:
     * Immediately catches user tapping "Reels" / "Shorts" navigation items to show the bar with 0 delay.
     * Also detects when user leaves Reels (taps Home, Search, Profile, Back, etc.) to hide the bar immediately.
     */
    private fun handleViewClicked(packageName: String, appName: String, event: AccessibilityEvent) {
        val textList = event.text.map { it.toString().lowercase() }
        val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""
        val clickedText = (textList + listOf(contentDesc)).joinToString(" ")
        val resId = (event.source?.viewIdResourceName ?: "").lowercase()

        // 1. User clicked into Reels or Shorts
        val isReelClick = clickedText.contains("reels") ||
                clickedText.contains("shorts") ||
                clickedText.contains("spotlight") ||
                resId.contains("reels") ||
                resId.contains("clips") ||
                resId.contains("shorts")

        if (isReelClick) {
            cachedIsReelContext = true
            if (!isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = true
                onReelVisibilityChanged?.invoke(packageName, appName, true)
            }
            return
        }

        // 2. User clicked out of Reels (Home feed, Search/Explore grid, Profile, Messages, Back)
        val isExitClick = clickedText.contains("home") ||
                clickedText.contains("search") ||
                clickedText.contains("explore") ||
                clickedText.contains("notifications") ||
                clickedText.contains("profile") ||
                clickedText.contains("direct") ||
                clickedText.contains("messages") ||
                clickedText.contains("subscriptions") ||
                clickedText.contains("library") ||
                clickedText.contains("you") ||
                clickedText.contains("close") ||
                clickedText.contains("back") ||
                resId.contains("home_tab") ||
                resId.contains("search_tab") ||
                resId.contains("profile_tab") ||
                resId.contains("direct_tab") ||
                resId.contains("action_bar_button_back")

        if (isExitClick) {
            cachedIsReelContext = false
            if (isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = false
                onReelVisibilityChanged?.invoke(packageName, appName, false)
            }
        }
    }

    /**
     * Checks if the event is strictly a vertical swipe.
     * Rejects horizontal swipes (left-right swiping across stories, carousels, tabs, cards).
     */
    private fun isStrictlyVerticalScroll(event: AccessibilityEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val deltaX = event.scrollDeltaX
            val deltaY = event.scrollDeltaY

            // In Android, -1 indicates SCROLL_DELTA_UNDEFINED. Only evaluate when coordinates are actually reported.
            if (deltaX != -1 && deltaY != -1) {
                // If horizontal movement occurred without vertical movement -> Left/Right swipe
                if (deltaX != 0 && deltaY == 0) {
                    return false
                }

                // If horizontal movement exceeds vertical movement -> horizontal swipe
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 2) {
                    return false
                }

                // If vertical delta is reported and positive (downward scroll to next reel)
                if (deltaY > 0) {
                    return true
                } else if (deltaY < 0) {
                    // Upward scroll, ignore
                    return false
                }
            }
        }

        // Fallback checks for ViewPager or RecyclerView scroll direction
        val className = event.className?.toString()?.lowercase() ?: ""
        if (className.contains("horizontal") ||
            className.contains("tablayout") ||
            className.contains("tabbar") ||
            className.contains("carousel") ||
            className.contains("storytray")
        ) {
            return false
        }

        return true
    }

    private fun evaluateScreenContext(
        packageName: String,
        appName: String,
        event: AccessibilityEvent,
        force: Boolean
    ) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFullEvalTimestamp < CACHE_EXPIRY_MS) {
            // Use cached state to preserve device battery
            return
        }
        lastFullEvalTimestamp = now

        val className = event.className?.toString() ?: ""

        // 1. Strict Negative Check: Regular newsfeeds, profiles, DMs, settings, search grids
        if (isUnambiguousNegativeScreen(packageName, className, event)) {
            cachedIsReelContext = false
            if (isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = false
                onReelVisibilityChanged?.invoke(packageName, appName, false)
            }
            return
        }

        // 2. Strict Positive Check: Dedicated Reels / Shorts / TikTok / Spotify video tab
        val isReel = isPositiveReelScreen(packageName, className, event)
        cachedIsReelContext = isReel

        if (isReel) {
            if (!isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = true
                onReelVisibilityChanged?.invoke(packageName, appName, true)
            }
        } else {
            // When in supported app but not confirmed reel tab (e.g. browsing main feed)
            if (isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = false
                onReelVisibilityChanged?.invoke(packageName, appName, false)
            }
        }
    }

    /**
     * Returns true if user is on a standard feed, profile, chat, or non-video tab.
     */
    private fun isUnambiguousNegativeScreen(packageName: String, className: String, event: AccessibilityEvent): Boolean {
        val lowerClass = className.lowercase()
        val textList = event.text.map { it.toString().lowercase() }
        val allText = (textList + listOfNotNull(event.contentDescription?.toString()?.lowercase())).joinToString(" ")

        when {
            // FACEBOOK: Main Newsfeed, Stories, Groups, Marketplace, Menu
            packageName.contains("facebook") -> {
                if (allText.contains("what's on your mind") ||
                    allText.contains("write a comment") ||
                    allText.contains("news feed") ||
                    allText.contains("marketplace") ||
                    allText.contains("groups") ||
                    allText.contains("notifications") ||
                    allText.contains("menu") ||
                    lowerClass.contains("newsfeed") ||
                    lowerClass.contains("feedunit") ||
                    lowerClass.contains("storytray") ||
                    lowerClass.contains("marketplace") ||
                    lowerClass.contains("settingsactivity")
                ) {
                    return true
                }
            }

            // INSTAGRAM: Home Feed, DMs, Profile, Settings
            packageName.contains("instagram") -> {
                if (allText.contains("your story") ||
                    allText.contains("add a comment") ||
                    allText.contains("direct messages") ||
                    allText.contains("search and explore") ||
                    lowerClass.contains("directinbox") ||
                    lowerClass.contains("directthread") ||
                    lowerClass.contains("feedfragment") ||
                    lowerClass.contains("profilefragment") ||
                    lowerClass.contains("editprofileactivity")
                ) {
                    return true
                }
            }

            // YOUTUBE: Home Feed, Subscriptions, Library, Search
            packageName.contains("youtube") -> {
                if ((allText.contains("home") && allText.contains("subscriptions") && !allText.contains("shorts")) ||
                    lowerClass.contains("homefragment") ||
                    lowerClass.contains("browsefragment") ||
                    lowerClass.contains("settingsactivity") ||
                    lowerClass.contains("searchactivity")
                ) {
                    return true
                }
            }

            // TIKTOK: Profile, Inbox, Search grid
            isTikTok(packageName) -> {
                if (lowerClass.contains("chatroom") ||
                    lowerClass.contains("imactivity") ||
                    lowerClass.contains("userprofile") ||
                    lowerClass.contains("profileactivity") ||
                    allText.contains("edit profile") ||
                    allText.contains("direct messages")
                ) {
                    return true
                }
            }

            // SPOTIFY: Track lists, Playlists, Albums, Search, Your Library
            packageName.contains("spotify") -> {
                if (allText.contains("your library") ||
                    allText.contains("playlists") ||
                    allText.contains("artists") ||
                    allText.contains("album") ||
                    lowerClass.contains("tracklist") ||
                    lowerClass.contains("playlistfragment") ||
                    lowerClass.contains("libraryfragment")
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Returns true ONLY if user is confirmed to be inside the dedicated Reels/Shorts/TikTok/Spotify Video tab.
     */
    private fun isPositiveReelScreen(packageName: String, className: String, event: AccessibilityEvent): Boolean {
        val lowerClass = className.lowercase()
        val textList = event.text.map { it.toString().lowercase() }
        val allText = (textList + listOfNotNull(event.contentDescription?.toString()?.lowercase())).joinToString(" ")

        return when {
            // FACEBOOK: Dedicated Reels viewer or Reels tab
            packageName.contains("facebook") -> {
                lowerClass.contains("reels") ||
                        lowerClass.contains("shorts") ||
                        allText.contains("original audio") ||
                        allText.contains("remix this reel") ||
                        allText.contains("use this audio") ||
                        checkNodeTreeStrictlyForReels(packageName)
            }

            // INSTAGRAM: Dedicated Reels tab or Clips viewer
            packageName.contains("instagram") -> {
                lowerClass.contains("clips") ||
                        lowerClass.contains("reel") ||
                        allText.contains("original audio") ||
                        allText.contains("remix") ||
                        allText.contains("use template") ||
                        checkNodeTreeStrictlyForReels(packageName)
            }

            // YOUTUBE: Shorts tab or Shorts player
            packageName.contains("youtube") -> {
                lowerClass.contains("shorts") ||
                        lowerClass.contains("reel") ||
                        (allText.contains("remix") && allText.contains("sound")) ||
                        allText.contains("shorts") ||
                        checkNodeTreeStrictlyForReels(packageName)
            }

            // TIKTOK: Full-screen video feed (default for TikTok main page)
            isTikTok(packageName) -> {
                lowerClass.contains("mainpagefragment") ||
                        lowerClass.contains("feedfragment") ||
                        lowerClass.contains("verticalviewpager") ||
                        lowerClass.contains("aweme") ||
                        allText.contains("for you") ||
                        allText.contains("following") ||
                        !isUnambiguousNegativeScreen(packageName, className, event)
            }

            // SPOTIFY: Canvas / Discovery vertical video feed / Clips player
            packageName.contains("spotify") -> {
                lowerClass.contains("verticalfeed") ||
                        lowerClass.contains("canvas") ||
                        lowerClass.contains("clips") ||
                        allText.contains("preview") ||
                        allText.contains("clips") ||
                        checkNodeTreeStrictlyForReels(packageName)
            }

            else -> false
        }
    }

    private fun isTikTok(pkg: String): Boolean {
        return pkg.contains("musically") || pkg.contains("ugc.trill") || pkg.contains("ugc.aweme")
    }

    /**
     * Inspects active accessibility node hierarchy strictly for dedicated Reels/Shorts/Clips viewers.
     * Limits traversal depth to 15 nodes and recycles nodes immediately to prevent battery drain.
     */
    private fun checkNodeTreeStrictlyForReels(packageName: String): Boolean {
        val serviceInstance = service ?: return false
        val root = try {
            serviceInstance.rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return false

        return try {
            when {
                packageName.contains("facebook") -> {
                    // Reject if News Feed items are in tree
                    if (root.findAccessibilityNodeInfosByText("What's on your mind?").isNotEmpty()) {
                        return false
                    }
                    findNodeByViewId(root, "reels") != null ||
                            findNodeByViewId(root, "shorts") != null ||
                            root.findAccessibilityNodeInfosByText("Original audio").isNotEmpty() ||
                            root.findAccessibilityNodeInfosByText("Remix this reel").isNotEmpty()
                }

                packageName.contains("instagram") -> {
                    findNodeByViewId(root, "clips") != null ||
                            findSelectedReelsTab(root) ||
                            root.findAccessibilityNodeInfosByText("Remix").isNotEmpty()
                }

                packageName.contains("youtube") -> {
                    findNodeByViewId(root, "reel") != null ||
                            findNodeByViewId(root, "shorts") != null ||
                            findSelectedShortsTab(root)
                }

                packageName.contains("spotify") -> {
                    findNodeByViewId(root, "canvas") != null ||
                            findNodeByViewId(root, "vertical") != null ||
                            findNodeByViewId(root, "clips") != null
                }

                else -> false
            }
        } catch (e: Exception) {
            false
        } finally {
            try { root.recycle() } catch (_: Exception) {}
        }
    }

    private fun findSelectedReelsTab(root: AccessibilityNodeInfo): Boolean {
        val reelTabs = root.findAccessibilityNodeInfosByText("Reels")
        for (node in reelTabs) {
            if (node.isSelected) return true
        }
        return false
    }

    private fun findSelectedShortsTab(root: AccessibilityNodeInfo): Boolean {
        val shortsTabs = root.findAccessibilityNodeInfosByText("Shorts")
        for (node in shortsTabs) {
            if (node.isSelected) return true
        }
        return false
    }

    private fun findNodeByViewId(root: AccessibilityNodeInfo, partialId: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var checked = 0
        while (queue.isNotEmpty() && checked < 20) {
            val node = queue.removeFirst()
            checked++
            val resId = node.viewIdResourceName?.lowercase() ?: ""
            if (resId.contains(partialId)) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun handleScroll(packageName: String, appName: String, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - lastScrollTimestamp < MIN_SCROLL_INTERVAL_MS) {
            return
        }

        // 1. STRICTLY VERTICAL SCROLL: Rejects horizontal swiping (photos, stories, carousels)
        if (!isStrictlyVerticalScroll(event)) {
            Log.d(TAG, "Rejected horizontal swipe on $packageName")
            return
        }

        val className = event.className?.toString() ?: ""

        // 2. Reject if confirmed on regular feed/profile/chat
        if (isUnambiguousNegativeScreen(packageName, className, event)) {
            cachedIsReelContext = false
            if (isCurrentlyWatchingReels) {
                isCurrentlyWatchingReels = false
                onReelVisibilityChanged?.invoke(packageName, appName, false)
            }
            return
        }

        // 3. Verify Reels / Shorts / TikTok / Spotify video context
        val isConfirmedReel = if (isTikTok(packageName)) {
            !isUnambiguousNegativeScreen(packageName, className, event)
        } else {
            cachedIsReelContext || isPositiveReelScreen(packageName, className, event)
        }

        if (!isConfirmedReel) {
            Log.d(TAG, "Scroll ignored - not in dedicated reels/shorts tab ($packageName)")
            return
        }

        // Validated vertical reel swipe!
        lastScrollTimestamp = now
        // Do not force cachedIsReelContext = true here, rely on evaluateScreenContext
        // to manage lifecycle properly and prevent sticking on tab switches.

        if (!isCurrentlyWatchingReels) {
            isCurrentlyWatchingReels = true
            onReelVisibilityChanged?.invoke(packageName, appName, true)
        }

        Log.d(TAG, "Valid vertical reel swipe detected on $appName ($packageName)")
        onScrollDetected(packageName, appName)
    }
}
