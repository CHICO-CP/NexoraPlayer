package com.nexora.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NexoraDeepPurple = Color(0xFF7C3AED)
private val NexoraCyan = Color(0xFF22D3EE)
private val NexoraBackgroundDark = Color(0xFF090B14)
private val NexoraSurfaceDark = Color(0xFF111827)
private val NexoraSurfaceVariantDark = Color(0xFF1F2937)
private val NexoraOnDark = Color(0xFFE5E7EB)

private val LightColors = lightColorScheme(
    primary = NexoraDeepPurple,
    secondary = NexoraCyan,
    tertiary = Color(0xFF0EA5E9),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E7EB),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val DarkColors = darkColorScheme(
    primary = NexoraDeepPurple,
    secondary = NexoraCyan,
    tertiary = Color(0xFFB8A1FF),
    background = NexoraBackgroundDark,
    surface = NexoraSurfaceDark,
    surfaceVariant = NexoraSurfaceVariantDark,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = NexoraOnDark,
    onSurface = NexoraOnDark,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
