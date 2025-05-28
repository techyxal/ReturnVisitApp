package com.xa.rv0

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xa.rv0.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create notification channel when the app starts
        NotificationHelper.createNotificationChannel(this)

        // Add Contact Button
        binding.btnAddContact.setOnClickListener {
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

        // NEW: Feedback Button
        binding.btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        // About Button
        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
