package com.nexpass.passwordmanager.autofill.model

import android.view.autofill.AutofillId

/**
 * Context information extracted from an autofill request.
 * Contains all relevant data needed to match and fill credentials.
 */
data class AutofillContext(
    /**
     * The package name of the app requesting autofill.
     * e.g., "com.android.chrome", "com.google.android.gm"
     */
    val packageName: String?,

    /**
     * The web domain if this is a browser autofill request.
     * e.g., "github.com", "google.com"
     */
    val webDomain: String?,

    /**
     * List of detected autofill fields (username, password, etc.)
     */
    val detectedFields: List<AutofillField>
)

/**
 * Represents a single autofill field detected in the view structure.
 */
data class AutofillField(
    /**
     * Unique identifier for this field in the autofill framework.
     */
    val autofillId: AutofillId,

    /**
     * The type of autofill this field accepts.
     * See [android.view.View.AUTOFILL_TYPE_*]
     */
    val autofillType: Int,

    /**
     * The autofill hint for this field.
     * e.g., "username", "password", "emailAddress"
     */
    val hint: String?,

    /**
     * Whether this field is currently focused.
     */
    val isFocused: Boolean,

    /**
     * The field type determined by our heuristics.
     */
    val fieldType: FieldType = FieldType.UNKNOWN
)

/**
 * Categorized field types based on autofill hints and heuristics.
 */
enum class FieldType {
    USERNAME,
    PASSWORD,
    EMAIL,
    UNKNOWN
}
