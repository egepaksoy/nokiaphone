package com.egepaksoy.kiss.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.egepaksoy.kiss.R

@Immutable
data class KissColors(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val line: Color,
    val soft: Color,
    val accent: Color,
    val onAccent: Color,
    val me: Color,
    val onMe: Color,
    val call: Color,
    val end: Color,
    val dark: Boolean,
)

val LightColors = KissColors(
    bg = Color(0xFFFBFBF8), fg = Color(0xFF121413), muted = Color(0xFF6F756F), line = Color(0xFFE3E5DF),
    soft = Color(0xFFEFF0EB), accent = Color(0xFF2B47D6), onAccent = Color.White,
    me = Color(0xFF2B47D6), onMe = Color.White, call = Color(0xFF1E9E57), end = Color(0xFFD93A2E), dark = false,
)

val DarkColors = KissColors(
    bg = Color(0xFF060706), fg = Color(0xFFF0F2EE), muted = Color(0xFF8A918B), line = Color(0xFF1F2320),
    soft = Color(0xFF141715), accent = Color(0xFF93A5FF), onAccent = Color(0xFF0B1030),
    me = Color(0xFF3B56E0), onMe = Color.White, call = Color(0xFF2DBA6A), end = Color(0xFFF0564A), dark = true,
)

val Display = FontFamily(
    Font(R.font.display_regular, FontWeight.Normal),
    Font(R.font.display_medium, FontWeight.Medium),
    Font(R.font.display_semibold, FontWeight.SemiBold),
)

val Body = FontFamily(
    Font(R.font.body_regular, FontWeight.Normal),
    Font(R.font.body_medium, FontWeight.Medium),
    Font(R.font.body_semibold, FontWeight.SemiBold),
    Font(R.font.body_semibold, FontWeight.Bold),
)

val LocalKissColors = staticCompositionLocalOf { LightColors }

object Kiss {
    val colors: KissColors
        @Composable get() = LocalKissColors.current
}

@Composable
fun KissTheme(content: @Composable () -> Unit) {
    val c = if (isSystemInDarkTheme()) DarkColors else LightColors
    val scheme = if (c.dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.fg,
        surface = c.bg, onSurface = c.fg, surfaceVariant = c.soft, onSurfaceVariant = c.muted, outline = c.line,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.fg,
        surface = c.bg, onSurface = c.fg, surfaceVariant = c.soft, onSurfaceVariant = c.muted, outline = c.line,
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalKissColors provides c,
            LocalContentColor provides c.fg,
            LocalTextStyle provides TextStyle(fontFamily = Body, fontSize = 16.sp, color = c.fg, lineHeight = 22.sp),
            content = content,
        )
    }
}
