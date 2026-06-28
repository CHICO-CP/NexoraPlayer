package com.nexora.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.AppThemeMode

private val NexoraRed = Color(0xFFF54047)
private val NexoraRose = Color(0xFFE64366)
private val NexoraInk = Color(0xFF090B14)
private val NexoraSurface = Color(0xFF111827)
private val NexoraSurfaceVariant = Color(0xFF2A2A2C)
private val NexoraPaper = Color(0xFFF7F6FB)
private val NexoraPaperAlt = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = NexoraRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = NexoraRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color.White,
    background = NexoraPaper,
    onBackground = Color(0xFF0F172A),
    surface = NexoraPaperAlt,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE7E5E4),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFD1D5DB)
)

private val DarkColors = darkColorScheme(
    primary = NexoraRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0A10),
    onPrimaryContainer = Color(0xFFFEE2E2),
    secondary = NexoraRose,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A0F16),
    onSecondaryContainer = Color(0xFFFED7E2),
    tertiary = Color(0xFFE11D48),
    onTertiary = Color.White,
    background = NexoraInk,
    onBackground = Color(0xFFE5E7EB),
    surface = NexoraSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = NexoraSurfaceVariant,
    onSurfaceVariant = Color(0xFFB6BCC7),
    outline = Color(0xFF3F3F46)
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFFFF375F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B000D),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFF64D2FF),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFFF2F2F7),
    surface = Color.Black,
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF2A2A2A)
)

private val FlamingoColors = darkColorScheme(
    primary = Color(0xFFFF4D6D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0814),
    secondary = Color(0xFFFF9F1C),
    onSecondary = Color.Black,
    tertiary = Color(0xFFFF7096),
    background = Color(0xFF100713),
    onBackground = Color(0xFFFFF1F5),
    surface = Color(0xFF1D0D22),
    onSurface = Color(0xFFFFF1F5),
    surfaceVariant = Color(0xFF35162D),
    onSurfaceVariant = Color(0xFFEFC1CE),
    outline = Color(0xFF64324D)
)

private val NeonColors = darkColorScheme(
    primary = Color(0xFF00F5D4),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003D36),
    secondary = Color(0xFFB517FF),
    onSecondary = Color.White,
    tertiary = Color(0xFFFFEA00),
    background = Color(0xFF050712),
    onBackground = Color(0xFFEAFBFF),
    surface = Color(0xFF0B1020),
    onSurface = Color(0xFFEAFBFF),
    surfaceVariant = Color(0xFF151B33),
    onSurfaceVariant = Color(0xFFB6C3E0),
    outline = Color(0xFF2B335C)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (themeMode) {
        AppThemeMode.SYSTEM, AppThemeMode.MATERIAL_YOU -> systemDark
        AppThemeMode.LIGHT, AppThemeMode.IOS_LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.NEXORA_DARK, AppThemeMode.AMOLED_BLACK, AppThemeMode.FLAMINGO, AppThemeMode.NEON -> true
    }
    val useDynamic = (themeMode == AppThemeMode.MATERIAL_YOU || dynamicColor) &&
        themeMode !in setOf(AppThemeMode.AMOLED_BLACK, AppThemeMode.FLAMINGO, AppThemeMode.NEON) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = when {
        useDynamic -> {
            val context = LocalContext.current
            if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == AppThemeMode.AMOLED_BLACK -> AmoledColors
        themeMode == AppThemeMode.FLAMINGO -> FlamingoColors
        themeMode == AppThemeMode.NEON -> NeonColors
        effectiveDark -> DarkColors
        else -> LightColors
    }

    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.MATERIAL_YOU -> baseScheme
        AppThemeMode.AMOLED_BLACK, AppThemeMode.FLAMINGO, AppThemeMode.NEON -> baseScheme
        else -> baseScheme.copy(
            primary = NexoraRed,
            onPrimary = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = AppShapes,
        content = content
    )
}
