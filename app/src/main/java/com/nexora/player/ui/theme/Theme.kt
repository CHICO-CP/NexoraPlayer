package com.nexora.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FlamingoRed = Color(0xFFF54047)
private val FlamingoRedDark = Color(0xFFE64366)
private val FlamingoRose = Color(0xFFE11D48)
private val FlamingoInk = Color(0xFF090B14)
private val FlamingoSurface = Color(0xFF111827)
private val FlamingoSurfaceAlt = Color(0xFF1C1C1E)
private val FlamingoSurfaceVariant = Color(0xFF2A2A2C)
private val FlamingoPaper = Color(0xFFF7F6FB)
private val FlamingoPaperAlt = Color(0xFFFFFFFF)
private val FlamingoTextDark = Color(0xFFE5E7EB)
private val FlamingoTextMutedDark = Color(0xFFB6BCC7)
private val FlamingoTextLight = Color(0xFF0F172A)
private val FlamingoTextMutedLight = Color(0xFF64748B)

private val LightColors = lightColorScheme(
    primary = FlamingoRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = FlamingoTextLight,
    secondary = FlamingoRedDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = FlamingoTextLight,
    tertiary = Color(0xFFFB7185),
    onTertiary = Color.White,
    background = FlamingoPaper,
    onBackground = FlamingoTextLight,
    surface = FlamingoPaperAlt,
    onSurface = FlamingoTextLight,
    surfaceVariant = Color(0xFFE7E5E4),
    onSurfaceVariant = FlamingoTextMutedLight,
    outline = Color(0xFFD1D5DB)
)

private val DarkColors = darkColorScheme(
    primary = FlamingoRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0A10),
    onPrimaryContainer = Color(0xFFFEE2E2),
    secondary = FlamingoRedDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A0F16),
    onSecondaryContainer = Color(0xFFFED7E2),
    tertiary = FlamingoRose,
    onTertiary = Color.White,
    background = FlamingoInk,
    onBackground = FlamingoTextDark,
    surface = FlamingoSurface,
    onSurface = FlamingoTextDark,
    surfaceVariant = FlamingoSurfaceVariant,
    onSurfaceVariant = FlamingoTextMutedDark,
    outline = Color(0xFF3F3F46)
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
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val colorScheme: ColorScheme = baseScheme.copy(
        primary = FlamingoRed,
        onPrimary = Color.White,
        primaryContainer = if (darkTheme) Color(0xFF3B0A10) else Color(0xFFFFE4E6),
        onPrimaryContainer = if (darkTheme) Color(0xFFFEE2E2) else FlamingoTextLight,
        secondary = FlamingoRedDark,
        onSecondary = Color.White,
        secondaryContainer = if (darkTheme) Color(0xFF2A0F16) else Color(0xFFFEE2E2),
        onSecondaryContainer = if (darkTheme) Color(0xFFFED7E2) else FlamingoTextLight,
        tertiary = if (darkTheme) FlamingoRose else Color(0xFFFB7185),
        onTertiary = Color.White,
        background = if (darkTheme) FlamingoInk else FlamingoPaper,
        onBackground = if (darkTheme) FlamingoTextDark else FlamingoTextLight,
        surface = if (darkTheme) FlamingoSurface else FlamingoPaperAlt,
        onSurface = if (darkTheme) FlamingoTextDark else FlamingoTextLight,
        surfaceVariant = if (darkTheme) FlamingoSurfaceVariant else Color(0xFFE7E5E4),
        onSurfaceVariant = if (darkTheme) FlamingoTextMutedDark else FlamingoTextMutedLight,
        outline = if (darkTheme) Color(0xFF3F3F46) else Color(0xFFD1D5DB)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = AppShapes,
        content = content
    )
}
