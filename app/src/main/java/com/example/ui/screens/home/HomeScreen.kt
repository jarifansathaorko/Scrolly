package com.example.ui.screens.home

import android.app.Activity
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppIconBadge
import com.example.ui.components.NotchBarPreview
import com.example.ui.components.NotchDynamicIslandCustomizer
import com.example.ui.components.ScrollyCharacter
import com.example.ui.components.ScrollyMascotState
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
import java.text.NumberFormat

private fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val todayStats by viewModel.todayStats.collectAsStateWithLifecycle()
    val liveTotalScrolls by viewModel.liveTotalScrolls.collectAsStateWithLifecycle()
    val trackedApps by viewModel.trackedApps.collectAsStateWithLifecycle()
    val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsStateWithLifecycle()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val showBatteryDialog by viewModel.showBatteryOptimizationDialog.collectAsStateWithLifecycle()
    val showOverlayDialog by viewModel.showOverlayPermissionDialog.collectAsStateWithLifecycle()

    val isNotchPreviewVisible by viewModel.isNotchBarPreviewVisible.collectAsStateWithLifecycle()
    val notchConfig by viewModel.notchConfig.collectAsStateWithLifecycle()
    val isNotchCustomizerOpen by viewModel.isNotchCustomizerOpen.collectAsStateWithLifecycle()
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    // Permissions can change while the app is backgrounded, so re-check on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val totalScrolls = todayStats?.totalScrolls ?: liveTotalScrolls
    val dailyLimit = todayStats?.goal ?: com.example.data.local.ScrollyDatabase.DEFAULT_DAILY_GOAL
    val mascotState = ScrollyMascotState.fromScrollCount(totalScrolls)
    val animatedCount by animateIntAsState(
        targetValue = totalScrolls,
        animationSpec = tween(durationMillis = 450),
        label = "scroll_counter"
    )

    var selectedApp by remember { mutableStateOf(HomeViewModel.DEFAULT_SIM_PACKAGE) }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBatteryOptimizationDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SleekGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = SleekGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Enable Background Usage",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Android aggressively pauses background services to save battery, which can stop the floating counter appearing when you switch apps.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = SleekTextSecondary)
                    )
                    Text(
                        text = "Scrolly's detection engine is built to use a negligible amount of battery while staying active. Disabling optimisation makes the counter appear instantly.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SleekTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.requestIgnoreBatteryOptimization() },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Turn Off Optimisation",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBatteryOptimizationDialog() }) {
                    Text("Maybe later", color = SleekTextMuted)
                }
            },
            containerColor = SleekCardSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOverlayPermissionDialog() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SleekPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Display Over Other Apps",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "To float the Dynamic Island counter above Instagram, YouTube and TikTok, Android requires the \"Display over other apps\" permission.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SleekTextSecondary)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.requestOverlayPermission() },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Open Settings",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOverlayPermissionDialog() }) {
                    Text("Cancel", color = SleekTextMuted)
                }
            },
            containerColor = SleekCardSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SleekBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(streakDays = streakDays)

            Spacer(modifier = Modifier.height(16.dp))

            HeroCard(
                totalScrolls = totalScrolls,
                animatedCount = animatedCount,
                dailyLimit = dailyLimit,
                mascotState = mascotState
            )

            Spacer(modifier = Modifier.height(20.dp))

            ActiveAppsSection(
                apps = trackedApps,
                selectedPackage = selectedApp,
                onSelect = { selectedApp = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotchDynamicIslandCustomizer(
                config = notchConfig,
                onSelectNotchType = viewModel::selectNotchType,
                onSetPlacementMode = viewModel::setPlacementMode,
                onSetCutoutGapWidth = viewModel::setCutoutGapWidth,
                onTriggerAutoDetect = { viewModel.triggerAutoDetectCutout(activity) },
                onAdjustPosition = viewModel::adjustNotchPosition,
                onSetPosition = viewModel::setNotchPosition,
                onSetDynamicIslandMode = viewModel::setDynamicIslandMode,
                onSetShowGuide = viewModel::setShowCutoutGuide,
                onResetDefaults = viewModel::resetNotchDefaults,
                onTestScroll = { viewModel.simulateScroll(selectedApp, 1) },
                isExpanded = isNotchCustomizerOpen,
                onToggleExpanded = viewModel::toggleNotchCustomizer
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.toggleNotchBarPreview() },
                enabled = isOverlayPermissionGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (isNotchPreviewVisible) {
                            "Hide the floating scroll counter"
                        } else {
                            "Show the floating scroll counter"
                        }
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNotchPreviewVisible) SleekCardSurfaceElevated else SleekPrimary,
                    disabledContainerColor = SleekCardSurface
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isNotchPreviewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isNotchPreviewVisible) SleekPillText else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        !isOverlayPermissionGranted -> "Grant overlay access to preview"
                        isNotchPreviewVisible -> "Hide floating counter"
                        else -> "Show floating counter"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isNotchPreviewVisible) SleekPillText else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ScrollTesterCard(
                selectedAppLabel = trackedApps.firstOrNull { it.first.packageName == selectedApp }
                    ?.first?.label ?: selectedApp,
                onSimulate = { viewModel.simulateScroll(selectedApp, it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReliabilityCard(
                isServiceActive = isServiceActive,
                isOverlayGranted = isOverlayPermissionGranted,
                isBatteryIgnored = isBatteryOptimizationIgnored,
                onEnableAccessibility = viewModel::openAccessibilitySettings,
                onGrantOverlay = { viewModel.promptOverlayPermissionDialog() },
                onFixBattery = { viewModel.promptBatteryOptimizationDialog() },
                onOpenAppSettings = viewModel::openAppDetailsSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Overlaid preview, positioned against physical display coordinates.
        NotchBarPreview(
            scrollCount = totalScrolls,
            appName = "Reels",
            dailyGoal = dailyLimit,
            visible = isNotchPreviewVisible,
            config = notchConfig,
            onDismiss = { viewModel.setNotchBarPreviewVisible(false) }
        )
    }
}

@Composable
private fun TopBar(streakDays: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Scrolly",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = SleekTextPrimary
                )
            )
            Text(
                text = "Take back your attention",
                style = MaterialTheme.typography.labelSmall.copy(color = SleekTextMuted)
            )
        }

        // Real streak rather than a hardcoded "7".
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SleekPillBg)
                .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .semantics { contentDescription = "$streakDays day streak" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = streakDays.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = SleekHeroText,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "DAY STREAK",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SleekPillText,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun HeroCard(
    totalScrolls: Int,
    animatedCount: Int,
    dailyLimit: Int,
    mascotState: ScrollyMascotState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = SleekHeroCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S SCROLLS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekHeroText
                    )
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SleekCardHighlight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekHeroText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatCount(animatedCount),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Normal,
                    color = SleekHeroText
                )
            )

            Text(
                text = if (totalScrolls == 1) "Short consumed today" else "Shorts & Reels consumed today",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SleekHeroText.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScrollyCharacter(scrollCount = totalScrolls, size = 130.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SleekCardSurface)
                    .border(1.dp, SleekBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mascotState.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = SleekPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = mascotState.quote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SleekTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val progressFraction = (totalScrolls.toFloat() / dailyLimit.coerceAtLeast(1)).coerceIn(0f, 1f)
            val isNearLimit = totalScrolls >= (dailyLimit * 0.8f).toInt()
            val remaining = (dailyLimit - totalScrolls).coerceAtLeast(0)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$totalScrolls / $dailyLimit limit",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = SleekHeroText,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    // Was `${((1f - fraction) * goal).toInt()}` — a float rounded toward zero,
                    // so 100 scrolls of a 100 limit read as "1 left" instead of "0 left".
                    text = if (remaining == 0) "limit reached" else "$remaining left",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isNearLimit) SleekRed else SleekHeroText,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (isNearLimit) SleekRed else SleekPrimary,
                trackColor = SleekCardHighlight,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Real per-app counts for today.
 *
 * Uses a wrapping grid instead of a fixed five-across [Row], which squeezed app labels
 * into ellipses on narrow phones and broke outright in landscape.
 */
@Composable
private fun ActiveAppsSection(
    apps: List<Pair<TrackedApp, Int>>,
    selectedPackage: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Apps",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SleekTextPrimary
                )
            )
            if (apps.isNotEmpty()) {
                Text(
                    text = "Tap to select",
                    style = MaterialTheme.typography.labelSmall.copy(color = SleekTextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (apps.isEmpty()) {
            Text(
                text = "No tracked apps yet.",
                style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted)
            )
            return@Column
        }

        LazyVerticalGrid(
            // Fixed height so the grid can live inside a vertically scrolling Column.
            modifier = Modifier
                .fillMaxWidth()
                .height(((apps.size + 2) / 3 * 104).dp),
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(apps, key = { it.first.packageName }) { (tracked, count) ->
                AppTile(
                    tracked = tracked,
                    count = count,
                    isSelected = tracked.packageName == selectedPackage,
                    onClick = { onSelect(tracked.packageName) },
                    modifier = Modifier.aspectRatio(0.92f)
                )
            }
        }
    }
}

