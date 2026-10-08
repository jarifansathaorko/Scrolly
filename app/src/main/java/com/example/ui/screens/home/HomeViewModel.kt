package com.example.ui.screens.home

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.data.repository.IslandPlacementMode
import com.example.data.repository.NotchConfiguration
import com.example.data.repository.NotchType
import com.example.tracking.accessibility.AccessibilityHelper
import com.example.tracking.accessibility.ScrollyAccessibilityService
import com.example.tracking.detector.AppRecognitionEngine
import com.example.tracking.detector.NotchDetectionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Apps the counter supports.
 *
 * Sourced from [com.example.tracking.detector.AppRecognitionEngine] rather than a
 * hand-maintained list, so the "Active Apps" row can never drift out of sync with what
 * the detector actually recognises.
 */
data class TrackedApp(
    val packageName: String,
    val label: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingRepo = ScrollyApp.instance.trackingRepository
    private val notchRepo = ScrollyApp.instance.notchSettingsRepository
    private val app = ScrollyApp.instance

    val todayStats: StateFlow<DailyStatsEntity?> = trackingRepo.getTodayStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val liveTotalScrolls: StateFlow<Int> = trackingRepo.todayTotalScrolls

    val todayAppStats: StateFlow<List<AppStatsEntity>> = trackingRepo.getTodayAppStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appLimits: StateFlow<List<AppLimitEntity>> = trackingRepo.getAppLimitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Real current streak (consecutive days at or under the daily goal).
     *
     * The Home screen previously showed a hardcoded `7`, which was both wrong and
     * impossible to keep in step with the Profile screen's own streak.
     */
    val streakDays: StateFlow<Int> = ScrollyApp.instance.gamificationRepository.userProfile
        .map { it.streakDays }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isServiceActive: StateFlow<Boolean> =
        ScrollyAccessibilityService.isServiceActive

    val notchConfig: StateFlow<NotchConfiguration> = notchRepo.configFlow

    /** Tracked apps paired with today's real counts, ready for the "Active Apps" row. */
    val trackedApps: StateFlow<List<Pair<TrackedApp, Int>>> = combine(
        todayAppStats,
        appLimits
    ) { stats, limits ->
        val counts = stats.associate { it.packageName to it.scrollCount }
        val labels = limits.associate { it.packageName to it.appName }
        val rows = canonicalTrackedApps()
            .map { tracked -> tracked to (counts[tracked.packageName] ?: 0) }
            .toMutableList()

        // Surface any limit the user added that the detector does not know about, so a
        // configured limit is never invisible on this screen.
        val known = rows.map { it.first.packageName }.toSet()
        limits.filter { it.packageName !in known }.forEach { extra ->
            rows += TrackedApp(extra.packageName, labels[extra.packageName] ?: extra.appName) to 0
        }
        rows
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The distinct apps Scrolly recognises, collapsing regional/lite variants.
     *
     * Derived from [AppRecognitionEngine.SUPPORTED_APPS] rather than a hand-maintained
     * list, so this row can never drift out of sync with what the detector recognises.
     */
    private fun canonicalTrackedApps(): List<TrackedApp> =
        AppRecognitionEngine.SUPPORTED_APPS.keys
            .map { canonicalPackageFor(it) }
            .distinct()
            .map { pkg ->
                TrackedApp(pkg, AppRecognitionEngine.getAppName(pkg))
            }

    private fun canonicalPackageFor(packageName: String): String = when {
        packageName.contains("instagram") -> "com.instagram.android"
        packageName.contains("youtube") -> "com.google.android.youtube"
        packageName.contains("musically") || packageName.contains("ugc.trill") ||
            packageName.contains("ugc.aweme") -> "com.zhiliaoapp.musically"
        packageName.contains("facebook") -> "com.facebook.katana"
        packageName.contains("snapchat") -> "com.snapchat.android"
        packageName.contains("spotify") -> "com.spotify.music"
        else -> packageName
    }

    // ── Permissions ───────────────────────────────────────────────────────

    private val _isOverlayPermissionGranted = MutableStateFlow(false)
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _showBatteryOptimizationDialog = MutableStateFlow(false)
    val showBatteryOptimizationDialog: StateFlow<Boolean> = _showBatteryOptimizationDialog.asStateFlow()

    private val _showOverlayPermissionDialog = MutableStateFlow(false)
    val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

    // ── Overlay preview ───────────────────────────────────────────────────

    private val _isNotchBarPreviewVisible = MutableStateFlow(false)
    val isNotchBarPreviewVisible: StateFlow<Boolean> = _isNotchBarPreviewVisible.asStateFlow()

    private val _isNotchCustomizerOpen = MutableStateFlow(false)
    val isNotchCustomizerOpen: StateFlow<Boolean> = _isNotchCustomizerOpen.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        _isOverlayPermissionGranted.value = AccessibilityHelper.canDrawOverlays(context)
        _isBatteryOptimizationIgnored.value = AccessibilityHelper.isBatteryOptimizationIgnored(context)
    }

    fun promptBatteryOptimizationDialog() {
        _showBatteryOptimizationDialog.value = true
    }

    fun dismissBatteryOptimizationDialog() {
        _showBatteryOptimizationDialog.value = false
    }

    fun requestIgnoreBatteryOptimization() {
        _showBatteryOptimizationDialog.value = false
        AccessibilityHelper.requestIgnoreBatteryOptimization(getApplication())
    }

    fun promptOverlayPermissionDialog() {
        _showOverlayPermissionDialog.value = true
    }

    fun dismissOverlayPermissionDialog() {
        _showOverlayPermissionDialog.value = false
    }

    fun requestOverlayPermission() {
        _showOverlayPermissionDialog.value = false
        AccessibilityHelper.openOverlaySettings(getApplication())
    }

    // ── Notch customiser ──────────────────────────────────────────────────

    fun toggleNotchCustomizer() {
        _isNotchCustomizerOpen.value = !_isNotchCustomizerOpen.value
    }

    fun selectNotchType(type: NotchType) = notchRepo.selectNotchType(type)

    fun setPlacementMode(mode: IslandPlacementMode) = notchRepo.setPlacementMode(mode)

    fun setCutoutGapWidth(width: Int) = notchRepo.setCutoutGapWidth(width)

    fun triggerAutoDetectCutout(activity: Activity?) {
        if (activity == null) return
        val detected = NotchDetectionHelper.detectFromWindow(activity.window, activity)
        notchRepo.autoCalibrateWithCutout(detected)
    }

    fun adjustNotchPosition(deltaX: Int, deltaY: Int) = notchRepo.adjustPosition(deltaX, deltaY)

    fun setNotchPosition(x: Int, y: Int) = notchRepo.setPosition(x, y)

    fun setDynamicIslandMode(enabled: Boolean) = notchRepo.setDynamicIslandMode(enabled)

    fun setShowCutoutGuide(show: Boolean) = notchRepo.setShowCutoutGuide(show)

    fun resetNotchDefaults() = notchRepo.resetToDefaults()

    /**
     * Toggles the on-screen Dynamic Island preview.
     *
     * Previously the Compose preview and the real WindowManager overlay were driven by one
     * flag, so toggling could leave a window-attached overlay running invisibly off-screen.
     * They are now independent, and a hide always detaches.
     */
    fun toggleNotchBarPreview() {
        setNotchBarPreviewVisible(!_isNotchBarPreviewVisible.value)
    }

    fun setNotchBarPreviewVisible(visible: Boolean) {
        _isNotchBarPreviewVisible.value = visible
        val bar = app.getActiveFloatingBar()

        if (visible) {
            if (!AccessibilityHelper.canDrawOverlays(getApplication())) {
                _showOverlayPermissionDialog.value = true
                // Do not show an overlay we cannot legally draw; the prompt explains why.
                _isNotchBarPreviewVisible.value = false
                return
            }
            bar?.show(
                appName = "Reels",
                scrollCount = liveTotalScrolls.value,
                dailyGoal = todayStats.value?.goal ?: 100
            )
        } else {
            bar?.hide(immediate = true, keepAttached = false)
        }
    }

    fun openAccessibilitySettings() =
        AccessibilityHelper.openAccessibilitySettings(getApplication())

    fun openAppDetailsSettings() =
        AccessibilityHelper.openAppDetailsSettings(getApplication())

    // ── Simulation ────────────────────────────────────────────────────────

    /**
     * Logs [count] synthetic scrolls for [packageName].
     *
     * Used by the in-app tester. It writes through the same repository path as real
     * detection, so limits, streaks, streaks/XP and the overlay all react identically —
     * rather than only moving the displayed number.
     */
    fun simulateScroll(packageName: String = DEFAULT_SIM_PACKAGE, count: Int = 1) {
        if (count <= 0) return
        viewModelScope.launch {
            trackingRepo.recordScroll(
                packageName = packageName,
                appName = AppRecognitionEngine.getAppName(packageName),
                delta = count
            )
            val total = liveTotalScrolls.value
            app.getActiveFloatingBar()?.updateCount(
                total,
                AppRecognitionEngine.getAppName(packageName)
            )
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { trackingRepo.setDailyGoal(goal) }
    }

    companion object {
        const val DEFAULT_SIM_PACKAGE = "com.instagram.android"
    }
}
