package com.example.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.entity.AchievementEntity
import com.example.data.repository.FriendData
import com.example.data.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val gamificationRepo = ScrollyApp.instance.gamificationRepository
    private val socialRepo = ScrollyApp.instance.socialRepository
    private val firebaseManager = ScrollyApp.instance.firebaseManager

    /**
     * Live, data-derived profile statistics.
     *
     * Replaces a `MutableStateFlow` seeded once from `getUserProfile()`, which returned
     * hardcoded values and never updated as scrolls were logged.
     */
    val userProfile: StateFlow<UserProfile?> = gamificationRepo.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _friends = MutableStateFlow<List<FriendData>>(emptyList())
    val friends: StateFlow<List<FriendData>> = _friends.asStateFlow()

    private val _isInviteAvailable = MutableStateFlow(false)
    val isInviteAvailable: StateFlow<Boolean> = _isInviteAvailable.asStateFlow()

    val achievements: StateFlow<List<AchievementEntity>> = gamificationRepo.getAchievementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        _isInviteAvailable.value = firebaseManager.isBackendReady
        loadFriends()
    }

    fun updateUsername(newUsername: String) {
        if (newUsername.isBlank()) return
        viewModelScope.launch {
            gamificationRepo.updateUsername(newUsername)
        }
    }

    fun reloadFriends() = loadFriends()

    private fun loadFriends() {
        viewModelScope.launch {
            _friends.value = socialRepo.getFriends()
        }
    }

    /**
     * The signed-in uid used to build an invite link.
     *
     * Goes through [com.example.data.repository.FirebaseManager] rather than
     * `FirebaseAuth.getInstance()` directly. The direct call threw an uncaught
     * `IllegalStateException` whenever Auth was unavailable — which is the normal state
     * for a build without `google-services.json` — crashing the screen on composition.
     */
    fun getMyUid(): String? = firebaseManager.currentUid
}