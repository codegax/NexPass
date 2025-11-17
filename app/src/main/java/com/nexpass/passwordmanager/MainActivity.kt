package com.nexpass.passwordmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.domain.model.ThemeMode
import com.nexpass.passwordmanager.ui.lifecycle.AutoLockManager
import com.nexpass.passwordmanager.ui.navigation.NexPassNavHost
import com.nexpass.passwordmanager.ui.navigation.NexPassRoutes
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import org.koin.android.ext.android.inject

/**
 * Main Activity for NexPass
 *
 * Single activity hosting all Compose screens.
 * Uses Jetpack Compose Navigation for screen transitions.
 *
 * Security features:
 * - Edge-to-edge display
 * - Dark theme by default
 * - Secure screen flag (prevents screenshots) can be enabled per-screen
 */
class MainActivity : ComponentActivity() {

    private val securePreferences: SecurePreferences by inject()
    private val autoLockManager: AutoLockManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            val themeMode = ThemeMode.fromString(securePreferences.getThemeMode())
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            NexPassTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val shouldNavigateToUnlock by autoLockManager.shouldNavigateToUnlock.collectAsState()

                // Handle auto-lock navigation
                LaunchedEffect(shouldNavigateToUnlock) {
                    if (shouldNavigateToUnlock) {
                        navController.navigate(NexPassRoutes.UNLOCK) {
                            popUpTo(0) { inclusive = true }
                        }
                        autoLockManager.resetNavigationFlag()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Determine start destination based on vault initialization
                    val startDestination = if (securePreferences.isVaultInitialized()) {
                        NexPassRoutes.UNLOCK
                    } else {
                        NexPassRoutes.ONBOARDING
                    }

                    NexPassNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
