package com.nexpass.passwordmanager.ui.theme

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
 * Dark color scheme for NexPass
 * Default theme - optimized for security and readability in low light
 */
private val DarkColorScheme = darkColorScheme(
    // Primary brand color
    primary = PrimaryBlue,
    onPrimary = OnDarkPrimary,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = OnDarkPrimary,

    // Secondary accent
    secondary = PrimaryTeal,
    onSecondary = OnDarkPrimary,
    secondaryContainer = PrimaryBlueDark,
    onSecondaryContainer = OnDarkPrimary,

    // Tertiary
    tertiary = SuccessGreen,
    onTertiary = OnDarkPrimary,

    // Background and surfaces
    background = DarkBackground,
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,

    // Error colors
    error = ErrorRed,
    onError = OnDarkPrimary,

    // Outline
    outline = OnDarkSurfaceVariant
)

/**
 * Light color scheme for NexPass
 * Optional light theme for user preference
 */
private val LightColorScheme = lightColorScheme(
    // Primary brand color
    primary = PrimaryBlueDark,
    onPrimary = OnLightPrimary,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = OnLightSurface,

    // Secondary accent
    secondary = PrimaryTeal,
    onSecondary = OnLightPrimary,
    secondaryContainer = PrimaryBlue,
    onSecondaryContainer = OnLightSurface,

    // Tertiary
    tertiary = SuccessGreen,
    onTertiary = OnLightPrimary,

    // Background and surfaces
    background = LightBackground,
    onBackground = OnLightSurface,
    surface = LightSurface,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,

    // Error colors
    error = ErrorRedLight,
    onError = OnLightPrimary,

    // Outline
    outline = OnLightSurfaceVariant
)

/**
 * NexPass Material 3 Theme
 *
 * Features:
 * - Dark mode by default (security-focused)
 * - Custom blue/teal color scheme
 * - Edge-to-edge display support
 * - Material 3 design system
 *
 * Note: Dynamic colors disabled for consistent branding
 *
 * @param darkTheme Whether to use dark theme. Defaults to true (dark mode first).
 * @param content Composable content to apply theme to.
 */
@Composable
fun NexPassTheme(
    darkTheme: Boolean = true,  // Dark mode by default for security apps
    content: @Composable () -> Unit
) {
    // Use our custom color schemes (no dynamic colors for consistent branding)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use transparent status bar for edge-to-edge
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // Adjust status bar icons based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
