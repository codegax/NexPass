package com.nexpass.passwordmanager.security.biometric

import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/**
 * Implementation of BiometricManager using AndroidX BiometricPrompt.
 *
 * This implementation provides biometric authentication with:
 * - Strong biometric support only (Class 3)
 * - Crypto object support for Keystore integration
 * - Proper error handling and callbacks
 *
 * Thread safety: BiometricPrompt must be called on the main thread.
 */
class BiometricManagerImpl : BiometricManager {

    override fun isBiometricAvailable(): BiometricAvailability {
        // Note: This needs Context which would be passed in a real implementation
        // For now, return a placeholder that will be implemented when integrated with Android
        return BiometricAvailability.NotAvailable
    }

    /**
     * Implementation that takes context for checking biometric availability.
     */
    fun isBiometricAvailable(activity: FragmentActivity): BiometricAvailability {
        val biometricManager = androidx.biometric.BiometricManager.from(activity)

        return when (biometricManager.canAuthenticate(Authenticators.BIOMETRIC_STRONG)) {
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability.Available
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability.NotAvailable
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability.Error("Biometric hardware is currently unavailable")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability.NotEnrolled
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricAvailability.Error("Security update required for biometric authentication")
            }
            androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                BiometricAvailability.NotStrong
            }
            androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                BiometricAvailability.Error("Unknown biometric status")
            }
            else -> {
                BiometricAvailability.Error("Unknown error occurred")
            }
        }
    }

    override fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        description: String?,
        negativeButtonText: String,
        callback: BiometricCallback
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.onAuthenticationSucceeded()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onAuthenticationFailed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onAuthenticationError(errorCode, errString.toString())
                }
            }
        )

        val promptInfo = buildPromptInfo(title, subtitle, description, negativeButtonText)
        biometricPrompt.authenticate(promptInfo)
    }

    override fun showBiometricPromptWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String?,
        description: String?,
        negativeButtonText: String,
        callback: BiometricCryptoCallback
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.let { crypto ->
                        callback.onAuthenticationSucceeded(crypto)
                    } ?: run {
                        callback.onAuthenticationError(
                            BiometricErrorCodes.ERROR_UNABLE_TO_PROCESS,
                            "Crypto object not available"
                        )
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onAuthenticationFailed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onAuthenticationError(errorCode, errString.toString())
                }
            }
        )

        val promptInfo = buildPromptInfo(title, subtitle, description, negativeButtonText)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    /**
     * Build BiometricPrompt.PromptInfo with the given parameters.
     */
    private fun buildPromptInfo(
        title: String,
        subtitle: String?,
        description: String?,
        negativeButtonText: String
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()
    }
}

/**
 * Simple implementation of BiometricCallback.
 */
class SimpleBiometricCallback(
    private val onSuccess: () -> Unit,
    private val onFailed: () -> Unit = {},
    private val onError: (Int, String) -> Unit = { _, _ -> }
) : BiometricCallback {

    override fun onAuthenticationSucceeded() {
        onSuccess()
    }

    override fun onAuthenticationFailed() {
        onFailed()
    }

    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
        onError(errorCode, errorMessage)
    }
}

/**
 * Simple implementation of BiometricCryptoCallback.
 */
class SimpleBiometricCryptoCallback(
    private val onSuccess: (BiometricPrompt.CryptoObject) -> Unit,
    private val onFailed: () -> Unit = {},
    private val onError: (Int, String) -> Unit = { _, _ -> }
) : BiometricCryptoCallback {

    override fun onAuthenticationSucceeded(cryptoObject: BiometricPrompt.CryptoObject) {
        onSuccess(cryptoObject)
    }

    override fun onAuthenticationFailed() {
        onFailed()
    }

    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
        onError(errorCode, errorMessage)
    }
}
