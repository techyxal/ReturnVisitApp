package com.xa.rv0

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
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
import com.xa.rv0.viewmodel.Contact
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import android.os.Build
import com.xa.rv0.viewmodel.AddEditContactViewModel
import java.util.Calendar
import java.util.Locale

class AddEditContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditContactBinding
    private lateinit var viewModel: AddEditContactViewModel
    private var contact: Contact? = null

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1001
    }

    private val requestPermissionLauncher =
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
            binding.etCallbackTime.setText(c.callbackTime)
            binding.btnSave.text = "Update"
            // NEW: Set the state of the reminders switch
            binding.switchRemindersEnabled.isChecked = c.remindersEnabled
        }

        viewModel.currentLatitude.observe(this) { lat ->
            binding.tvCoordinates.text = getString(R.string.coordinates_format, lat, viewModel.currentLongitude.value ?: 0.0)
        }
        viewModel.currentLongitude.observe(this) { lon ->
            binding.tvCoordinates.text = getString(R.string.coordinates_format, viewModel.currentLatitude.value ?: 0.0, lon)
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
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                binding.etCallbackTime.setText(selectedTime)
            },
            hour,
            minute,
            true
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
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
        val remindersEnabled = binding.switchRemindersEnabled.isChecked // NEW: Get switch state

        if (name.isEmpty()) {
            Snackbar.make(binding.root, "Name and Phone Number are required.", Snackbar.LENGTH_SHORT).show()
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
            remindersEnabled // NEW: Pass switch state
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
