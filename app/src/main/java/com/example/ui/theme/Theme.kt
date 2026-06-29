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

private val DarkColorScheme =
  darkColorScheme(
    primary = MinimalPrimaryContainer,
    secondary = MinimalSecondaryContainer,
    tertiary = MinimalErrorContainer,
    background = DarkBlueBg,
    surface = DarkSurface,
    onBackground = MinimalBackground,
    onSurface = MinimalBackground
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MinimalPrimary,
    primaryContainer = MinimalPrimaryContainer,
    secondary = MinimalSecondaryContainer,
    secondaryContainer = MinimalSecondaryContainer,
    tertiary = MinimalError,
    background = MinimalBackground,
    surface = LightSurface,
    onBackground = MinimalOnBackground,
    onSurface = MinimalOnBackground,
    outline = MinimalBorder,
    error = MinimalError,
    errorContainer = MinimalErrorContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic system color overrides by default to preserve the Shree Ram branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
