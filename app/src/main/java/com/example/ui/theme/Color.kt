package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Palette
//
// The original values were Material 3's *baseline light* scheme copied verbatim
// (bg 0xFFFEF7FF, surface 0xFFF3EDF7, outline 0xFFCAC4D0). Because the background and
// the card surface sat only a few values apart, cards had no visible edge against the
// page, and the 0xFFCAC4D0 borders fell below the 3:1 non-text contrast ratio WCAG
// requires for control boundaries. The values below keep the same lavender identity but
// separate the surfaces meaningfully and darken the outlines and muted text.
// ─────────────────────────────────────────────────────────────────────────────

// Surfaces
val SleekBg = Color(0xFFFBF7FF)
val SleekCardSurface = Color(0xFFFFFFFF)
val SleekCardSurfaceSecondary = Color(0xFFE7E0EC)
val SleekCardSurfaceElevated = Color(0xFFEDE4FA)
val SleekCardHighlight = Color(0xFFEADDFF)
val SleekHeroCard = Color(0xFFCFBDF7)

// Text, all >= 4.5:1 on SleekCardSurface
val SleekTextPrimary = Color(0xFF1D1B20)
val SleekTextSecondary = Color(0xFF49454F)
val SleekTextMuted = Color(0xFF625C68)
val SleekHeroText = Color(0xFF1B1040)

// Lines
val SleekBorder = Color(0xFF9A93A6)
val SleekBorderDark = Color(0xFF625C68)

// Accents
val SleekPrimary = Color(0xFF5B3FA8)
val SleekOnPrimary = Color(0xFFFFFFFF)
val SleekPrimaryContainer = Color(0xFFEADDFF)
val SleekOnPrimaryContainer = Color(0xFF21005D)
val SleekPillBg = Color(0xFFEDE4FA)
val SleekPillText = Color(0xFF1D192B)

// Status
val SleekGreen = Color(0xFF14663C)
val SleekRed = Color(0xFFA8231C)
val SleekGold = Color(0xFF7A4A00)
val SleekBlue = Color(0xFF1B5391)

// ─────────────────────────────────────────────────────────────────────────────
// In-app preview surfaces
//
// The Dynamic Island preview and the notch customiser diagram were hard-coded dark
// (`0xFF141A22`, `0xFF0D1117`, `Color.Black`) while the app around them was light, so
// those panels read as pasted-in foreign objects. These tokens keep the "device frame"
// look but derive from the app palette so it stays coherent.
// ─────────────────────────────────────────────────────────────────────────────

val SleekFrameSurface = Color(0xFFF3EFF9)
val SleekFrameBorder = Color(0xFFD6CEE0)
val SleekFrameInset = Color(0xFFE4DDEE)

// ─────────────────────────────────────────────────────────────────────────────
// Backwards-compatible aliases
// ─────────────────────────────────────────────────────────────────────────────

val ScrollyDarkBg = SleekBg
val ScrollyCardSurface = SleekCardSurface
val ScrollyCardSurfaceElevated = SleekCardSurfaceElevated
val ScrollyBorder = SleekBorder
val ScrollyBorderLight = SleekCardSurfaceSecondary

val ScrollyCream = SleekTextPrimary
val ScrollyCreamSecondary = SleekTextSecondary
val ScrollyCreamMuted = SleekTextMuted

val ScrollyAmber = SleekPrimary
val ScrollyAmberDark = SleekHeroText
val ScrollyGold = SleekGold
val ScrollyMascotPink = SleekHeroCard
val ScrollyMascotPinkDark = SleekPrimary

val ScrollyGreen = SleekGreen
val ScrollyRed = SleekRed
val ScrollyBlue = SleekBlue

// App brand colors
val BrandInstagram = Color(0xFFE1306C)
val BrandYouTube = Color(0xFFE62117)
val BrandFacebook = Color(0xFF1877F2)
val BrandSnapchat = Color(0xFFB88900)

/** Outline used by the mascot canvas, which always draws on light surfaces. */
val MascotOutline = Color(0xFF1D1B20)