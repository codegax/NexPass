package com.nexpass.passwordmanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * NexPass Color Palette
 * Security-focused dark theme with blue/teal accent
 */

// Primary brand colors - Blue/Teal for security/trust
val PrimaryBlue = Color(0xFF2196F3)        // Bright blue
val PrimaryBlueDark = Color(0xFF1976D2)    // Darker blue
val PrimaryTeal = Color(0xFF00BCD4)        // Teal accent

// Dark theme colors (default)
val DarkBackground = Color(0xFF121212)      // Almost black
val DarkSurface = Color(0xFF1E1E1E)         // Slightly lighter
val DarkSurfaceVariant = Color(0xFF2C2C2C) // Cards, elevated surfaces
val OnDarkPrimary = Color(0xFFFFFFFF)       // White text on primary
val OnDarkSurface = Color(0xFFE0E0E0)       // Light grey text
val OnDarkSurfaceVariant = Color(0xFFB0B0B0) // Dimmer text

// Light theme colors
val LightBackground = Color(0xFFFAFAFA)     // Off-white
val LightSurface = Color(0xFFFFFFFF)        // Pure white
val LightSurfaceVariant = Color(0xFFF5F5F5) // Light grey
val OnLightPrimary = Color(0xFFFFFFFF)      // White text on primary
val OnLightSurface = Color(0xFF1C1C1C)      // Dark text
val OnLightSurfaceVariant = Color(0xFF757575) // Grey text

// Semantic colors for password manager
val ErrorRed = Color(0xFFCF6679)            // Material error (dark-friendly)
val ErrorRedLight = Color(0xFFB00020)       // Material error (light theme)
val SuccessGreen = Color(0xFF66BB6A)        // Success/secure
val WarningOrange = Color(0xFFFFB74D)       // Warning/attention
val InfoBlue = Color(0xFF42A5F5)            // Informational

// Password strength indicator colors
val StrengthWeak = Color(0xFFEF5350)        // Red - weak
val StrengthMedium = Color(0xFFFFB74D)      // Orange - medium
val StrengthStrong = Color(0xFF66BB6A)      // Green - strong
val StrengthVeryStrong = Color(0xFF42A5F5)  // Blue - very strong

// Special UI colors
val BiometricBlue = Color(0xFF2196F3)       // Biometric authentication
val SyncGreen = Color(0xFF4CAF50)           // Sync status
val OfflineGrey = Color(0xFF9E9E9E)         // Offline mode
val QuarantineYellow = Color(0xFFFFA726)    // Quarantined entries
