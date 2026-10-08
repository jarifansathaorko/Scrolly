package com.example.tracking.detector

import android.content.Context
import android.os.Build
import android.view.DisplayCutout
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import com.example.data.repository.HardwareCutoutInfo
import com.example.data.repository.NotchShape

/**
 * Facade over [DisplayCutoutDetectionService].
 *
 * Every entry point here is safe on `minSdk 24`: cutout APIs only exist from API 28, so
 * [parseDisplayCutout] is guarded and the insets-based helpers degrade to a status-bar
 * fallback. The unguarded pass-through previously produced a `NewApi` lint error and would
 * have thrown `NoClassDefFoundError` on API 24-27.
 */
object NotchDetectionHelper {

    /** Always safe; falls back to the status-bar estimate when there is no cutout. */
    fun detectFromWindowInsets(insets: WindowInsets, context: Context): HardwareCutoutInfo =
        DisplayCutoutDetectionService.extractFromWindowInsets(insets, context)

    /** Always safe; internally probes the cutout APIs it can reach. */
    fun detectFromWindow(window: Window, context: Context): HardwareCutoutInfo =
        DisplayCutoutDetectionService.queryCutout(context, window)

    fun getFallbackCutout(context: Context): HardwareCutoutInfo =
        DisplayCutoutDetectionService.queryCutout(context, null)

    /**
     * Parses a real [DisplayCutout], which only exists from API 28.
     *
     * On older devices this returns the status-bar fallback instead of crashing, so
     * callers need no version check of their own.
     */
    fun parseDisplayCutout(cutout: DisplayCutout?, context: Context): HardwareCutoutInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && cutout != null) {
            parseApi28(cutout, context)
        } else {
            DisplayCutoutDetectionService.fallbackCutout(context)
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun parseApi28(cutout: DisplayCutout, context: Context): HardwareCutoutInfo =
        DisplayCutoutDetectionService.parseDisplayCutout(cutout, context)

    /** Shape classification is pure arithmetic and available on every API level. */
    fun classifyNotchShape(centerXDp: Int, widthDp: Int, heightDp: Int): NotchShape =
        DisplayCutoutDetectionService.classifyNotchShape(centerXDp, widthDp, heightDp)
}