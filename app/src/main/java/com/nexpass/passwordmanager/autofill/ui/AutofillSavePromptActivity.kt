package com.nexpass.passwordmanager.autofill.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexpass.passwordmanager.ui.screens.autofill.AutofillSavePromptScreen
import com.nexpass.passwordmanager.ui.theme.NexPassTheme
import com.nexpass.passwordmanager.ui.viewmodel.AutofillSavePromptViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity for prompting user to save autofilled credentials.
 * Displayed as a dialog over the app requesting autofill.
 */
class AutofillSavePromptActivity : ComponentActivity() {

    private val viewModel: AutofillSavePromptViewModel by viewModel()

    companion object {
        const val EXTRA_USERNAME = "username"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_WEB_DOMAIN = "webDomain"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val RESULT_SAVED = 1
        const val RESULT_UPDATED = 2
        const val RESULT_CANCELLED = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract data from intent
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
        val password = intent.getStringExtra(EXTRA_PASSWORD) ?: ""
        val webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        // Validate required data
        if (password.isEmpty() || packageName.isEmpty()) {
            Toast.makeText(
                this,
                "Missing required data for autosave",
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            NexPassTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AutofillSavePromptScreen(
                        packageName = packageName,
                        webDomain = webDomain,
                        viewModel = viewModel,
                        onSaveSuccess = { isNew ->
                            // Show toast notification
                            val message = if (isNew) {
                                "Password saved to NexPass"
                            } else {
                                "Password updated"
                            }
                            Toast.makeText(this@AutofillSavePromptActivity, message, Toast.LENGTH_SHORT).show()

                            // Return result
                            val resultCode = if (isNew) RESULT_SAVED else RESULT_UPDATED
                            setResult(resultCode)
                            finish()
                        },
                        onCancel = {
                            // Show toast for cancellation
                            Toast.makeText(
                                this@AutofillSavePromptActivity,
                                "Password not saved",
                                Toast.LENGTH_SHORT
                            ).show()

                            setResult(RESULT_CANCELLED)
                            finish()
                        }
                    )
                }
            }
        }

        // Initialize ViewModel with autofill data
        viewModel.initializeWithAutofillData(
            username = username,
            password = password,
            domain = webDomain,
            packageName = packageName
        )
    }
}
