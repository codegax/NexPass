package com.nexpass.passwordmanager.autofill.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.nexpass.passwordmanager.R
import com.nexpass.passwordmanager.autofill.model.AutofillField
import com.nexpass.passwordmanager.autofill.model.FieldType
import com.nexpass.passwordmanager.domain.model.PasswordEntry

/**
 * Builder for creating autofill responses (FillResponse and Dataset).
 * Handles presentation of credentials to the user and filling of fields.
 */
class AutofillResponseBuilder(private val context: Context) {

    /**
     * Build a FillResponse containing datasets for matching password entries.
     */
    fun buildFillResponse(
        entries: List<PasswordEntry>,
        fields: List<AutofillField>,
        packageName: String
    ): FillResponse? {
        if (entries.isEmpty()) {
            return null
        }

        val fillResponseBuilder = FillResponse.Builder()

        // Add dataset for each matching entry
        entries.forEach { entry ->
            val dataset = buildDataset(entry, fields, packageName)
            dataset?.let { fillResponseBuilder.addDataset(it) }
        }

        // Add "Save Password" manual trigger dataset
        android.util.Log.d("AutofillResponseBuilder", "=== Attempting to build manual save dataset ===")
        android.util.Log.d("AutofillResponseBuilder", "Fields count: ${fields.size}, Package: $packageName")
        val saveDataset = buildManualSaveDataset(fields, packageName)
        if (saveDataset != null) {
            android.util.Log.d("AutofillResponseBuilder", "✅ Manual save dataset created successfully, adding to response")
            fillResponseBuilder.addDataset(saveDataset)
        } else {
            android.util.Log.w("AutofillResponseBuilder", "❌ Manual save dataset is NULL - not added to response")
        }

        // Add save info to allow saving new credentials (still needed for native apps)
        val saveInfo = buildSaveInfo(fields)
        if (saveInfo != null) {
            fillResponseBuilder.setSaveInfo(saveInfo)
            android.util.Log.d("AutofillResponseBuilder", "SaveInfo added to FillResponse")
        } else {
            android.util.Log.w("AutofillResponseBuilder", "SaveInfo is NULL - won't trigger save dialog!")
        }

        return try {
            fillResponseBuilder.build()
        } catch (e: Exception) {
            android.util.Log.e("AutofillResponseBuilder", "Failed to build FillResponse", e)
            null
        }
    }

