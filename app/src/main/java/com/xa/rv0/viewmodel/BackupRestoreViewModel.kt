package com.xa.rv0.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xa.rv0.data.ContactDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()
    private val _backupRestoreStatus = MutableLiveData<String>()
    val backupRestoreStatus: LiveData<String> = _backupRestoreStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun backupContacts(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = contactDao.getAllContacts()
            val gson = Gson()
            val json = gson.toJson(contacts)

            try {
                // FIX: Use getApplication() to access the application context
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    _backupRestoreStatus.value = "Backup created successfully."
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _backupRestoreStatus.value = "Error during backup: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }

    fun restoreContacts(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // FIX: Use getApplication() to access the application context
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.use { it.readText() }

                val gson = Gson()
                val type = object : TypeToken<List<Contact>>() {}.type
                val restoredContacts: List<Contact> = gson.fromJson(json, type)

                contactDao.deleteAllContacts() // Clear existing contacts
                contactDao.insertContacts(restoredContacts) // Insert restored contacts

                withContext(Dispatchers.Main) {
                    _backupRestoreStatus.value = "Contacts restored successfully."
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _backupRestoreStatus.value = "Error during restore: ${e.message}"
                    _isLoading.value = false
                }
            }
        }
    }
}
