package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = AnimePrimaryDark,
    onPrimary = AnimeOnPrimaryDark,
    secondary = AnimeSecondaryDark,
    tertiary = AnimeTertiaryDark,
    background = AnimeBackgroundDark,
    surface = AnimeSurfaceDark,
    surfaceVariant = AnimeSurfaceVariantDark,
    onSurface = Color(0xFFF3EEFA)
)

private val LightColorScheme = lightColorScheme(
    primary = AnimePrimaryLight,
    onPrimary = AnimeOnPrimaryLight,
    secondary = AnimeSecondaryLight,
    tertiary = AnimeTertiaryLight,
    background = AnimeBackgroundLight,
    surface = AnimeSurfaceLight,
    surfaceVariant = AnimeSurfaceVariantLight,
    onSurface = Color(0xFF1E1B2E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature anime colors consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
