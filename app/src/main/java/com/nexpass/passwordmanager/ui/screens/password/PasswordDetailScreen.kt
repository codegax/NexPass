package com.nexpass.passwordmanager.ui.screens.password

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.ui.components.NexPassPrimaryButton
import com.nexpass.passwordmanager.ui.components.NexPassSecondaryButton
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.PasswordDetailViewModel
import com.nexpass.passwordmanager.ui.viewmodel.PasswordDetailUiState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    passwordId: String,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordDetailViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(passwordId) {
        viewModel.loadPassword(passwordId)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState is PasswordDetailUiState.Success) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            val password = (uiState as PasswordDetailUiState.Success).password
                            Icon(
                                if (password.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                "Toggle Favorite"
                            )
                        }
                        IconButton(onClick = { onEdit(passwordId) }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PasswordDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PasswordDetailUiState.Success -> {
                val password = state.password

                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = password.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Divider()

                    // Username
                    DetailField(
                        label = "Username",
                        value = password.username,
                        onCopy = { viewModel.copyUsername(context, password.username) }
                    )

                    // Password
                    DetailField(
                        label = "Password",
                        value = password.password,
                        isPassword = true,
                        showPassword = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        onCopy = { viewModel.copyPassword(context, password.password) }
                    )

                    // URL
                    if (!password.url.isNullOrBlank()) {
                        DetailField(
                            label = "URL",
                            value = password.url
                        )
                    }

                    // Notes
                    if (!password.notes.isNullOrBlank()) {
                        Column {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = password.notes,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    NexPassPrimaryButton(
                        text = "Copy Password",
                        onClick = { viewModel.copyPassword(context, password.password) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Delete confirmation dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Password?") },
                        text = { Text("Are you sure you want to delete \"${password.title}\"? This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePassword(passwordId, onNavigateBack)
                                    showDeleteDialog = false
                                }
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            is PasswordDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        NexPassPrimaryButton(
                            text = "Go Back",
                            onClick = onNavigateBack
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isPassword && !showPassword) "••••••••" else value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Row {
                if (isPassword) {
                    IconButton(onClick = { onToggleVisibility?.invoke() }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            "Toggle visibility"
                        )
                    }
                }
                if (onCopy != null) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordDetailScreenPreview() {
    NexPassTheme {
        PasswordDetailScreen(
            passwordId = "123",
            onNavigateBack = {},
            onEdit = {}
        )
    }
}
