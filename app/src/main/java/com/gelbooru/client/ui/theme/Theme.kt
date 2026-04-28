package com.gelbooru.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.gelbooru.client.data.model.ThemeMode

/**
 * Tactile Minimalism theme.
 * Soft, physical surfaces with subtle depth — no neon, no cyberpunk.
 */
private val LightColorPalette = TactileColors(
    surfaceBase = Color(0xFAF9F7),
    surfaceElevated = Color(0xFFFFFFFF),
    surfacePressed = Color(0xF0EFED),
    surfaceShadow = Color(0x14000000),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    textTertiary = Color(0xFF9E9E9E),
    accentPrimary = Color(0xFF2C2C2C),
    accentSecondary = Color(0xFF5C5C5C),
    divider = Color(0xFFE8E6E3),
    error = Color(0xFFC62828),
    success = Color(0xFF2E7D32),
    overlay = Color(0x99000000),
    progressTrack = Color(0xFFE8E6E3),
    progressFill = Color(0xFF2C2C2C),
    scrim = Color(0x40000000),
    fabBackground = Color(0xFF2C2C2C),
    fabForeground = Color(0xFFFAF9F7)
)

private val DarkColorPalette = TactileColors(
    surfaceBase = Color(0xFF1A1A1A),
    surfaceElevated = Color(0xFF242424),
    surfacePressed = Color(0xFF2E2E2E),
    surfaceShadow = Color(0x30000000),
    textPrimary = Color(0xFFF0EFED),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF6B6B6B),
    accentPrimary = Color(0xFFE8E6E3),
    accentSecondary = Color(0xFF9E9E9E),
    divider = Color(0xFF333333),
    error = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
    overlay = Color(0xCC000000),
    progressTrack = Color(0xFF333333),
    progressFill = Color(0xFFE8E6E3),
    scrim = Color(0x60000000),
    fabBackground = Color(0xFFE8E6E3),
    fabForeground = Color(0xFF1A1A1A)
)

data class TactileColors(
    val surfaceBase: Color,
    val surfaceElevated: Color,
    val surfacePressed: Color,
    val surfaceShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val divider: Color,
    val error: Color,
    val success: Color,
    val overlay: Color,
    val progressTrack: Color,
    val progressFill: Color,
    val scrim: Color,
    val fabBackground: Color,
    val fabForeground: Color
)

object TactileTheme {
    val colors: TactileColors
        @Composable
        get() = LocalTactileColors.current
}

private val LocalTactileColors = compositionLocalOf { LightColorPalette }

@Composable
fun GelbooruTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    // Resolve dark theme state from the user's chosen mode
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    CompositionLocalProvider(LocalTactileColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            typography = TactileTypography,
            content = content
        )
    }
}
