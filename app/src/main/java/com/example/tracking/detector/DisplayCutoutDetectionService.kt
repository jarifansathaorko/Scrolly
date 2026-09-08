package com.example.tracking.detector

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.DisplayCutout
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import com.example.data.repository.HardwareCutoutInfo
import com.example.data.repository.NotchSettingsRepository

/**
 * DisplayCutoutDetectionService
 * Dynamically queries WindowInsets and DisplayCutout to get the exact safe area
 * and bounds of the device's hardware camera notch / punch-hole cutout.
 */
object DisplayCutoutDetectionService {

    private const val TAG = "DisplayCutoutService"

    /**
     * Queries display cutout dynamically from system WindowManager or Window.
     */
    fun queryCutout(context: Context, window: Window? = null): HardwareCutoutInfo {
        // 1. Try Window decorView rootWindowInsets if window is provided
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            try {
                val insets = window.decorView.rootWindowInsets
                if (insets != null) {
                    val info = extractFromWindowInsets(insets, context)
                    if (info.hasCutout) return info
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query cutout from window", e)
            }
        }

        // 2. Query system WindowManager currentWindowMetrics (Android R+, API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                if (wm != null) {
                    val insets = wm.currentWindowMetrics.windowInsets
                    val info = extractFromWindowInsets(insets, context)
                    if (info.hasCutout) return info
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query cutout from WindowMetrics", e)
            }
        }

        // 3. Query Display cutout directly (Android Q+, API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display
                } else {
                    @Suppress("DEPRECATION")
                    (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
                }
                val cutout = display?.cutout
                if (cutout != null) {
                    return parseDisplayCutout(cutout, context)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query cutout from Display", e)
            }
        }

        // 4. Fallback status bar calculation
        return getStatusBarFallback(context)
    }

    /**
     * Extracts cutout info from WindowInsets.
     */
    fun extractFromWindowInsets(insets: WindowInsets, context: Context): HardwareCutoutInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = insets.displayCutout
            if (cutout != null) {
                return parseDisplayCutout(cutout, context)
            }
        }
        return getStatusBarFallback(context)
    }

    /**
     * Parses the DisplayCutout bounds to get exact notch center X, top, bottom, width, height, and safe top insets.
     */
    fun parseDisplayCutout(cutout: DisplayCutout, context: Context): HardwareCutoutInfo {
        val density = context.resources.displayMetrics.density
        val screenWidthPx = context.resources.displayMetrics.widthPixels
        val safeTopDp = (cutout.safeInsetTop / density).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val rects = cutout.boundingRects
            if (!rects.isNullOrEmpty()) {
                // Find cutout along top edge
                val topCutout = rects.firstOrNull { it.top <= cutout.safeInsetTop } ?: rects.first()

                val centerXPx = (topCutout.left + topCutout.right) / 2f
                val screenCenterXPx = screenWidthPx / 2f
                val offsetXPx = centerXPx - screenCenterXPx

                val centerXDp = (offsetXPx / density).toInt()
                val widthDp = (topCutout.width() / density).toInt().coerceAtLeast(24)
                val heightDp = (topCutout.height() / density).toInt().coerceAtLeast(24)
                val topDp = (topCutout.top / density).toInt()
                val bottomDp = (topCutout.bottom / density).toInt()

                Log.d(
                    TAG,
                    "Detected hardware cutout: bounds=[${topCutout.left},${topCutout.top},${topCutout.right},${topCutout.bottom}]px " +
                            "size=${widthDp}x${heightDp}dp centerXDp=$centerXDp safeTopDp=$safeTopDp"
                )

                return HardwareCutoutInfo(
                    hasCutout = true,
                    centerX = centerXDp,
                    top = topDp,
                    bottom = maxOf(bottomDp, heightDp),
                    width = widthDp,
                    height = heightDp,
                    safeInsetTop = safeTopDp
                )
            }
        }

        // If safeInsetTop > 0 but no boundingRects (some OEM devices)
        if (safeTopDp > 0) {
            return HardwareCutoutInfo(
                hasCutout = true,
                centerX = 0,
                top = 0,
                bottom = safeTopDp,
                width = 36,
                height = safeTopDp,
                safeInsetTop = safeTopDp
            )
        }

        return getStatusBarFallback(context)
    }

    private fun getStatusBarFallback(context: Context): HardwareCutoutInfo {
        val density = context.resources.displayMetrics.density
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarPx = if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        val statusBarDp = (statusBarPx / density).toInt().coerceAtLeast(28)

        return HardwareCutoutInfo(
            hasCutout = false,
            centerX = 0,
            top = 0,
            bottom = statusBarDp,
            width = 36,
            height = statusBarDp,
            safeInsetTop = statusBarDp
        )
    }

    /**
     * Synchronizes and applies the detected hardware cutout to the repository.
     */
    fun syncWithRepository(context: Context, repo: NotchSettingsRepository, window: Window? = null) {
        try {
            val detected = queryCutout(context, window)
            repo.autoCalibrateWithCutout(detected)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync cutout with repository", e)
        }
    }
}
