package com.example.tracking.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * DynamicIslandAnimationController
 * Controls the morphing transitions of the floating notch counter:
 * - Circular Compact Pill -> Elongated Dynamic Island wrapping around hardware notch bounds.
 * - Pulse & spring bounce on scroll activity.
 * - Auto-collapse to compact circular pill when idle to avoid screen obstruction.
 * - Smooth pop-in entrance and pop-out exit animations safely within WindowManager surface bounds.
 */
class DynamicIslandAnimationController(
    private val pillContainer: View,
    private val leftWingView: View,
    private val centerSpacerView: View,
    private val rightWingView: View,
    private val compactIconView: View,
    private val dpToPx: (Int) -> Int,
    private val onLayoutRequested: (() -> Unit)? = null
) {
    enum class IslandState {
        HIDDEN,
        COMPACT_PILL,
        ELONGATED_ISLAND
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentState = IslandState.HIDDEN
    private var currentWidthAnimator: ValueAnimator? = null
    private var pulseAnimatorSet: AnimatorSet? = null

    // Idle collapse callback (collapses back to sleek compact pill after inactivity)
    private val idleCollapseRunnable = Runnable {
        if (currentState == IslandState.ELONGATED_ISLAND) {
            transitionToCompactPill()
        }
    }

    private val IDLE_TIMEOUT_MS = 4500L

    fun getState(): IslandState = currentState

    /**
     * Expands the counter from a small circular pill to an elongated 'Dynamic Island' shape
     * that wraps around the hardware notch bounds.
     */
    fun expandToDynamicIsland(
        cutoutWidthDp: Int,
        onComplete: (() -> Unit)? = null
    ) {
        mainHandler.removeCallbacks(idleCollapseRunnable)
        currentWidthAnimator?.cancel()

        // Calculate dynamic width: Left wing (~50dp) + Hardware Notch Gap + Right wing (~65dp) + Padding
        val targetWidthDp = (cutoutWidthDp + 115).coerceIn(150, 320)
        val targetWidthPx = dpToPx(targetWidthDp)
        val targetHeightPx = dpToPx(34)
        val startWidthPx = pillContainer.width.takeIf { it > 0 } ?: dpToPx(36)

        pillContainer.visibility = View.VISIBLE
        pillContainer.alpha = 1f
        pillContainer.scaleX = 1f
        pillContainer.scaleY = 1f
        pillContainer.translationY = 0f

        val widthAnim = ValueAnimator.ofInt(startWidthPx, targetWidthPx).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.15f)
            addUpdateListener { animator ->
                val lp = pillContainer.layoutParams
                if (lp != null) {
                    lp.width = animator.animatedValue as Int
                    lp.height = targetHeightPx
                    pillContainer.layoutParams = lp
                }
                onLayoutRequested?.invoke()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentState = IslandState.ELONGATED_ISLAND
                    onLayoutRequested?.invoke()
                    onComplete?.invoke()
                }
            })
        }

        // Crossfade compact icon into left/center/right wings
        compactIconView.visibility = View.GONE
        compactIconView.alpha = 0f

        leftWingView.visibility = View.VISIBLE
        leftWingView.alpha = 1f

        centerSpacerView.visibility = View.VISIBLE
        val spacerLp = centerSpacerView.layoutParams
        if (spacerLp != null) {
            spacerLp.width = dpToPx(cutoutWidthDp)
            centerSpacerView.layoutParams = spacerLp
        }

        rightWingView.visibility = View.VISIBLE
        rightWingView.alpha = 1f

        currentWidthAnimator = widthAnim
        widthAnim.start()
        currentState = IslandState.ELONGATED_ISLAND

        // Schedule idle collapse
        resetIdleTimer()
    }

    /**
     * Collapses from elongated Dynamic Island into a small circular pill.
     */
    fun transitionToCompactPill(onComplete: (() -> Unit)? = null) {
        mainHandler.removeCallbacks(idleCollapseRunnable)
        currentWidthAnimator?.cancel()

        val startWidthPx = pillContainer.width.takeIf { it > 0 } ?: dpToPx(160)
        val targetWidthPx = dpToPx(36)
        val targetHeightPx = dpToPx(34)

        // Fade out wings first
        leftWingView.visibility = View.GONE
        rightWingView.visibility = View.GONE
        centerSpacerView.visibility = View.GONE

        val widthAnim = ValueAnimator.ofInt(startWidthPx, targetWidthPx).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val lp = pillContainer.layoutParams
                if (lp != null) {
                    lp.width = animator.animatedValue as Int
                    lp.height = targetHeightPx
                    pillContainer.layoutParams = lp
                }
                onLayoutRequested?.invoke()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentState = IslandState.COMPACT_PILL
                    compactIconView.visibility = View.VISIBLE
                    compactIconView.alpha = 1f
                    onLayoutRequested?.invoke()
                    onComplete?.invoke()
                }
            })
        }

        currentWidthAnimator = widthAnim
        widthAnim.start()
    }

    /**
     * Triggers elastic spring bounce and luminous pulse on new scroll.
     */
    fun triggerScrollBounce(cutoutWidthDp: Int) {
        // If compact or hidden, expand immediately
        if (currentState != IslandState.ELONGATED_ISLAND) {
            expandToDynamicIsland(cutoutWidthDp)
        } else {
            resetIdleTimer()
        }

        try {
            pillContainer.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}

        pulseAnimatorSet?.cancel()

        val scaleX = ObjectAnimator.ofFloat(pillContainer, "scaleX", 1.0f, 1.10f, 1.0f).apply {
            duration = 260
            interpolator = OvershootInterpolator(2.2f)
        }
        val scaleY = ObjectAnimator.ofFloat(pillContainer, "scaleY", 1.0f, 1.10f, 1.0f).apply {
            duration = 260
            interpolator = OvershootInterpolator(2.2f)
        }

        val bg = pillContainer.background as? GradientDrawable
        val colorAnim = ValueAnimator.ofArgb(
            Color.parseColor("#381E72"), // Sleek luminous purple
            Color.parseColor("#000000")  // Pure Apple Obsidian Black
        ).apply {
            duration = 320
            addUpdateListener { animator ->
                bg?.setColor(animator.animatedValue as Int)
            }
        }

        pulseAnimatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, colorAnim)
            start()
        }
    }

    /**
     * Smooth pop-in entrance animation that stays fully inside WindowManager bounds.
     */
    fun animateEntrance(cutoutWidthDp: Int) {
        pillContainer.animate().cancel()
        pillContainer.visibility = View.VISIBLE
        pillContainer.alpha = 0f
        pillContainer.scaleX = 0.85f
        pillContainer.scaleY = 0.85f
        pillContainer.translationY = 0f

        expandToDynamicIsland(cutoutWidthDp)

        pillContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    /**
     * Smooth exit collapsing down.
     */
    fun animateExit(onEnd: () -> Unit) {
        mainHandler.removeCallbacks(idleCollapseRunnable)
        pillContainer.animate().cancel()

        pillContainer.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                pillContainer.visibility = View.INVISIBLE
                currentState = IslandState.HIDDEN
                onEnd()
            }
            .start()
    }

    fun resetIdleTimer() {
        mainHandler.removeCallbacks(idleCollapseRunnable)
        mainHandler.postDelayed(idleCollapseRunnable, IDLE_TIMEOUT_MS)
    }

    fun onDestroy() {
        mainHandler.removeCallbacks(idleCollapseRunnable)
        currentWidthAnimator?.cancel()
        pulseAnimatorSet?.cancel()
        pillContainer.animate().cancel()
    }
}
