package com.xa.rv0.viewmodel

import android.app.Application
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.xa.rv0.data.ContactDatabase
import com.xa.rv0.viewmodel.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.AccessControlException

class AddEditContactViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(application)

    private val _currentLatitude = MutableLiveData<Double>()
    val currentLatitude: LiveData<Double> = _currentLatitude

    private val _currentLongitude = MutableLiveData<Double>()
    val currentLongitude: LiveData<Double> = _currentLongitude

    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String> = _operationStatus

    private val _locationError = MutableLiveData<String?>()
    val locationError: LiveData<String?> = _locationError

    private val _isLocating = MutableLiveData<Boolean>(false)
    val isLocating: LiveData<Boolean> = _isLocating

    private val _locationStatusMessage = MutableLiveData<String?>()
    val locationStatusMessage: LiveData<String?> = _locationStatusMessage

    private val _resolveLocationSettings = MutableLiveData<ResolvableApiException>()
    val resolveLocationSettings: LiveData<ResolvableApiException> = _resolveLocationSettings

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    init {
        createLocationRequest()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    _currentLatitude.value = location.latitude
                    _currentLongitude.value = location.longitude
                    _isLocating.value = false
                    stopLocationUpdates()
                    _locationError.postValue(null)
                    _locationStatusMessage.postValue("Location acquired!")
                    Log.d("AddEditContactVM", "Location received: ${location.latitude}, ${location.longitude}")
                } ?: run {
                    _locationError.postValue("Failed to get location from updates. Try again.")
                    _isLocating.value = false
                    _locationStatusMessage.postValue(null)
                    Log.e("AddEditContactVM", "LocationResult was null or contained no location.")
                }
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
        }
    }

    fun setInitialCoordinates(latitude: Double, longitude: Double) {
        _currentLatitude.value = latitude
        _currentLongitude.value = longitude
    }

    fun startLocationUpdates() {
        _isLocating.value = true
        _locationError.value = null
        _locationStatusMessage.value = "Getting location..."

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { locationSettingsResponse: LocationSettingsResponse? ->
                try {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                    Log.d("AddEditContactVM", "Location settings satisfied. Requesting updates.")
                } catch (e: SecurityException) {
                    _isLocating.value = false
                    val errorMessage = "Location permission denied. Please grant permission in app settings."
                    _locationError.value = errorMessage
                    _locationStatusMessage.value = null
                    Log.e("AddEditContactVM", "SecurityException during location updates request.", e)
                }
            }
            .addOnFailureListener { exception: Exception ->
                _isLocating.value = false
                Log.e("AddEditContactVM", "Location settings check failed.", exception)

                if (exception is ResolvableApiException) {
                    _resolveLocationSettings.value = exception
                } else {
                    val errorMessage = "Location settings are inadequate. " +
                            (exception.message?.takeIf { it.isNotBlank() } ?: "Unknown error.")
                    _locationError.value = errorMessage
                }
                _locationStatusMessage.value = null
            }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isLocating.value = false
        Log.d("AddEditContactVM", "Stopped location updates.")
    }

    fun saveOrUpdateContact(
        contactId: Int,
        name: String,
        phoneNumber: String,
        address: String,
        subject: String,
        callbackDays: String,
        callbackTime: String,
        remindersEnabled: Boolean // NEW: Add remindersEnabled parameter
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val latitude = _currentLatitude.value ?: 0.0
            val longitude = _currentLongitude.value ?: 0.0

            val newContact = Contact(
                id = contactId,
                name = name,
                phoneNumber = phoneNumber,
                address = address,
                latitude = latitude,
                longitude = longitude,
                subject = subject,
                callbackDays = callbackDays,
                callbackTime = callbackTime,
                remindersEnabled = remindersEnabled // NEW: Assign to Contact object
            )

            try {
                if (contactId != 0) {
                    contactDao.updateContact(newContact)
                    _operationStatus.postValue("Contact Updated")
                } else {
                    contactDao.insertContact(newContact)
                    _operationStatus.postValue("Contact Saved")
                }
            } catch (e: Exception) {
                _operationStatus.postValue("Error saving contact: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        Log.d("AddEditContactVM", "ViewModel cleared. Location updates stopped.")
    }
}