    /**
     * Build a special "Save Password" dataset that launches manual save flow.
     */
    private fun buildManualSaveDataset(
        fields: List<AutofillField>,
        packageName: String
    ): Dataset? {
        android.util.Log.d("AutofillResponseBuilder", "buildManualSaveDataset() called for package: $packageName")

        // Create intent to launch manual save capture
        val saveIntent = Intent(context, com.nexpass.passwordmanager.autofill.ui.ManualSaveCaptureActivity::class.java).apply {
            putExtra("packageName", packageName)
            // NOTE: Dataset authentication does NOT provide EXTRA_ASSIST_STRUCTURE automatically!
            // This is a known Android limitation - only FillResponse.setAuthentication() provides it
        }

        val savePendingIntent = PendingIntent.getActivity(
            context,
            1, // Different request code from auth prompt
            saveIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        // Create presentation for "Save Password" option
        val presentation = RemoteViews(context.packageName, R.layout.autofill_item)
        presentation.setTextViewText(R.id.autofill_title, "\uD83D\uDCBE Save This Password")
        presentation.setTextViewText(R.id.autofill_username, "Tap to save current credentials")
        presentation.setImageViewResource(R.id.autofill_icon, android.R.drawable.ic_menu_save)

        val datasetBuilder = Dataset.Builder(presentation)

        // Set authentication - when user selects this, Android launches our activity
        // with EXTRA_ASSIST_STRUCTURE containing current field values
        val usernameField = fields.firstOrNull {
            it.fieldType == FieldType.USERNAME || it.fieldType == FieldType.EMAIL
        }
        val passwordField = fields.firstOrNull { it.fieldType == FieldType.PASSWORD }

        // We need at least one field to attach the authentication to
        passwordField?.let { field ->
            android.util.Log.d("AutofillResponseBuilder", "Found password field for manual save dataset")
            datasetBuilder.setValue(
                field.autofillId,
                null, // Don't actually fill any value
                presentation
            )
        } ?: run {
            android.util.Log.w("AutofillResponseBuilder", "⚠️ No password field found - manual save dataset may not work!")
        }

        // Set authentication on the dataset
        datasetBuilder.setAuthentication(savePendingIntent.intentSender)
        android.util.Log.d("AutofillResponseBuilder", "Set authentication on manual save dataset")

        return try {
            val dataset = datasetBuilder.build()
            android.util.Log.d("AutofillResponseBuilder", "✅ Manual save dataset built successfully")
            dataset
        } catch (e: Exception) {
            android.util.Log.e("AutofillResponseBuilder", "❌ Failed to build manual save dataset", e)
            null
        }
    }

    /**
     * Build a Dataset for a single password entry.
     */
    private fun buildDataset(
        entry: PasswordEntry,
        fields: List<AutofillField>,
        packageName: String
    ): Dataset? {
        val datasetBuilder = Dataset.Builder()

        // Create presentation for the dataset (how it appears in autofill UI)
        val presentation = buildPresentation(entry)

        // Find username and password fields
        val usernameField = fields.firstOrNull {
            it.fieldType == FieldType.USERNAME || it.fieldType == FieldType.EMAIL
        }
        val passwordField = fields.firstOrNull { it.fieldType == FieldType.PASSWORD }

        // Set values for username field
        usernameField?.let { field ->
            datasetBuilder.setValue(
                field.autofillId,
                AutofillValue.forText(entry.username),
                presentation
            )
        }

        // Set values for password field
        passwordField?.let { field ->
            datasetBuilder.setValue(
                field.autofillId,
                AutofillValue.forText(entry.password),
                presentation
            )
        }

        // For Android 11+, add inline presentation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addInlinePresentation(datasetBuilder, entry)
        }

        return try {
            datasetBuilder.build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build RemoteViews presentation for the dataset.
     * This is what the user sees in the autofill dropdown.
     */
    private fun buildPresentation(entry: PasswordEntry): RemoteViews {
        val presentation = RemoteViews(context.packageName, R.layout.autofill_item)

        // Set title
        presentation.setTextViewText(R.id.autofill_title, entry.title)

        // Set username
        presentation.setTextViewText(R.id.autofill_username, entry.username)

        // Set icon (use app icon for now)
        presentation.setImageViewResource(R.id.autofill_icon, R.mipmap.ic_launcher)

        return presentation
    }

    /**
     * Add inline presentation for Android 11+ (autofill in IME).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun addInlinePresentation(
        datasetBuilder: Dataset.Builder,
        entry: PasswordEntry
    ) {
        // TODO: Implement inline presentation with InlinePresentationSpec
        // This requires checking available inline presentation specs and building
        // appropriate inline suggestions. For MVP, we can skip this.
    }

    /**
     * Build SaveInfo to enable saving new credentials from autofill.
     */
    private fun buildSaveInfo(fields: List<AutofillField>): SaveInfo? {
        val usernameField = fields.firstOrNull {
            it.fieldType == FieldType.USERNAME || it.fieldType == FieldType.EMAIL
        }
        val passwordField = fields.firstOrNull { it.fieldType == FieldType.PASSWORD }

        android.util.Log.d("AutofillResponseBuilder", "buildSaveInfo - usernameField: ${usernameField != null}, passwordField: ${passwordField != null}")

        // We need at least a password field to save
        if (passwordField == null) {
            android.util.Log.w("AutofillResponseBuilder", "No password field found - cannot build SaveInfo")
            return null
        }

        // Only password is required, username is optional
        val requiredIds = arrayOf(passwordField.autofillId)

        val builder = SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            requiredIds
        )

        // Add optional username field if present
        usernameField?.let {
            builder.setOptionalIds(arrayOf(it.autofillId))
            android.util.Log.d("AutofillResponseBuilder", "Added optional username field to SaveInfo")
        }

        // Set flags to trigger save more aggressively
        // FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE: Trigger save when all views become invisible (form submission/navigation)
        // FLAG_DELAY_SAVE: Delay the save UI to better detect form submissions
        val flags = SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE or SaveInfo.FLAG_DELAY_SAVE
        builder.setFlags(flags)
        android.util.Log.d("AutofillResponseBuilder", "Set SaveInfo flags: FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE | FLAG_DELAY_SAVE")

        android.util.Log.d("AutofillResponseBuilder", "Successfully built SaveInfo")
        return builder.build()
    }

    /**
     * Build a FillResponse that prompts the user to unlock the vault.
     */
    fun buildAuthenticationResponse(
        fields: List<AutofillField>,
        packageName: String,
        webDomain: String?
    ): FillResponse {
        val authIntent = Intent(context, com.nexpass.passwordmanager.autofill.ui.AutofillPromptActivity::class.java).apply {
            putExtra("packageName", packageName)
            putExtra("webDomain", webDomain)
            // Pass field IDs (AutofillId is Parcelable)
            putExtra("autofillIds", fields.map { it.autofillId }.toTypedArray())
            // Pass field types
            putExtra("fieldTypes", fields.map { it.fieldType.name }.toTypedArray())
        }

        val authPendingIntent = PendingIntent.getActivity(
            context,
            0,
            authIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        val presentation = RemoteViews(context.packageName, R.layout.autofill_unlock_item)
        presentation.setTextViewText(
            R.id.autofill_unlock_text,
            context.getString(R.string.autofill_unlock_vault)
        )

        val responseBuilder = FillResponse.Builder()
        responseBuilder.setAuthentication(
            fields.map { it.autofillId }.toTypedArray(),
            authPendingIntent.intentSender,
            presentation
        )

        // IMPORTANT: Add SaveInfo so Android triggers save dialog after form submission
        val saveInfo = buildSaveInfo(fields)
        if (saveInfo != null) {
            responseBuilder.setSaveInfo(saveInfo)
            android.util.Log.d("AutofillResponseBuilder", "SaveInfo added to authentication response")
        } else {
            android.util.Log.w("AutofillResponseBuilder", "SaveInfo is NULL in authentication response")
        }

        return responseBuilder.build()
    }
}
