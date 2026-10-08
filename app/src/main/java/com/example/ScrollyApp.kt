package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.ScrollyDatabase
import com.example.data.repository.FirebaseManager
import com.example.data.repository.GamificationRepository
import com.example.data.repository.NotchSettingsRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.TrackingRepository
import com.example.tracking.accessibility.AccessibilityHelper
import com.example.tracking.accessibility.ScrollyAccessibilityService
import com.example.tracking.overlay.NotchFloatingBarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Composition root for the app graph.
 *
 * Everything is created here once and exposed as read-only properties, so the
 * `ScrollyApp.instance` lookup used throughout the codebase has exactly one
 * initialisation site. Repositories are constructor-injected into ViewModels, which is
 * what makes them testable without an Android `Application`.
 */
class ScrollyApp : Application() {

    /** Application-lifetime scope. Cancelled in [onTerminate]; not used for UI work. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: ScrollyDatabase
        private set

    lateinit var trackingRepository: TrackingRepository
        private set

    lateinit var gamificationRepository: GamificationRepository
        private set

    lateinit var socialRepository: SocialRepository
        private set

    lateinit var notchSettingsRepository: NotchSettingsRepository
        private set

    lateinit var firebaseManager: FirebaseManager
        private set

    private var appFloatingBar: NotchFloatingBarManager? = null

    override fun onCreate() {
        super.onCreate()
        // Set first: repositories constructed below already reach back through `instance`.
        instance = this

        firebaseManager = FirebaseManager(this)

        database = ScrollyDatabase.getInstance(this)
        val dao = database.scrollyDao()
        trackingRepository = TrackingRepository(dao)
        gamificationRepository = GamificationRepository(dao, this, firebaseManager, trackingRepository)
        socialRepository = SocialRepository(dao, firebaseManager, trackingRepository)
        notchSettingsRepository = NotchSettingsRepository(this)

        // Challenge claim state is per-day, so it resets with the tracked date.
        trackingRepository.addOnNewDayListener { gamificationRepository.resetChallenges() }

        // Scoped to the application instead of GlobalScope, which previously leaked a
        // Job for the whole process lifetime and could outlive a destroyed service.
        appScope.launch {
            firebaseManager.signInAnonymously()
        }

        // Housekeeping on launch: friend_battles has no retention policy of its own, so a
        // long-lived install would accumulate a row per challenge forever.
        appScope.launch {
            runCatching {
                socialRepository.pruneBattlesOlderThan(ScrollyDatabase.getDateOffset(BATTLE_RETENTION_DAYS))
            }.onFailure { Log.w(TAG, "Battle pruning failed", it) }
        }
    }

    /**
     * Retrieves the overlay that is currently authoritative.
     *
     * Prefers the accessibility service's overlay (the one that can draw over other
     * apps), and falls back to an application-context overlay when the service is not
     * running but the overlay permission has been granted.
     */
    fun getActiveFloatingBar(): NotchFloatingBarManager? {
        val serviceInstance = ScrollyAccessibilityService.activeServiceInstance
        if (serviceInstance != null) {
            serviceInstance.getFloatingBarManager()?.let { return it }
        }

        if (AccessibilityHelper.canDrawOverlays(this)) {
            appFloatingBar?.let { return it }
            return try {
                NotchFloatingBarManager(this).also { appFloatingBar = it }
            } catch (e: Exception) {
                Log.w(TAG, "Could not initialize appFloatingBar", e)
                null
            }
        }

        return null
    }

    override fun onTerminate() {
        appScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        appFloatingBar?.onDestroy()
        appFloatingBar = null
        super.onTerminate()
    }

    companion object {
        private const val TAG = "ScrollyApp"

        /** Battles older than this are pruned on launch. */
        private const val BATTLE_RETENTION_DAYS = -30

        /**
         * The running application instance.
         *
         * `lateinit` is intentional: every consumer is either an `AndroidViewModel` or
         * an Android component, both of which are created after `Application.onCreate`,
         * so the field is always initialised by the time it is read.
         */
        lateinit var instance: ScrollyApp
            private set
    }
}