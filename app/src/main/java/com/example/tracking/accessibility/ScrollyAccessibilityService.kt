package com.example.tracking.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.ScrollyApp
import com.example.tracking.detector.ScrollDetectionEngine
import com.example.tracking.overlay.NotchFloatingBarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScrollyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ScrollyAccessService"
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        var activeServiceInstance: ScrollyAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var notchFloatingBarManager: NotchFloatingBarManager? = null
    private var scrollDetectionEngine: ScrollDetectionEngine? = null

    fun getFloatingBarManager(): NotchFloatingBarManager? = notchFloatingBarManager

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true
        activeServiceInstance = this

        try {
            notchFloatingBarManager = NotchFloatingBarManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NotchFloatingBarManager", e)
        }

        scrollDetectionEngine = ScrollDetectionEngine(
            service = this,
            onScrollDetected = { pkg, appName ->
                incrementCount(pkg, appName)
            },
            onReelVisibilityChanged = { pkg, appName, isWatchingReels ->
                mainHandler.post {
                    if (isWatchingReels) {
                        val count = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value
                        notchFloatingBarManager?.attachWindowIfNeeded()
                        notchFloatingBarManager?.show(appName, count)
                    } else {
                        notchFloatingBarManager?.hide(immediate = true, keepAttached = false)
                    }
                }
            }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = AccessibilityServiceInfo().apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_SCROLLED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                notificationTimeout = 100
            }
            this.serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Could not configure serviceInfo", e)
        }
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        scrollDetectionEngine?.processAccessibilityEvent(event)
    }

    private fun incrementCount(pkg: String, appName: String) {
        val optimisticCount = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value + 1
        mainHandler.post {
            notchFloatingBarManager?.updateCount(optimisticCount, appName)
        }
        serviceScope.launch {
            try {
                ScrollyApp.instance.trackingRepository.recordScroll(pkg, appName, 1)
            } catch (e: Exception) {
                Log.e(TAG, "Error recording scroll event", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Scrolly accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        activeServiceInstance = null
        notchFloatingBarManager?.onDestroy()
        notchFloatingBarManager = null
        scrollDetectionEngine = null
    }
}
