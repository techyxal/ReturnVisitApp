package com.xa.rv0

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.xa.rv0.databinding.ActivityContactDetailBinding
import com.xa.rv0.model.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailBinding
    private var contact: Contact? = null
    // FIXED: Changed dateFormatter to use 'hh:mm a' for AM/PM format
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val TAG = "ContactDetailActivity"
        const val REQUEST_CODE_EDIT_CONTACT = 100
    }

    private val requestCallPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                performCall()
            } else {
                Snackbar.make(binding.root, "Call permission denied.", Snackbar.LENGTH_SHORT).show()
                Log.w(TAG, "CALL_PHONE permission denied.")
            }
        }

    private val editContactLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Contact updated. Consider refreshing details.")
            Snackbar.make(binding.root, "Contact updated successfully.", Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_contact_details)

        contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("contact", Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("contact")
        }

        contact?.let {
            binding.contact = it
            binding.tvDetailName.text = it.name
            binding.tvDetailPhone.text = it.phoneNumber
            binding.tvDetailAddress.text = it.address.ifBlank { getString(R.string.no_address_set) }
            binding.tvDetailCoordinates.text = getString(R.string.coordinates_format, it.latitude, it.longitude)
            binding.tvDetailSubject.text = it.subject.ifBlank { getString(R.string.no_subject_set) }
            binding.tvDetailCallbackDays.text = it.callbackDays.ifBlank { getString(R.string.no_callback_days_set) }

            if (it.callbackTime.isNotBlank()) {
                try {
                    val parsedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.callbackTime)
                    parsedTime?.let { time ->
                        binding.tvDetailCallbackTime.text = timeFormatter.format(time)
                    } ?: run {
                        binding.tvDetailCallbackTime.text = it.callbackTime // Fallback
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing callback time: ${it.callbackTime}", e)
                    binding.tvDetailCallbackTime.text = it.callbackTime // Fallback
                }
            } else {
                binding.tvDetailCallbackTime.text = getString(R.string.no_callback_time_set)
            }

            // THIS IS THE CRUCIAL LINE FOR TIMESTAMP DISPLAY
            binding.tvTimestamp.text = getString(
                R.string.contact_added_timestamp_format,
                dateFormatter.format(Date(it.creationTimestamp))
            )

            setupClickListeners(it)
        } ?: run {
            Snackbar.make(binding.root, "Error: Contact details not found.", Snackbar.LENGTH_LONG).show()
            Log.e(TAG, "No contact object passed to ContactDetailActivity.")
            finish()
        }
    }

    private fun setupClickListeners(contact: Contact) {
        binding.btnCall.setOnClickListener {
            checkCallPermissionAndCall(contact.phoneNumber)
        }

        binding.btnMessage.setOnClickListener {
            sendMessage(contact.phoneNumber)
        }

        binding.btnViewOnMap.setOnClickListener {
            viewOnMap(contact.latitude, contact.longitude, contact.name)
        }

        binding.btnEditContact.setOnClickListener {
            editContact(contact)
        }
    }

    private fun checkCallPermissionAndCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            performCall()
        } else {
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun performCall() {
        val phoneNumber = contact?.phoneNumber ?: return
        if (phoneNumber.isBlank()) {
            Snackbar.make(binding.root, "Phone number is empty.", Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Snackbar.make(binding.root, "No app found to make calls.", Snackbar.LENGTH_SHORT).show()
                Log.e(TAG, "No app found to handle ACTION_CALL for $phoneNumber")
            }
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, "Permission needed to make calls.", Snackbar.LENGTH_LONG).show()
            Log.e(TAG, "SecurityException: CALL_PHONE permission not granted or missing.", e)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error initiating call: ${e.message}", Snackbar.LENGTH_LONG).show()
            Log.e(TAG, "Error initiating call for $phoneNumber", e)
        }
    }

    private fun sendMessage(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Snackbar.make(binding.root, "Phone number is empty.", Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Snackbar.make(binding.root, "No app found to send messages.", Snackbar.LENGTH_SHORT).show()
            Log.e(TAG, "No app found to handle ACTION_VIEW for SMS to $phoneNumber")
        }
    }

    private fun viewOnMap(latitude: Double, longitude: Double, contactName: String?) {
        if (latitude == 0.0 && longitude == 0.0) {
            Snackbar.make(binding.root, "No valid coordinates for this contact.", Snackbar.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${contactName ?: ""})")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            mapIntent.setPackage(null)
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Snackbar.make(binding.root, "No map application found.", Snackbar.LENGTH_SHORT).show()
                Log.e(TAG, "No map application found to view coordinates: $latitude, $longitude")
            }
        }
    }

    private fun editContact(contact: Contact) {
        val intent = Intent(this, AddEditContactActivity::class.java).apply {
            putExtra("contact", contact)
        }
        editContactLauncher.launch(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
    