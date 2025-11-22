package com.nexpass.passwordmanager.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.nexpass.passwordmanager.domain.model.SyncEngineStatus
import com.nexpass.passwordmanager.domain.model.ThemeMode
import com.nexpass.passwordmanager.ui.components.NexPassPrimaryButton
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLocked: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToFolders: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(uiState.nextcloudServerUrl) }
    var username by remember { mutableStateOf(uiState.nextcloudUsername) }
    var appPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Export/Import state
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPasswordVisible by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var importPasswordVisible by remember { mutableStateOf(false) }
    var exportFileUri by remember { mutableStateOf<Uri?>(null) }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }
    var replaceExisting by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Export file picker
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            exportFileUri = it
            showExportPasswordDialog = true
        }
    }

    // Import file picker
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            importFileUri = it
            showImportPasswordDialog = true
        }
    }

    // Handle export with password
    fun handleExport(uri: Uri, password: String) {
        isExporting = true
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                viewModel.exportVault(outputStream, password) { success, message ->
                    isExporting = false
                    snackbarMessage = message
                    exportPassword = ""
                    showExportPasswordDialog = false
                }
            } else {
                isExporting = false
                snackbarMessage = "Failed to open file for writing"
            }
        } catch (e: Exception) {
            isExporting = false
            snackbarMessage = "Export failed: ${e.message}"
        }
    }

    // Handle import with password
    fun handleImport(uri: Uri, password: String, replace: Boolean) {
        isImporting = true
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                viewModel.importVault(inputStream, password, replace) { success, message ->
                    isImporting = false
                    snackbarMessage = message
                    importPassword = ""
                    showImportPasswordDialog = false
                    importFileUri = null
                }
            } else {
                isImporting = false
                snackbarMessage = "Failed to open file for reading"
            }
        } catch (e: Exception) {
            isImporting = false
            snackbarMessage = "Import failed: ${e.message}"
        }
    }

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.headlineSmall
            )

            // Theme selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.updateThemeMode(ThemeMode.LIGHT) },
                            label = { Text("Light") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.themeMode == ThemeMode.DARK,
                            onClick = { viewModel.updateThemeMode(ThemeMode.DARK) },
                            label = { Text("Dark") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.updateThemeMode(ThemeMode.SYSTEM) },
                            label = { Text("System") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> "Always use light theme"
                            ThemeMode.DARK -> "Always use dark theme"
                            ThemeMode.SYSTEM -> "Follow system theme"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Auto-lock timeout
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Auto-Lock Timeout",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Timeout options
                    val timeoutOptions = listOf(
                        1 to "1 minute",
                        5 to "5 minutes",
                        15 to "15 minutes",
                        30 to "30 minutes",
                        -1 to "Never"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        timeoutOptions.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.autoLockTimeout == minutes,
                                    onClick = { viewModel.updateAutoLockTimeout(minutes) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automatically lock vault after period of inactivity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.headlineSmall
            )

            // Biometric unlock
            if (uiState.isBiometricAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Unlock",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Use fingerprint or face to unlock",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleBiometric(enabled) {
                                    // TODO: Trigger biometric prompt if needed
                                }
                            }
                        )
                    }
                }
            }

            // Lock vault button
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                NexPassPrimaryButton(
                    text = "Lock Vault Now",
                    onClick = { viewModel.lockVault(onLocked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Autofill & Autosave Section
            Text(
                text = "Autofill & Autosave",
                style = MaterialTheme.typography.headlineSmall
            )

            // Autosave toggle
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ask to Save Passwords",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Prompt to save credentials when you log in to apps and websites",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.autosaveEnabled,
                        onCheckedChange = { viewModel.toggleAutosave(it) }
                    )
                }
            }

            // Clear never-save list
            if (uiState.neverSaveDomainsCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Never Save List",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "You've chosen not to save passwords for ${uiState.neverSaveDomainsCount} sites/apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { viewModel.clearNeverSaveList() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Never Save List")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Management Section
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Export your passwords to an encrypted backup file or import from a previous backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export button
                        OutlinedButton(
                            onClick = {
                                exportFileLauncher.launch("nexpass_backup_${System.currentTimeMillis()}.nexpass")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isExporting) "Exporting..." else "Export")
                        }

                        // Import button
                        Button(
                            onClick = {
                                importFileLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isImporting) "Importing..." else "Import")
                        }
                    }

                    Text(
                        text = "Note: Backups are encrypted with a password separate from your master password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nextcloud Sync Section
            Text(
                text = "Nextcloud Sync",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.nextcloudConfigured) {
                        Text(
                            text = "Sync your passwords with Nextcloud",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://cloud.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            placeholder = { Text("your-username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = appPassword,
                            onValueChange = { appPassword = it },
                            label = { Text("App Password") },
                            placeholder = { Text("xxxxx-xxxxx-xxxxx-xxxxx-xxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testNextcloudConnection { success, message ->
                                        connectionTestResult = success to message
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = serverUrl.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test")
                            }

                            Button(
                                onClick = {
                                    viewModel.configureNextcloud(serverUrl, username, appPassword)
                                    connectionTestResult = null
                                },
                                modifier = Modifier.weight(1f),
                                enabled = serverUrl.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }

                        // Connection test result
                        connectionTestResult?.let { (success, message) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        // Configured state
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sync Enabled",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = uiState.nextcloudServerUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "User: ${uiState.nextcloudUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.syncEnabled,
                                onCheckedChange = { viewModel.toggleSync(it) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Last sync status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Last Sync",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = when {
                                        uiState.lastSyncTimestamp != null -> {
                                            val date = Date(uiState.lastSyncTimestamp!!)
                                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
                                        }
                                        else -> "Never"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sync status indicator
                            when (uiState.syncStatus) {
                                is SyncEngineStatus.Syncing -> {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                                is SyncEngineStatus.Success -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Synced",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                is SyncEngineStatus.Error -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Sync error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {}
                            }
                        }

                        // Sync status message
                        when (val status = uiState.syncStatus) {
                            is SyncEngineStatus.Success -> {
                                Text(
                                    text = "Synced ${status.passwordsUpdated} passwords",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is SyncEngineStatus.Error -> {
                                Text(
                                    text = status.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {}
                        }

                        // Sync now button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.clearNextcloudConfig() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disconnect")
                            }

                            Button(
                                onClick = { viewModel.triggerSync() },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.syncEnabled && uiState.syncStatus !is SyncEngineStatus.Syncing
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Organization Section
            Text(
                text = "Organization",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToFolders
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manage Folders",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create and organize password folders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToTags
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Manage Tags",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create colorful tags for passwords",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToAbout
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "About NexPass",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            // Clear vault data
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Clear All Vault Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "This will delete all passwords and reset the app. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Vault Data")
                    }
                }
            }

            // Error message
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Clear data confirmation dialog
            if (showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDataDialog = false },
                    title = { Text("Clear All Vault Data?") },
                    text = {
                        Text(
                            "This will permanently delete all your passwords, folders, and settings. " +
                                    "You will need to set up the vault again. This action CANNOT be undone."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearVaultData(onLocked)
                                showClearDataDialog = false
                            }
                        ) {
                            Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDataDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Export password dialog
            if (showExportPasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showExportPasswordDialog = false
                        exportPassword = ""
                        exportFileUri = null
                    },
                    title = { Text("Export Vault") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter a password to encrypt the backup file. This is separate from your master password.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = exportPassword,
                                onValueChange = { exportPassword = it },
                                label = { Text("Backup Password") },
                                placeholder = { Text("Enter a strong password") },
                                visualTransformation = if (exportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { exportPasswordVisible = !exportPasswordVisible }) {
                                        Icon(
                                            imageVector = if (exportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (exportPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                exportFileUri?.let { uri ->
                                    handleExport(uri, exportPassword)
                                }
                            },
                            enabled = exportPassword.length >= 8
                        ) {
                            Text("Export")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showExportPasswordDialog = false
                                exportPassword = ""
                                exportFileUri = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Import password dialog
            if (showImportPasswordDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showImportPasswordDialog = false
                        importPassword = ""
                        importFileUri = null
                    },
                    title = { Text("Import Vault") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter the password used to encrypt this backup file.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = importPassword,
                                onValueChange = { importPassword = it },
                                label = { Text("Backup Password") },
                                placeholder = { Text("Enter backup password") },
                                visualTransformation = if (importPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { importPasswordVisible = !importPasswordVisible }) {
                                        Icon(
                                            imageVector = if (importPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (importPasswordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = replaceExisting,
                                    onCheckedChange = { replaceExisting = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Replace existing passwords",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (replaceExisting) {
                                Text(
                                    text = "Warning: This will delete all existing passwords before importing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                importFileUri?.let { uri ->
                                    handleImport(uri, importPassword, replaceExisting)
                                }
                            },
                            enabled = importPassword.isNotEmpty()
                        ) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showImportPasswordDialog = false
                                importPassword = ""
                                importFileUri = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NexPassTheme {
        SettingsScreen(
            onNavigateBack = {},
            onLocked = {},
            onNavigateToAbout = {}
        )
    }
}
