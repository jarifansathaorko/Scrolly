package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.AppStatsEntity
import com.example.data.local.entity.DailyStatsEntity
import com.example.tracking.accessibility.AccessibilityHelper
import com.example.tracking.accessibility.ScrollyAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val dailyStats: DailyStatsEntity? = null,
    val appStats: List<AppStatsEntity> = emptyList(),
    val appLimits: List<AppLimitEntity> = emptyList(),
    val isAccessibilityActive: Boolean = false,
    val isOverlayGranted: Boolean = false,
    val isBatteryOptimizedIgnored: Boolean = false,
    val isSimulating: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingRepo = ScrollyApp.instance.trackingRepository
    private val notchRepo = ScrollyApp.instance.notchSettingsRepository

    val todayStats: StateFlow<DailyStatsEntity?> = trackingRepo.getTodayStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val liveTotalScrolls: StateFlow<Int> = trackingRepo.todayTotalScrolls

    val todayAppStats: StateFlow<List<AppStatsEntity>> = trackingRepo.getTodayAppStatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appLimits: StateFlow<List<AppLimitEntity>> = trackingRepo.getAppLimitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isServiceActive: StateFlow<Boolean> = ScrollyAccessibilityService.isServiceActive

    val notchConfig: StateFlow<com.example.data.repository.NotchConfiguration> = notchRepo.configFlow

    // Permissions & Battery Optimization States
    private val _isOverlayPermissionGranted = MutableStateFlow(
        AccessibilityHelper.canDrawOverlays(application)
    )
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(
        AccessibilityHelper.isBatteryOptimizationIgnored(application)
    )
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    // Dialog prompts
    private val _showBatteryOptimizationDialog = MutableStateFlow(false)
    val showBatteryOptimizationDialog: StateFlow<Boolean> = _showBatteryOptimizationDialog.asStateFlow()

    private val _showOverlayPermissionDialog = MutableStateFlow(false)
    val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

    private val _isNotchBarPreviewVisible = MutableStateFlow(false)
    val isNotchBarPreviewVisible: StateFlow<Boolean> = _isNotchBarPreviewVisible.asStateFlow()

    private val _isNotchCustomizerOpen = MutableStateFlow(false)
    val isNotchCustomizerOpen: StateFlow<Boolean> = _isNotchCustomizerOpen.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _isOverlayPermissionGranted.value = AccessibilityHelper.canDrawOverlays(app)
        _isBatteryOptimizationIgnored.value = AccessibilityHelper.isBatteryOptimizationIgnored(app)
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

    fun toggleNotchCustomizer() {
        _isNotchCustomizerOpen.value = !_isNotchCustomizerOpen.value
    }

    fun setNotchCustomizerOpen(open: Boolean) {
        _isNotchCustomizerOpen.value = open
    }

    fun selectNotchType(type: com.example.data.repository.NotchType) {
        notchRepo.selectNotchType(type)
    }

    fun setPlacementMode(mode: com.example.data.repository.IslandPlacementMode) {
        notchRepo.setPlacementMode(mode)
    }

    fun setCutoutGapWidth(width: Int) {
        notchRepo.setCutoutGapWidth(width)
    }

    fun triggerAutoDetectCutout(activity: android.app.Activity?) {
        if (activity != null) {
            val detected = com.example.tracking.detector.NotchDetectionHelper.detectFromWindow(activity.window, activity)
            notchRepo.autoCalibrateWithCutout(detected)
        }
    }

    fun adjustNotchPosition(deltaX: Int, deltaY: Int) {
        notchRepo.adjustPosition(deltaX, deltaY)
    }

    fun setNotchPosition(x: Int, y: Int) {
        notchRepo.setPosition(x, y)
    }

    fun setDynamicIslandMode(enabled: Boolean) {
        notchRepo.setDynamicIslandMode(enabled)
    }

    fun setShowCutoutGuide(show: Boolean) {
        notchRepo.setShowCutoutGuide(show)
    }

    fun resetNotchDefaults() {
        notchRepo.resetToDefaults()
    }

    fun toggleNotchBarPreview() {
        val willBeVisible = !_isNotchBarPreviewVisible.value
        _isNotchBarPreviewVisible.value = willBeVisible

        if (willBeVisible) {
            // Check if overlay permission is granted
            if (!AccessibilityHelper.canDrawOverlays(getApplication())) {
                _showOverlayPermissionDialog.value = true
            }
            ScrollyApp.instance.getActiveFloatingBar()?.show("Instagram Reels", liveTotalScrolls.value)
        } else {
            ScrollyApp.instance.getActiveFloatingBar()?.hide(immediate = false, keepAttached = false)
        }
    }

    fun setNotchBarPreviewVisible(visible: Boolean) {
        _isNotchBarPreviewVisible.value = visible
        if (visible) {
            ScrollyApp.instance.getActiveFloatingBar()?.show("Instagram Reels", liveTotalScrolls.value)
        } else {
            ScrollyApp.instance.getActiveFloatingBar()?.hide(immediate = false, keepAttached = false)
        }
    }

    fun checkAccessibilityPermission(): Boolean {
        return AccessibilityHelper.isAccessibilityServiceEnabled(
            getApplication(),
            ScrollyAccessibilityService::class.java
        )
    }

    fun openAccessibilitySettings() {
        AccessibilityHelper.openAccessibilitySettings(getApplication())
    }

    fun simulateScroll(packageName: String = "com.instagram.android", count: Int = 1) {
        viewModelScope.launch {
            val appName = when (packageName) {
                "com.instagram.android" -> "Instagram Reels"
                "com.google.android.youtube" -> "YouTube Shorts"
                "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme" -> "TikTok"
                "com.spotify.music", "com.spotify.lite" -> "Spotify Video"
                "com.snapchat.android" -> "Snapchat Spotlight"
                "com.facebook.katana" -> "Facebook Reels"
                else -> "App"
            }
            trackingRepo.recordScroll(packageName, appName, count)

            // Update live floating bar if preview is active or service running
            val newTotal = liveTotalScrolls.value + count
            ScrollyApp.instance.getActiveFloatingBar()?.updateCount(newTotal, appName)
        }
    }
}
