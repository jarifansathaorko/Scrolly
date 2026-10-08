package com.example.ui.screens.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AppLimitEntity
import com.example.data.repository.GamificationRepository
import com.example.data.repository.HealthyChallenge
import com.example.ui.components.AppIconBadge
import com.example.ui.components.ScrollyCharacter
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardHighlight
import com.example.ui.theme.SleekCardSurface
import com.example.ui.theme.SleekCardSurfaceElevated
import com.example.ui.theme.SleekGreen
import com.example.ui.theme.SleekHeroText
import com.example.ui.theme.SleekPillBg
import com.example.ui.theme.SleekPillText
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekRed
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.SleekTextSecondary

/** Scrolls granted by the temporary override. */
private const val EMERGENCY_OVERRIDE_SCROLLS = 10

@Composable
fun BlockScreen(
    viewModel: BlockViewModel,
    modifier: Modifier = Modifier
) {
    val limits by viewModel.appLimits.collectAsStateWithLifecycle()
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val blockedPackages by viewModel.blockedPackages.collectAsStateWithLifecycle()
    val rewardTarget by viewModel.rewardTargetPackage.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    var interventionPreview by remember { mutableStateOf<AppLimitEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── TITLE ────────────────────────────────────────────────────────
        Text(
            text = "Block Reels",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Set strict caps before doomscrolling takes over your day.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = SleekTextSecondary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── MASCOT / INTERCEPTOR ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollyCharacter(scrollCount = 75, size = 72.dp)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scrolly Interceptor",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Text(
                        text = "When a daily limit is hit, Scrolly raises a full-screen intervention to break the loop.",
                        style = MaterialTheme.typography.bodySmall.copy(color = SleekTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { interventionPreview = limits.firstOrNull() },
                        enabled = limits.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preview Intervention",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── DAILY APP LIMITS ─────────────────────────────────────────────
        Text(
            text = "Daily App Limits",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (limits.isEmpty()) {
            EmptyState(
                message = "No app limits configured yet. They appear here once tracking is set up."
            )
        } else {
            limits.forEach { limit ->
                AppLimitCard(
                    limit = limit,
                    isBlocked = limit.packageName in blockedPackages,
                    isRewardTarget = limit.packageName == rewardTarget,
                    onToggle = { enabled ->
                        viewModel.setRewardTarget(limit.packageName)
                        viewModel.updateLimit(
                            packageName = limit.packageName,
                            newLimit = limit.dailyLimit,
                            warningThreshold = limit.warningThreshold,
                            isEnabled = enabled
                        )
                    },
                    onLimitChange = { newLimit ->
                        viewModel.updateLimit(
                            packageName = limit.packageName,
                            newLimit = newLimit,
                            warningThreshold = (newLimit * WARNING_RATIO).toInt(),
                            isEnabled = limit.isEnabled
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── EARN MORE SCROLLS ────────────────────────────────────────────
        Text(
            text = "Earn Extra Scrolls",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        Text(
            text = "Complete a healthy reset to unlock a temporary bonus. Bonuses last for today only and never change your own limit.",
            style = MaterialTheme.typography.bodySmall.copy(color = SleekTextSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        if (challenges.isEmpty()) {
            EmptyState(message = "No challenges available.")
        } else {
            challenges.forEach { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    onClaim = { viewModel.completeChallengeAndEarn(challenge.id, challenge.rewardScrolls) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ── NOTICE ───────────────────────────────────────────────────────
        val currentNotice = notice
        if (currentNotice != null) {
            Spacer(modifier = Modifier.height(4.dp))
            LaunchedEffect(currentNotice) {
                kotlinx.coroutines.delay(2_600)
                viewModel.clearNotice()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekGreen.copy(alpha = 0.15f))
                    .border(1.dp, SleekGreen, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentNotice,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SleekGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ── FULL-SCREEN INTERVENTION PREVIEW ─────────────────────────────────
    interventionPreview?.let { limit ->
        InterventionPreview(
            appName = limit.appName,
            dailyLimit = limit.dailyLimit,
            onDismiss = { interventionPreview = null },
            onEarnFocusReset = {
                viewModel.completeChallengeAndEarn(
                    GamificationRepository.CHALLENGE_BREATHS,
                    10
                )
                interventionPreview = null
            },
            onEmergencyOverride = {
                viewModel.completeChallengeAndEarn(
                    GamificationRepository.CHALLENGE_WALK,
                    EMERGENCY_OVERRIDE_SCROLLS
                )
                interventionPreview = null
            }
        )
    }
}

private const val WARNING_RATIO = 0.8f

@Composable
private fun AppLimitCard(
    limit: AppLimitEntity,
    isBlocked: Boolean,
    isRewardTarget: Boolean,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    // Local drag state, committed on release. Keyed on the *package* so the slider does
    // not jump mid-drag every time a database write echoes back a new limit.
    var sliderValue by remember(limit.packageName) { mutableFloatStateOf(limit.dailyLimit.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (isBlocked) SleekRed else SleekBorder,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconBadge(packageName = limit.packageName, size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = limit.appName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                        )
                        Text(
                            text = when {
                                isBlocked -> "Limit reached — blocked"
                                else -> "${sliderValue.toInt()} reels per day"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isBlocked) SleekRed else SleekPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Switch(
                    checked = limit.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SleekPrimary,
                        uncheckedThumbColor = SleekTextMuted,
                        uncheckedTrackColor = SleekCardSurfaceElevated
                    )
                )
            }

            if (limit.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        // Only commit on release: the old code wrote to the database on
                        // every pixel of drag, causing dozens of writes per gesture.
                        onLimitChange(sliderValue.toInt())
                    },
                    valueRange = 10f..200f,
                    steps = 18,
                    modifier = Modifier.semantics {
                        contentDescription = "${limit.appName} daily scroll limit"
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPrimary,
                        activeTrackColor = SleekPrimary,
                        inactiveTrackColor = SleekCardSurfaceElevated
                    )
                )

                // Reflect external changes (auto-calibration, day rollover) unless the
                // user currently has a finger on the slider.
                LaunchedEffect(limit.dailyLimit) {
                    if (!isDragging) sliderValue = limit.dailyLimit.toFloat()
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    challenge: HealthyChallenge,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted) SleekCardSurfaceElevated else SleekCardSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (challenge.iconName) {
                "walk" -> Icons.Default.DirectionsWalk
                "phone" -> Icons.Default.PhoneAndroid
                "wind" -> Icons.Default.SelfImprovement
                else -> Icons.Default.Timer
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (challenge.isCompleted) SleekGreen.copy(alpha = 0.2f) else SleekCardHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (challenge.isCompleted) SleekGreen else SleekHeroText
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                )
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SleekTextSecondary,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+${challenge.rewardScrolls} scrolls • ${challenge.durationMinutes} min • +${challenge.rewardXp} XP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SleekPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onClaim,
                // Disabling after the first claim is what stops unlimited bonus scrolls.
                enabled = !challenge.isCompleted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekPillBg,
                    disabledContainerColor = SleekCardSurfaceElevated,
                    disabledContentColor = SleekTextMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (challenge.isCompleted) "Claimed" else "Claim",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (challenge.isCompleted) SleekTextMuted else SleekPillText,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * The full-screen "you have had enough" intervention.
 *
 * Every control here performs a real action. It used to contain an "Emergency Override"
 * [Text] styled to look like a link that did nothing at all.
 */
@Composable
private fun InterventionPreview(
    appName: String,
    dailyLimit: Int,
    onDismiss: () -> Unit,
    onEarnFocusReset: () -> Unit,
    onEmergencyOverride: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SleekBg.copy(alpha = 0.98f))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SleekRed.copy(alpha = 0.12f))
                        .border(2.dp, SleekRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Scroll limit reached",
                        tint = SleekRed,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "That's enough scrolling.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = SleekTextPrimary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You've reached your $appName limit of $dailyLimit scrolls today. Your brain needs a breather.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                ScrollyCharacter(scrollCount = 250, size = 140.dp)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Take a Break",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onEarnFocusReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekTextPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Earn +10 Scrolls with Focus Reset",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Was a clickable-looking Text that did nothing; now a real button that
                // grants a session-only bonus.
                OutlinedButton(
                    onClick = onEmergencyOverride,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Emergency override · +$EMERGENCY_OVERRIDE_SCROLLS scrolls",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
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