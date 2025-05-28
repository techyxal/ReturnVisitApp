package com.xa.rv0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope // Not directly usable in BroadcastReceiver, but useful for context
import com.xa.rv0.data.ContactDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMIND_CONTACT = "com.xa.rv0.ACTION_REMIND_CONTACT"
        const val EXTRA_CONTACT_ID = "extra_contact_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REMIND_CONTACT) {
            val contactId = intent.getIntExtra(EXTRA_CONTACT_ID, -1)
            if (contactId != -1) {
                Log.d("ReminderReceiver", "Received reminder for contact ID: $contactId")
                // Fetch contact details from the database and show notification
                // Note: BroadcastReceiver has a limited lifespan. Use a CoroutineScope that
                // does not outlive the receiver's onReceive method, or a WorkManager for longer tasks.
                // For a quick DB fetch and notification, a simple coroutine is often fine.

                // Using a new coroutine scope for the database operation
                // This is a simplified approach for a BroadcastReceiver.
                // For more complex background tasks, consider WorkManager.
                @Suppress("DEPRECATION") // Suppress for globalScope, consider more structured approach for production
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    val contactDao = ContactDatabase.getDatabase(context).contactDao()
                    val contact = contactDao.getContactById(contactId)
                    withContext(Dispatchers.Main) {
                        contact?.let {
                            val title = "Contact Reminder: ${it.name}"
                            val message = "It's time to follow up with ${it.name} regarding: ${it.subject.ifBlank { "No specific subject" }}"
                            NotificationHelper.showNotification(context, it.id, title, message)
                            Log.i("ReminderReceiver", "Notification shown for ${it.name}")
                        } ?: run {
                            Log.w("ReminderReceiver", "Contact with ID $contactId not found for reminder.")
                        }
                    }
                }
            } else {
                Log.e("ReminderReceiver", "Invalid contact ID received for reminder.")
            }
        }
    }
}