@Composable
private fun AppTile(
    tracked: TrackedApp,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.5.dp,
                color = if (isSelected) SleekPrimary else SleekBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${tracked.label}, $count scrolls today"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SleekPillBg else SleekCardSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIconBadge(packageName = tracked.packageName, size = 26.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tracked.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) SleekHeroText else SleekTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isSelected) SleekPrimary else SleekTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun ScrollTesterCard(
    selectedAppLabel: String,
    onSimulate: (Int) -> Unit
) {
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
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = SleekPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scroll Tester",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Log test scrolls for $selectedAppLabel to verify the counter, limits and floating island without opening another app.",
                style = MaterialTheme.typography.bodySmall.copy(color = SleekTextSecondary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 20).forEachIndexed { index, amount ->
                    Button(
                        onClick = { onSimulate(amount) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Log $amount test scrolls" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (index == 0) SleekPrimary else SleekCardSurfaceElevated
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "+$amount",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (index == 0) Color.White else SleekPillText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReliabilityCard(
    isServiceActive: Boolean,
    isOverlayGranted: Boolean,
    isBatteryIgnored: Boolean,
    onEnableAccessibility: () -> Unit,
    onGrantOverlay: () -> Unit,
    onFixBattery: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Setup & Reliability",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = SleekTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                TextButton(onClick = onOpenAppSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SleekTextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("App settings", color = SleekTextSecondary, fontSize = 12.sp)
                }
            }

            StatusRow(
                title = "Accessibility Service",
                ok = isServiceActive,
                okIcon = Icons.Default.CheckCircle,
                pendingIcon = Icons.Default.Security,
                okText = "Active • counting Reels & Shorts",
                pendingText = "Required to detect reel swipes",
                actionLabel = "Enable",
                onAction = onEnableAccessibility
            )

            StatusRow(
                title = "Display Over Other Apps",
                ok = isOverlayGranted,
                okIcon = Icons.Default.CheckCircle,
                pendingIcon = Icons.Default.Layers,
                okText = "Granted • counter floats over apps",
                pendingText = "Needed to show the counter over apps",
                actionLabel = "Grant",
                onAction = onGrantOverlay
            )

            StatusRow(
                title = "Background Battery Usage",
                ok = isBatteryIgnored,
                okIcon = Icons.Default.CheckCircle,
                pendingIcon = Icons.Default.Warning,
                okText = "Unrestricted • no background freeze",
                pendingText = "Android may pause the counter",
                pendingColor = SleekRed,
                actionLabel = "Fix",
                onAction = onFixBattery
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    ok: Boolean,
    okIcon: androidx.compose.ui.graphics.vector.ImageVector,
    pendingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    okText: String,
    pendingText: String,
    actionLabel: String,
    onAction: () -> Unit,
    pendingColor: androidx.compose.ui.graphics.Color = SleekPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (ok) SleekGreen.copy(alpha = 0.15f) else pendingColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (ok) okIcon else pendingIcon,
                contentDescription = null,
                tint = if (ok) SleekGreen else pendingColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = SleekTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = if (ok) okText else pendingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (ok) SleekTextSecondary else pendingColor
                )
            )
        }

        if (!ok) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            }
        } else {
            Text("Ready", style = MaterialTheme.typography.labelSmall.copy(color = SleekGreen, fontWeight = FontWeight.Bold))
        }
    }
}