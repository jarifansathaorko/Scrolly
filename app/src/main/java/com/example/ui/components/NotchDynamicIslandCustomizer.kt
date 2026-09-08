package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.IslandPlacementMode
import com.example.data.repository.NotchConfiguration
import com.example.data.repository.NotchType
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardHighlight
import com.example.ui.theme.SleekCardSurface
import com.example.ui.theme.SleekCardSurfaceElevated
import com.example.ui.theme.SleekCardSurfaceSecondary
import com.example.ui.theme.SleekGreen
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekTextMuted
import com.example.ui.theme.SleekTextPrimary
import com.example.ui.theme.SleekTextSecondary

/**
 * Interactive iPhone-Style Dynamic Island Customizer & Auto-Calibrator.
 * - Detects hardware cutout size and position automatically.
 * - Supports Wrap Around Notch (camera framed in center gap) and Below Notch modes.
 * - Adjusts center cutout clearance gap so content is NEVER hidden by the notch.
 * - Keypad and precision sliders for micro-calibration matching social media screen boundaries.
 */
@Composable
fun NotchDynamicIslandCustomizer(
    config: NotchConfiguration,
    onSelectNotchType: (NotchType) -> Unit,
    onSetPlacementMode: (IslandPlacementMode) -> Unit,
    onSetCutoutGapWidth: (Int) -> Unit,
    onTriggerAutoDetect: () -> Unit,
    onAdjustPosition: (deltaX: Int, deltaY: Int) -> Unit,
    onSetPosition: (x: Int, y: Int) -> Unit,
    onSetDynamicIslandMode: (Boolean) -> Unit,
    onSetShowGuide: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onTestScroll: () -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = SleekCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row with Title and Expand/Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SleekCardHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🏝️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dynamic Island & Notch",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SleekTextPrimary
                            )
                        )
                        Text(
                            text = "${config.placementMode.title.substringBefore(" (")} • Y: ${config.offsetY}dp, Gap: ${config.cutoutGapWidth}dp",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SleekGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Notch Settings",
                        tint = SleekTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AUTO-CALIBRATION HARDWARE STATUS BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141A22))
                    .border(1.dp, SleekPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📐", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (config.detectedCutout.hasCutout)
                                    "Hardware Cutout: ${config.detectedCutout.width}×${config.detectedCutout.height}dp (Center: ${config.detectedCutout.centerX}dp)"
                                else
                                    "Status Bar Inset: ${config.detectedCutout.safeInsetTop}dp safe space",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = if (config.placementMode == IslandPlacementMode.WRAP_AROUND_NOTCH)
                                "Island envelopes camera lens. Content sits safely on wings."
                            else
                                "Floating capsule sits safely beneath the camera.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SleekTextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Button(
                        onClick = onTriggerAutoDetect,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto Detect",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Fit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // --- EXPANDED CALIBRATION CONTROLS ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // 1. PLACEMENT MODE SELECTOR
                    Text(
                        text = "DYNAMIC ISLAND PLACEMENT STYLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IslandPlacementMode.entries.forEach { mode ->
                            val isSelected = config.placementMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) SleekPrimary.copy(alpha = 0.22f) else SleekCardSurfaceSecondary)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) SleekPrimary else SleekBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onSetPlacementMode(mode) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = mode.iconEmoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mode.title.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) SleekTextPrimary else SleekTextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. CAMERA CLEARANCE GAP WIDTH (When in Wrap Around Notch Mode)
                    if (config.placementMode == IslandPlacementMode.WRAP_AROUND_NOTCH) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekCardSurfaceSecondary)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Camera Cutout Gap Width",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SleekTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${config.cutoutGapWidth} dp",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SleekPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "Clears empty space for hardware lens so numbers are never covered",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SleekTextMuted,
                                    fontSize = 10.sp
                                )
                            )
                            Slider(
                                value = config.cutoutGapWidth.toFloat(),
                                onValueChange = { onSetCutoutGapWidth(it.toInt()) },
                                valueRange = 20f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SleekPrimary,
                                    activeTrackColor = SleekPrimary,
                                    inactiveTrackColor = SleekBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. HARDWARE NOTCH PRESETS
                    Text(
                        text = "NOTCH / CUTOUT PRESET TYPE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotchType.entries.forEach { type ->
                            val isSelected = config.notchType == type
                            Box(
                                modifier = Modifier
                                    .testTag("notch_type_${type.name.lowercase()}")
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) SleekPrimary.copy(alpha = 0.2f) else SleekCardSurfaceSecondary)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) SleekPrimary else SleekBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onSelectNotchType(type) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = type.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) SleekTextPrimary else SleekTextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. DIRECTIONAL ARROW KEYPAD FOR MICRO-TUNING
                    Text(
                        text = "FINE-TUNING KEYPAD (1-2 DP STEPS)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SleekTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Keypad layout
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // UP ARROW
                            Box(
                                modifier = Modifier
                                    .testTag("arrow_up_button")
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SleekCardSurfaceElevated)
                                    .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                    .clickable { onAdjustPosition(0, -2) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up (Decrease Y)",
                                    tint = SleekTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // MIDDLE ROW: LEFT, RESET, RIGHT
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // LEFT ARROW
                                Box(
                                    modifier = Modifier
                                        .testTag("arrow_left_button")
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SleekCardSurfaceElevated)
                                        .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                        .clickable { onAdjustPosition(-2, 0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Move Left",
                                        tint = SleekTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // CENTER RESET BUTTON
                                Box(
                                    modifier = Modifier
                                        .testTag("arrow_reset_button")
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SleekPrimary.copy(alpha = 0.15f))
                                        .border(1.2.dp, SleekPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .clickable { onResetDefaults() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset To Defaults",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // RIGHT ARROW
                                Box(
                                    modifier = Modifier
                                        .testTag("arrow_right_button")
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SleekCardSurfaceElevated)
                                        .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                        .clickable { onAdjustPosition(2, 0) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Move Right",
                                        tint = SleekTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // DOWN ARROW
                            Box(
                                modifier = Modifier
                                    .testTag("arrow_down_button")
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SleekCardSurfaceElevated)
                                    .border(1.dp, SleekBorder, RoundedCornerShape(14.dp))
                                    .clickable { onAdjustPosition(0, 2) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down (Increase Y)",
                                    tint = SleekTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Quick step jump buttons & test scroll
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Quick Step Nudge",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SleekTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onAdjustPosition(0, 8) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekCardSurfaceSecondary)
                                ) {
                                    Text("⬇️ +8dp", fontSize = 10.sp, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onAdjustPosition(0, -8) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekCardSurfaceSecondary)
                                ) {
                                    Text("⬆️ -8dp", fontSize = 10.sp, color = SleekTextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onTestScroll,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("🔥 +1 Test Scroll Bounce", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. PRECISION SLIDERS FOR Y AND X
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Vertical Offset (Y from screen top)",
                                style = MaterialTheme.typography.labelSmall.copy(color = SleekTextSecondary)
                            )
                            Text(
                                text = "${config.offsetY} dp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Slider(
                            value = config.offsetY.toFloat(),
                            onValueChange = { onSetPosition(config.offsetX, it.toInt()) },
                            valueRange = 0f..120f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekPrimary,
                                activeTrackColor = SleekPrimary,
                                inactiveTrackColor = SleekBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Horizontal Offset (X from center)",
                                style = MaterialTheme.typography.labelSmall.copy(color = SleekTextSecondary)
                            )
                            Text(
                                text = "${if (config.offsetX > 0) "+${config.offsetX}" else config.offsetX} dp",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SleekTextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Slider(
                            value = config.offsetX.toFloat(),
                            onValueChange = { onSetPosition(it.toInt(), config.offsetY) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekPrimary,
                                activeTrackColor = SleekPrimary,
                                inactiveTrackColor = SleekBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. TOGGLES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SleekCardSurfaceSecondary)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Cutout Silhouette Guide",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextPrimary
                                )
                            )
                            Text(
                                text = "Draws purple indicator where the camera hole sits",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SleekTextMuted,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Switch(
                            checked = config.showCutoutGuide,
                            onCheckedChange = onSetShowGuide,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SleekPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
