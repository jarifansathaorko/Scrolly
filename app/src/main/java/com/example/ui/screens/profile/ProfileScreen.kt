package com.example.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ScrollyCharacter
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardHighlight
import com.example.ui.theme.SleekCardSurface
import com.example.ui.theme.SleekCardSurfaceElevated
import com.example.ui.theme.SleekCardSurfaceSecondary
import com.example.ui.theme.SleekGreen
import com.example.ui.theme.SleekHeroText
import com.example.ui.theme.SleekPillBg
import com.example.ui.theme.SleekPillText
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.SleekTextSecondary
import java.text.NumberFormat

private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    // Null while the first database read is in flight; every field below has an
    // explicit empty state rather than defaulting to a fake number.
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val isInviteAvailable by viewModel.isInviteAvailable.collectAsStateWithLifecycle()

    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Edit Username", color = SleekTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUsername(newUsername)
                        showEditUsernameDialog = false
                    },
                    enabled = newUsername.isNotBlank()
                ) { Text("Save", color = SleekPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel", color = SleekTextMuted)
                }
            },
            containerColor = SleekCardSurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── PROFILE HEADER ────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(SleekCardHighlight)
                        .border(2.dp, SleekPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // The avatar tracks the user's own level, not a fixed sample value.
                    ScrollyCharacter(scrollCount = profile?.currentLevel?.times(20) ?: 15, size = 68.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            newUsername = profile?.username.orEmpty()
                            showEditUsernameDialog = true
                        }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "@${profile?.username ?: "…"}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit username",
                        tint = SleekTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = profile?.let { "Level ${it.currentLevel} Focus Scholar" } ?: "Loading…",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                val currentXp = profile?.currentXp ?: 0
                val xpForNext = (profile?.xpForNextLevel ?: 1).coerceAtLeast(1)
                val xpFraction = (currentXp.toFloat() / xpForNext).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "XP Progress",
                        style = MaterialTheme.typography.labelSmall.copy(color = SleekTextMuted)
                    )
                    Text(
                        text = "${formatCount(currentXp)} / ${formatCount(xpForNext)} XP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { xpFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = SleekPrimary,
                    trackColor = SleekCardHighlight,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── LIFETIME STATS (all derived from real data) ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = profile?.totalScrollsAllTime?.let(::formatCount) ?: "—",
                label = "Total Scrolls"
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = profile?.bestStreak?.let { "$it" } ?: "—",
                label = "Best Streak",
                valueColor = SleekPrimary,
                suffix = " days"
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = profile?.takeIf { it.hasBattles }?.battleRecord ?: "—",
                label = "Battles",
                valueColor = SleekGreen
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── CURRENT STREAK ────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current streak",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Text(
                        text = when (val streak = profile?.streakDays ?: 0) {
                            0 -> "Scroll past your limit today to break the streak."
                            1 -> "1 day under your daily limit. Keep it going!"
                            else -> "$streak days under your daily limit."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = SleekTextSecondary)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "🔥 ${profile?.activeDays ?: 0} active days",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── ACHIEVEMENTS ──────────────────────────────────────────────────
        Text(
            text = "Achievements & Badges",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (achievements.isEmpty()) {
            EmptyStateCard(message = "No achievements yet. Log a scroll to unlock your first badge.")
        } else {
            achievements.chunked(2).forEach { rowAchievements ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowAchievements.forEach { ach ->
                        AchievementCard(
                            title = ach.title,
                            description = ach.description,
                            isUnlocked = ach.isUnlocked,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAchievements.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── PRIVACY ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = SleekGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy-First Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scrolly tracks scrolling behavior, not what you watch. It never reads video titles, messages, passwords or personal screens. Your statistics stay on this device unless you explicitly connect with friends.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SleekTextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── FRIENDS ──────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Friends",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    if (friends.isNotEmpty()) {
                        TextButton(onClick = { viewModel.reloadFriends() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SleekTextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = SleekTextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isInviteAvailable) {
                    // The invite link needs a backend identity; explain instead of
                    // silently doing nothing when the button is tapped.
                    Text(
                        text = "Friend battles need a signed-in account. Yours isn't available right now, so this device's stats stay local.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted)
                    )
                } else if (friends.isEmpty()) {
                    Text(
                        text = "You haven't connected with any friends yet.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted)
                    )
                } else {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${friend.username}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = SleekTextPrimary
                                )
                            )
                            Text(
                                text = "${formatCount(friend.todayScrolls)} today",
                                style = MaterialTheme.typography.bodySmall.copy(color = SleekPrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val uid = viewModel.getMyUid() ?: return@Button
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Connect with me on Scrolly and let's battle our screen time! scrolly://invite?uid=$uid"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Invite Friend"))
                    },
                    enabled = isInviteAvailable,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimary,
                        disabledContainerColor = SleekCardSurfaceElevated,
                        disabledContentColor = SleekTextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isInviteAvailable) "Invite a Friend" else "Sign in to invite",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = SleekTextPrimary,
    suffix: String = ""
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value + suffix,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = SleekTextMuted),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AchievementCard(
    title: String,
    description: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                if (isUnlocked) SleekPrimary.copy(alpha = 0.4f) else SleekBorder,
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) SleekCardSurface else SleekCardSurfaceSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) SleekPillBg else SleekBorder.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) SleekHeroText else SleekTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isUnlocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) SleekTextPrimary else SleekTextMuted
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SleekTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted),
            modifier = Modifier.padding(18.dp)
        )
    }
}