package com.nexpass.passwordmanager.security.biometric

import androidx.fragment.app.FragmentActivity

/**
 * BiometricManager handles biometric authentication using Android BiometricPrompt.
 *
 * This interface abstracts biometric authentication operations including:
 * - Checking biometric availability
 * - Showing biometric prompt
 * - Handling authentication callbacks
 *
 * Security properties:
 * - Only strong biometric authentication (Class 3)
 * - Fallback to master password after max failed attempts
 * - Secure crypto object support for Keystore integration
 */
interface BiometricManager {

    /**
     * Check if strong biometric authentication is available on the device.
     *
     * Strong biometrics (Class 3) include:
     * - Fingerprint
     * - Iris
     * - Face (if strong enough)
     *
     * @return BiometricAvailability status
     */
    fun isBiometricAvailable(): BiometricAvailability

    /**
     * Show the biometric prompt for authentication.
     *
     * @param activity The FragmentActivity to host the biometric prompt
     * @param title Title shown in the biometric prompt dialog
     * @param subtitle Optional subtitle for additional context
     * @param description Optional description explaining why authentication is needed
     * @param negativeButtonText Text for the negative button (default: "Cancel")
     * @param callback Callback for authentication results
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        callback: BiometricCallback
    )

    /**
     * Show the biometric prompt with crypto object support.
     * This is used when decrypting data that requires biometric authentication.
     *
     * @param activity The FragmentActivity to host the biometric prompt
     * @param cryptoObject The crypto object (Cipher) that requires biometric auth
     * @param title Title shown in the biometric prompt dialog
     * @param subtitle Optional subtitle for additional context
     * @param description Optional description explaining why authentication is needed
     * @param negativeButtonText Text for the negative button (default: "Cancel")
     * @param callback Callback for authentication results with crypto object
     */
    fun showBiometricPromptWithCrypto(
        activity: FragmentActivity,
        cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        callback: BiometricCryptoCallback
    )
}

/**
 * Biometric availability status.
 */
sealed class BiometricAvailability {
    /**
     * Strong biometric authentication is available and enrolled.
     */
    object Available : BiometricAvailability()

    /**
     * Biometric hardware is available but no biometrics are enrolled.
     */
    object NotEnrolled : BiometricAvailability()

    /**
     * Biometric hardware is not available on this device.
     */
    object NotAvailable : BiometricAvailability()

    /**
     * Biometric authentication is available but not strong enough (Class 2 or lower).
     */
    object NotStrong : BiometricAvailability()

    /**
     * Unknown error occurred while checking biometric availability.
     */
    data class Error(val message: String) : BiometricAvailability()
}

/**
 * Callback for biometric authentication results.
 */
interface BiometricCallback {
    /**
     * Called when biometric authentication succeeds.
     */
    fun onAuthenticationSucceeded()

    /**
     * Called when biometric authentication fails.
     * This is called for each failed attempt (e.g., wrong fingerprint).
     */
    fun onAuthenticationFailed()

    /**
     * Called when an error occurs during authentication.
     * This includes:
     * - Too many failed attempts (lockout)
     * - User canceled the prompt
     * - Hardware error
     *
     * @param errorCode Error code from BiometricPrompt
     * @param errorMessage Human-readable error message
     */
    fun onAuthenticationError(errorCode: Int, errorMessage: String)
}

/**
 * Callback for biometric authentication with crypto object support.
 */
interface BiometricCryptoCallback {
    /**
     * Called when biometric authentication succeeds with a crypto object.
     *
     * @param cryptoObject The authenticated crypto object that can now be used
     */
    fun onAuthenticationSucceeded(cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject)

    /**
     * Called when biometric authentication fails.
     */
    fun onAuthenticationFailed()

    /**
     * Called when an error occurs during authentication.
     *
     * @param errorCode Error code from BiometricPrompt
     * @param errorMessage Human-readable error message
     */
    fun onAuthenticationError(errorCode: Int, errorMessage: String)
}

/**
 * Biometric error codes (from BiometricPrompt).
 */
object BiometricErrorCodes {
    const val ERROR_HW_UNAVAILABLE = 1
    const val ERROR_UNABLE_TO_PROCESS = 2
    const val ERROR_TIMEOUT = 3
    const val ERROR_NO_SPACE = 4
    const val ERROR_CANCELED = 5
    const val ERROR_LOCKOUT = 7
    const val ERROR_VENDOR = 8
    const val ERROR_LOCKOUT_PERMANENT = 9
    const val ERROR_USER_CANCELED = 10
    const val ERROR_NO_BIOMETRICS = 11
    const val ERROR_HW_NOT_PRESENT = 12
    const val ERROR_NEGATIVE_BUTTON = 13
    const val ERROR_NO_DEVICE_CREDENTIAL = 14
    const val ERROR_SECURITY_UPDATE_REQUIRED = 15
}
