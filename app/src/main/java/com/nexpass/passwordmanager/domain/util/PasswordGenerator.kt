package com.nexpass.passwordmanager.domain.util

import java.security.SecureRandom

/**
 * Password generator utility.
 *
 * Generates cryptographically secure random passwords with customizable options.
 */
object PasswordGenerator {

    private val secureRandom = SecureRandom()

    // Character sets
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    /**
     * Generate a random password with specified options.
     *
     * @param length Password length (default: 16)
     * @param includeLowercase Include lowercase letters
     * @param includeUppercase Include uppercase letters
     * @param includeDigits Include digits
     * @param includeSpecial Include special characters
     * @return Generated password
     */
    fun generate(
        length: Int = 16,
        includeLowercase: Boolean = true,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSpecial: Boolean = true
    ): String {
        require(length >= 4) { "Password length must be at least 4 characters" }
        require(includeLowercase || includeUppercase || includeDigits || includeSpecial) {
            "At least one character set must be enabled"
        }

        // Build character pool
        val charPool = buildString {
            if (includeLowercase) append(LOWERCASE)
            if (includeUppercase) append(UPPERCASE)
            if (includeDigits) append(DIGITS)
            if (includeSpecial) append(SPECIAL)
        }

        // Ensure at least one character from each enabled set
        val password = StringBuilder()

        if (includeLowercase) {
            password.append(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }
        if (includeUppercase) {
            password.append(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
        }
        if (includeDigits) {
            password.append(DIGITS[secureRandom.nextInt(DIGITS.length)])
        }
        if (includeSpecial) {
            password.append(SPECIAL[secureRandom.nextInt(SPECIAL.length)])
        }

        // Fill remaining characters randomly
        while (password.length < length) {
            password.append(charPool[secureRandom.nextInt(charPool.length)])
        }

        // Shuffle to avoid predictable pattern
        return password.toString().toCharArray().apply {
            // Fisher-Yates shuffle
            for (i in size - 1 downTo 1) {
                val j = secureRandom.nextInt(i + 1)
                val temp = this[i]
                this[i] = this[j]
                this[j] = temp
            }
        }.concatToString()
    }

    /**
     * Generate a memorable passphrase (word-based).
     *
     * @param wordCount Number of words (default: 4)
     * @param separator Separator between words (default: "-")
     * @return Generated passphrase
     */
    fun generatePassphrase(
        wordCount: Int = 4,
        separator: String = "-"
    ): String {
        require(wordCount >= 2) { "Passphrase must have at least 2 words" }

        // Simple word list for demo (in production, use EFF word list)
        val words = listOf(
            "apple", "banana", "cherry", "dragon", "elephant", "falcon",
            "giraffe", "harbor", "island", "jungle", "kangaroo", "lemon",
            "mountain", "nebula", "ocean", "penguin", "quasar", "rocket",
            "sunset", "thunder", "umbrella", "volcano", "whisper", "xylophone",
            "yellow", "zebra"
        )

        return List(wordCount) {
            words[secureRandom.nextInt(words.size)]
        }.joinToString(separator)
    }

    /**
     * Calculate password strength score (0-4).
     *
     * @param password Password to evaluate
     * @return Strength score
     */
    fun calculateStrength(password: String): PasswordStrength {
        var score = 0

        // Length check
        when {
            password.length >= 16 -> score += 2
            password.length >= 12 -> score += 1
        }

        // Character diversity
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        // Complexity bonus
        if (password.length >= 20) score++

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            score <= 6 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}

/**
 * Password strength levels.
 */
enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}
