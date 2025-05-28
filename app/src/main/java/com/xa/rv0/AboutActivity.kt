package com.xa.rv0

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xa.rv0.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the action bar/toolbar for the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_about)

        // Set app version dynamically (if you want to get it from BuildConfig)
        // For simplicity, we'll use a string resource directly as requested.
        // If you want to get it programmatically:
        // binding.tvAboutVersion.text = getString(R.string.app_version_format, BuildConfig.VERSION_NAME)

        // Optional: Set up privacy policy button click listener
        binding.btnPrivacyPolicy.setOnClickListener {
            val privacyPolicyUrl = "https://www.example.com/privacy" // Replace with your actual URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Handle case where no browser is available
                // You might show a Snackbar or Toast here
            }
        }
    }

    // Handle the back button in the action bar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun finish() {
        super.finish()
        // Apply custom exit animation when finishing this activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,  // Animation for the activity that is appearing (MainActivity)
                R.anim.slide_out_right // Animation for this activity (AboutActivity) that is disappearing
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
