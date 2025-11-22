package com.nexpass.passwordmanager.autofill.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.nexpass.passwordmanager.ui.theme.NexPassTheme

/**
 * Activity launched from notification to manually input password for saving.
 * Shows a dialog with username and password fields.
 */
class NotificationPasswordInputActivity : ComponentActivity() {

    companion object {
        private const val TAG = "NotificationPasswordInput"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_WEB_DOMAIN = "webDomain"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)

        Log.d(TAG, "Launched for package: $packageName, domain: $webDomain")

        setContent {
            NexPassTheme {
                PasswordInputDialog(
                    packageName = packageName,
                    webDomain = webDomain,
                    onSave = { username, password ->
                        savePassword(username, password, packageName, webDomain)
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }

    /**
     * Save the password and launch the save prompt activity.
     */
    private fun savePassword(username: String, password: String, packageName: String, webDomain: String?) {
        if (password.isEmpty()) {
            Log.w(TAG, "Password is empty, not saving")
            finish()
            return
        }

        Log.d(TAG, "Launching save prompt with username: $username, domain: $webDomain")

        // Launch the autofill save prompt activity
        val intent = android.content.Intent(this, AutofillSavePromptActivity::class.java).apply {
            putExtra(AutofillSavePromptActivity.EXTRA_USERNAME, username)
            putExtra(AutofillSavePromptActivity.EXTRA_PASSWORD, password)
            putExtra(AutofillSavePromptActivity.EXTRA_WEB_DOMAIN, webDomain)
            putExtra(AutofillSavePromptActivity.EXTRA_PACKAGE_NAME, packageName)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
        finish()
    }
}

/**
 * Composable dialog for password input.
 */
@Composable
fun PasswordInputDialog(
    packageName: String,
    webDomain: String?,
    onSave: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val displayName = webDomain ?: packageName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Save Password for $displayName")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter the credentials you want to save:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(username, password)
                },
                enabled = password.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
