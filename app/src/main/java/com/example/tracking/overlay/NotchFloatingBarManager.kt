package com.example.tracking.overlay

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ScrollyApp
import com.example.data.repository.IslandPlacementMode
import com.example.tracking.accessibility.AccessibilityHelper
import com.example.tracking.detector.DisplayCutoutDetectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * NotchFloatingBarManager
 * Renders and manages the floating 'Dynamic Island' pill overlay over social media apps and hardware notches.
 * - Uses TYPE_ACCESSIBILITY_OVERLAY or TYPE_APPLICATION_OVERLAY with top-most priority.
 * - Displays live scroll count, app emoji badge, and goal indicator.
 * - Smoothly expands from compact pill to elongated dynamic island wrapping hardware notch cutout.
 * - Maintains window attachment during social sessions to eliminate flickering.
 */
class NotchFloatingBarManager(
    private val context: Context,
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
) {
    companion object {
        private const val TAG = "NotchFloatingBar"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var rootView: FrameLayout? = null
    private var pillContainer: LinearLayout? = null
    private var leftWingView: LinearLayout? = null
    private var centerSpacerView: View? = null
    private var rightWingView: LinearLayout? = null
    private var compactIconView: TextView? = null

    private var iconView: TextView? = null
    private var countTextView: TextView? = null
    private var statusDot: View? = null

    private var animationController: DynamicIslandAnimationController? = null

    private var isWindowAttached = false
    private var isReelActive = false
    private var currentAppName: String = "Shorts"
    private var currentScrollCount = 0
    private var targetDailyGoal = 100

    private var statsJob: Job? = null
    private var configJob: Job? = null

    init {
        // Observe live scroll count from repository
        statsJob = scope.launch {
            try {
                ScrollyApp.instance.trackingRepository.todayTotalScrolls.collectLatest { scrolls ->
                    currentScrollCount = scrolls
                    if (isReelActive) {
                        mainHandler.post {
                            updateUI(scrolls, currentAppName, targetDailyGoal, animatePulse = true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in stats flow", e)
            }
        }

        // Observe notch configuration (placement mode, offsets, cutout width)
        configJob = scope.launch {
            try {
                ScrollyApp.instance.notchSettingsRepository.configFlow.collectLatest { config ->
                    mainHandler.post {
                        applyConfigUpdate(config.offsetX, config.offsetY, config.cutoutGapWidth)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in config flow", e)
            }
        }
    }

    /**
     * Ensures the overlay window is attached to WindowManager.
     */
    fun attachWindowIfNeeded() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            attachWindowInternal()
        } else {
            mainHandler.post { attachWindowInternal() }
        }
    }

    private fun attachWindowInternal() {
        if (isWindowAttached && rootView != null && rootView?.isAttachedToWindow == true) {
            return
        }

        try {
            if (rootView == null) {
                createNotchView()
            }

            val params = createLayoutParams()
            windowManager.addView(rootView, params)
            isWindowAttached = true
            Log.d(TAG, "Overlay window attached successfully")

            if (isReelActive) {
                pillContainer?.visibility = View.VISIBLE
                pillContainer?.alpha = 1f
                animationController?.enforceStaticCompactPill()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay window", e)
            isWindowAttached = false
        }
    }

    /**
     * Shows the Dynamic Island counter when reel/short activity is detected.
     */
    fun show(appName: String, scrollCount: Int = currentScrollCount, dailyGoal: Int = targetDailyGoal) {
        currentAppName = appName
        currentScrollCount = scrollCount
        targetDailyGoal = dailyGoal
        isReelActive = true

        val action = Runnable {
            attachWindowInternal()
            updateUI(scrollCount, appName, dailyGoal, animatePulse = false)
            pillContainer?.visibility = View.VISIBLE
            pillContainer?.alpha = 1f
            animationController?.enforceStaticCompactPill()
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Updates live scroll count instantly without layout morphing or expanding animations.
     * Prevents numbers from shifting behind the hardware notch/cutout.
     */
    fun updateCount(newCount: Int, appName: String = currentAppName, dailyGoal: Int = targetDailyGoal) {
        currentScrollCount = newCount
        currentAppName = appName
        targetDailyGoal = dailyGoal
        isReelActive = true

        val action = Runnable {
            if (!isWindowAttached || rootView == null || rootView?.isAttachedToWindow != true) {
                show(appName, newCount, dailyGoal)
            } else {
                pillContainer?.visibility = View.VISIBLE
                pillContainer?.alpha = 1f
                updateUI(newCount, appName, dailyGoal, animatePulse = false)
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Hides the Dynamic Island when leaving reels tab or social media app.
     * Keeps the WindowManager window attached if keepAttached is true (prevents flicker).
     */
    fun hide(immediate: Boolean = false, keepAttached: Boolean = true) {
        isReelActive = false

        val action = Runnable {
            if (isWindowAttached && rootView != null) {
                if (immediate) {
                    pillContainer?.visibility = View.INVISIBLE
                    if (!keepAttached) {
                        detachWindow()
                    }
                } else {
                    animationController?.animateExit {
                        if (!keepAttached && !isReelActive) {
                            detachWindow()
                        }
                    }
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    private fun detachWindow() {
        try {
            if (rootView != null && rootView?.isAttachedToWindow == true) {
                windowManager.removeViewImmediate(rootView)
                Log.d(TAG, "Overlay window detached")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing overlay view", e)
        } finally {
            rootView = null
            pillContainer = null
            isWindowAttached = false
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val windowType = when {
            context is AccessibilityService -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            context is Activity -> WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        val config = ScrollyApp.instance.notchSettingsRepository.configFlow.value
        val safeY = if (config.offsetY <= 0 && config.placementMode == IslandPlacementMode.BELOW_NOTCH) 38 else config.offsetY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = dpToPx(config.offsetX)
            y = dpToPx(safeY)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.fitInsetsTypes = 0
            params.fitInsetsSides = 0
        }

        return params
    }

    private fun createNotchView() {
        val ctx = context
        val root = FrameLayout(ctx)
        rootView = root

        val config = ScrollyApp.instance.notchSettingsRepository.configFlow.value

        // Dynamic Cutout listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            root.setOnApplyWindowInsetsListener { _, insets ->
                val cutout = insets.displayCutout
                if (cutout != null) {
                    val detected = DisplayCutoutDetectionService.parseDisplayCutout(cutout, ctx)
                    val repo = ScrollyApp.instance.notchSettingsRepository
                    if (!repo.configFlow.value.autoAdjusted) {
                        repo.autoCalibrateWithCutout(detected)
                    }
                }
                insets
            }
        }

        // Apple Dynamic Island pure obsidian capsule container
        val pill = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(6), dpToPx(14), dpToPx(6))

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#000000")) // Pure Apple black
                setStroke(dpToPx(1), Color.parseColor("#333333")) // Subtle rim
            }
            background = shape
            elevation = dpToPx(12).toFloat()
            visibility = View.VISIBLE
            alpha = 1f
        }
        pillContainer = pill

        // 1. COMPACT IDLE ICON
        val compactIcon = TextView(ctx).apply {
            text = "⚡"
            textSize = 12f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        compactIconView = compactIcon
        pill.addView(compactIcon)

        // 2. LEFT WING: Emoji / App Identifier
        val leftWing = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        leftWingView = leftWing

        val icon = TextView(ctx).apply {
            text = "🔥"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, dpToPx(4), 0)
        }
        iconView = icon
        leftWing.addView(icon)
        pill.addView(leftWing)

        // 3. CENTER HARDWARE CUTOUT CLEARANCE GAP (Kept hidden to maintain solid compact pill)
        val centerSpacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(20))
            visibility = View.GONE
        }
        centerSpacerView = centerSpacer
        pill.addView(centerSpacer)

        // 4. RIGHT WING: Live Scroll Count + Status Dot
        val rightWing = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(6)
            }
            layoutParams = lp
        }
        rightWingView = rightWing

        val countText = TextView(ctx).apply {
            text = "0"
            textSize = 13f
            setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD), Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        countTextView = countText
        rightWing.addView(countText)

        val dot = View(ctx).apply {
            val dotLp = LinearLayout.LayoutParams(dpToPx(7), dpToPx(7)).apply {
                leftMargin = dpToPx(5)
            }
            layoutParams = dotLp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4CAF50"))
            }
        }
        statusDot = dot
        rightWing.addView(dot)
        pill.addView(rightWing)

        // Add pill into root
        root.addView(
            pill,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        // Initialize Animation Controller
        animationController = DynamicIslandAnimationController(
            pillContainer = pill,
            leftWingView = leftWing,
            centerSpacerView = centerSpacer,
            rightWingView = rightWing,
            compactIconView = compactIcon,
            dpToPx = { dpToPx(it) },
            onLayoutRequested = {
                try {
                    rootView?.requestLayout()
                } catch (_: Exception) {}
            }
        )
    }

    private fun updateUI(count: Int, appName: String, dailyGoal: Int, animatePulse: Boolean) {
        val icon = "🔥"
        iconView?.text = icon

        countTextView?.text = count.toString()

        val isNearLimit = count >= (dailyGoal * 0.8f)
        val isOverLimit = count >= dailyGoal

        val dotColor = when {
            isOverLimit -> Color.parseColor("#FF5252") // Alert Red
            isNearLimit -> Color.parseColor("#FFAB00") // Warning Amber
            else -> Color.parseColor("#4CAF50") // Safe Green
        }
        (statusDot?.background as? GradientDrawable)?.setColor(dotColor)
    }

    private fun applyConfigUpdate(offsetX: Int, offsetY: Int, cutoutGapWidth: Int) {
        val config = ScrollyApp.instance.notchSettingsRepository.configFlow.value

        // Center spacer gap is kept collapsed (0 width, GONE) to enforce compact pill
        centerSpacerView?.let { spacer ->
            val lp = spacer.layoutParams
            lp.width = 0
            spacer.layoutParams = lp
            spacer.visibility = View.GONE
        }

        // Reposition overlay on WindowManager safely below punch hole
        if (isWindowAttached && rootView != null && rootView?.isAttachedToWindow == true) {
            try {
                val params = rootView?.layoutParams as? WindowManager.LayoutParams ?: createLayoutParams()
                params.x = dpToPx(offsetX)
                val safeY = if (offsetY <= 0 && config.placementMode == IslandPlacementMode.BELOW_NOTCH) 38 else offsetY
                params.y = dpToPx(safeY)
                windowManager.updateViewLayout(rootView, params)
            } catch (e: Exception) {
                Log.w(TAG, "Error updating window layout params", e)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    fun onDestroy() {
        statsJob?.cancel()
        configJob?.cancel()
        animationController?.onDestroy()
        detachWindow()
    }
}
