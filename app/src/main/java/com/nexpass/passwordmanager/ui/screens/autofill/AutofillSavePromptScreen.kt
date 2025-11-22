package com.nexpass.passwordmanager.ui.screens.autofill

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.components.*
import com.nexpass.passwordmanager.ui.viewmodel.AutofillSavePromptViewModel
import com.nexpass.passwordmanager.ui.viewmodel.AutofillSavePromptUiState
import com.nexpass.passwordmanager.ui.viewmodel.AutofillSaveFormField

/**
 * Screen for prompting user to save autofilled credentials.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillSavePromptScreen(
    packageName: String,
    webDomain: String?,
    viewModel: AutofillSavePromptViewModel,
    onSaveSuccess: (Boolean) -> Unit,  // true for new, false for update
    onCancel: () -> Unit
) {
    val formData by viewModel.formData.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val existingEntry by viewModel.existingEntry.collectAsState()

    // Handle successful save
    LaunchedEffect(uiState) {
        when (uiState) {
            is AutofillSavePromptUiState.SavedNew -> onSaveSuccess(true)
            is AutofillSavePromptUiState.SavedUpdate -> onSaveSuccess(false)
            is AutofillSavePromptUiState.Cancelled -> onCancel()
            else -> {}
        }
    }

    val validationErrors = (uiState as? AutofillSavePromptUiState.ValidationError)?.errors ?: emptyList()
    val isLoading = uiState is AutofillSavePromptUiState.Saving

    // Helper function to check if field has error
    fun hasFieldError(field: String): Boolean {
        return validationErrors.any { error ->
            when (error) {
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.RequiredFieldMissing ->
                    error.fieldName == field
                is com.nexpass.passwordmanager.domain.model.AppError.Validation.InvalidInput ->
                    error.fieldName == field
                else -> false
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
                else -> false
            }
        }?.message
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Text(
                text = if (existingEntry != null) "Update Password" else "Save Password",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "For ${webDomain ?: packageName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scrollable form content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show update notice if updating existing entry
                if (existingEntry != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "A password for this site already exists. Saving will update the existing entry.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Title field
                NexPassTextField(
                    value = formData.title,
                    onValueChange = { viewModel.updateField(AutofillSaveFormField.TITLE, it) },
                    label = "Title",
                    placeholder = "My Account",
                    isError = hasFieldError("Title"),
                    errorMessage = getFieldErrorMessage("Title"),
                    modifier = Modifier.fillMaxWidth()
                )

                // Username field (read-only preview)
                OutlinedTextField(
                    value = formData.username,
                    onValueChange = { viewModel.updateField(AutofillSaveFormField.USERNAME, it) },
                    label = { Text("Username / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasFieldError("Username")
                )

                // Password field (masked preview)
                OutlinedTextField(
                    value = formData.password,
                    onValueChange = { viewModel.updateField(AutofillSaveFormField.PASSWORD, it) },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasFieldError("Password")
                )

                // URL field
                NexPassTextField(
                    value = formData.url,
                    onValueChange = { viewModel.updateField(AutofillSaveFormField.URL, it) },
                    label = "Website URL (Optional)",
                    placeholder = "https://example.com",
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes field
                NexPassTextField(
                    value = formData.notes,
                    onValueChange = { viewModel.updateField(AutofillSaveFormField.NOTES, it) },
                    label = "Notes (Optional)",
                    placeholder = "Additional information...",
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder selection
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
                                .menuAnchor()
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

                // Tags selection (compact)
                if (tags.isNotEmpty()) {
                    Text(
                        text = "Tags (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = formData.selectedTagIds.contains(tag.id),
                                        onValueChange = { viewModel.toggleTag(tag.id) },
                                        role = Role.Checkbox
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = formData.selectedTagIds.contains(tag.id),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Never save again checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = formData.neverSaveAgain,
                            onValueChange = { viewModel.toggleNeverSaveAgain() },
                            role = Role.Checkbox
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = formData.neverSaveAgain,
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Never save passwords for this site",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (uiState is AutofillSavePromptUiState.Error) {
                ErrorMessage(
                    error = (uiState as AutofillSavePromptUiState.Error).error,
                    onDismiss = { viewModel.clearError() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancel() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("Don't Save")
                }

                Button(
                    onClick = { viewModel.savePassword() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (existingEntry != null) "Update" else "Save")
                    }
                }
            }
        }
    }
}
