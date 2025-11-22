package com.nexpass.passwordmanager.autofill.service

import android.content.Intent
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import com.nexpass.passwordmanager.autofill.matcher.AutofillMatcher
import com.nexpass.passwordmanager.autofill.model.AutofillContext
import com.nexpass.passwordmanager.autofill.model.AutofillField
import com.nexpass.passwordmanager.autofill.model.FieldType
import com.nexpass.passwordmanager.autofill.ui.AutofillSavePromptActivity
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.UUID

/**
 * AutofillService implementation for NexPass.
 * Handles autofill requests from the Android system and provides password suggestions.
 */
class PasswordAutofillService : AutofillService() {

    private val autofillMatcher: AutofillMatcher by inject()
    private val passwordRepository: PasswordRepository by inject()
    private val securePreferences: SecurePreferences by inject()
    private val autofillResponseBuilder by lazy { AutofillResponseBuilder(this) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "PasswordAutofillService"
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        Log.d(TAG, "onFillRequest called")

        serviceScope.launch {
            try {
                // Parse the autofill request
                val context = parseAutofillContext(request)

                if (context == null) {
                    Log.w(TAG, "Failed to parse autofill context")
                    callback.onSuccess(null)
                    return@launch
                }

                Log.d(TAG, "Autofill context - Package: ${context.packageName}, Domain: ${context.webDomain}")

                // Check if vault is unlocked
                val isUnlocked = securePreferences.isVaultUnlocked()

                val response = if (isUnlocked) {
                    // Vault is unlocked, try to find matching entries
                    try {
                        buildUnlockedResponse(context)
                    } catch (e: IllegalStateException) {
                        // Vault state is inconsistent, treat as locked
                        Log.w(TAG, "Vault state inconsistent, showing unlock prompt", e)
                        buildLockedResponse(context)
                    }
                } else {
                    // Vault is locked, prompt for authentication
                    buildLockedResponse(context)
                }

                callback.onSuccess(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fill request", e)
                callback.onFailure(e.message)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Log.d(TAG, "onSaveRequest called")

        serviceScope.launch {
            try {
                // Extract username and password from the save request
                val contexts = request.fillContexts
                if (contexts.isEmpty()) {
                    callback.onSuccess()
                    return@launch
                }

                val latestContext = contexts.last()
                val structure = latestContext.structure

                // Extract package name and domain
                val packageName = structure.activityComponent.packageName
                val webDomain = extractWebDomain(structure)

                // Find username and password fields in client state
                var username = ""
                var password = ""

                if (structure.windowNodeCount > 0) {
                    structure.getWindowNodeAt(0)?.rootViewNode?.let { rootNode ->
                        extractCredentials(rootNode, request)?.let { (user, pass) ->
                            username = user
                            password = pass
                        }
                    }
                }

                Log.d(TAG, "Save request - Username: $username, Package: $packageName, Domain: $webDomain")

                // Check if autosave is enabled
                if (!securePreferences.isAutosaveEnabled()) {
                    Log.d(TAG, "Autosave is disabled in settings")
                    callback.onSuccess()
                    return@launch
                }

                // Check if this domain/package is in the never-save list
                val identifier = webDomain ?: packageName
                if (securePreferences.getNeverSaveDomains().contains(identifier)) {
                    Log.d(TAG, "Domain/package is in never-save list: $identifier")
                    callback.onSuccess()
                    return@launch
                }

                // Launch the save prompt activity if password is not empty
                if (password.isNotEmpty()) {
                    val intent = Intent(this@PasswordAutofillService, AutofillSavePromptActivity::class.java).apply {
                        putExtra(AutofillSavePromptActivity.EXTRA_USERNAME, username)
                        putExtra(AutofillSavePromptActivity.EXTRA_PASSWORD, password)
                        putExtra(AutofillSavePromptActivity.EXTRA_WEB_DOMAIN, webDomain)
                        putExtra(AutofillSavePromptActivity.EXTRA_PACKAGE_NAME, packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }

                    startActivity(intent)
                    Log.d(TAG, "Launched save prompt activity for: ${webDomain ?: packageName}")
                }

                callback.onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling save request", e)
                callback.onFailure(e.message)
            }
        }
    }

    /**
     * Build a fill response when the vault is unlocked.
     */
    private suspend fun buildUnlockedResponse(context: AutofillContext): FillResponse? {
        // Find matching password entries
        val matchingEntries = autofillMatcher.findMatchingEntries(context)

        Log.d(TAG, "Found ${matchingEntries.size} matching entries")

        // If no matches, show unlock prompt to allow manual selection
        if (matchingEntries.isEmpty()) {
            Log.d(TAG, "No matches found, showing manual search prompt")
            return buildLockedResponse(context)
        }

        // Build fill response with datasets
        return autofillResponseBuilder.buildFillResponse(
            entries = matchingEntries,
            fields = context.detectedFields,
            packageName = context.packageName ?: ""
        )
    }

    /**
     * Build a fill response when the vault is locked.
     * This prompts the user to unlock.
     */
    private fun buildLockedResponse(context: AutofillContext): FillResponse {
        Log.d(TAG, "Vault is locked, returning authentication response")

        return autofillResponseBuilder.buildAuthenticationResponse(
            fields = context.detectedFields,
            packageName = context.packageName ?: "",
            webDomain = context.webDomain
        )
    }

    /**
     * Parse the autofill context from the fill request.
     */
    private fun parseAutofillContext(request: FillRequest): AutofillContext? {
        val contexts = request.fillContexts
        if (contexts.isEmpty()) {
            return null
        }

        val latestContext = contexts.last()
        val structure = latestContext.structure

        val packageName = structure.activityComponent.packageName
        val webDomain = extractWebDomain(structure)

        // Parse fields from the view structure
        val fields = mutableListOf<AutofillField>()

        if (structure.windowNodeCount > 0) {
            structure.getWindowNodeAt(0)?.rootViewNode?.let { rootNode ->
                parseNode(rootNode, fields)
            }
        }

        Log.d(TAG, "Parsed ${fields.size} autofill fields")

        return AutofillContext(
            packageName = packageName,
            webDomain = webDomain,
            detectedFields = fields
        )
    }

    /**
     * Recursively parse view nodes to find autofillable fields.
     */
    private fun parseNode(
        node: android.app.assist.AssistStructure.ViewNode,
        fields: MutableList<AutofillField>
    ) {
        val autofillId = node.autofillId
        val autofillType = node.autofillType

        // Only process text fields with autofill IDs
        if (autofillId != null && autofillType == View.AUTOFILL_TYPE_TEXT) {
            val autofillHints = node.autofillHints
            val hint = autofillHints?.firstOrNull()
            val nodeHint = node.hint
            val inputType = node.inputType
            val idEntry = node.idEntry
            val htmlInfo = node.htmlInfo

            // Skip URL bar and search fields
            val shouldSkip = shouldSkipField(idEntry, nodeHint, autofillHints)
            if (shouldSkip) {
                // This is likely a URL bar or search field, skip it
            } else {
                // Determine field type from multiple sources
                val fieldType = determineFieldTypeFromNode(hint, nodeHint, inputType, idEntry, htmlInfo)

                // Only add if we can identify the field type
                if (fieldType != FieldType.UNKNOWN) {
                    fields.add(
                        AutofillField(
                            autofillId = autofillId,
                            autofillType = autofillType,
                            hint = hint,
                            isFocused = node.isFocused,
                            fieldType = fieldType
                        )
                    )

                    Log.d(TAG, "Found autofill field - Hint: $hint, NodeHint: $nodeHint, InputType: $inputType, IdEntry: $idEntry, Type: $fieldType, Focused: ${node.isFocused}")
                }
            }
        }

        // Recursively parse child nodes
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { parseNode(it, fields) }
        }
    }

    /**
     * Check if a field should be skipped (URL bars, search fields, etc.)
     */
    private fun shouldSkipField(
        idEntry: String?,
        nodeHint: CharSequence?,
        autofillHints: Array<String>?
    ): Boolean {
        // Patterns to skip (case-insensitive)
        val skipPatterns = listOf(
            "url", "address", "toolbar",           // URL/address bars
            "search", "query", "find", "lookup",   // Search fields
            "autocomplete", "suggest", "complete"  // Autocomplete/suggestion fields
        )

        // Check ID entry
        idEntry?.lowercase()?.let { id ->
            if (skipPatterns.any { pattern -> id.contains(pattern) }) {
                return true
            }
        }

        // Check node hint
        nodeHint?.toString()?.lowercase()?.let { hint ->
            if (skipPatterns.any { pattern -> hint.contains(pattern) }) {
                return true
            }
        }

        // Check autofill hints
        autofillHints?.forEach { autofillHint ->
            autofillHint.lowercase().let { hint ->
                if (skipPatterns.any { pattern -> hint.contains(pattern) }) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Determine the field type from multiple sources.
     */
    private fun determineFieldTypeFromNode(
        autofillHint: String?,
        nodeHint: CharSequence?,
        inputType: Int,
        idEntry: String?,
        htmlInfo: android.view.ViewStructure.HtmlInfo?
    ): FieldType {
        // Search field patterns to exclude
        val searchPatterns = listOf("search", "query", "find", "lookup", "autocomplete", "suggest", "complete")

        // Check if this is a search field and return UNKNOWN if so
        // This is a safeguard in case shouldSkipField() missed it
        autofillHint?.lowercase()?.let { hint ->
            if (searchPatterns.any { pattern -> hint.contains(pattern) }) {
                return FieldType.UNKNOWN
            }
        }

        nodeHint?.toString()?.lowercase()?.let { hint ->
            if (searchPatterns.any { pattern -> hint.contains(pattern) }) {
                return FieldType.UNKNOWN
            }
        }

        idEntry?.lowercase()?.let { id ->
            if (searchPatterns.any { pattern -> id.contains(pattern) }) {
                return FieldType.UNKNOWN
            }
        }

        // Check HTML attributes for search fields (WebViews)
        htmlInfo?.let { html ->
            val htmlType = html.attributes?.firstOrNull { it.first == "type" }?.second?.lowercase()
            if (htmlType == "search") {
                return FieldType.UNKNOWN
            }

            val htmlName = html.attributes?.firstOrNull { it.first == "name" }?.second?.lowercase()
            htmlName?.let { name ->
                if (searchPatterns.any { pattern -> name.contains(pattern) }) {
                    return FieldType.UNKNOWN
                }
            }

            val htmlId = html.attributes?.firstOrNull { it.first == "id" }?.second?.lowercase()
            htmlId?.let { id ->
                if (searchPatterns.any { pattern -> id.contains(pattern) }) {
                    return FieldType.UNKNOWN
                }
            }
        }

        // Check autofill hints first (most reliable)
        autofillHint?.lowercase()?.let { hint ->
            when {
                hint.contains("password") -> return FieldType.PASSWORD
                hint.contains("username") -> return FieldType.USERNAME
                hint.contains("email") -> return FieldType.EMAIL
                else -> Unit
            }
        }

        // Check input type (for native Android views)
        val isPassword = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                        (inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                        (inputType and android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
        if (isPassword) {
            return FieldType.PASSWORD
        }

        val isEmail = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
                     (inputType and android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0
        if (isEmail) {
            return FieldType.EMAIL
        }

        // Check HTML attributes (for WebViews)
        htmlInfo?.let { html ->
            val htmlTag = html.tag?.lowercase()
            val htmlType = html.attributes?.firstOrNull { it.first == "type" }?.second?.lowercase()

            if (htmlType == "password") {
                return FieldType.PASSWORD
            }
            if (htmlType == "email") {
                return FieldType.EMAIL
            }

            // Check HTML name/id attributes
            val htmlName = html.attributes?.firstOrNull { it.first == "name" }?.second?.lowercase()
            val htmlId = html.attributes?.firstOrNull { it.first == "id" }?.second?.lowercase()

            htmlName?.let { name ->
                when {
                    name.contains("password") || name.contains("pass") -> return FieldType.PASSWORD
                    name.contains("email") -> return FieldType.EMAIL
                    name.contains("user") || name.contains("login") -> return FieldType.USERNAME
                    else -> Unit
                }
            }

            htmlId?.let { id ->
                when {
                    id.contains("password") || id.contains("pass") -> return FieldType.PASSWORD
                    id.contains("email") -> return FieldType.EMAIL
                    id.contains("user") || id.contains("login") -> return FieldType.USERNAME
                    else -> Unit
                }
            }
        }

        // Check node hint
        nodeHint?.toString()?.lowercase()?.let { hint ->
            when {
                hint.contains("password") || hint.contains("pass") -> return FieldType.PASSWORD
                hint.contains("email") -> return FieldType.EMAIL
                hint.contains("user") || hint.contains("login") -> return FieldType.USERNAME
                else -> Unit
            }
        }

        // Check ID entry (resource name)
        idEntry?.lowercase()?.let { id ->
            when {
                id.contains("password") || id.contains("pass") -> return FieldType.PASSWORD
                id.contains("email") -> return FieldType.EMAIL
                id.contains("user") || id.contains("login") -> return FieldType.USERNAME
                else -> Unit
            }
        }

        return FieldType.UNKNOWN
    }

    /**
     * Extract web domain from the assist structure.
     */
    private fun extractWebDomain(structure: android.app.assist.AssistStructure): String? {
        return try {
            if (structure.windowNodeCount > 0) {
                val rootNode = structure.getWindowNodeAt(0)?.rootViewNode

                // First try the simple webDomain property (works for native WebViews)
                rootNode?.webDomain?.let { return it }

                // Recursively search for web domain in all nodes
                rootNode?.let { findWebDomain(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract web domain", e)
            null
        }
    }

    /**
     * Recursively find web domain in view nodes.
     */
    private fun findWebDomain(node: android.app.assist.AssistStructure.ViewNode): String? {
        // Check node's web domain (skip if empty)
        node.webDomain?.let {
            if (it.isNotBlank()) {
                Log.d(TAG, "Found webDomain property: $it")
                return it
            }
        }

        // Check HTML info
        node.htmlInfo?.let { html ->
            // Look for origin or action attributes
            val origin = html.attributes?.firstOrNull { it.first == "origin" }?.second
            if (origin != null) {
                Log.d(TAG, "Found origin attribute: $origin")
                extractDomainFromUrl(origin)?.let { return it }
            }

            val action = html.attributes?.firstOrNull { it.first == "action" }?.second
            if (action != null) {
                Log.d(TAG, "Found action attribute: $action")
                extractDomainFromUrl(action)?.let { return it }
            }
        }

        // For browsers, check URL bar text
        val idEntry = node.idEntry
        if (idEntry != null && (idEntry.contains("url") || idEntry.contains("address"))) {
            val urlText = node.text?.toString()
            Log.d(TAG, "Found URL bar field (idEntry=$idEntry), text: $urlText")
            urlText?.let {
                extractDomainFromUrl(it)?.let { domain ->
                    Log.d(TAG, "Extracted domain from URL bar: $domain")
                    return domain
                }
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { child ->
                findWebDomain(child)?.let { return it }
            }
        }

        return null
    }

    /**
     * Extract domain from a URL string.
     */
    private fun extractDomainFromUrl(url: String): String? {
        return try {
            val trimmedUrl = url.trim()

            // Skip if it's not a URL
            if (!trimmedUrl.contains(".") || trimmedUrl.length < 4) {
                return null
            }

            // Add scheme if missing
            val fullUrl = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                "https://$trimmedUrl"
            } else {
                trimmedUrl
            }

            val uri = android.net.Uri.parse(fullUrl)
            uri.host
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract domain from URL: $url", e)
            null
        }
    }

    /**
     * Extract username and password from the save request.
     */
    private fun extractCredentials(
        node: android.app.assist.AssistStructure.ViewNode,
        request: SaveRequest
    ): Pair<String, String>? {
        var username = ""
        var password = ""

        fun extractFromNode(n: android.app.assist.AssistStructure.ViewNode) {
            val hints = n.autofillHints
            val value = n.autofillValue

            if (hints != null && value != null && value.isText) {
                val text = value.textValue.toString()
                val hint = hints.firstOrNull()?.lowercase() ?: ""

                when {
                    hint.contains("username") || hint.contains("email") -> {
                        username = text
                    }
                    hint.contains("password") -> {
                        password = text
                    }
                }
            }

            for (i in 0 until n.childCount) {
                n.getChildAt(i)?.let { extractFromNode(it) }
            }
        }

        extractFromNode(node)

        return if (password.isNotEmpty()) {
            Pair(username, password)
        } else {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
