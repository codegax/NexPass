package com.nexpass.passwordmanager.ui.screens.password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.nexpass.passwordmanager.ui.components.*
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.PasswordFormViewModel
import com.nexpass.passwordmanager.ui.viewmodel.PasswordFormUiState
import com.nexpass.passwordmanager.ui.viewmodel.FormField
import org.koin.androidx.compose.koinViewModel

/**
 * Password Form Screen
 *
 * Handles both create and edit password operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordFormScreen(
    passwordId: String? = null,
    savedStateHandle: SavedStateHandle? = null,
    onNavigateBack: () -> Unit,
    onOpenGenerator: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: PasswordFormViewModel = koinViewModel()
) {
    val formData by viewModel.formData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val isEditMode = passwordId != null

    // Load password for editing
    LaunchedEffect(passwordId) {
        if (passwordId != null) {
            viewModel.loadPassword(passwordId)
        }
    }

    // Handle generated password from generator screen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("generated_password", null)?.collect { password ->
            if (password != null) {
                viewModel.updateField(FormField.PASSWORD, password)
                savedStateHandle.remove<String>("generated_password") // Clear after use
            }
        }
    }

    // Handle success
    LaunchedEffect(uiState) {
        if (uiState is PasswordFormUiState.Success) {
            onNavigateBack()
        }
    }

    val validationErrors = (uiState as? PasswordFormUiState.ValidationError)?.errors ?: emptyList()
    val isLoading = uiState is PasswordFormUiState.Loading

    // Helper function to check if field has error
    fun hasFieldError(field: String): Boolean {
        return validationErrors.any { error ->
            when (error) {
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.RequiredFieldMissing ->
                    error.fieldName == field
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.InvalidInput ->
                    error.fieldName == field
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.InvalidUrl ->
                    field == "Website URL (Optional)"
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.WeakPassword ->
                    field == "Password"
            }
        }
    }

    // Helper function to get field error message
    fun getFieldErrorMessage(field: String): String? {
        return validationErrors.firstOrNull { error ->
            when (error) {
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.RequiredFieldMissing ->
                    error.fieldName == field
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.InvalidInput ->
                    error.fieldName == field
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.InvalidUrl ->
                    field == "Website URL (Optional)"
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.WeakPassword ->
                    field == "Password"
            }
        }?.message
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Password" else "New Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (formData.favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            "Toggle Favorite"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            NexPassTextField(
                value = formData.title,
                onValueChange = { viewModel.updateField(FormField.TITLE, it) },
                label = "Title",
                placeholder = "My Bank Account",
                isError = hasFieldError("Title"),
                errorMessage = getFieldErrorMessage("Title"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Username field
            NexPassTextField(
                value = formData.username,
                onValueChange = { viewModel.updateField(FormField.USERNAME, it) },
                label = "Username / Email",
                placeholder = "john@example.com",
                isError = hasFieldError("Username"),
                errorMessage = getFieldErrorMessage("Username"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Password field
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (onOpenGenerator != null) {
                        TextButton(onClick = onOpenGenerator) {
                            Text("Generate")
                        }
                    }
                }
                NexPassPasswordField(
                    value = formData.password,
                    onValueChange = { viewModel.updateField(FormField.PASSWORD, it) },
                    label = "",
                    placeholder = "Enter a strong password",
                    isError = hasFieldError("Password"),
                    errorMessage = getFieldErrorMessage("Password"),
                    imeAction = ImeAction.Next
                )

                if (formData.password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PasswordStrengthIndicator(password = formData.password)
                }
            }

            // URL field
            NexPassTextField(
                value = formData.url,
                onValueChange = { viewModel.updateField(FormField.URL, it) },
                label = "Website URL (Optional)",
                placeholder = "https://example.com",
                isError = hasFieldError("Website URL (Optional)"),
                errorMessage = getFieldErrorMessage("Website URL (Optional)"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            // Notes field
            NexPassTextField(
                value = formData.notes,
                onValueChange = { viewModel.updateField(FormField.NOTES, it) },
                label = "Notes (Optional)",
                placeholder = "Additional information...",
                isError = hasFieldError("Notes (Optional)"),
                errorMessage = getFieldErrorMessage("Notes (Optional)"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = false,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Folder selection dropdown
            if (folders.isNotEmpty()) {
                var folderDropdownExpanded by remember { mutableStateOf(false) }
                val selectedFolder = folders.find { it.id == formData.folderId }

                ExposedDropdownMenuBox(
                    expanded = folderDropdownExpanded,
                    onExpandedChange = { folderDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "No folder",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Folder (Optional)") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, "Select folder")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    ExposedDropdownMenu(
                        expanded = folderDropdownExpanded,
                        onDismissRequest = { folderDropdownExpanded = false }
                    ) {
                        // No folder option
                        DropdownMenuItem(
                            text = { Text("No folder") },
                            onClick = {
                                viewModel.updateFolder(null)
                                folderDropdownExpanded = false
                            }
                        )

                        HorizontalDivider()

                        // All folders
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    viewModel.updateFolder(folder.id)
                                    folderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error message
            if (uiState is PasswordFormUiState.Error) {
                com.nexpass.passwordmanager.ui.components.ErrorMessage(
                    error = (uiState as PasswordFormUiState.Error).error,
                    onDismiss = { viewModel.clearError() }
                )
            }

            // Save button
            NexPassPrimaryButton(
                text = if (isEditMode) "Save Changes" else "Create Password",
                onClick = { viewModel.savePassword() },
                loading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isEditMode) {
                NexPassSecondaryButton(
                    text = "Cancel",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordFormScreenPreview() {
    NexPassTheme {
        PasswordFormScreen(
            passwordId = null,
            onNavigateBack = {},
            onOpenGenerator = {}
        )
    }
}
