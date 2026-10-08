package com.example.ui.screens.battles

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.FriendBattleEntity
import com.example.ui.components.ScrollyCharacter
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardHighlight
import com.example.ui.theme.SleekCardSurface
import com.example.ui.theme.SleekCardSurfaceElevated
import com.example.ui.theme.SleekGreen
import com.example.ui.theme.SleekHeroCard
import com.example.ui.theme.SleekHeroText
import com.example.ui.theme.SleekPillBg
import com.example.ui.theme.SleekPillText
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekRed
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.SleekTextSecondary
import kotlin.math.abs

@Composable
fun BattlesScreen(
    viewModel: BattlesViewModel,
    modifier: Modifier = Modifier
) {
    val battles by viewModel.battles.collectAsStateWithLifecycle()
    val todayScrolls by viewModel.todayScrolls.collectAsStateWithLifecycle()
    val hasBattles by viewModel.hasBattles.collectAsStateWithLifecycle()
    val message by viewModel.battleMessage.collectAsStateWithLifecycle()

    var showChallengeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "lightning_glow")
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

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
            text = "Battle your\nFriends",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SleekTextPrimary,
                lineHeight = 40.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        Text(
            text = "Lower scroll count wins. Control your feed together.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = SleekTextSecondary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // ── FEATURED MATCHUP (real numbers) ───────────────────────────────
        val featured = battles.firstOrNull()

        if (featured != null) {
            FeaturedMatchup(
                battle = featured,
                userScrolls = todayScrolls,
                lightningAlpha = lightningAlpha
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── LEADERBOARD HEADER ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasBattles) "Friend Leaderboard" else "Start a battle",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            )

            Button(
                onClick = { showChallengeDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Battle",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── EMPTY STATE ───────────────────────────────────────────────────
        if (!hasBattles) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, SleekBorder, RoundedCornerShape(20.dp)),
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
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SleekCardHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No battles yet",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Invite a friend with \"New Battle\". Whoever scrolls less today wins.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            battles.forEach { battle ->
                BattleRow(battle = battle, userScrolls = todayScrolls)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // ── MESSAGE ───────────────────────────────────────────────────────
        message?.let { note ->
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SleekGreen.copy(alpha = 0.15f))
                    .border(1.dp, SleekGreen, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SleekGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showChallengeDialog) {
        ChallengeDialog(
            onDismiss = {
                showChallengeDialog = false
                viewModel.clearMessage()
            },
            onConfirm = { username ->
                viewModel.addFriendChallenge(username, BattlesViewModel.firstNameOf(username))
                showChallengeDialog = false
            }
        )
    }
}

/**
 * Head-to-head card for the current battle.
 *
 * Both scores come from tracked data. This previously hardcoded 36 vs 93 and
 * "Alex has scrolled 57 more reels", which had no relationship to the battle list
 * rendered directly beneath it.
 */
@Composable
private fun FeaturedMatchup(
    battle: FriendBattleEntity,
    userScrolls: Int,
    lightningAlpha: Float
) {
    val userIsWinning = userScrolls < battle.friendScrolls
    val margin = abs(userScrolls - battle.friendScrolls)

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
                    .clip(RoundedCornerShape(20.dp))
                    .background(SleekPillBg)
                    .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "TODAY · ${BattlesViewModel.firstNameOf(battle.friendName).uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SleekPillText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScrollyCharacter(scrollCount = userScrolls, size = 80.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ScorePill(
                        score = userScrolls,
                        background = SleekHeroCard,
                        contentDescription = "Your scroll count today"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Text(
                        text = if (userIsWinning) "Winning" else "Behind",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (userIsWinning) SleekGreen else SleekRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = SleekPrimary.copy(alpha = lightningAlpha),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekTextPrimary
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScrollyCharacter(scrollCount = battle.friendScrolls, size = 80.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ScorePill(
                        score = battle.friendScrolls,
                        background = SleekCardHighlight,
                        contentDescription =
                            "${BattlesViewModel.firstNameOf(battle.friendName)}'s scroll count today"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = BattlesViewModel.firstNameOf(battle.friendName).uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SleekTextPrimary
                        )
                    )
                    Text(
                        text = if (userIsWinning) "Behind" else "Winning",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (userIsWinning) SleekRed else SleekGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = when {
                    margin == 0 -> "Dead heat — you're level today."
                    userIsWinning ->
                        "You're ${BattlesViewModel.firstNameOf(battle.friendName)} up by $margin. Keep it up!"
                    else ->
                        "${BattlesViewModel.firstNameOf(battle.friendName)} is up by $margin. Room to catch up."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = SleekTextSecondary,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun ScorePill(
    score: Int,
    background: Color,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SleekHeroText
            ),
            modifier = Modifier.semantics { this.contentDescription = contentDescription }
        )
    }
}

@Composable
private fun BattleRow(battle: FriendBattleEntity, userScrolls: Int) {
    val userIsWinning = userScrolls < battle.friendScrolls
    val isTied = userScrolls == battle.friendScrolls

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SleekCardSurfaceElevated)
                    .border(1.dp, SleekBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SleekPillText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = battle.friendName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${battle.friendUsername}",
                    style = MaterialTheme.typography.labelSmall.copy(color = SleekTextMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$userScrolls vs ${battle.friendScrolls}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                )
                Text(
                    text = when {
                        isTied -> "Level"
                        userIsWinning -> "You're winning"
                        else -> "Friend winning"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = when {
                            isTied -> SleekTextMuted
                            userIsWinning -> SleekGreen
                            else -> SleekRed
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun ChallengeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                Text(
                    text = "Challenge a Friend",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your friend's @username to start a battle for today.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = SleekTextSecondary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("@username", color = SleekTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (input.isNotBlank()) onConfirm(input) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekTextPrimary,
                        unfocusedTextColor = SleekTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekCardSurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = SleekPillText)
                    }

                    Button(
                        // Disabled rather than silently doing nothing on empty input.
                        onClick = { onConfirm(input) },
                        enabled = input.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start Battle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}