package com.nexpass.passwordmanager.security.encryption

/**
 * Memory security utilities for handling sensitive data
 *
 * CRITICAL SECURITY: This object provides utilities to securely handle
 * sensitive data in memory, preventing it from remaining accessible after use.
 *
 * Best Practices:
 * - Always wipe sensitive data as soon as you're done with it
 * - Use try-finally blocks to ensure wiping happens even on exceptions
 * - Never pass sensitive data to logging or error messages
 * - Minimize the time sensitive data exists in memory
 */
object MemoryUtils {

    /**
     * Wipe a ByteArray by overwriting it with zeros
     *
     * @param array The array to wipe (modified in place)
     */
    fun wipeByteArray(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Wipe a CharArray by overwriting it with null characters
     *
     * @param array The array to wipe (modified in place)
     */
    fun wipeCharArray(array: CharArray) {
        array.fill('\u0000')
    }

    /**
     * Wipe multiple ByteArrays at once
     *
     * @param arrays Variable number of arrays to wipe
     */
    fun wipeByteArrays(vararg arrays: ByteArray) {
        arrays.forEach { it.fill(0) }
    }

    /**
     * Execute a block with a ByteArray and ensure it's wiped afterwards
     *
     * Example usage:
     * ```
     * val result = MemoryUtils.useByteArray(sensitiveData) { data ->
     *     // Use data here
     *     processData(data)
     * }
     * // data is automatically wiped after block executes
     * ```
     *
     * @param array The array to use
     * @param block The block to execute with the array
     * @return The result of the block
     */
    inline fun <T> useByteArray(array: ByteArray, block: (ByteArray) -> T): T {
        try {
            return block(array)
        } finally {
            wipeByteArray(array)
        }
    }

    /**
     * Execute a block with a CharArray and ensure it's wiped afterwards
     *
     * @param array The array to use
     * @param block The block to execute with the array
     * @return The result of the block
     */
    inline fun <T> useCharArray(array: CharArray, block: (CharArray) -> T): T {
        try {
            return block(array)
        } finally {
            wipeCharArray(array)
        }
    }

    /**
     * Convert a String to ByteArray, execute a block, then wipe both arrays
     *
     * IMPORTANT: This creates a ByteArray from the String and wipes it after use,
     * but the original String object cannot be wiped (JVM limitation).
     * Avoid keeping sensitive Strings in memory when possible.
     *
     * @param string The string to convert and use
     * @param block The block to execute with the ByteArray
     * @return The result of the block
     */
    inline fun <T> useStringAsBytes(string: String, block: (ByteArray) -> T): T {
        val bytes = string.toByteArray()
        return useByteArray(bytes, block)
    }

    /**
     * Check if a ByteArray has been wiped (all zeros)
     *
     * Useful for testing and verification.
     *
     * @param array The array to check
     * @return true if all bytes are zero, false otherwise
     */
    fun isWiped(array: ByteArray): Boolean {
        return array.all { it == 0.toByte() }
    }

    /**
     * Check if a CharArray has been wiped (all null characters)
     *
     * @param array The array to check
     * @return true if all chars are null, false otherwise
     */
    fun isWiped(array: CharArray): Boolean {
        return array.all { it == '\u0000' }
    }
}
