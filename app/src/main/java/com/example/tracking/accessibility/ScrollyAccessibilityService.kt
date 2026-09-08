package com.example.tracking.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.ScrollyApp
import com.example.tracking.detector.AppRecognitionEngine
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
    private lateinit var scrollDetectionEngine: ScrollDetectionEngine
    private var notchFloatingBarManager: NotchFloatingBarManager? = null
    private var currentForegroundPackage: String? = null

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
            onScrollDetected = { packageName, appName ->
                // Immediately increment count with pulse animation on UI thread
                val optimisticCount = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value + 1
                mainHandler.post {
                    notchFloatingBarManager?.updateCount(optimisticCount, appName)
                }

                // Persist scroll event to Room database
                serviceScope.launch {
                    try {
                        ScrollyApp.instance.trackingRepository.recordScroll(packageName, appName, 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording scroll event", e)
                    }
                }
            },
            onReelVisibilityChanged = { packageName, appName, isWatchingReels ->
                mainHandler.post {
                    val isStillInSocialApp = AppRecognitionEngine.isSupported(currentForegroundPackage)
                    if (isWatchingReels) {
                        val count = ScrollyApp.instance.trackingRepository.todayTotalScrolls.value
                        notchFloatingBarManager?.show(appName, count)
                    } else {
                        // Maintain constant window-attach state while in supported social app to prevent flicker
                        notchFloatingBarManager?.hide(immediate = false, keepAttached = isStillInSocialApp)
                    }
                }
            }
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            info.notificationTimeout = 40
            this.serviceInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Could not configure serviceInfo", e)
        }
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString()

        if (pkg != null && pkg != packageName && pkg != "com.example") {
            currentForegroundPackage = pkg

            // CRITICAL: Maintain constant window-attach state when detecting social media package names.
            // This pins the top-most overlay layer to the status bar area regardless of system UI shifts.
            if (AppRecognitionEngine.isSupported(pkg)) {
                notchFloatingBarManager?.attachWindowIfNeeded()
            }
        }

        if (::scrollDetectionEngine.isInitialized) {
            scrollDetectionEngine.processAccessibilityEvent(event)
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
    }
}
