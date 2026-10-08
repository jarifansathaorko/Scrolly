package com.example.ui.screens.block

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.entity.AppLimitEntity
import com.example.data.repository.HealthyChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingRepo = ScrollyApp.instance.trackingRepository
    private val gamificationRepo = ScrollyApp.instance.gamificationRepository

    val appLimits: StateFlow<List<AppLimitEntity>> = trackingRepo.getAppLimitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Observable so a challenge's claimed state survives recomposition. */
    val challenges: StateFlow<List<HealthyChallenge>> = gamificationRepo.challenges

    /** Package the reward is granted against; matches the app Scrolly blocks first. */
    private val _rewardTargetPackage = MutableStateFlow(DEFAULT_REWARD_PACKAGE)
    val rewardTargetPackage: StateFlow<String> = _rewardTargetPackage.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Apps whose daily limit has been hit — drives the "blocked" treatment in the list. */
    val blockedPackages: StateFlow<Set<String>> = appLimits
        .map { limits -> limits.filter { it.isEnabled && it.isBlocked }.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setRewardTarget(packageName: String) {
        _rewardTargetPackage.value = packageName
    }

    fun updateLimit(packageName: String, newLimit: Int, warningThreshold: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            trackingRepo.updateAppLimit(packageName, newLimit, warningThreshold, isEnabled)
            _notice.value = if (isEnabled) {
                "Limit updated to $newLimit scrolls/day"
            } else {
                "Limit turned off"
            }
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            trackingRepo.setDailyGoal(goal)
            _notice.value = "Daily goal set to $goal scrolls"
        }
    }

    /**
     * Claims a challenge's bonus.
     *
     * [GamificationRepository.completeChallenge] returns `false` for an already-claimed
     * challenge, so the button cannot be spammed for unlimited extra scrolls the way the
     * previous unconditional call allowed.
     */
    fun completeChallengeAndEarn(challengeId: String, bonusScrolls: Int) {
        viewModelScope.launch {
            val granted = gamificationRepo.completeChallenge(challengeId, _rewardTargetPackage.value)
            _notice.value = if (granted) {
                "+$bonusScrolls bonus scrolls unlocked"
            } else {
                "Already claimed — pick another reset"
            }
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    private companion object {
        const val DEFAULT_REWARD_PACKAGE = "com.instagram.android"
    }
}