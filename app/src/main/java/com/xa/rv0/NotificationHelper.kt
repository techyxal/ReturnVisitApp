package com.xa.rv0

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat // Import ContextCompat

object NotificationHelper {

    private const val CHANNEL_ID = "contact_reminder_channel"
    private const val CHANNEL_NAME = "Contact Reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for contacting people."

    /**
     * Creates a notification channel for Android O (API 26) and above.
     * This must be called before displaying any notifications on API 26+.
     * @param context The application context.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a notification.
     * @param context The application context.
     * @param contactId The ID of the contact, used to make the notification unique.
     * @param title The title of the notification.
     * @param message The main text content of the notification.
     */
    fun showNotification(context: Context, contactId: Int, title: String, message: String) {
        // Check for POST_NOTIFICATIONS permission on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted. Do not show notification.
                // In a real app, you might want to log this or inform the user.
                return
            }
        }

        // Intent to open ContactDetailActivity when notification is tapped
        val intent = Intent(context, ContactDetailActivity::class.java).apply {
            // Pass contact ID so ContactDetailActivity can load the correct contact
            putExtra("contact_id_for_notification", contactId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            contactId, // Use contactId as request code for uniqueness
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE is required for Android S+
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // Now this icon should be found
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent to launch when notification is clicked
            .setAutoCancel(true) // Automatically removes the notification when the user taps it

        with(NotificationManagerCompat.from(context)) {
            notify(contactId, builder.build()) // Use contactId as notification ID for uniqueness
        }
    }
}
