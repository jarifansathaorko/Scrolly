package com.example.tracking.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.ScrollyApp
import com.example.tracking.detector.AppRecognitionEngine
import com.example.tracking.detector.ScrollDetectionEngine
import com.example.tracking.overlay.NotchFloatingBarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ScrollyAccessibilityService
 *
 *  1. Tracks the foreground package via [foregroundPackageFlow] for debug UI.
 *  2. Force-hides the overlay whenever a non-social app moves to the foreground, so the
 *     counter can never linger on the home screen or another app.
 *  3. Delegates scroll/reel detection to [ScrollDetectionEngine].
 *  4. Owns the [NotchFloatingBarManager] lifecycle.
 */
class ScrollyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ScrollyAccessService"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        /** Foreground package in real time, for observers such as the debug UI. */
        private val _foregroundPackageFlow = MutableStateFlow("")
        val foregroundPackageFlow = _foregroundPackageFlow.asStateFlow()

        /** Current window class for real-time debugging. */
        private val _windowTitleFlow = MutableStateFlow("")
        val windowTitleFlow = _windowTitleFlow.asStateFlow()

        var activeServiceInstance: ScrollyAccessibilityService? = null
            private set

        /**
         * Packages that appear in the window event stream but must never trigger a
         * counter change: status bar, IME, GMS, Play Store and Scrolly itself.
         *
         * Kept as a set so the lookup is O(1) on a hot path. [ScrollDetectionEngine]
         * filters the same set internally for its own state machine.
         */
        private val IGNORED_UI_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.gms",
            "com.google.android.webview",
            "com.android.vending"
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var notchFloatingBarManager: NotchFloatingBarManager? = null
    private var scrollDetectionEngine: ScrollDetectionEngine? = null

    fun getFloatingBarManager(): NotchFloatingBarManager? = notchFloatingBarManager

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true
        activeServiceInstance = this
        Log.i(TAG, "[SERVICE] onCreate")

        try {
            notchFloatingBarManager = NotchFloatingBarManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "[SERVICE] Failed to initialize NotchFloatingBarManager", e)
        }

        scrollDetectionEngine = ScrollDetectionEngine(
            service = this,
            onScrollDetected = { pkg, appName -> incrementCount(pkg, appName) },
            onReelVisibilityChanged = { pkg, appName, isWatchingReels ->
                mainHandler.post {
                    if (isWatchingReels) {
                        val count = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value
                        notchFloatingBarManager?.attachWindowIfNeeded()
                        notchFloatingBarManager?.show(appName, count)
                        Log.d(TAG, "[OVERLAY] show pkg=$pkg app=$appName count=$count")
                    } else {
                        Log.d(TAG, "[OVERLAY] hide pkg=$pkg app=$appName")
                        notchFloatingBarManager?.forceHide()
                    }
                }
            }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "[SERVICE] onServiceConnected")
        try {
            serviceInfo = AccessibilityServiceInfo().apply {
                eventTypes =
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags =
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
            }
        } catch (e: Exception) {
            Log.w(TAG, "[SERVICE] Could not configure serviceInfo", e)
        }
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString().orEmpty()
            val className = event.className?.toString().orEmpty()
            val title = event.text.firstOrNull()?.toString().orEmpty()

            if (pkg != _foregroundPackageFlow.value) {
                _foregroundPackageFlow.value = pkg
            }
            _windowTitleFlow.value = "$pkg / $className"

            Log.d(
                TAG,
                "[WINDOW] pkg=$pkg class=$className title=\"$title\" " +
                    "supported=${AppRecognitionEngine.isSupported(pkg)}"
            )

            // Non-social foreground: hide immediately rather than waiting for the
            // detector's own window evaluation.
            if (pkg.isNotEmpty() && !AppRecognitionEngine.isSupported(pkg) && !isIgnoredUiPackage(pkg)) {
                mainHandler.post {
                    if (notchFloatingBarManager?.isReelActive == true) {
                        Log.d(TAG, "[OVERLAY] force-hide: non-social foreground $pkg")
                        notchFloatingBarManager?.forceHide()
                    }
                }
            }
        }

        scrollDetectionEngine?.processAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        Log.w(TAG, "[SERVICE] onInterrupt")
        mainHandler.post { notchFloatingBarManager?.forceHide() }
        scrollDetectionEngine?.resetState()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.i(TAG, "[SERVICE] onUnbind")
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "[SERVICE] onDestroy")
        teardown()
    }

    private fun teardown() {
        _isServiceActive.value = false
        if (activeServiceInstance === this) activeServiceInstance = null
        scrollDetectionEngine?.resetState()
        scrollDetectionEngine = null
        notchFloatingBarManager?.onDestroy()
        notchFloatingBarManager = null
        serviceScope.cancel()
    }

    // ── Scroll recording ───────────────────────────────────────────────────

    /**
     * Records a scroll and refreshes the overlay.
     *
     * The overlay is driven purely by [com.example.data.repository.TrackingRepository]'s
     * optimistic counter rather than by a locally computed `value + 1`. The old code read
     * `todayTotalScrolls.value + 1` on the accessibility thread and pushed that straight
     * to the overlay, which could overwrite a *newer* real value with a stale one whenever
     * two scrolls landed in quick succession and make the counter skip or jump backwards.
     * Since the repository already publishes an immediate optimistic total, reading it
     * here is both simpler and race-free.
     */
    private fun incrementCount(pkg: String, appName: String) {
        serviceScope.launch {
            try {
                ScrollyApp.instance.trackingRepository.recordScroll(pkg, appName, 1)
                val count = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value
                mainHandler.post { notchFloatingBarManager?.updateCount(count, appName) }
            } catch (e: Exception) {
                Log.e(TAG, "[SERVICE] Error recording scroll event", e)
            }
        }
    }

    /** True for system UI packages that must not affect the counter. */
    private fun isIgnoredUiPackage(pkg: String): Boolean =
        pkg == packageName || IGNORED_UI_PACKAGES.any { pkg == it || pkg.startsWith(it) }
}