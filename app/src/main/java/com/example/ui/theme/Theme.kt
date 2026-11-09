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
    primary = PrismCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF80F3FF),
    secondary = PrismPurple,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4F378B),
    onSecondaryContainer = Color(0xFFE8DDFF),
    tertiary = PrismMagenta,
    onTertiary = Color(0xFF492532),
    background = ObsidianBgDark,
    onBackground = Color(0xFFE1E2E8),
    surface = ObsidianSurfaceDark,
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = ObsidianCardDark,
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF2F3746)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF6750A4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DDFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF984061),
    onTertiary = Color.White,
    background = ObsidianBgLight,
    onBackground = Color(0xFF191C1E),
    surface = ObsidianSurfaceLight,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = ObsidianCardLight,
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFFC0CAD0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature compatible with template
    content: @Composable () -> Unit
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
