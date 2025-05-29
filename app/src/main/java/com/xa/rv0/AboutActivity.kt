package com.xa.rv0

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.xa.rv0.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button
        supportActionBar?.title = getString(R.string.title_about)

        // Set app name and version
        binding.tvAppNameAbout.text = getString(R.string.app_name)
        binding.tvAppVersion.text = getString(R.string.app_version)
        binding.tvAppDescription.text = getString(R.string.app_description)

        // Privacy Policy Button - NOW SHOWS POPUP
        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicyPopup()
        }

        // Feedback Button
        binding.btnFeedbackAbout.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }

    // NEW: Function to display the Privacy Policy popup
    private fun showPrivacyPolicyPopup() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.privacy_policy)) // Use the privacy policy title
            .setMessage(getString(R.string.privacy_policy_message)) // Your specific message
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
