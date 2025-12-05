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
import com.nexpass.passwordmanager.autofill.notification.AutosaveNotificationManager
import com.nexpass.passwordmanager.domain.model.PasswordEntry
import com.nexpass.passwordmanager.domain.repository.PasswordRepository
import com.nexpass.passwordmanager.data.local.preferences.SecurePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val notificationManager by lazy { AutosaveNotificationManager(this) }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Track pending notification jobs to avoid duplicate notifications
    private val pendingNotificationJobs = mutableMapOf<String, Job>()

    companion object {
        private const val TAG = "PasswordAutofillService"
        private const val NOTIFICATION_DELAY_MS = 8000L // Wait 8 seconds after field focus before showing notification
        private const val NEXPASS_PACKAGE_DEBUG = "com.nexpass.passwordmanager.debug"
        private const val NEXPASS_PACKAGE_RELEASE = "com.nexpass.passwordmanager"
        
        // Regex pattern for detecting username-related ID fields (with word boundaries)
        private val USERNAME_ID_PATTERN = Regex("\\b(user_?id|login_?id|uid)\\b")
        // Regex patterns for more precise matching with word boundaries
        private val USERNAME_PATTERN = Regex("\\b(user|login|account|identifier)\\b")
        private val EMAIL_PATTERN = Regex("\\b(email|e-mail|e_mail)\\b")
        private val PASSWORD_PATTERN = Regex("\\bpassword\\b")
        private val PASS_PATTERN = Regex("\\bpass\\b")
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

                // Skip NexPass's own package to avoid saving master password
                if (packageName == NEXPASS_PACKAGE_DEBUG || packageName == NEXPASS_PACKAGE_RELEASE) {
                    Log.d(TAG, "Skipping save request - this is NexPass's own package")
                    callback.onSuccess()
                    return@launch
                }

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

                // Check if we have existing entries for this site
                val existingEntries = try {
                    passwordRepository.getAll().filter { entry ->
                        // Check if entry matches by URL or package name
                        (entry.url == identifier) || entry.packageNames.contains(identifier)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not check existing entries: ${e.message}")
                    emptyList()
                }

                if (password.isNotEmpty()) {
                    if (existingEntries.isNotEmpty()) {
                        // For existing sites: Show direct save dialog (password update scenario)
                        Log.d(TAG, "💾 Launching direct save dialog for password update on existing site: $identifier")
                        val intent = Intent(this@PasswordAutofillService, AutofillSavePromptActivity::class.java).apply {
                            putExtra(AutofillSavePromptActivity.EXTRA_USERNAME, username)
                            putExtra(AutofillSavePromptActivity.EXTRA_PASSWORD, password)
                            putExtra(AutofillSavePromptActivity.EXTRA_WEB_DOMAIN, webDomain)
                            putExtra(AutofillSavePromptActivity.EXTRA_PACKAGE_NAME, packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    } else {
                        // For new sites: Show notification (user will manually re-enter)
                        Log.d(TAG, "📢 Showing autosave notification for new site: ${webDomain ?: packageName}")
                        notificationManager.showSavePasswordNotification(
                            packageName = packageName,
                            webDomain = webDomain
                        )
                    }
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

        // If no matches, schedule notification and show unlock prompt
        if (matchingEntries.isEmpty()) {
            Log.d(TAG, "⚠️ No matches found, showing unlock/search prompt")

            // Schedule delayed notification for NEW sites if autosave is enabled
            if (securePreferences.isAutosaveEnabled() && hasPasswordField(context.detectedFields)) {
                val packageName = context.packageName ?: ""

                // Skip NexPass's own package to avoid saving master password
                if (packageName == NEXPASS_PACKAGE_DEBUG || packageName == NEXPASS_PACKAGE_RELEASE) {
                    Log.d(TAG, "Skipping notification - this is NexPass's own package")
                } else {
                    val identifier = context.webDomain ?: packageName
                    // Only show notification if not in never-save list
                    if (identifier.isNotEmpty() && !securePreferences.getNeverSaveDomains().contains(identifier)) {
                        scheduleDelayedNotification(
                            identifier = identifier,
                            packageName = packageName,
                            webDomain = context.webDomain
                        )
                        Log.d(TAG, "📢 Scheduled notification for NEW site: $identifier")
                    } else {
                        Log.d(TAG, "Not scheduling notification - site is in never-save list or invalid")
                    }
                }
            } else {
                Log.d(TAG, "Not scheduling notification - autosave disabled or no password field")
            }

            return buildLockedResponse(context)
        }

        // Build fill response with datasets for existing entries
        // This includes a manual "Save" option in the autofill dropdown for updating passwords
        Log.d(TAG, "Building fill response with ${matchingEntries.size} entries and manual save option")
        val response = autofillResponseBuilder.buildFillResponse(
            entries = matchingEntries,
            fields = context.detectedFields,
            packageName = context.packageName ?: ""
        )
        Log.d(TAG, "Fill response built: ${if (response != null) "✅ Success" else "❌ NULL"}")
        return response
    }

    /**
     * Build a fill response when the vault is locked.
     * This prompts the user to unlock.
     *
     * @param showNotification Whether to show autosave notification (true for locked vault, false when called after checking entries)
     */
    private fun buildLockedResponse(context: AutofillContext, showNotification: Boolean = false): FillResponse {
        Log.d(TAG, "Vault is locked, returning authentication response")

        // Show notification to save password if requested
        if (showNotification && securePreferences.isAutosaveEnabled() && hasPasswordField(context.detectedFields)) {
            val identifier = context.webDomain ?: context.packageName
            if (identifier != null && !securePreferences.getNeverSaveDomains().contains(identifier)) {
                Log.d(TAG, "📢 Showing autosave notification (vault locked): $identifier")
                notificationManager.showSavePasswordNotification(
                    packageName = context.packageName ?: "",
                    webDomain = context.webDomain
                )
            }
        }

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
        val allTextFields = mutableListOf<AutofillField>()

        if (structure.windowNodeCount > 0) {
            structure.getWindowNodeAt(0)?.rootViewNode?.let { rootNode ->
                parseNode(rootNode, fields, allTextFields)
            }
        }

        // Apply heuristics to identify username fields from unidentified text fields
        applyUsernameHeuristics(fields, allTextFields)

        Log.d(TAG, "Parsed ${fields.size} autofill fields")

        return AutofillContext(
            packageName = packageName,
            webDomain = webDomain,
            detectedFields = fields
        )
    }

    /**
     * Apply heuristics to identify username fields from unidentified text fields.
     * If we have a password field but no username/email field, and there are unidentified
     * text fields, we assume the first unidentified text field is the username field.
     */
    private fun applyUsernameHeuristics(
        identifiedFields: MutableList<AutofillField>,
        unidentifiedFields: List<AutofillField>
    ) {
        // Check if we have a password field
        val hasPasswordField = identifiedFields.any { it.fieldType == FieldType.PASSWORD }
        
        // Check if we already have a username or email field
        val hasUsernameField = identifiedFields.any { 
            it.fieldType == FieldType.USERNAME || it.fieldType == FieldType.EMAIL 
        }

        // If we have a password but no username, and there are unidentified fields,
        // assume the first unidentified field is the username field
        if (hasPasswordField && !hasUsernameField && unidentifiedFields.isNotEmpty()) {
            val usernameField = unidentifiedFields.first().copy(fieldType = FieldType.USERNAME)
            identifiedFields.add(usernameField)
            Log.d(TAG, "Applied heuristic: Identified unidentified text field as USERNAME (appears with password field)")
        }
    }

    /**
     * Recursively parse view nodes to find autofillable fields.
     * Stores both identified fields and potential unidentified text fields for heuristic analysis.
     */
    private fun parseNode(
        node: android.app.assist.AssistStructure.ViewNode,
        fields: MutableList<AutofillField>,
        allTextFields: MutableList<AutofillField>
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

                val field = AutofillField(
                    autofillId = autofillId,
                    autofillType = autofillType,
                    hint = hint,
                    isFocused = node.isFocused,
                    fieldType = fieldType
                )

                // Add to identified fields if we can determine the type
                if (fieldType != FieldType.UNKNOWN) {
                    fields.add(field)
                    Log.d(TAG, "Found autofill field - Hint: $hint, NodeHint: $nodeHint, InputType: $inputType, IdEntry: $idEntry, Type: $fieldType, Focused: ${node.isFocused}")
                } else {
                    // Store unidentified text fields for potential heuristic analysis
                    allTextFields.add(field)
                    Log.d(TAG, "Found unidentified text field - Hint: $hint, NodeHint: $nodeHint, InputType: $inputType, IdEntry: $idEntry")
                }
            }
        }

        // Recursively parse child nodes
        for (i in 0 until node.childCount) {
            node.getChildAt(i)?.let { parseNode(it, fields, allTextFields) }
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
     * Check if a text matches common username field patterns.
     */
    private fun isUsernamePattern(text: String): Boolean {
        return USERNAME_PATTERN.containsMatchIn(text) || USERNAME_ID_PATTERN.containsMatchIn(text)
    }

    /**
     * Check if a text matches common email field patterns.
     */
    private fun isEmailPattern(text: String): Boolean {
        return EMAIL_PATTERN.containsMatchIn(text)
    }

    /**
     * Check if a text matches common password field patterns.
     */
    private fun isPasswordPattern(text: String): Boolean {
        // Check for "password", "pass" as standalone words, or "passwd"
        return PASSWORD_PATTERN.containsMatchIn(text) || 
               PASS_PATTERN.containsMatchIn(text) ||
               text.contains("passwd")
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
                isPasswordPattern(hint) -> return FieldType.PASSWORD
                isEmailPattern(hint) -> return FieldType.EMAIL
                isUsernamePattern(hint) || hint.contains("username") -> return FieldType.USERNAME
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

            // Check HTML name/id attributes with expanded patterns
            val htmlName = html.attributes?.firstOrNull { it.first == "name" }?.second?.lowercase()
            val htmlId = html.attributes?.firstOrNull { it.first == "id" }?.second?.lowercase()

            htmlName?.let { name ->
                when {
                    isPasswordPattern(name) -> return FieldType.PASSWORD
                    isEmailPattern(name) -> return FieldType.EMAIL
                    isUsernamePattern(name) -> return FieldType.USERNAME
                    else -> Unit
                }
            }

            htmlId?.let { id ->
                when {
                    isPasswordPattern(id) -> return FieldType.PASSWORD
                    isEmailPattern(id) -> return FieldType.EMAIL
                    isUsernamePattern(id) -> return FieldType.USERNAME
                    else -> Unit
                }
            }

            // Check HTML autocomplete attribute (standard HTML5 attribute)
            val htmlAutocomplete = html.attributes?.firstOrNull { it.first == "autocomplete" }?.second?.lowercase()
            htmlAutocomplete?.let { autocomplete ->
                when {
                    autocomplete.contains("current-password") || autocomplete.contains("new-password") -> return FieldType.PASSWORD
                    autocomplete.contains("email") -> return FieldType.EMAIL
                    autocomplete.contains("username") || autocomplete.contains("nickname") -> return FieldType.USERNAME
                    else -> Unit
                }
            }
        }

        // Check node hint with expanded patterns
        nodeHint?.toString()?.lowercase()?.let { hint ->
            when {
                isPasswordPattern(hint) -> return FieldType.PASSWORD
                isEmailPattern(hint) -> return FieldType.EMAIL
                isUsernamePattern(hint) -> return FieldType.USERNAME
                else -> Unit
            }
        }

        // Check ID entry (resource name) with expanded patterns
        idEntry?.lowercase()?.let { id ->
            when {
                isPasswordPattern(id) -> return FieldType.PASSWORD
                isEmailPattern(id) -> return FieldType.EMAIL
                isUsernamePattern(id) -> return FieldType.USERNAME
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

    /**
     * Schedule a delayed notification to save password.
     * Delays showing the notification to give user time to enter credentials.
     *
     * @param identifier Unique identifier for this login form (domain or package name)
     * @param packageName The app package name
     * @param webDomain The web domain (if browser), null otherwise
     */
    private fun scheduleDelayedNotification(
        identifier: String,
        packageName: String,
        webDomain: String?
    ) {
        // Cancel any existing job for this identifier to avoid duplicate notifications
        pendingNotificationJobs[identifier]?.cancel()

        Log.d(TAG, "⏱️ Scheduling delayed notification for $identifier (delay: ${NOTIFICATION_DELAY_MS}ms)")

        // Schedule new delayed notification
        val job = serviceScope.launch {
            delay(NOTIFICATION_DELAY_MS)

            // Double-check autosave is still enabled and site not in never-save list
            if (securePreferences.isAutosaveEnabled() &&
                !securePreferences.getNeverSaveDomains().contains(identifier)) {

                Log.d(TAG, "📢 Showing delayed autosave notification for: $identifier")
                notificationManager.showSavePasswordNotification(
                    packageName = packageName,
                    webDomain = webDomain
                )
            } else {
                Log.d(TAG, "Cancelled notification - autosave disabled or site in never-save list")
            }

            // Remove job from map after completion
            pendingNotificationJobs.remove(identifier)
        }

        pendingNotificationJobs[identifier] = job
    }

    /**
     * Check if the detected fields include a password field.
     */
    private fun hasPasswordField(fields: List<AutofillField>): Boolean {
        return fields.any { it.fieldType == FieldType.PASSWORD }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending notification jobs
        pendingNotificationJobs.values.forEach { it.cancel() }
        pendingNotificationJobs.clear()
        serviceScope.cancel()
    }
}
