package com.matripixel.ai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * MatriPixel Theme
 * High contrast Material3 theme optimized for healthcare field workers
 */

private val LightColorScheme = lightColorScheme(
    // Primary - Medical Blue
    primary = MedicalBlue,
    onPrimary = SurfaceLight,
    primaryContainer = MedicalBlueContainer,
    onPrimaryContainer = OnMedicalBlueContainer,
    
    // Secondary - Safe Green
    secondary = SafeGreen,
    onSecondary = SurfaceLight,
    secondaryContainer = SafeGreenContainer,
    onSecondaryContainer = OnSafeGreenContainer,
    
    // Tertiary - Amber Warning
    tertiary = AmberWarning,
    onTertiary = OnSurfaceDark,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    
    // Error - Emergency Red
    error = EmergencyRed,
    onError = SurfaceLight,
    errorContainer = EmergencyRedContainer,
    onErrorContainer = OnEmergencyRedContainer,
    
    // Surface
    background = SurfaceLight,
    onBackground = OnSurfaceDark,
    surface = SurfaceLight,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = TextMuted
)

private val DarkColorScheme = darkColorScheme(
    // Primary - Medical Blue (lighter for dark mode)
    primary = MedicalBlueLight,
    onPrimary = MedicalBlueDark,
    primaryContainer = MedicalBlueDark,
    onPrimaryContainer = MedicalBlueContainer,
    
    // Secondary - Safe Green
    secondary = SafeGreenLight,
    onSecondary = SafeGreenDark,
    secondaryContainer = SafeGreenDark,
    onSecondaryContainer = SafeGreenContainer,
    
    // Tertiary - Amber Warning
    tertiary = AmberWarningLight,
    onTertiary = AmberWarningDark,
    tertiaryContainer = AmberWarningDark,
    onTertiaryContainer = AmberContainer,
    
    // Error - Emergency Red
    error = EmergencyRedLight,
    onError = EmergencyRedDark,
    errorContainer = EmergencyRedDark,
    onErrorContainer = EmergencyRedContainer,
    
    // Surface
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextMuted
)

@Composable
fun MatriPixelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // For ASHA workers in field, we default to light mode for better outdoor visibility
    forceLightTheme: Boolean = true,
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme && !forceLightTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme && !forceLightTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = 
                !darkTheme || forceLightTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MatriPixelTypography,
        content = content
    )
}
