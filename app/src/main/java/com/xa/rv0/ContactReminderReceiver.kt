package com.xa.rv0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xa.rv0.data.ContactDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

                val pendingResult: BroadcastReceiver.PendingResult = goAsync()
                val receiverScope = CoroutineScope(Dispatchers.IO + Job())

                receiverScope.launch {
                    try {
                        val contactDao = ContactDatabase.getDatabase(context).contactDao()
                        val contact = contactDao.getContactById(contactId)
                        withContext(Dispatchers.Main) {
                            contact?.let {
                                // NEW: Check if reminders are enabled for this contact
                                if (it.remindersEnabled) {
                                    val title = "Contact Reminder: ${it.name}"
                                    val message = "It's time to follow up with ${it.name} regarding: ${it.subject.ifBlank { "No specific subject" }}"
                                    NotificationHelper.showNotification(context, it.id, title, message)
                                    Log.i("ReminderReceiver", "Notification shown for ${it.name}")
                                } else {
                                    Log.i("ReminderReceiver", "Reminders disabled for ${it.name}. Notification skipped.")
                                }
                            } ?: run {
                                Log.w("ReminderReceiver", "Contact with ID $contactId not found for reminder.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderReceiver", "Error fetching contact or showing notification: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                        Log.d("ReminderReceiver", "PendingResult finished for contact ID: $contactId")
                    }
                }
            } else {
                Log.e("ReminderReceiver", "Invalid contact ID received for reminder.")
            }
        }
    }
}
