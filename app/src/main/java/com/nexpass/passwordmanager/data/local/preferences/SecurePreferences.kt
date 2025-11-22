package com.nexpass.passwordmanager.data.local.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences wrapper for storing sensitive metadata.
 *
 * Uses AndroidX Security library for encryption.
 * Stores:
 * - Last sync timestamp
 * - Server URL
 * - Username
 * - Vault state
 */
class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "nexpass_secure_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_VAULT_UNLOCKED = "vault_unlocked"
        private const val KEY_MASTER_SALT = "master_password_salt"
        private const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_VAULT_INITIALIZED = "vault_initialized"

        // Nextcloud sync configuration
        private const val KEY_NEXTCLOUD_SERVER_URL = "nextcloud_server_url"
        private const val KEY_NEXTCLOUD_USERNAME = "nextcloud_username"
        private const val KEY_NEXTCLOUD_APP_PASSWORD = "nextcloud_app_password"
        private const val KEY_SYNC_ENABLED = "sync_enabled"

        // App preferences
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"

        // Autosave preferences
        private const val KEY_AUTOSAVE_ENABLED = "autosave_enabled"
        private const val KEY_AUTOSAVE_DEFAULT_FOLDER = "autosave_default_folder"
        private const val KEY_NEVER_SAVE_DOMAINS = "never_save_domains"
    }

    fun getLastSyncTimestamp(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC, 0L)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun getServerUrl(): String? {
        return sharedPreferences.getString(KEY_SERVER_URL, null)
    }

    fun setServerUrl(url: String) {
        sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun setUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun isVaultUnlocked(): Boolean {
        return sharedPreferences.getBoolean(KEY_VAULT_UNLOCKED, false)
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VAULT_UNLOCKED, unlocked).apply()
    }

    fun getMasterSalt(): String? {
        return sharedPreferences.getString(KEY_MASTER_SALT, null)
    }

    fun setMasterSalt(salt: String) {
        sharedPreferences.edit().putString(KEY_MASTER_SALT, salt).apply()
    }

    fun getEncryptedVaultKey(): String? {
        return sharedPreferences.getString(KEY_ENCRYPTED_VAULT_KEY, null)
    }

    fun setEncryptedVaultKey(encryptedKey: String) {
        sharedPreferences.edit().putString(KEY_ENCRYPTED_VAULT_KEY, encryptedKey).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isVaultInitialized(): Boolean {
        return sharedPreferences.getBoolean(KEY_VAULT_INITIALIZED, false)
    }

    fun setVaultInitialized(initialized: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_VAULT_INITIALIZED, initialized).apply()
    }

    // Nextcloud configuration methods

    fun getNextcloudServerUrl(): String? {
        return sharedPreferences.getString(KEY_NEXTCLOUD_SERVER_URL, null)
    }

    fun setNextcloudServerUrl(url: String) {
        sharedPreferences.edit().putString(KEY_NEXTCLOUD_SERVER_URL, url).apply()
    }

    fun getNextcloudUsername(): String? {
        return sharedPreferences.getString(KEY_NEXTCLOUD_USERNAME, null)
    }

    fun setNextcloudUsername(username: String) {
        sharedPreferences.edit().putString(KEY_NEXTCLOUD_USERNAME, username).apply()
    }

    fun getNextcloudAppPassword(): String? {
        return sharedPreferences.getString(KEY_NEXTCLOUD_APP_PASSWORD, null)
    }

    fun setNextcloudAppPassword(password: String) {
        sharedPreferences.edit().putString(KEY_NEXTCLOUD_APP_PASSWORD, password).apply()
    }

    fun isSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }

    fun isNextcloudConfigured(): Boolean {
        return !getNextcloudServerUrl().isNullOrEmpty() &&
               !getNextcloudUsername().isNullOrEmpty() &&
               !getNextcloudAppPassword().isNullOrEmpty()
    }

    fun clearNextcloudConfig() {
        sharedPreferences.edit()
            .remove(KEY_NEXTCLOUD_SERVER_URL)
            .remove(KEY_NEXTCLOUD_USERNAME)
            .remove(KEY_NEXTCLOUD_APP_PASSWORD)
            .remove(KEY_SYNC_ENABLED)
            .remove(KEY_LAST_SYNC)
            .apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    // Theme and UI preferences

    /**
     * Get theme mode preference.
     * @return "LIGHT", "DARK", or "SYSTEM" (default: "SYSTEM")
     */
    fun getThemeMode(): String {
        return sharedPreferences.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    /**
     * Set theme mode preference.
     * @param mode "LIGHT", "DARK", or "SYSTEM"
     */
    fun setThemeMode(mode: String) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    /**
     * Get auto-lock timeout in minutes.
     * @return Timeout in minutes (default: 5, -1 for never)
     */
    fun getAutoLockTimeout(): Int {
        return sharedPreferences.getInt(KEY_AUTO_LOCK_TIMEOUT, 5)
    }

    /**
     * Set auto-lock timeout in minutes.
     * @param minutes Timeout in minutes (-1 for never)
     */
    fun setAutoLockTimeout(minutes: Int) {
        sharedPreferences.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, minutes).apply()
    }

    // Autosave preferences

    /**
     * Check if autosave is enabled.
     * @return True if autosave is enabled (default: true)
     */
    fun isAutosaveEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTOSAVE_ENABLED, true)
    }

    /**
     * Set autosave enabled state.
     * @param enabled True to enable autosave, false to disable
     */
    fun setAutosaveEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTOSAVE_ENABLED, enabled).apply()
    }

    /**
     * Get default folder ID for autosaved passwords.
     * @return Folder ID or null for no default folder
     */
    fun getAutosaveDefaultFolder(): String? {
        return sharedPreferences.getString(KEY_AUTOSAVE_DEFAULT_FOLDER, null)
    }

    /**
     * Set default folder for autosaved passwords.
     * @param folderId Folder ID or null for no default folder
     */
    fun setAutosaveDefaultFolder(folderId: String?) {
        if (folderId == null) {
            sharedPreferences.edit().remove(KEY_AUTOSAVE_DEFAULT_FOLDER).apply()
        } else {
            sharedPreferences.edit().putString(KEY_AUTOSAVE_DEFAULT_FOLDER, folderId).apply()
        }
    }

    /**
     * Get the set of domains/packages that should never be saved.
     * @return Set of domains/packages to never save
     */
    fun getNeverSaveDomains(): Set<String> {
        val serialized = sharedPreferences.getString(KEY_NEVER_SAVE_DOMAINS, null)
        return if (serialized != null && serialized.isNotEmpty()) {
            serialized.split(",").toSet()
        } else {
            emptySet()
        }
    }

    /**
     * Add a domain/package to the never-save list.
     * @param domain Domain or package name to never save
     */
    fun addNeverSaveDomain(domain: String) {
        val current = getNeverSaveDomains().toMutableSet()
        current.add(domain)
        sharedPreferences.edit()
            .putString(KEY_NEVER_SAVE_DOMAINS, current.joinToString(","))
            .apply()
    }

    /**
     * Clear the never-save domains list.
     */
    fun clearNeverSaveDomains() {
        sharedPreferences.edit().remove(KEY_NEVER_SAVE_DOMAINS).apply()
    }
}
