package com.nexpass.passwordmanager.security.encryption

/**
 * Data class representing encrypted data with authentication
 *
 * This class encapsulates the result of AES-GCM encryption:
 * - Ciphertext: The encrypted data
 * - IV: Initialization Vector (unique per encryption)
 * - Auth Tag: Authentication tag for GCM mode (ensures integrity)
 *
 * @property ciphertext The encrypted data bytes
 * @property iv The initialization vector used for encryption (12 bytes for GCM)
 * @property authTag The authentication tag for integrity verification (16 bytes for GCM)
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val authTag: ByteArray
) {
    /**
     * Equals override to properly compare ByteArray contents
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!authTag.contentEquals(other.authTag)) return false

        return true
    }

    /**
     * HashCode override to properly hash ByteArray contents
     */
    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + authTag.contentHashCode()
        return result
    }

    /**
     * Combined byte array for storage (IV + Auth Tag + Ciphertext)
     * Format: [IV(12 bytes)][Auth Tag(16 bytes)][Ciphertext(variable)]
     */
    fun toByteArray(): ByteArray {
        return iv + authTag + ciphertext
    }

    companion object {
        const val IV_LENGTH = 12 // 96 bits for AES-GCM
        const val AUTH_TAG_LENGTH = 16 // 128 bits for AES-GCM

        /**
         * Parse encrypted data from combined byte array
         *
         * @param data Combined byte array in format: [IV][AuthTag][Ciphertext]
         * @return EncryptedData instance
         * @throws IllegalArgumentException if data is too short
         */
        fun fromByteArray(data: ByteArray): EncryptedData {
            require(data.size >= IV_LENGTH + AUTH_TAG_LENGTH) {
                "Data too short: expected at least ${IV_LENGTH + AUTH_TAG_LENGTH} bytes, got ${data.size}"
            }

            val iv = data.copyOfRange(0, IV_LENGTH)
            val authTag = data.copyOfRange(IV_LENGTH, IV_LENGTH + AUTH_TAG_LENGTH)
            val ciphertext = data.copyOfRange(IV_LENGTH + AUTH_TAG_LENGTH, data.size)

            return EncryptedData(ciphertext, iv, authTag)
        }
    }
}
