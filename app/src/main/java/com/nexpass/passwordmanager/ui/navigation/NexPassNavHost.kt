package com.nexpass.passwordmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexpass.passwordmanager.ui.screens.onboarding.OnboardingScreen
import com.nexpass.passwordmanager.ui.screens.unlock.UnlockScreen
import com.nexpass.passwordmanager.ui.screens.vault.VaultListScreen
import com.nexpass.passwordmanager.ui.screens.password.PasswordDetailScreen
import com.nexpass.passwordmanager.ui.screens.password.PasswordFormScreen
import com.nexpass.passwordmanager.ui.screens.generator.PasswordGeneratorScreen
import com.nexpass.passwordmanager.ui.screens.settings.SettingsScreen
import com.nexpass.passwordmanager.ui.screens.about.AboutScreen
import com.nexpass.passwordmanager.ui.screens.folder.FolderListScreen
import com.nexpass.passwordmanager.ui.screens.tag.TagListScreen

/**
 * NexPass Navigation Host
 *
 * Sets up all navigation routes and screens
 *
 * @param navController Navigation controller for the app
 * @param startDestination Initial destination (onboarding for new users, unlock for existing)
 * @param modifier Modifier for layout customization
 */
@Composable
fun NexPassNavHost(
    navController: NavHostController,
    startDestination: String = NexPassRoutes.ONBOARDING,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding flow - first time setup
        composable(NexPassRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NexPassRoutes.UNLOCK) {
                        popUpTo(NexPassRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Unlock screen - enter master password or biometric
        composable(NexPassRoutes.UNLOCK) {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(NexPassRoutes.VAULT_LIST) {
                        popUpTo(NexPassRoutes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        // Vault list - main screen with all passwords
        composable(NexPassRoutes.VAULT_LIST) {
            VaultListScreen(
                onPasswordClick = { passwordId ->
                    navController.navigate(NexPassRoutes.passwordDetail(passwordId))
                },
                onCreatePassword = {
                    navController.navigate(NexPassRoutes.PASSWORD_CREATE)
                },
                onNavigateToSettings = {
                    navController.navigate(NexPassRoutes.SETTINGS)
                }
            )
        }

        // Password detail - view/copy password
        composable(
            route = NexPassRoutes.PASSWORD_DETAIL,
            arguments = listOf(
                navArgument(NexPassArguments.PASSWORD_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getString(NexPassArguments.PASSWORD_ID) ?: ""
            PasswordDetailScreen(
                passwordId = passwordId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEdit = { id ->
                    navController.navigate(NexPassRoutes.passwordEdit(id))
                }
            )
        }

        // Password create - add new password
        composable(NexPassRoutes.PASSWORD_CREATE) { backStackEntry ->
            PasswordFormScreen(
                passwordId = null,
                savedStateHandle = backStackEntry.savedStateHandle,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenGenerator = {
                    navController.navigate(NexPassRoutes.PASSWORD_GENERATOR)
                }
            )
        }

        // Password edit - modify existing password
        composable(
            route = NexPassRoutes.PASSWORD_EDIT,
            arguments = listOf(
                navArgument(NexPassArguments.PASSWORD_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getString(NexPassArguments.PASSWORD_ID) ?: ""
            PasswordFormScreen(
                passwordId = passwordId,
                savedStateHandle = backStackEntry.savedStateHandle,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenGenerator = {
                    navController.navigate(NexPassRoutes.PASSWORD_GENERATOR)
                }
            )
        }

        // Password generator - generate secure passwords
        composable(NexPassRoutes.PASSWORD_GENERATOR) {
            PasswordGeneratorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUsePassword = { password ->
                    // TODO: Pass password back to form screen
                    navController.previousBackStackEntry?.savedStateHandle?.set("generated_password", password)
                    navController.popBackStack()
                }
            )
        }

        // Settings - app configuration
        composable(NexPassRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLocked = {
                    navController.navigate(NexPassRoutes.UNLOCK) {
                        popUpTo(NexPassRoutes.VAULT_LIST) { inclusive = true }
                    }
                },
                onNavigateToAbout = {
                    navController.navigate(NexPassRoutes.ABOUT)
                },
                onNavigateToFolders = {
                    navController.navigate(NexPassRoutes.FOLDER_MANAGE)
                },
                onNavigateToTags = {
                    navController.navigate(NexPassRoutes.TAG_MANAGE)
                }
            )
        }

        // About screen
        composable(NexPassRoutes.ABOUT) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Folder management screen
        composable(NexPassRoutes.FOLDER_MANAGE) {
            FolderListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tag management screen
        composable(NexPassRoutes.TAG_MANAGE) {
            TagListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
