package com.example.pulse.theme

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

private val WarmColorScheme = lightColorScheme(
  primary = PulseBlue,
  onPrimary = Color.White,
  primaryContainer = PulseBlueLight,
  onPrimaryContainer = PulseBlue,
  background = WarmBackground,
  onBackground = TextCharcoal,
  surface = CardBackground,
  onSurface = TextCharcoal,
  outline = BorderWarm,
  secondary = TextMuted,
  onSecondary = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = PulseBlue,
  onPrimary = Color.White,
  background = Color(0xFF1C1917), // Dark slate/charcoal matching standard warm-dark theme
  onBackground = Color(0xFFF5F0EA),
  surface = Color(0xFF262524),
  onSurface = Color(0xFFF5F0EA),
  outline = Color(0xFF44403C)
)

@Composable
fun PulseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else WarmColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
