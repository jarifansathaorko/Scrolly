package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class IslandPlacementMode(
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    WRAP_AROUND_NOTCH(
        title = "Wrap Around Notch (Dynamic Island)",
        description = "Surrounds camera cutout with content on left/right wings like iPhone",
        iconEmoji = "🏝️"
    ),
    BELOW_NOTCH(
        title = "Below Notch (Floating Capsule)",
        description = "Floats cleanly beneath the camera cutout and status bar",
        iconEmoji = "⬇️"
    )
}

enum class NotchType(
    val title: String,
    val description: String,
    val defaultOffsetX: Int,
    val defaultOffsetY: Int,
    val defaultCutoutWidth: Int,
    val iconEmoji: String
) {
    DYNAMIC_ISLAND(
        title = "Dynamic Island (iPhone)",
        description = "Apple-style compact pill safely below camera cutout",
        defaultOffsetX = 0,
        defaultOffsetY = 38,
        defaultCutoutWidth = 0,
        iconEmoji = "🏝️"
    ),
    ROUND_PUNCH_HOLE_CENTER(
        title = "Punch-Hole (Center)",
        description = "Standard round top-center camera",
        defaultOffsetX = 0,
        defaultOffsetY = 38,
        defaultCutoutWidth = 0,
        iconEmoji = "⚪"
    ),
    WIDE_NOTCH(
        title = "Wide Notch (Classic)",
        description = "Broad notch requiring wide center clearance",
        defaultOffsetX = 0,
        defaultOffsetY = 48,
        defaultCutoutWidth = 84,
        iconEmoji = "📱"
    ),
    WATERDROP_TEARDROP(
        title = "Waterdrop (Teardrop)",
        description = "U/V tear-shaped camera notch",
        defaultOffsetX = 0,
        defaultOffsetY = 0,
        defaultCutoutWidth = 40,
        iconEmoji = "💧"
    ),
    PUNCH_HOLE_LEFT(
        title = "Punch-Hole (Left)",
        description = "Top-left corner camera cutout",
        defaultOffsetX = -80,
        defaultOffsetY = 0,
        defaultCutoutWidth = 32,
        iconEmoji = "↖️"
    ),
    PUNCH_HOLE_RIGHT(
        title = "Punch-Hole (Right)",
        description = "Top-right corner camera cutout",
        defaultOffsetX = 80,
        defaultOffsetY = 0,
        defaultCutoutWidth = 32,
        iconEmoji = "↗️"
    ),
    BELOW_STATUS_BAR(
        title = "Below Status Bar",
        description = "Positioned safely below status bar without obscuring any UI",
        defaultOffsetX = 0,
        defaultOffsetY = 42,
        defaultCutoutWidth = 32,
        iconEmoji = "⬇️"
    )
}

data class HardwareCutoutInfo(
    val hasCutout: Boolean = false,
    val centerX: Int = 0,
    val top: Int = 0,
    val bottom: Int = 28,
    val width: Int = 34,
    val height: Int = 28,
    val safeInsetTop: Int = 32
)

data class NotchConfiguration(
    val notchType: NotchType = NotchType.DYNAMIC_ISLAND,
    val placementMode: IslandPlacementMode = IslandPlacementMode.BELOW_NOTCH,
    val offsetX: Int = 0, // Horizontal offset in dp (-150 to +150)
    val offsetY: Int = 38, // Vertical offset in dp from top of physical screen (below camera cutout)
    val cutoutGapWidth: Int = 0, // Gap width in dp (0 for sleek unified compact pill)
    val isDynamicIslandMode: Boolean = true,
    val showCutoutGuide: Boolean = false,
    val autoAdjusted: Boolean = false,
    val detectedCutout: HardwareCutoutInfo = HardwareCutoutInfo()
)

class NotchSettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<NotchConfiguration> = _configFlow.asStateFlow()

    private fun loadConfig(): NotchConfiguration {
        val typeName = prefs.getString(KEY_NOTCH_TYPE, NotchType.DYNAMIC_ISLAND.name)
        val notchType = try {
            NotchType.valueOf(typeName ?: NotchType.DYNAMIC_ISLAND.name)
        } catch (e: Exception) {
            NotchType.DYNAMIC_ISLAND
        }

        val modeName = prefs.getString(KEY_PLACEMENT_MODE, IslandPlacementMode.BELOW_NOTCH.name)
        val placementMode = try {
            IslandPlacementMode.valueOf(modeName ?: IslandPlacementMode.BELOW_NOTCH.name)
        } catch (e: Exception) {
            IslandPlacementMode.BELOW_NOTCH
        }

        val defaultY = if (placementMode == IslandPlacementMode.BELOW_NOTCH && notchType.defaultOffsetY == 0) 38 else notchType.defaultOffsetY
        val offsetX = prefs.getInt(KEY_OFFSET_X, notchType.defaultOffsetX)
        val offsetY = prefs.getInt(KEY_OFFSET_Y, defaultY)
        val defaultGap = if (placementMode == IslandPlacementMode.BELOW_NOTCH) 0 else notchType.defaultCutoutWidth
        val cutoutGapWidth = prefs.getInt(KEY_CUTOUT_GAP_WIDTH, defaultGap)
        val isIsland = prefs.getBoolean(KEY_IS_DYNAMIC_ISLAND, true)
        val showGuide = prefs.getBoolean(KEY_SHOW_GUIDE, false)
        val autoAdjusted = prefs.getBoolean(KEY_AUTO_ADJUSTED, false)

        val hasCutout = prefs.getBoolean(KEY_HAS_CUTOUT, false)
        val cutoutCenterX = prefs.getInt(KEY_CUTOUT_CENTER_X, 0)
        val cutoutTop = prefs.getInt(KEY_CUTOUT_TOP, 0)
        val cutoutBottom = prefs.getInt(KEY_CUTOUT_BOTTOM, 28)
        val cutoutWidth = prefs.getInt(KEY_CUTOUT_WIDTH, 34)
        val cutoutHeight = prefs.getInt(KEY_CUTOUT_HEIGHT, 28)
        val safeTop = prefs.getInt(KEY_CUTOUT_SAFE_TOP, 32)

        return NotchConfiguration(
            notchType = notchType,
            placementMode = placementMode,
            offsetX = offsetX,
            offsetY = offsetY,
            cutoutGapWidth = cutoutGapWidth,
            isDynamicIslandMode = isIsland,
            showCutoutGuide = showGuide,
            autoAdjusted = autoAdjusted,
            detectedCutout = HardwareCutoutInfo(
                hasCutout = hasCutout,
                centerX = cutoutCenterX,
                top = cutoutTop,
                bottom = cutoutBottom,
                width = cutoutWidth,
                height = cutoutHeight,
                safeInsetTop = safeTop
            )
        )
    }

    fun autoCalibrateWithCutout(cutout: HardwareCutoutInfo) {
        val current = _configFlow.value
        val detectedNotchType = when {
            cutout.centerX < -20 -> NotchType.PUNCH_HOLE_LEFT
            cutout.centerX > 20 -> NotchType.PUNCH_HOLE_RIGHT
            cutout.width > 70 -> NotchType.WIDE_NOTCH
            cutout.height > 34 -> NotchType.WATERDROP_TEARDROP
            else -> NotchType.DYNAMIC_ISLAND
        }

        // Calibrate position directly over / below the notch area so camera never covers count
        val recommendedY = if (cutout.hasCutout) {
            (cutout.bottom + 4).coerceAtLeast(38)
        } else {
            (cutout.safeInsetTop + 4).coerceAtLeast(38)
        }

        val updated = current.copy(
            detectedCutout = cutout,
            notchType = detectedNotchType,
            placementMode = IslandPlacementMode.BELOW_NOTCH,
            offsetX = cutout.centerX,
            offsetY = recommendedY,
            cutoutGapWidth = 0,
            autoAdjusted = true
        )
        saveConfig(updated)
    }

    /**
     * Finds device hardware notch and automatically adjusts dynamic bar directly over the notch area.
     */
    fun findAndAdjustOverNotch(cutout: HardwareCutoutInfo) {
        autoCalibrateWithCutout(cutout)
    }

    fun setPlacementMode(mode: IslandPlacementMode) {
        val current = _configFlow.value
        val newY = if (mode == IslandPlacementMode.WRAP_AROUND_NOTCH) {
            current.detectedCutout.top.coerceAtLeast(0)
        } else {
            (current.detectedCutout.bottom + 4).coerceAtLeast(38)
        }
        val updated = current.copy(
            placementMode = mode,
            offsetY = newY
        )
        saveConfig(updated)
    }

    fun setCutoutGapWidth(gapWidth: Int) {
        val current = _configFlow.value
        val updated = current.copy(cutoutGapWidth = gapWidth.coerceIn(0, 160))
        saveConfig(updated)
    }

    fun selectNotchType(notchType: NotchType) {
        val current = _configFlow.value
        val updated = current.copy(
            notchType = notchType,
            offsetX = notchType.defaultOffsetX,
            offsetY = notchType.defaultOffsetY,
            cutoutGapWidth = notchType.defaultCutoutWidth
        )
        saveConfig(updated)
    }

    fun adjustPosition(deltaX: Int, deltaY: Int) {
        val current = _configFlow.value
        val newX = (current.offsetX + deltaX).coerceIn(-150, 150)
        val newY = (current.offsetY + deltaY).coerceIn(0, 140)
        val updated = current.copy(offsetX = newX, offsetY = newY)
        saveConfig(updated)
    }

    fun setPosition(x: Int, y: Int) {
        val current = _configFlow.value
        val newX = x.coerceIn(-150, 150)
        val newY = y.coerceIn(0, 140)
        val updated = current.copy(offsetX = newX, offsetY = newY)
        saveConfig(updated)
    }

    fun setDynamicIslandMode(enabled: Boolean) {
        val current = _configFlow.value
        val updated = current.copy(isDynamicIslandMode = enabled)
        saveConfig(updated)
    }

    fun setShowCutoutGuide(show: Boolean) {
        val current = _configFlow.value
        val updated = current.copy(showCutoutGuide = show)
        saveConfig(updated)
    }

    fun resetToDefaults() {
        val current = _configFlow.value
        val cutout = current.detectedCutout
        val safeY = if (cutout.hasCutout) maxOf(cutout.bottom + 4, 38) else maxOf(cutout.safeInsetTop + 4, 38)
        val updated = NotchConfiguration(
            notchType = NotchType.DYNAMIC_ISLAND,
            placementMode = IslandPlacementMode.BELOW_NOTCH,
            offsetX = cutout.centerX,
            offsetY = safeY,
            cutoutGapWidth = 0,
            isDynamicIslandMode = true,
            showCutoutGuide = false,
            autoAdjusted = cutout.hasCutout,
            detectedCutout = cutout
        )
        saveConfig(updated)
    }

    private fun saveConfig(config: NotchConfiguration) {
        prefs.edit()
            .putString(KEY_NOTCH_TYPE, config.notchType.name)
            .putString(KEY_PLACEMENT_MODE, config.placementMode.name)
            .putInt(KEY_OFFSET_X, config.offsetX)
            .putInt(KEY_OFFSET_Y, config.offsetY)
            .putInt(KEY_CUTOUT_GAP_WIDTH, config.cutoutGapWidth)
            .putBoolean(KEY_IS_DYNAMIC_ISLAND, config.isDynamicIslandMode)
            .putBoolean(KEY_SHOW_GUIDE, config.showCutoutGuide)
            .putBoolean(KEY_AUTO_ADJUSTED, config.autoAdjusted)
            .putBoolean(KEY_HAS_CUTOUT, config.detectedCutout.hasCutout)
            .putInt(KEY_CUTOUT_CENTER_X, config.detectedCutout.centerX)
            .putInt(KEY_CUTOUT_TOP, config.detectedCutout.top)
            .putInt(KEY_CUTOUT_BOTTOM, config.detectedCutout.bottom)
            .putInt(KEY_CUTOUT_WIDTH, config.detectedCutout.width)
            .putInt(KEY_CUTOUT_HEIGHT, config.detectedCutout.height)
            .putInt(KEY_CUTOUT_SAFE_TOP, config.detectedCutout.safeInsetTop)
            .apply()
        _configFlow.value = config
    }

    companion object {
        private const val PREFS_NAME = "scrolly_notch_preferences"
        private const val KEY_NOTCH_TYPE = "key_notch_type"
        private const val KEY_PLACEMENT_MODE = "key_placement_mode"
        private const val KEY_OFFSET_X = "key_offset_x"
        private const val KEY_OFFSET_Y = "key_offset_y"
        private const val KEY_CUTOUT_GAP_WIDTH = "key_cutout_gap_width"
        private const val KEY_IS_DYNAMIC_ISLAND = "key_is_dynamic_island"
        private const val KEY_SHOW_GUIDE = "key_show_guide"
        private const val KEY_AUTO_ADJUSTED = "key_auto_adjusted"
        private const val KEY_HAS_CUTOUT = "key_has_cutout"
        private const val KEY_CUTOUT_CENTER_X = "key_cutout_center_x"
        private const val KEY_CUTOUT_TOP = "key_cutout_top"
        private const val KEY_CUTOUT_BOTTOM = "key_cutout_bottom"
        private const val KEY_CUTOUT_WIDTH = "key_cutout_width"
        private const val KEY_CUTOUT_HEIGHT = "key_cutout_height"
        private const val KEY_CUTOUT_SAFE_TOP = "key_cutout_safe_top"
    }
}
