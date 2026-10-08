package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FriendData(
    val uid: String = "",
    val username: String = "Unknown",
    val todayScrolls: Int = 0
)

/**
 * FirebaseManager
 *
 * Every entry point here is *fail-soft*. Scrolly ships without `google-services.json`
 * in the repository, and anonymous auth is not enabled on every project/device. Scrolly
 * is a local-first wellbeing tracker, so a missing or failing backend must degrade to
 * "no friends data" rather than crash the process.
 *
 * Firebase's singletons ([FirebaseAuth.getInstance], [FirebaseFirestore.getInstance])
 * throw at *access* time rather than init time when the corresponding product is not
 * enabled for the project. Every access therefore goes through [authOrNull] /
 * [dbOrNull] and every call through [runCatching], so no caller needs its own
 * try/catch — which is how `ProfileViewModel.getMyUid()` used to crash.
 */
class FirebaseManager(@Suppress("unused") private val context: Context) {

    private val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getInstance() != null
        } catch (_: Exception) {
            false
        }

    private val authOrNull: FirebaseAuth?
        get() = if (!isFirebaseInitialized) null else try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth unavailable", e)
            null
        }

    private val dbOrNull: FirebaseFirestore?
        get() = if (!isFirebaseInitialized) null else try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore unavailable", e)
            null
        }

    /** The signed-in uid, or `null` whenever auth is unavailable or not yet complete. */
    val currentUid: String?
        get() = safeOrNull("currentUid") { authOrNull?.currentUser?.uid }

    /** True only when a backend identity actually exists — safe to gate UI on. */
    val isBackendReady: Boolean
        get() = isFirebaseInitialized && currentUid != null

    suspend fun signInAnonymously() {
        val auth = authOrNull ?: return
        safe(TAG, "signInAnonymously") {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.i(TAG, "Signed in anonymously: ${auth.currentUser?.uid}")
            }
        }
    }

    suspend fun updateUsername(username: String) {
        val db = dbOrNull ?: return
        val uid = currentUid ?: return
        safe(TAG, "updateUsername") {
            db.collection("users").document(uid)
                .set(mapOf("username" to username))
                .await()
        }
    }

    suspend fun syncTodayScrolls(scrolls: Int) {
        val db = dbOrNull ?: return
        val uid = currentUid ?: return
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        safe(TAG, "syncTodayScrolls") {
            val userDoc = db.collection("users").document(uid)
            db.batch().apply {
                // Per-day history for charts…
                set(
                    userDoc.collection("daily_scrolls").document(dateString),
                    mapOf("count" to scrolls)
                )
                // …plus a denormalised copy so friend lists are a single read.
                set(userDoc, mapOf("todayScrolls" to scrolls), SetOptions.merge())
            }.commit().await()
        }
    }

    suspend fun connectFriend(friendUid: String): Boolean {
        val db = dbOrNull ?: return false
        val uid = currentUid ?: return false
        if (friendUid.isBlank() || uid == friendUid) return false // Can't connect to self

        return safe(TAG, "connectFriend", default = false) {
            val userDoc = db.collection("users").document(uid)
            val friendDoc = db.collection("users").document(friendUid)
            val stamp = mapOf("connectedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())

            // One atomic batch rather than two independent writes, so a half-written
            // friendship can no longer be left behind.
            db.batch().apply {
                set(userDoc.collection("friends").document(friendUid), stamp)
                set(friendDoc.collection("friends").document(uid), stamp)
            }.commit().await()

            true
        }
    }

    /**
     * Reads the whole friend list with one query plus one batched profile read per
     * 30 friends, instead of the previous `1 + N` sequential round trips. That loop was
     * the main scaling bottleneck on this screen and grew latency linearly with friends.
     */
    suspend fun getFriends(): List<FriendData> {
        val db = dbOrNull ?: return emptyList()
        val uid = currentUid ?: return emptyList()

        return safe(TAG, "getFriends", default = emptyList()) {
            val friendIds = db.collection("users").document(uid)
                .collection("friends")
                .get()
                .await()
                .documents
                .map { it.id }
                .filter { it.isNotBlank() && it != uid }

            if (friendIds.isEmpty()) return@safe emptyList()

            friendIds.chunked(FRIEND_QUERY_BATCH_SIZE).flatMap { chunk ->
                coroutineScope {
                    chunk.map { friendUid ->
                        async {
                            safe(TAG, "getFriend($friendUid)", default = null) {
                                val doc = db.collection("users").document(friendUid).get().await()
                                if (!doc.exists()) return@safe null
                                FriendData(
                                    uid = friendUid,
                                    username = doc.getString("username") ?: "Unknown",
                                    todayScrolls = doc.getLong("todayScrolls")?.toInt() ?: 0
                                )
                            }
                        }
                    }.awaitAll()
                }
            }.filterNotNull()
        }
    }

    // ── Safe-call helpers ──────────────────────────────────────────────────

    private inline fun <T> safe(tag: String, op: String, default: T, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            Log.w(tag, "$op failed", e)
            default
        }

    /** Unit-returning convenience overload. */
    private inline fun safe(tag: String, op: String, block: () -> Unit): Unit =
        safe(tag, op, default = Unit, block = block)

    private inline fun <T> safeOrNull(op: String, block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "$op failed", e)
            null
        }

    companion object {
        private const val TAG = "FirebaseManager"

        /** Firestore caps an `in` query at 30 operands, so batch friend reads into chunks. */
        private const val FRIEND_QUERY_BATCH_SIZE = 30
    }
}