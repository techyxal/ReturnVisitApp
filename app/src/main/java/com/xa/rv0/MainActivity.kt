package com.xa.rv0

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Import for MaterialAlertDialogBuilder
import com.xa.rv0.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "MyReturnVisitDiaryPrefs"
    private val KEY_TERMS_ACCEPTED = "terms_accepted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create notification channel when the app starts
        NotificationHelper.createNotificationChannel(this)

        // Check if terms and agreement have been accepted
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val termsAccepted = sharedPrefs.getBoolean(KEY_TERMS_ACCEPTED, false)

        if (!termsAccepted) {
            showTermsAndAgreementDialog()
        }

        // Floating Action Button for Add Contact
        binding.fabAddContact.setOnClickListener {
            val intent = Intent(this, AddEditContactActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
        }

        // View Contacts Button
        binding.btnViewContacts.setOnClickListener {
            startActivity(Intent(this, ViewContactsActivity::class.java))
        }

        // Backup/Restore Button
        binding.btnBackupRestore.setOnClickListener {
            startActivity(Intent(this, BackupRestoreActivity::class.java))
        }

        // About Button
        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun showTermsAndAgreementDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.terms_agreement_title))
            .setMessage(getString(R.string.terms_agreement_content))
            .setPositiveButton(getString(R.string.button_accept)) { dialog, _ ->
                // User accepted, save the preference
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_TERMS_ACCEPTED, true)
                    .apply()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.button_decline)) { dialog, _ ->
                // User declined, close the app
                dialog.dismiss()
                finish() // Close the MainActivity
            }
            .setCancelable(false) // Prevent dialog from being dismissed by back button or outside touch
            .show()
    }
}
