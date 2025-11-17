package com.nexpass.passwordmanager.ui.screens.generator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nexpass.passwordmanager.domain.util.PasswordStrength
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.GeneratorType
import com.nexpass.passwordmanager.ui.viewmodel.PasswordGeneratorViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    onNavigateBack: () -> Unit,
    onUsePassword: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: PasswordGeneratorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Regenerate button
                    IconButton(onClick = { viewModel.generatePassword() }) {
                        Icon(Icons.Default.Refresh, "Generate new password")
                    }
                }
            )
        },
        snackbarHost = {
            if (showCopiedSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Password copied to clipboard")
                }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedSnackbar = false
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Generator type selector
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.generatorType == GeneratorType.PASSWORD,
                            onClick = { viewModel.setGeneratorType(GeneratorType.PASSWORD) },
                            label = { Text("Password") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.generatorType == GeneratorType.PASSPHRASE,
                            onClick = { viewModel.setGeneratorType(GeneratorType.PASSPHRASE) },
                            label = { Text("Passphrase") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Generated password display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generated Password",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    SelectionContainer {
                        Text(
                            text = uiState.generatedPassword,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Password strength indicator
                    PasswordStrengthIndicator(
                        strength = uiState.passwordStrength,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.generatedPassword))
                                showCopiedSnackbar = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }

                        if (onUsePassword != null) {
                            Button(
                                onClick = { onUsePassword(uiState.generatedPassword) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Use Password")
                            }
                        }
                    }
                }
            }

            // Options
            when (uiState.generatorType) {
                GeneratorType.PASSWORD -> PasswordOptions(
                    length = uiState.length,
                    includeLowercase = uiState.includeLowercase,
                    includeUppercase = uiState.includeUppercase,
                    includeDigits = uiState.includeDigits,
                    includeSpecial = uiState.includeSpecial,
                    onLengthChange = { viewModel.updateLength(it) },
                    onToggleLowercase = { viewModel.toggleLowercase(it) },
                    onToggleUppercase = { viewModel.toggleUppercase(it) },
                    onToggleDigits = { viewModel.toggleDigits(it) },
                    onToggleSpecial = { viewModel.toggleSpecial(it) }
                )
                GeneratorType.PASSPHRASE -> PassphraseOptions(
                    wordCount = uiState.passphraseWordCount,
                    separator = uiState.passphraseSeparator,
                    onWordCountChange = { viewModel.updatePassphraseWordCount(it) },
                    onSeparatorChange = { viewModel.updatePassphraseSeparator(it) }
                )
            }
        }
    }
}

@Composable
private fun PasswordOptions(
    length: Int,
    includeLowercase: Boolean,
    includeUppercase: Boolean,
    includeDigits: Boolean,
    includeSpecial: Boolean,
    onLengthChange: (Int) -> Unit,
    onToggleLowercase: (Boolean) -> Unit,
    onToggleUppercase: (Boolean) -> Unit,
    onToggleDigits: (Boolean) -> Unit,
    onToggleSpecial: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium
            )

            // Length slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Length",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$length",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = length.toFloat(),
                    onValueChange = { onLengthChange(it.toInt()) },
                    valueRange = 8f..64f,
                    steps = 55
                )
            }

            HorizontalDivider()

            Text(
                text = "Character Sets",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Character set checkboxes
            CharacterSetCheckbox(
                label = "Lowercase (a-z)",
                checked = includeLowercase,
                onCheckedChange = onToggleLowercase
            )
            CharacterSetCheckbox(
                label = "Uppercase (A-Z)",
                checked = includeUppercase,
                onCheckedChange = onToggleUppercase
            )
            CharacterSetCheckbox(
                label = "Numbers (0-9)",
                checked = includeDigits,
                onCheckedChange = onToggleDigits
            )
            CharacterSetCheckbox(
                label = "Special (!@#$%...)",
                checked = includeSpecial,
                onCheckedChange = onToggleSpecial
            )
        }
    }
}

@Composable
private fun PassphraseOptions(
    wordCount: Int,
    separator: String,
    onWordCountChange: (Int) -> Unit,
    onSeparatorChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium
            )

            // Word count slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Number of Words",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$wordCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = wordCount.toFloat(),
                    onValueChange = { onWordCountChange(it.toInt()) },
                    valueRange = 2f..8f,
                    steps = 5
                )
            }

            // Separator input
            OutlinedTextField(
                value = separator,
                onValueChange = onSeparatorChange,
                label = { Text("Separator") },
                placeholder = { Text("-") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun CharacterSetCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    val (color, label, progress) = when (strength) {
        PasswordStrength.WEAK -> Triple(
            MaterialTheme.colorScheme.error,
            "Weak",
            0.25f
        )
        PasswordStrength.MEDIUM -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "Medium",
            0.5f
        )
        PasswordStrength.STRONG -> Triple(
            MaterialTheme.colorScheme.primary,
            "Strong",
            0.75f
        )
        PasswordStrength.VERY_STRONG -> Triple(
            MaterialTheme.colorScheme.primary,
            "Very Strong",
            1f
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordGeneratorScreenPreview() {
    NexPassTheme {
        PasswordGeneratorScreen(
            onNavigateBack = {},
            onUsePassword = {}
        )
    }
}
