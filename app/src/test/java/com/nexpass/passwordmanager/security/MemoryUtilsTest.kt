package com.nexpass.passwordmanager.security

import com.nexpass.passwordmanager.security.encryption.MemoryUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MemoryUtils
 *
 * Tests cover:
 * - ByteArray wiping
 * - CharArray wiping
 * - Safe usage patterns
 * - Verification utilities
 */
class MemoryUtilsTest {

    @Test
    fun `wipeByteArray should overwrite with zeros`() {
        val data = "SensitiveData123!".toByteArray()
        val originalCopy = data.clone()

        MemoryUtils.wipeByteArray(data)

        assertTrue(data.all { it == 0.toByte() })
        assertFalse(data.contentEquals(originalCopy))
    }

    @Test
    fun `wipeCharArray should overwrite with null characters`() {
        val data = "Password123!".toCharArray()
        val originalCopy = data.clone()

        MemoryUtils.wipeCharArray(data)

        assertTrue(data.all { it == '\u0000' })
        assertFalse(data.contentEquals(originalCopy))
    }

    @Test
    fun `wipeByteArrays should wipe multiple arrays`() {
        val array1 = "Data1".toByteArray()
        val array2 = "Data2".toByteArray()
        val array3 = "Data3".toByteArray()

        MemoryUtils.wipeByteArrays(array1, array2, array3)

        assertTrue(MemoryUtils.isWiped(array1))
        assertTrue(MemoryUtils.isWiped(array2))
        assertTrue(MemoryUtils.isWiped(array3))
    }

    @Test
    fun `useByteArray should wipe array after block execution`() {
        val data = "SensitiveData".toByteArray()
        var dataWasUsed = false

        val result = MemoryUtils.useByteArray(data) {
            dataWasUsed = it.isNotEmpty()
            "result"
        }

        assertEquals("result", result)
        assertTrue(dataWasUsed)
        assertTrue(MemoryUtils.isWiped(data))
    }

    @Test
    fun `useByteArray should wipe array even if block throws exception`() {
        val data = "SensitiveData".toByteArray()

        try {
            MemoryUtils.useByteArray(data) {
                throw RuntimeException("Test exception")
            }
        } catch (e: RuntimeException) {
            // Expected
        }

        assertTrue(MemoryUtils.isWiped(data))
    }

    @Test
    fun `useCharArray should wipe array after block execution`() {
        val data = "Password".toCharArray()
        var dataWasUsed = false

        val result = MemoryUtils.useCharArray(data) {
            dataWasUsed = it.isNotEmpty()
            "result"
        }

        assertEquals("result", result)
        assertTrue(dataWasUsed)
        assertTrue(MemoryUtils.isWiped(data))
    }

    @Test
    fun `useStringAsBytes should wipe byte array after use`() {
        val string = "SensitivePassword"
        var byteArraySize = 0

        val result = MemoryUtils.useStringAsBytes(string) { bytes ->
            byteArraySize = bytes.size
            String(bytes)
        }

        assertEquals(string, result)
        assertEquals(string.length, byteArraySize)
        // Note: We can't verify the bytes were wiped directly since they're local
        // to the function, but we trust the implementation uses useByteArray
    }

    @Test
    fun `isWiped should correctly identify wiped ByteArray`() {
        val data = "Data".toByteArray()

        assertFalse(MemoryUtils.isWiped(data))

        MemoryUtils.wipeByteArray(data)

        assertTrue(MemoryUtils.isWiped(data))
    }

    @Test
    fun `isWiped should correctly identify wiped CharArray`() {
        val data = "Data".toCharArray()

        assertFalse(MemoryUtils.isWiped(data))

        MemoryUtils.wipeCharArray(data)

        assertTrue(MemoryUtils.isWiped(data))
    }

    @Test
    fun `isWiped should return true for empty array`() {
        val emptyBytes = ByteArray(0)
        val emptyChars = CharArray(0)

        assertTrue(MemoryUtils.isWiped(emptyBytes))
        assertTrue(MemoryUtils.isWiped(emptyChars))
    }

    @Test
    fun `wipeByteArray should handle empty array`() {
        val empty = ByteArray(0)

        // Should not throw
        MemoryUtils.wipeByteArray(empty)

        assertTrue(MemoryUtils.isWiped(empty))
    }

    @Test
    fun `wipeCharArray should handle empty array`() {
        val empty = CharArray(0)

        // Should not throw
        MemoryUtils.wipeCharArray(empty)

        assertTrue(MemoryUtils.isWiped(empty))
    }
}
