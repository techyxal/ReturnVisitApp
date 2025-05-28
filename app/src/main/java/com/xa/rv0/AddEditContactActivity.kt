package com.xa.rv0

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.xa.rv0.databinding.ActivityAddEditContactBinding
import com.xa.rv0.model.Contact
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import android.os.Build
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import com.xa.rv0.model.AddEditContactViewModel
import java.text.SimpleDateFormat // Import SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEditContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditContactBinding
    private lateinit var viewModel: AddEditContactViewModel
    private var contact: Contact? = null

    // NEW: Define a SimpleDateFormat for 12-hour format (hh:mm a)
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val TAG = "AddEditContactActivity"
        private const val REQUEST_CHECK_SETTINGS = 1001
    }

    private val requestCallPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Handle denied permission if needed, e.g., for CALL_PHONE
            }
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.startLocationUpdates()
            } else {
                Snackbar.make(
                    binding.root,
                    "Location permission is required to get coordinates.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

    private val resolutionForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            viewModel.startLocationUpdates()
        } else {
            Snackbar.make(
                binding.root,
                "Location settings not enabled. Cannot get current coordinates.",
                Snackbar.LENGTH_LONG
            ).show()
            viewModel.stopLocationUpdates()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flag)

            viewModel.setSelectedImageUri(uri)
            binding.ivContactImage.setImageURI(uri)
        } else {
            Snackbar.make(binding.root, "No image selected.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AddEditContactViewModel::class.java)

        val daysOfWeek = resources.getStringArray(R.array.days_of_week)
        val daysAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, daysOfWeek)
        binding.actCallbackDays.setAdapter(daysAdapter)

        binding.etCallbackTime.setOnClickListener {
            showTimePickerDialog()
        }
        binding.etCallbackTime.isFocusable = false
        binding.etCallbackTime.isClickable = true

        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("contact", Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("contact")
        }
        contact?.let { c ->
            binding.etName.setText(c.name)
            binding.etPhone.setText(c.phoneNumber)
            binding.etAddress.setText(c.address)
            viewModel.setInitialCoordinates(c.latitude, c.longitude)
            binding.tvCoordinates.text = getString(R.string.coordinates_format, c.latitude, c.longitude)
            binding.etSubject.setText(c.subject)
            binding.actCallbackDays.setText(c.callbackDays, false)
            binding.etCallbackTime.setText(c.callbackTime) // Display existing time as is
            binding.btnSave.text = "Update"

            c.imageUri?.let { uriString ->
                try {
                    val imageUri = Uri.parse(uriString)
                    viewModel.setSelectedImageUri(imageUri)
                    binding.ivContactImage.setImageURI(imageUri)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing image URI: $uriString", e)
                    binding.ivContactImage.setImageResource(R.drawable.ic_contact_avatar_placeholder)
                }
            }
        }

        viewModel.currentLatitude.observe(this) { lat ->
            binding.tvCoordinates.text = getString(R.string.coordinates_format, lat, viewModel.currentLongitude.value ?: 0.0)
        }
        viewModel.currentLongitude.observe(this) { lon ->
            binding.tvCoordinates.text = getString(R.string.coordinates_format, viewModel.currentLatitude.value ?: 0.0, lon)
        }

        viewModel.selectedImageUri.observe(this) { uri ->
            uri?.let {
                binding.ivContactImage.setImageURI(it)
            } ?: binding.ivContactImage.setImageResource(R.drawable.ic_contact_avatar_placeholder)
        }

        viewModel.operationStatus.observe(this) { status ->
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            if (status.contains("Saved") || status.contains("Updated")) {
                finish()
            }
        }

        viewModel.locationError.observe(this) { error ->
            val messageToShow = if (error.isNullOrBlank()) {
                "An unknown location error occurred."
            } else {
                error
            }
            Snackbar.make(binding.root, messageToShow, Snackbar.LENGTH_LONG).show()
        }

        viewModel.isLocating.observe(this) { isLoading ->
            binding.progressBarLocation.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnGetLocation.isEnabled = !isLoading
        }

        viewModel.locationStatusMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.resolveLocationSettings.observe(this) { resolvableApiException ->
            try {
                val intentSenderRequest = IntentSenderRequest.Builder(resolvableApiException.resolution).build()
                resolutionForResult.launch(intentSenderRequest)
            } catch (sendEx: IntentSender.SendIntentException) {
                Snackbar.make(binding.root, "Error showing location settings dialog.", Snackbar.LENGTH_LONG).show()
            }
        }

        binding.btnGetLocation.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }

        binding.btnSave.setOnClickListener {
            saveOrUpdateContact()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLocationUpdates()
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // NEW: Format selected time to 12-hour format with AM/PM
                val selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }.time // Get Date object from Calendar
                binding.etCallbackTime.setText(timeFormatter.format(selectedTime)) // Format and set text
            },
            hour,
            minute,
            false // CHANGED: Set to false for 12-hour format
        )
        timePickerDialog.show()
    }

    private fun checkLocationPermissionAndGetLocation() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startLocationUpdates()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    "This app needs location access to save contact coordinates.",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Grant") {
                        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    .show()
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun saveOrUpdateContact() {
        val name = binding.etName.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val subject = binding.etSubject.text.toString().trim()
        val callbackDays = binding.actCallbackDays.text.toString().trim()
        val callbackTime = binding.etCallbackTime.text.toString().trim()

        val currentImageUri = viewModel.selectedImageUri.value?.toString()

        if (name.isEmpty()) {
            Snackbar.make(binding.root, "Name is required.", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewModel.saveOrUpdateContact(
            contact?.id ?: 0,
            name,
            phoneNumber,
            address,
            subject,
            callbackDays,
            callbackTime,
            currentImageUri
        )
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
