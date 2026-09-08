package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.ScrollyDatabase
import com.example.data.repository.GamificationRepository
import com.example.data.repository.NotchSettingsRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.TrackingRepository
import com.example.tracking.accessibility.AccessibilityHelper
import com.example.tracking.accessibility.ScrollyAccessibilityService
import com.example.tracking.overlay.NotchFloatingBarManager

class ScrollyApp : Application() {

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

    private var appFloatingBar: NotchFloatingBarManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = ScrollyDatabase.getInstance(this)
        trackingRepository = TrackingRepository(database.scrollyDao())
        gamificationRepository = GamificationRepository(database.scrollyDao())
        socialRepository = SocialRepository(database.scrollyDao())
        notchSettingsRepository = NotchSettingsRepository(this)
    }

    /**
     * Retrieves the active NotchFloatingBarManager.
     * Prioritizes the service's overlay if running, or initializes an application-level overlay if allowed.
     */
    fun getActiveFloatingBar(): NotchFloatingBarManager? {
        val serviceInstance = ScrollyAccessibilityService.activeServiceInstance
        if (serviceInstance != null) {
            val bar = serviceInstance.getFloatingBarManager()
            if (bar != null) return bar
        }

        if (AccessibilityHelper.canDrawOverlays(this)) {
            if (appFloatingBar == null) {
                try {
                    appFloatingBar = NotchFloatingBarManager(this)
                } catch (e: Exception) {
                    Log.w("ScrollyApp", "Could not initialize appFloatingBar", e)
                }
            }
            return appFloatingBar
        }

        return null
    }

    companion object {
        lateinit var instance: ScrollyApp
            private set
    }
}
