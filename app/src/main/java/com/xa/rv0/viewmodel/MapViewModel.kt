package com.xa.rv0.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xa.rv0.data.ContactDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()

    private val _contactsForMap = MutableLiveData<List<Contact>>()
    val contactsForMap: LiveData<List<Contact>> = _contactsForMap

    fun loadContactsForMap() {
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = contactDao.getAllContacts()
            withContext(Dispatchers.Main) {
                _contactsForMap.value = contacts
            }
        }
    }
}