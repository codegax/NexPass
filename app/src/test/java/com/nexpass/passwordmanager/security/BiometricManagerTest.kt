package com.nexpass.passwordmanager.security

import com.nexpass.passwordmanager.security.biometric.BiometricAvailability
import com.nexpass.passwordmanager.security.biometric.BiometricCallback
import com.nexpass.passwordmanager.security.biometric.BiometricCryptoCallback
import com.nexpass.passwordmanager.security.biometric.BiometricErrorCodes
import com.nexpass.passwordmanager.security.biometric.BiometricManager
import com.nexpass.passwordmanager.security.biometric.BiometricManagerImpl
import com.nexpass.passwordmanager.security.biometric.SimpleBiometricCallback
import com.nexpass.passwordmanager.security.biometric.SimpleBiometricCryptoCallback
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BiometricManager.
 *
 * Note: These tests are designed to run on a JVM environment.
 * Full integration tests with actual biometric hardware require
 * an Android device or emulator (instrumented tests).
 *
 * These tests verify:
 * - Interface contracts
 * - Callback mechanisms
 * - Error code constants
 * - Availability status handling
 */
class BiometricManagerTest {

    private lateinit var biometricManager: BiometricManager

    @Before
    fun setup() {
        biometricManager = BiometricManagerImpl()
    }

    /**
     * Test: BiometricAvailability sealed class structure
     */
    @Test
    fun testBiometricAvailability_types() {
        // Verify all availability types can be instantiated
        val available = BiometricAvailability.Available
        val notEnrolled = BiometricAvailability.NotEnrolled
        val notAvailable = BiometricAvailability.NotAvailable
        val notStrong = BiometricAvailability.NotStrong
        val error = BiometricAvailability.Error("Test error")

        assertNotNull("Available should not be null", available)
        assertNotNull("NotEnrolled should not be null", notEnrolled)
        assertNotNull("NotAvailable should not be null", notAvailable)
        assertNotNull("NotStrong should not be null", notStrong)
        assertNotNull("Error should not be null", error)
        assertEquals("Error message should match", "Test error", error.message)
    }

    /**
     * Test: BiometricAvailability is sealed (exhaustive when)
     */
    @Test
    fun testBiometricAvailability_exhaustiveWhen() {
        val availability: BiometricAvailability = BiometricAvailability.Available

        // Test exhaustive when - should compile without else branch
        val message = when (availability) {
            is BiometricAvailability.Available -> "Available"
            is BiometricAvailability.NotEnrolled -> "Not enrolled"
            is BiometricAvailability.NotAvailable -> "Not available"
            is BiometricAvailability.NotStrong -> "Not strong"
            is BiometricAvailability.Error -> "Error: ${availability.message}"
        }

        assertEquals("Should return correct message", "Available", message)
    }

    /**
     * Test: BiometricCallback implementation
     */
    @Test
    fun testBiometricCallback_success() {
        var successCalled = false
        var failedCalled = false
        var errorCalled = false

        val callback = object : BiometricCallback {
            override fun onAuthenticationSucceeded() {
                successCalled = true
            }

            override fun onAuthenticationFailed() {
                failedCalled = true
            }

            override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                errorCalled = true
            }
        }

