package com.example.ui.screens.battles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.entity.FriendBattleEntity
import com.example.data.repository.FirebaseManager
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class BattlesViewModel(application: Application) : AndroidViewModel(application) {

    private val socialRepo: SocialRepository = ScrollyApp.instance.socialRepository
    private val trackingRepo = ScrollyApp.instance.trackingRepository
    private val firebaseManager: FirebaseManager = ScrollyApp.instance.firebaseManager

    val battles: StateFlow<List<FriendBattleEntity>> = socialRepo.getBattlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Drives the screen's empty state instead of rendering a blank card. */
    val hasBattles: StateFlow<Boolean> = battles
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _todayScrolls = MutableStateFlow(0)
    val todayScrolls: StateFlow<Int> = _todayScrolls.asStateFlow()

    private val _battleMessage = MutableStateFlow<String?>(null)
    val battleMessage: StateFlow<String?> = _battleMessage.asStateFlow()

    /** False when no backend identity exists, so the invite path can be explained. */
    private val _isInviteAvailable = MutableStateFlow(false)
    val isInviteAvailable: StateFlow<Boolean> = _isInviteAvailable.asStateFlow()

    init {
        _isInviteAvailable.value = firebaseManager.isBackendReady

        viewModelScope.launch {
            trackingRepo.todayTotalScrolls.collect { _todayScrolls.value = it }
        }

        // Results depend on the user's own total, so keep them in step with it.
        viewModelScope.launch {
            trackingRepo.todayTotalScrolls.collect { socialRepo.refreshBattleOutcomes() }
        }

        // A new day invalidates every result.
        viewModelScope.launch {
            trackingRepo.trackedDate.collect { socialRepo.refreshBattleOutcomes() }
        }
    }

    /**
     * Starts a battle against [username].
     *
     * The previous implementation fabricated the score — `userScrolls` hardcoded to 36,
     * `friendScrolls` a random 50–120 — and stored `date = "Today"`, a literal string in
     * a column typed and queried as `yyyy-MM-dd`. That broke date handling and made the
     * scoreboard fiction. A new battle now starts from both participants' real logged
     * counts on a real date.
     */
    fun addFriendChallenge(username: String, friendName: String) {
        val handle = username.trim().removePrefix("@")
        if (handle.isEmpty()) {
            _battleMessage.value = "Enter a friend's @username first."
            return
        }

        viewModelScope.launch {
            val userScrolls = trackingRepo.todayTotalScrolls.value
            val friendScrolls = friendScrollsFor(handle)

            socialRepo.upsertBattle(
                FriendBattleEntity(
                    // Stable per friend + day, so re-challenging updates rather than duplicates.
                    id = "battle_${handle.lowercase(Locale.ROOT)}_${trackingRepo.trackedDate.value}",
                    friendName = friendName.ifBlank { handle }
                        .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    friendUsername = handle,
                    userScrolls = userScrolls,
                    friendScrolls = friendScrolls,
                    date = trackingRepo.trackedDate.value,
                    status = socialRepo.statusFor(userScrolls, friendScrolls)
                )
            )
            _battleMessage.value = "Battle started against @$handle"
        }
    }

    fun clearMessage() {
        _battleMessage.value = null
    }

    /**
     * The friend's logged total when their account is linked.
     *
     * Falls back to 0 — a standing start — rather than a random number when the friend
     * has no connected account or the backend is unreachable.
     */
    private suspend fun friendScrollsFor(handle: String): Int {
        val friends = runCatching { socialRepo.getFriends() }.getOrDefault(emptyList())
        return friends.firstOrNull { it.username.equals(handle, ignoreCase = true) }
            ?.todayScrolls
            ?: 0
    }

    companion object {
        /** First name only, for the featured matchup heading. */
        fun firstNameOf(fullName: String): String =
            fullName.trim().split(' ', '-')
                .firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                .orEmpty()
                .ifBlank { fullName }
    }
}