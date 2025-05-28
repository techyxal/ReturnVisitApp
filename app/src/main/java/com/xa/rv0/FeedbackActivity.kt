package com.xa.rv0

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xa.rv0.databinding.ActivityFeedbackBinding
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button
        supportActionBar?.title = getString(R.string.title_feedback)

        // Set listener for GitHub feedback button
        binding.btnSendFeedbackGithub.setOnClickListener { // Renamed ID
            sendFeedbackToGitHub()
        }

        // NEW: Set listener for Email feedback button
        binding.btnSendFeedbackEmail.setOnClickListener {
            sendFeedbackEmail()
        }
    }

    private fun sendFeedbackToGitHub() {
        val subjectInput = binding.etFeedbackSubject.text.toString().trim()
        val messageInput = binding.etFeedbackMessage.text.toString().trim()

        if (subjectInput.isEmpty() || messageInput.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_feedback_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val feedbackType = when (binding.rgFeedbackType.checkedRadioButtonId) {
            R.id.rb_bug_report -> getString(R.string.feedback_type_bug_label)
            R.id.rb_feature_request -> getString(R.string.feedback_type_feature_label)
            else -> "" // Should not happen with a default checked button
        }

        val issueTitle = if (feedbackType.isNotBlank()) {
            "[$feedbackType] $subjectInput"
        } else {
            subjectInput
        }

        // Add app version and device info to the message body
        val issueBody = "$messageInput\n\n---\n" +
                "App Version: ${getString(R.string.app_version)}\n" +
                "Device: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})"

        val githubRepoUrl = getString(R.string.github_issues_url)

        val encodedTitle = URLEncoder.encode(issueTitle, StandardCharsets.UTF_8.toString())
        val encodedBody = URLEncoder.encode(issueBody, StandardCharsets.UTF_8.toString())

        val githubIssueUrl = Uri.parse("$githubRepoUrl?title=$encodedTitle&body=$encodedBody")

        val webIntent = Intent(Intent.ACTION_VIEW, githubIssueUrl)

        if (webIntent.resolveActivity(packageManager) != null) {
            startActivity(webIntent)
            finish() // Close FeedbackActivity after opening browser
        } else {
            Toast.makeText(this, getString(R.string.toast_no_browser_found), Toast.LENGTH_LONG).show()
        }
    }

    // NEW: Function to send feedback via email
    private fun sendFeedbackEmail() {
        val subject = binding.etFeedbackSubject.text.toString().trim()
        val message = binding.etFeedbackMessage.text.toString().trim()

        if (subject.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_feedback_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val recipientEmail = getString(R.string.feedback_email_address) // Get from strings.xml

        // Add app version and device info to the email body
        val emailBody = "$message\n\n---\n" +
                "App Version: ${getString(R.string.app_version)}\n" +
                "Device: ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})"

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(emailIntent)
            finish() // Close FeedbackActivity after sending intent
        } else {
            Toast.makeText(this, getString(R.string.toast_no_email_client), Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
