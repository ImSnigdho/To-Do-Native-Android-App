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

    background = Color(0xFFFDF8F6),
    onBackground = Color(0xFF1F1A18),

    surface = Color.White,
    onSurface = Color(0xFF1F1A18),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF514441),
    
    outline = Color(0xFF85736E)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = ProfessionalLightColorScheme, typography = Typography, content = content)
}
