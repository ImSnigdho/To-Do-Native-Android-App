package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val ProfessionalLightColorScheme = lightColorScheme(
    primary = Color(0xFF8F4C38),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF8F4C38),

    secondary = Color(0xFF514441),
    secondaryContainer = Color(0xFFF5DED8),
    onSecondaryContainer = Color(0xFF514441),

    tertiary = Color(0xFFD9411E),
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFFD9411E),

    background = Color(0xFFF6F5F4),
    onBackground = Color(0xFF1F1A18),

    surface = Color(0xFFF6F5F4),
    onSurface = Color(0xFF1F1A18),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF514441),
    
    outline = Color(0xFF85736E)
)

private val ProfessionalDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF561F0F),
    primaryContainer = Color(0xFF723523),
    onPrimaryContainer = Color(0xFFFFDBD1),

    secondary = Color(0xFFE7C1B8),
    secondaryContainer = Color(0xFF5D4038),
    onSecondaryContainer = Color(0xFFF5DED8),

    tertiary = Color(0xFFFFB4A5),
    tertiaryContainer = Color(0xFF862208),
    onTertiaryContainer = Color(0xFFFFDAD4),

    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DD),

    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),

    outline = Color(0xFFA08C87)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ProfessionalDarkColorScheme else ProfessionalLightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
