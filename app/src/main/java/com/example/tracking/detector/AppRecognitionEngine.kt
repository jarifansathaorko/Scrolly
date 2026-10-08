package com.example.tracking.detector

import com.example.data.repository.AppRecognitionNames

/**
 * Recognised short-form video apps.
 *
 * [SUPPORTED_APPS] is the single source of truth for *which* packages are tracked, and
 * display names delegate to [AppRecognitionNames] so the overlay, the database seed and
 * the UI can never disagree about what an app is called.
 *
 * Adding an app is a one-line change here — no other file needs to know its package name.
 */
object AppRecognitionEngine {

    private fun config(appName: String, feedKeywords: List<String>) =
        SupportedAppConfig(appName = appName, feedKeywords = feedKeywords)

    val SUPPORTED_APPS: Map<String, SupportedAppConfig> = mapOf(
        "com.instagram.android" to config(
            "Instagram Reels",
            listOf("reel", "clips", "feed", "viewer", "video", "media")
        ),
        "com.instagram.lite" to config(
            "Instagram Reels",
            listOf("reel", "clips", "feed", "viewer", "video", "media")
        ),
        "com.google.android.youtube" to config(
            "YouTube Shorts",
            listOf("shorts", "reel", "player", "watch", "vertical")
        ),
        "com.zhiliaoapp.musically" to config(
            "TikTok",
            listOf("feed", "video", "foryou", "player", "main", "vertical")
        ),
        "com.zhiliaoapp.musically.go" to config(
            "TikTok",
            listOf("feed", "video", "foryou", "player", "main", "vertical")
        ),
        "com.ss.android.ugc.trill" to config(
            "TikTok",
            listOf("feed", "video", "foryou", "player", "main", "vertical")
        ),
        "com.ss.android.ugc.aweme" to config(
            "TikTok",
            listOf("feed", "video", "foryou", "player", "main", "vertical")
        ),
        "com.snapchat.android" to config(
            "Snapchat Spotlight",
            listOf("spotlight", "discover", "story", "stories", "feed")
        ),
        "com.facebook.katana" to config(
            "Facebook Reels",
            listOf("reel", "watch", "video", "feed", "story")
        ),
        "com.facebook.lite" to config(
            "Facebook Reels",
            listOf("reel", "watch", "video", "feed", "story")
        ),
        "com.spotify.music" to config(
            "Spotify Video",
            listOf("canvas", "video", "clips", "discover", "npv")
        ),
        "com.spotify.lite" to config(
            "Spotify Video",
            listOf("canvas", "video", "clips", "discover", "npv")
        )
    )

    /** Packages Scrolly is allowed to observe at all. */
    fun isSupported(packageName: String?): Boolean =
        !packageName.isNullOrBlank() && SUPPORTED_APPS.containsKey(packageName)

    /**
     * Feed-accurate display name, e.g. `"Instagram Reels"`.
     *
     * Falls back to [AppRecognitionNames] for packages Scrolly does not track, because
     * the Block screen lets a user set a limit for an app the detector has never seen
     * and the Battle screen creates entries for invited friends.
     */
    fun getAppName(packageName: String): String =
        SUPPORTED_APPS[packageName]?.appName
            ?: AppRecognitionNames.displayNameFor(packageName)
}

data class SupportedAppConfig(
    val appName: String,
    val feedKeywords: List<String>,
    val isEnabled: Boolean = true
)