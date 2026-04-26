package com.bbsrevival.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── CGA-inspired palette ─────────────────────────────────────────────────────
object BbsColors {
    val Bg           = Color(0xFF0A0A0F)
    val BgSurface    = Color(0xFF111118)
    val BgRaised     = Color(0xFF16161F)
    val BgOverlay    = Color(0xFF1A1A24)

    val Fg           = Color(0xFFC8C8D4)
    val FgBright     = Color(0xFFE8E8F0)
    val FgDim        = Color(0xFF666680)

    val Cyan         = Color(0xFF00D4D4)
    val CyanDim      = Color(0xFF004444)
    val Yellow       = Color(0xFFF0C040)
    val Green        = Color(0xFF40C060)
    val Red          = Color(0xFFFF4444)
    val BrightRed    = Color(0xFFFF6666)
    val Magenta      = Color(0xFFCC44CC)
    val Blue         = Color(0xFF4444CC)

    val Border       = Color(0xFF2A2A3A)
    val BorderDim    = Color(0xFF1E1E2A)
}

// ── Monospace font family ─────────────────────────────────────────────────────
// We bundle JetBrains Mono — add the TTF files to res/font/
// Fallback is system monospace if not present.
val MonoFontFamily = FontFamily.Monospace

// ── Typography ────────────────────────────────────────────────────────────────
val BbsTypography = Typography(
    displayLarge  = TextStyle(fontFamily = MonoFontFamily, fontSize = 28.sp, color = BbsColors.Cyan,    letterSpacing = 4.sp),
    displayMedium = TextStyle(fontFamily = MonoFontFamily, fontSize = 22.sp, color = BbsColors.Cyan,    letterSpacing = 3.sp),
    headlineLarge = TextStyle(fontFamily = MonoFontFamily, fontSize = 18.sp, color = BbsColors.FgBright, fontWeight = FontWeight.Bold),
    headlineMedium= TextStyle(fontFamily = MonoFontFamily, fontSize = 15.sp, color = BbsColors.FgBright),
    titleLarge    = TextStyle(fontFamily = MonoFontFamily, fontSize = 16.sp, color = BbsColors.Cyan,    letterSpacing = 1.sp),
    titleMedium   = TextStyle(fontFamily = MonoFontFamily, fontSize = 14.sp, color = BbsColors.Fg),
    bodyLarge     = TextStyle(fontFamily = MonoFontFamily, fontSize = 14.sp, color = BbsColors.Fg,      lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontFamily = MonoFontFamily, fontSize = 13.sp, color = BbsColors.Fg,      lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = MonoFontFamily, fontSize = 11.sp, color = BbsColors.FgDim),
    labelLarge    = TextStyle(fontFamily = MonoFontFamily, fontSize = 12.sp, color = BbsColors.Cyan,    letterSpacing = 1.sp),
    labelMedium   = TextStyle(fontFamily = MonoFontFamily, fontSize = 11.sp, color = BbsColors.FgDim,   letterSpacing = 1.sp),
    labelSmall    = TextStyle(fontFamily = MonoFontFamily, fontSize = 10.sp, color = BbsColors.FgDim,   letterSpacing = 2.sp),
)

// ── Dark color scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = BbsColors.Cyan,
    onPrimary        = BbsColors.Bg,
    primaryContainer = BbsColors.CyanDim,
    secondary        = BbsColors.Yellow,
    onSecondary      = BbsColors.Bg,
    tertiary         = BbsColors.Green,
    background       = BbsColors.Bg,
    onBackground     = BbsColors.Fg,
    surface          = BbsColors.BgSurface,
    onSurface        = BbsColors.Fg,
    surfaceVariant   = BbsColors.BgRaised,
    outline          = BbsColors.Border,
    error            = BbsColors.Red,
    onError          = BbsColors.BgSurface,
)

@Composable
fun BbsRevivalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = BbsTypography,
        content     = content,
    )
}
