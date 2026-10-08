package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SleekColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekTextSecondary,
    onSecondary = SleekOnPrimary,
    secondaryContainer = SleekCardSurfaceElevated,
    onSecondaryContainer = SleekPillText,
    tertiary = SleekGold,
    onTertiary = SleekOnPrimary,
    error = SleekRed,
    onError = SleekOnPrimary,
    background = SleekBg,
    onBackground = SleekTextPrimary,
    surface = SleekCardSurface,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekCardSurfaceSecondary,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekBorderDark,
    outlineVariant = SleekBorder
)

/**
 * Scrolly's theme.
 *
 * Single scheme on purpose. Every screen paints with explicit palette tokens rather than
 * `MaterialTheme.colorScheme`, so wiring a dark scheme into `MaterialTheme` alone would
 * have themed only a handful of components and left the rest bright white. A partial
 * dark mode reads as a bug, so this stays light until the tokens themselves become
 * theme-aware.
 */
@Composable
fun ScrollyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekColorScheme,
        typography = Typography,
        content = content
    )
}