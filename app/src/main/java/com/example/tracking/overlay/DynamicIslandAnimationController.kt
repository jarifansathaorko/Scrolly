package com.example.tracking.overlay

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup

/**
 * DynamicIslandAnimationController
 * Optimized controller for the floating dynamic bar overlay.
 * Maintains a solid, stable, static-sized compact capsule [ 🔥  <count>  • ]
 * without unwanted expanding animations, keeping the count away from the notch camera.
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
        VISIBLE
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentState = IslandState.HIDDEN

    fun getState(): IslandState = currentState

    /**
     * Enforces the solid, sleek compact pill shape without expanding across the screen.
     */
    fun enforceStaticCompactPill() {
        centerSpacerView.visibility = View.GONE
        compactIconView.visibility = View.GONE

        leftWingView.visibility = View.VISIBLE
        leftWingView.alpha = 1f
        rightWingView.visibility = View.VISIBLE
        rightWingView.alpha = 1f

        pillContainer.visibility = View.VISIBLE
        pillContainer.alpha = 1f
        pillContainer.scaleX = 1f
        pillContainer.scaleY = 1f
        pillContainer.translationY = 0f

        val lp = pillContainer.layoutParams
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            pillContainer.layoutParams = lp
        }
        currentState = IslandState.VISIBLE
        onLayoutRequested?.invoke()
    }

    /**
     * Backwards-compatible hook that ensures the bar stays in its compact first-shown form.
     */
    fun expandToDynamicIsland(
        cutoutWidthDp: Int = 0,
        onComplete: (() -> Unit)? = null
    ) {
        enforceStaticCompactPill()
        onComplete?.invoke()
    }

    /**
     * No-op to avoid morphing or hiding numbers.
     */
    fun transitionToCompactPill(onComplete: (() -> Unit)? = null) {
        enforceStaticCompactPill()
        onComplete?.invoke()
    }

    /**
     * Highly optimized scroll trigger: No layout resizing or expansion.
     * Keeps the count perfectly in place.
     */
    fun triggerScrollBounce(cutoutWidthDp: Int = 0) {
        if (currentState != IslandState.VISIBLE) {
            enforceStaticCompactPill()
        }
    }

    /**
     * Shows the dynamic bar instantly in its clean, first-shown compact form.
     */
    fun animateEntrance(cutoutWidthDp: Int = 0) {
        enforceStaticCompactPill()
    }

    /**
     * Smooth exit when leaving reels/shorts.
     */
    fun animateExit(onEnd: () -> Unit) {
        pillContainer.visibility = View.GONE
        currentState = IslandState.HIDDEN
        onEnd()
    }

    fun resetIdleTimer() {
        // Bar remains steady while reels are open
    }

    fun onDestroy() {
        pillContainer.animate().cancel()
    }
}