        callback.onAuthenticationSucceeded()
        assertTrue("Success callback should be called", successCalled)
        assertFalse("Failed callback should not be called", failedCalled)
        assertFalse("Error callback should not be called", errorCalled)
    }

    /**
     * Test: BiometricCallback implementation - failure
     */
    @Test
    fun testBiometricCallback_failed() {
        var successCalled = false
        var failedCalled = false
        var errorCalled = false

        val callback = object : BiometricCallback {
            override fun onAuthenticationSucceeded() {
                successCalled = true
            }

            override fun onAuthenticationFailed() {
                failedCalled = true
            }

            override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                errorCalled = true
            }
        }

        callback.onAuthenticationFailed()
        assertFalse("Success callback should not be called", successCalled)
        assertTrue("Failed callback should be called", failedCalled)
        assertFalse("Error callback should not be called", errorCalled)
    }

    /**
     * Test: BiometricCallback implementation - error
     */
    @Test
    fun testBiometricCallback_error() {
        var successCalled = false
        var failedCalled = false
        var errorCode = -1
        var errorMessage = ""

        val callback = object : BiometricCallback {
            override fun onAuthenticationSucceeded() {
                successCalled = true
            }

            override fun onAuthenticationFailed() {
                failedCalled = true
            }

            override fun onAuthenticationError(code: Int, message: String) {
                errorCode = code
                errorMessage = message
            }
        }

        callback.onAuthenticationError(BiometricErrorCodes.ERROR_LOCKOUT, "Too many attempts")
        assertFalse("Success callback should not be called", successCalled)
        assertFalse("Failed callback should not be called", failedCalled)
        assertEquals("Error code should match", BiometricErrorCodes.ERROR_LOCKOUT, errorCode)
        assertEquals("Error message should match", "Too many attempts", errorMessage)
    }

    /**
     * Test: SimpleBiometricCallback helper class
     */
    @Test
    fun testSimpleBiometricCallback_success() {
        var successCalled = false

        val callback = SimpleBiometricCallback(
            onSuccess = { successCalled = true }
        )

        callback.onAuthenticationSucceeded()
        assertTrue("Success callback should be called", successCalled)
    }

    /**
     * Test: SimpleBiometricCallback helper class - all callbacks
     */
    @Test
    fun testSimpleBiometricCallback_allCallbacks() {
        var successCalled = false
        var failedCalled = false
        var errorCode = -1

        val callback = SimpleBiometricCallback(
            onSuccess = { successCalled = true },
            onFailed = { failedCalled = true },
            onError = { code, _ -> errorCode = code }
        )

        callback.onAuthenticationSucceeded()
        assertTrue("Success should be called", successCalled)

        callback.onAuthenticationFailed()
        assertTrue("Failed should be called", failedCalled)

        callback.onAuthenticationError(BiometricErrorCodes.ERROR_TIMEOUT, "Timeout")
        assertEquals("Error code should match", BiometricErrorCodes.ERROR_TIMEOUT, errorCode)
    }

    /**
     * Test: BiometricCryptoCallback implementation
     */
    @Test
    fun testBiometricCryptoCallback_success() {
        var cryptoObjectReceived = false

        val callback = object : BiometricCryptoCallback {
            override fun onAuthenticationSucceeded(cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject) {
                cryptoObjectReceived = true
            }

            override fun onAuthenticationFailed() {}

            override fun onAuthenticationError(errorCode: Int, errorMessage: String) {}
        }

        // Note: Can't create actual CryptoObject without Android framework
        // This test just verifies the interface structure
        assertNotNull("Callback should not be null", callback)
    }

    /**
     * Test: SimpleBiometricCryptoCallback helper class
     */
    @Test
    fun testSimpleBiometricCryptoCallback() {
        var cryptoObjectReceived = false
        var failedCalled = false
        var errorCode = -1

        val callback = SimpleBiometricCryptoCallback(
            onSuccess = { cryptoObjectReceived = true },
            onFailed = { failedCalled = true },
            onError = { code, _ -> errorCode = code }
        )

        // Verify all callbacks are accessible
        callback.onAuthenticationFailed()
        assertTrue("Failed should be called", failedCalled)

        callback.onAuthenticationError(BiometricErrorCodes.ERROR_CANCELED, "Canceled")
        assertEquals("Error code should match", BiometricErrorCodes.ERROR_CANCELED, errorCode)
    }

    /**
     * Test: BiometricErrorCodes constants
     */
    @Test
    fun testBiometricErrorCodes_values() {
        assertEquals("ERROR_HW_UNAVAILABLE should be 1", 1, BiometricErrorCodes.ERROR_HW_UNAVAILABLE)
        assertEquals("ERROR_UNABLE_TO_PROCESS should be 2", 2, BiometricErrorCodes.ERROR_UNABLE_TO_PROCESS)
        assertEquals("ERROR_TIMEOUT should be 3", 3, BiometricErrorCodes.ERROR_TIMEOUT)
        assertEquals("ERROR_NO_SPACE should be 4", 4, BiometricErrorCodes.ERROR_NO_SPACE)
        assertEquals("ERROR_CANCELED should be 5", 5, BiometricErrorCodes.ERROR_CANCELED)
        assertEquals("ERROR_LOCKOUT should be 7", 7, BiometricErrorCodes.ERROR_LOCKOUT)
        assertEquals("ERROR_VENDOR should be 8", 8, BiometricErrorCodes.ERROR_VENDOR)
        assertEquals("ERROR_LOCKOUT_PERMANENT should be 9", 9, BiometricErrorCodes.ERROR_LOCKOUT_PERMANENT)
        assertEquals("ERROR_USER_CANCELED should be 10", 10, BiometricErrorCodes.ERROR_USER_CANCELED)
        assertEquals("ERROR_NO_BIOMETRICS should be 11", 11, BiometricErrorCodes.ERROR_NO_BIOMETRICS)
        assertEquals("ERROR_HW_NOT_PRESENT should be 12", 12, BiometricErrorCodes.ERROR_HW_NOT_PRESENT)
        assertEquals("ERROR_NEGATIVE_BUTTON should be 13", 13, BiometricErrorCodes.ERROR_NEGATIVE_BUTTON)
        assertEquals("ERROR_NO_DEVICE_CREDENTIAL should be 14", 14, BiometricErrorCodes.ERROR_NO_DEVICE_CREDENTIAL)
        assertEquals("ERROR_SECURITY_UPDATE_REQUIRED should be 15", 15, BiometricErrorCodes.ERROR_SECURITY_UPDATE_REQUIRED)
    }

    /**
     * Test: BiometricAvailability equality
     */
    @Test
    fun testBiometricAvailability_equality() {
        val available1 = BiometricAvailability.Available
        val available2 = BiometricAvailability.Available
        val notAvailable = BiometricAvailability.NotAvailable

        assertEquals("Same instances should be equal", available1, available2)
        assertNotEquals("Different types should not be equal", available1, notAvailable)
    }

    /**
     * Test: BiometricAvailability.Error equality
     */
    @Test
    fun testBiometricAvailabilityError_equality() {
        val error1 = BiometricAvailability.Error("Test error")
        val error2 = BiometricAvailability.Error("Test error")
        val error3 = BiometricAvailability.Error("Different error")

        assertEquals("Errors with same message should be equal", error1, error2)
        assertNotEquals("Errors with different messages should not be equal", error1, error3)
    }

    /**
     * Test: BiometricManager isBiometricAvailable returns NotAvailable on JVM
     */
    @Test
    fun testIsBiometricAvailable_onJVM() {
        // On JVM without Android framework, should return NotAvailable
        val availability = biometricManager.isBiometricAvailable()
        assertEquals("Should return NotAvailable on JVM", BiometricAvailability.NotAvailable, availability)
    }

    /**
     * Test: Error code mapping
     */
    @Test
    fun testErrorCodeMapping() {
        val errorCodes = mapOf(
            BiometricErrorCodes.ERROR_LOCKOUT to "Lockout",
            BiometricErrorCodes.ERROR_USER_CANCELED to "User canceled",
            BiometricErrorCodes.ERROR_NO_BIOMETRICS to "No biometrics enrolled",
            BiometricErrorCodes.ERROR_HW_NOT_PRESENT to "Hardware not present"
        )

        errorCodes.forEach { (code, description) ->
            assertTrue("Error code $code should be positive", code > 0)
            assertNotNull("Description should not be null", description)
        }
    }

    /**
     * Test: Callback error handling with all error codes
     */
    @Test
    fun testCallback_allErrorCodes() {
        val errorCodes = listOf(
            BiometricErrorCodes.ERROR_HW_UNAVAILABLE,
            BiometricErrorCodes.ERROR_UNABLE_TO_PROCESS,
            BiometricErrorCodes.ERROR_TIMEOUT,
            BiometricErrorCodes.ERROR_NO_SPACE,
            BiometricErrorCodes.ERROR_CANCELED,
            BiometricErrorCodes.ERROR_LOCKOUT,
            BiometricErrorCodes.ERROR_VENDOR,
            BiometricErrorCodes.ERROR_LOCKOUT_PERMANENT,
            BiometricErrorCodes.ERROR_USER_CANCELED,
            BiometricErrorCodes.ERROR_NO_BIOMETRICS,
            BiometricErrorCodes.ERROR_HW_NOT_PRESENT,
            BiometricErrorCodes.ERROR_NEGATIVE_BUTTON,
            BiometricErrorCodes.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricErrorCodes.ERROR_SECURITY_UPDATE_REQUIRED
        )

        val receivedCodes = mutableListOf<Int>()

        val callback = SimpleBiometricCallback(
            onSuccess = {},
            onError = { code, _ -> receivedCodes.add(code) }
        )

        // Test all error codes
        errorCodes.forEach { code ->
            callback.onAuthenticationError(code, "Test error")
        }

        assertEquals("All error codes should be received", errorCodes.size, receivedCodes.size)
        errorCodes.forEach { code ->
            assertTrue("Error code $code should be received", receivedCodes.contains(code))
        }
    }
}
