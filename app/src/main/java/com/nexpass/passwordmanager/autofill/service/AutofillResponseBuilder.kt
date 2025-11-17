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

        // Add save info to allow saving new credentials
        val saveInfo = buildSaveInfo(fields)
        saveInfo?.let { fillResponseBuilder.setSaveInfo(it) }

        return try {
            fillResponseBuilder.build()
        } catch (e: Exception) {
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

        // We need at least a password field to save
        if (passwordField == null) {
            return null
        }

        val requiredIds = mutableListOf<AutofillId>(passwordField.autofillId)
        usernameField?.let { requiredIds.add(it.autofillId) }

        return SaveInfo.Builder(
            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
            requiredIds.toTypedArray()
        ).build()
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

        return responseBuilder.build()
    }
}
