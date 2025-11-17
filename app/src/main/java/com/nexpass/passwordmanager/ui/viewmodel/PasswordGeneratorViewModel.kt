package com.nexpass.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.nexpass.passwordmanager.domain.util.PasswordGenerator
import com.nexpass.passwordmanager.domain.util.PasswordStrength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for password generator screen.
 *
 * Manages password generation options and generated password state.
 */
class PasswordGeneratorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordGeneratorUiState())
    val uiState: StateFlow<PasswordGeneratorUiState> = _uiState.asStateFlow()

    init {
        // Generate initial password
        generatePassword()
    }

    /**
     * Generate a new password with current options.
     */
    fun generatePassword() {
        val state = _uiState.value

        val password = if (state.generatorType == GeneratorType.PASSWORD) {
            PasswordGenerator.generate(
                length = state.length,
                includeLowercase = state.includeLowercase,
                includeUppercase = state.includeUppercase,
                includeDigits = state.includeDigits,
                includeSpecial = state.includeSpecial
            )
        } else {
            PasswordGenerator.generatePassphrase(
                wordCount = state.passphraseWordCount,
                separator = state.passphraseSeparator
            )
        }

        val strength = PasswordGenerator.calculateStrength(password)

        _uiState.value = state.copy(
            generatedPassword = password,
            passwordStrength = strength
        )
    }

    /**
     * Update password length.
     */
    fun updateLength(length: Int) {
        _uiState.value = _uiState.value.copy(length = length)
        generatePassword()
    }

    /**
     * Toggle lowercase letters.
     */
    fun toggleLowercase(enabled: Boolean) {
        // Ensure at least one character set is enabled
        if (!enabled && !canDisableCharacterSet()) {
            return
        }
        _uiState.value = _uiState.value.copy(includeLowercase = enabled)
        generatePassword()
    }

    /**
     * Toggle uppercase letters.
     */
    fun toggleUppercase(enabled: Boolean) {
        if (!enabled && !canDisableCharacterSet()) {
            return
        }
        _uiState.value = _uiState.value.copy(includeUppercase = enabled)
        generatePassword()
    }

    /**
     * Toggle digits.
     */
    fun toggleDigits(enabled: Boolean) {
        if (!enabled && !canDisableCharacterSet()) {
            return
        }
        _uiState.value = _uiState.value.copy(includeDigits = enabled)
        generatePassword()
    }

    /**
     * Toggle special characters.
     */
    fun toggleSpecial(enabled: Boolean) {
        if (!enabled && !canDisableCharacterSet()) {
            return
        }
        _uiState.value = _uiState.value.copy(includeSpecial = enabled)
        generatePassword()
    }

    /**
     * Switch between password and passphrase generator.
     */
    fun setGeneratorType(type: GeneratorType) {
        _uiState.value = _uiState.value.copy(generatorType = type)
        generatePassword()
    }

    /**
     * Update passphrase word count.
     */
    fun updatePassphraseWordCount(count: Int) {
        _uiState.value = _uiState.value.copy(passphraseWordCount = count)
        generatePassword()
    }

    /**
     * Update passphrase separator.
     */
    fun updatePassphraseSeparator(separator: String) {
        _uiState.value = _uiState.value.copy(passphraseSeparator = separator)
        generatePassword()
    }

    /**
     * Check if a character set can be disabled (at least one must remain enabled).
     */
    private fun canDisableCharacterSet(): Boolean {
        val state = _uiState.value
        val enabledCount = listOf(
            state.includeLowercase,
            state.includeUppercase,
            state.includeDigits,
            state.includeSpecial
        ).count { it }
        return enabledCount > 1
    }
}

/**
 * UI state for password generator screen.
 */
data class PasswordGeneratorUiState(
    val generatedPassword: String = "",
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK,
    val generatorType: GeneratorType = GeneratorType.PASSWORD,
    val length: Int = 16,
    val includeLowercase: Boolean = true,
    val includeUppercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSpecial: Boolean = true,
    val passphraseWordCount: Int = 4,
    val passphraseSeparator: String = "-"
)

/**
 * Password generator types.
 */
enum class GeneratorType {
    PASSWORD,
    PASSPHRASE
}
