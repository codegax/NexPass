package com.nexpass.passwordmanager.autofill.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nexpass.passwordmanager.R

/**
 * Manages notifications for autosave feature.
 * Shows notifications prompting users to save passwords.
 */
class AutosaveNotificationManager(private val context: Context) {

    companion object {
        private const val TAG = "AutosaveNotificationMgr"
        private const val CHANNEL_ID = "autosave_channel"
        private const val CHANNEL_NAME = "Password Save Requests"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_WEB_DOMAIN = "webDomain"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationManagerCompat = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel for autosave (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications asking if you want to save passwords to NexPass"
                setShowBadge(true)
                enableLights(true)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a notification prompting to save password.
     *
     * @param packageName The package name of the app with the login form
     * @param webDomain The web domain (if browser), null otherwise
     */
    fun showSavePasswordNotification(packageName: String, webDomain: String?) {
        Log.d(TAG, "=== showSavePasswordNotification called ===")
        Log.d(TAG, "Package: $packageName, Domain: $webDomain")

        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w(TAG, "⚠️ POST_NOTIFICATIONS permission NOT GRANTED (Android 13+)")
                Log.w(TAG, "User needs to grant notification permission in app settings")
                return
            }
            Log.d(TAG, "✅ POST_NOTIFICATIONS permission granted (Android 13+)")
        }

        // Check if notifications are enabled (for older Android versions)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            Log.w(TAG, "⚠️ Notifications are DISABLED for NexPass!")
            Log.w(TAG, "User needs to enable notifications in Settings > Apps > NexPass > Notifications")
            return
        }

        Log.d(TAG, "✅ Notifications are enabled")

        val displayName = webDomain ?: packageName
        Log.d(TAG, "Display name: $displayName")

        // Create intent to launch password input activity when notification is tapped
        val intent = Intent(context, com.nexpass.passwordmanager.autofill.ui.NotificationPasswordInputActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_WEB_DOMAIN, webDomain)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        Log.d(TAG, "Building notification...")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Save password to NexPass?")
            .setContentText("Tap to save password for $displayName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true) // Dismiss when tapped
            .setContentIntent(pendingIntent)
            .build()

        try {
            Log.d(TAG, "Posting notification with ID: $NOTIFICATION_ID")
            notificationManagerCompat.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "✅ Notification posted successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to post notification", e)
        }
    }

    /**
     * Cancel the save password notification.
     */
    fun cancelSavePasswordNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
