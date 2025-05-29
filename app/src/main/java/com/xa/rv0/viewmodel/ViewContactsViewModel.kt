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

class ViewContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = ContactDatabase.getDatabase(application).contactDao()

    private val _allContacts = MutableLiveData<List<Contact>>()
    val allContacts: LiveData<List<Contact>> = _allContacts

    private val _filteredContacts = MutableLiveData<List<Contact>>()
    val filteredContacts: LiveData<List<Contact>> = _filteredContacts

    var filterType: String = "Name" // Expose filterType to be observed

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val contactsFromDb = contactDao.getAllContacts()
            withContext(Dispatchers.Main) {
                _allContacts.value = contactsFromDb
                // Re-apply current filter after loading new contacts
                filterContacts(_filterQuery.value ?: "")
            }
        }
    }

    private val _filterQuery = MutableLiveData<String>("")

    fun setFilterQuery(query: String) {
        _filterQuery.value = query
        filterContacts(query)
    }

    private fun filterContacts(query: String) {
        val currentAllContacts = _allContacts.value ?: emptyList()
        val newFilteredList = if (query.isEmpty()) {
            currentAllContacts.toList()
        } else {
            currentAllContacts.filter { contact ->
                when (filterType) {
                    "Name" -> contact.name.contains(query, ignoreCase = true)
                    "Subject" -> contact.subject.contains(query, ignoreCase = true)
                    "Callback Days" -> contact.callbackDays.contains(query, ignoreCase = true)
                    "Phone" -> contact.phoneNumber!!.contains(query, ignoreCase = true)
                    "Location" -> {
                        // Filter by address or by string representation of coordinates
                        contact.address.contains(query, ignoreCase = true) ||
                                (contact.latitude != 0.0 || contact.longitude != 0.0) &&
                                "${contact.latitude}, ${contact.longitude}".contains(query, ignoreCase = true)
                    }
                    else -> false
                }
            }
        }
        _filteredContacts.value = newFilteredList
    }

    // This could be called from the Activity when the spinner selection changes
    fun setFilterTypeAndFilter(type: String) {
        filterType = type
        filterContacts(_filterQuery.value ?: "")
    }

    /**
     * Deletes a contact from the database and reloads the contact list.
     * @param contact The Contact object to be deleted.
     */
    fun deleteContact(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            contactDao.deleteContact(contact)
            loadContacts() // Reload contacts after deletion
        }
    }
}
