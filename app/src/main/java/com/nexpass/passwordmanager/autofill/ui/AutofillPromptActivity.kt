package com.nexpass.passwordmanager.autofill.ui

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexpass.passwordmanager.R
import com.nexpass.passwordmanager.autofill.matcher.AutofillMatcher
import com.nexpass.passwordmanager.autofill.model.AutofillContext
import com.nexpass.passwordmanager.autofill.model.AutofillField
import com.nexpass.passwordmanager.autofill.service.AutofillResponseBuilder
import android.view.autofill.AutofillId
import android.util.Base64
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import com.nexpass.passwordmanager.security.biometric.BiometricManager
import com.nexpass.passwordmanager.security.vault.VaultKeyManager
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity shown when the vault needs to be unlocked during an autofill request.
 * This is displayed as a transparent dialog over the app requesting autofill.
 */
class AutofillPromptActivity : FragmentActivity() {

    private val viewModel: AutofillPromptViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract context from intent
        val packageName = intent.getStringExtra("packageName")
        val webDomain = intent.getStringExtra("webDomain")
        @Suppress("DEPRECATION")
        val autofillIds = intent.getParcelableArrayExtra("autofillIds")
            ?.filterIsInstance<AutofillId>()
            ?.toTypedArray()
        val fieldTypes = intent.getStringArrayExtra("fieldTypes")

        setContent {
            NexPassTheme {
                AutofillPromptScreen(
                    packageName = packageName ?: "",
                    webDomain = webDomain,
                    viewModel = viewModel,
                    onUnlockSuccess = { response ->
                        // Return the fill response to the autofill framework
                        // If response is null, the autofill framework will retry the request
                        val replyIntent = Intent().apply {
                            if (response != null) {
                                putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
                            }
                        }
                        setResult(Activity.RESULT_OK, replyIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }

        // Initialize ViewModel with autofill context
        viewModel.setAutofillContext(packageName, webDomain, autofillIds, fieldTypes)

        // Try biometric unlock automatically if available
        viewModel.tryBiometricUnlock(this)
    }
}

@Composable
fun AutofillPromptScreen(
    packageName: String,
    webDomain: String?,
    viewModel: AutofillPromptViewModel,
    onUnlockSuccess: (FillResponse?) -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle successful unlock
    LaunchedEffect(uiState) {
        if (uiState is AutofillPromptUiState.UnlockSuccess) {
            val response = (uiState as AutofillPromptUiState.UnlockSuccess).response
            onUnlockSuccess(response)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is AutofillPromptUiState.Locked -> {
                UnlockPromptContent(
                    packageName = packageName,
                    webDomain = webDomain,
                    errorMessage = state.errorMessage,
                    onUnlock = { password ->
                        viewModel.unlockWithPassword(password, packageName, webDomain)
                    },
                    onCancel = onCancel
                )
            }
            is AutofillPromptUiState.Unlocking -> {
                LoadingContent()
            }
            is AutofillPromptUiState.UnlockSuccess -> {
                // Will be handled by LaunchedEffect above
            }
        }
    }
}

@Composable
fun UnlockPromptContent(
    packageName: String,
    webDomain: String?,
    errorMessage: String?,
    onUnlock: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.autofill_unlock_title),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.autofill_unlock_message,
                    webDomain ?: packageName
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.enter_master_password)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = { onUnlock(password) },
                    modifier = Modifier.weight(1f),
                    enabled = password.isNotEmpty()
                ) {
                    Text(stringResource(R.string.unlock))
                }
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * ViewModel for the autofill prompt activity.
 */
class AutofillPromptViewModel(
    private val vaultKeyManager: VaultKeyManager,
    private val securePreferences: SecurePreferences,
    private val biometricManager: BiometricManager,
    private val autofillMatcher: AutofillMatcher,
    private val autofillResponseBuilder: AutofillResponseBuilder
) : ViewModel() {

    private val _uiState = MutableStateFlow<AutofillPromptUiState>(
        AutofillPromptUiState.Locked()
    )
    val uiState: StateFlow<AutofillPromptUiState> = _uiState.asStateFlow()

    // Store autofill context
    private var autofillContext: AutofillContext? = null

    /**
     * Set the autofill context from intent extras.
     */
    fun setAutofillContext(
        packageName: String?,
        webDomain: String?,
        autofillIds: Array<AutofillId>?,
        fieldTypes: Array<String>?
    ) {
        if (autofillIds != null && fieldTypes != null && autofillIds.size == fieldTypes.size) {
            val fields = autofillIds.mapIndexed { index, id ->
                AutofillField(
                    autofillId = id,
                    autofillType = android.view.View.AUTOFILL_TYPE_TEXT,
                    hint = null,
                    isFocused = false,
                    fieldType = com.nexpass.passwordmanager.autofill.model.FieldType.valueOf(fieldTypes[index])
                )
            }
            autofillContext = AutofillContext(
                packageName = packageName,
                webDomain = webDomain,
                detectedFields = fields
            )
        }
    }

    /**
     * Try to unlock with biometric authentication.
     */
    fun tryBiometricUnlock(activity: FragmentActivity) {
        viewModelScope.launch {
            val hasBiometric = securePreferences.isBiometricEnabled()

            if (hasBiometric) {
                biometricManager.showBiometricPrompt(
                    activity = activity,
                    title = "Unlock NexPass",
                    subtitle = "Unlock to autofill passwords",
                    callback = object : com.nexpass.passwordmanager.security.biometric.BiometricCallback {
                        override fun onAuthenticationSucceeded() {
                            // Biometric authentication successful
                            // Mark vault as unlocked
                            securePreferences.setVaultUnlocked(true)
                            _uiState.value = AutofillPromptUiState.UnlockSuccess(null)
                        }

                        override fun onAuthenticationFailed() {
                            // Single authentication attempt failed, but can retry
                        }

                        override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                            // Biometric failed permanently, fall back to password
                            _uiState.value = AutofillPromptUiState.Locked()
                        }
                    }
                )
            }
        }
    }

    /**
     * Unlock the vault with master password.
     */
    fun unlockWithPassword(password: String, packageName: String, webDomain: String?) {
        viewModelScope.launch {
            _uiState.value = AutofillPromptUiState.Unlocking

            try {
                // Get stored salt
                val saltBase64 = securePreferences.getMasterSalt()
                    ?: throw IllegalStateException("Vault not initialized")

                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)

                // Attempt to unlock vault
                vaultKeyManager.unlockWithPassword(password, salt)

                // Mark vault as unlocked
                securePreferences.setVaultUnlocked(true)

                // Build fill response with matching entries
                val fillResponse = autofillContext?.let { context ->
                    // Find matching password entries
                    val matchingEntries = autofillMatcher.findMatchingEntries(context)

                    if (matchingEntries.isNotEmpty()) {
                        // Build fill response with datasets
                        autofillResponseBuilder.buildFillResponse(
                            entries = matchingEntries,
                            fields = context.detectedFields,
                            packageName = context.packageName ?: ""
                        )
                    } else {
                        null
                    }
                }

                _uiState.value = AutofillPromptUiState.UnlockSuccess(fillResponse)
            } catch (e: Exception) {
                _uiState.value = AutofillPromptUiState.Locked(
                    errorMessage = e.message ?: "Failed to unlock vault"
                )
            }
        }
    }
}

/**
 * UI state for the autofill prompt.
 */
sealed class AutofillPromptUiState {
    data class Locked(val errorMessage: String? = null) : AutofillPromptUiState()
    object Unlocking : AutofillPromptUiState()
    data class UnlockSuccess(val response: FillResponse?) : AutofillPromptUiState()
}
