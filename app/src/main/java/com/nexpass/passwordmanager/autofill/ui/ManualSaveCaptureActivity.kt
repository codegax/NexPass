package com.nexpass.passwordmanager.autofill.ui

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.autofill.AutofillManager
import androidx.activity.ComponentActivity
import com.nexpass.passwordmanager.autofill.model.FieldType

/**
 * Activity launched when user selects "Save Password" from autofill dropdown.
 * Captures the current field values and launches the save prompt.
 */
class ManualSaveCaptureActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ManualSaveCaptureActivity"
        const val EXTRA_ASSIST_STRUCTURE = "assistStructure"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_WEB_DOMAIN = "webDomain"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== ManualSaveCaptureActivity launched ===")

        // Log all intent extras to debug what we receive
        val extras = intent.extras
        if (extras != null) {
            Log.d(TAG, "Intent extras received:")
            for (key in extras.keySet()) {
                Log.d(TAG, "  - $key: ${extras.get(key)}")
            }
        } else {
            Log.w(TAG, "⚠️ No intent extras received at all!")
        }

        // Extract assist structure from intent
        @Suppress("DEPRECATION")
        val structure = intent.getParcelableExtra<AssistStructure>(EXTRA_ASSIST_STRUCTURE)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        Log.d(TAG, "Package name: $packageName")
        Log.d(TAG, "AssistStructure: ${if (structure != null) "✅ Present" else "❌ NULL"}")

        // Extract web domain from structure if not provided
        var webDomain = intent.getStringExtra(EXTRA_WEB_DOMAIN)
        if (webDomain == null && structure != null) {
            webDomain = extractWebDomain(structure)
        }

        if (structure == null) {
            Log.e(TAG, "❌ CRITICAL: No AssistStructure provided - Dataset authentication doesn't provide it!")
            Log.e(TAG, "This is a known Android limitation: Dataset.setAuthentication() does NOT include EXTRA_ASSIST_STRUCTURE")
            Log.e(TAG, "Only FillResponse.setAuthentication() provides EXTRA_ASSIST_STRUCTURE")
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        // Extract username and password from the structure
        val credentials = extractCredentials(structure)

        if (credentials == null || credentials.second.isEmpty()) {
            Log.w(TAG, "No password found in form")
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        val (username, password) = credentials

        // Launch the save prompt activity
        val saveIntent = Intent(this, AutofillSavePromptActivity::class.java).apply {
            putExtra(AutofillSavePromptActivity.EXTRA_USERNAME, username)
            putExtra(AutofillSavePromptActivity.EXTRA_PASSWORD, password)
            putExtra(AutofillSavePromptActivity.EXTRA_WEB_DOMAIN, webDomain)
            putExtra(AutofillSavePromptActivity.EXTRA_PACKAGE_NAME, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(saveIntent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    /**
     * Extract username and password from AssistStructure.
     */
    private fun extractCredentials(structure: AssistStructure): Pair<String, String>? {
        var username = ""
        var password = ""

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            windowNode.rootViewNode?.let { rootNode ->
                extractFromNode(rootNode) { hint, value ->
                    when {
                        hint.contains("password", ignoreCase = true) -> {
                            password = value
                        }
                        hint.contains("username", ignoreCase = true) ||
                        hint.contains("email", ignoreCase = true) -> {
                            username = value
                        }
                    }
                }
            }
        }

        return if (password.isNotEmpty()) {
            Pair(username, password)
        } else {
            null
        }
    }

    /**
     * Recursively extract field values from view nodes.
     */
    private fun extractFromNode(
        node: AssistStructure.ViewNode,
        onField: (hint: String, value: String) -> Unit
    ) {
        // Check autofill hints
        node.autofillHints?.forEach { hint ->
            node.autofillValue?.textValue?.toString()?.let { value ->
                if (value.isNotEmpty()) {
                    onField(hint, value)
                }
            }
        }

        // Check HTML attributes for web forms
        node.htmlInfo?.attributes?.forEach { attr ->
            if (attr.first == "type" || attr.first == "name" || attr.first == "id") {
                node.autofillValue?.textValue?.toString()?.let { value ->
                    if (value.isNotEmpty()) {
                        onField(attr.second.toString(), value)
                    }
                }
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { extractFromNode(it, onField) }
        }
    }

    /**
     * Extract web domain from AssistStructure.
     */
    private fun extractWebDomain(structure: AssistStructure): String? {
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            windowNode.rootViewNode?.let { rootNode ->
                val domain = extractDomainFromNode(rootNode)
                if (domain != null) return domain
            }
        }
        return null
    }

    /**
     * Recursively search for web domain in view nodes.
     */
    private fun extractDomainFromNode(node: AssistStructure.ViewNode): String? {
        // Check webDomain property (most reliable for browsers)
        node.webDomain?.let { return it }

        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { child ->
                val domain = extractDomainFromNode(child)
                if (domain != null) return domain
            }
        }

        return null
    }
}
