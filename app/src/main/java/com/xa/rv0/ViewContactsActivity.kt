package com.xa.rv0

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xa.rv0.adapter.ContactAdapter
import com.xa.rv0.databinding.ActivityViewContactsBinding
import com.xa.rv0.viewmodel.Contact
import com.xa.rv0.viewmodel.ViewContactsViewModel

class ViewContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewContactsBinding
    private lateinit var adapter: ContactAdapter
    private lateinit var viewModel: ViewContactsViewModel

    // Moved filterType declaration to the top for clarity
    private var filterType = "Name" // Default filter type

    companion object {
        private const val TAG = "ViewContactsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ViewContactsViewModel::class.java)

        supportActionBar?.title = getString(R.string.title_view_contacts)

        val filterTypes = resources.getStringArray(R.array.filter_types)
        val filterTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterTypes)
        (binding.tilFilterType.editText as? AutoCompleteTextView)?.setAdapter(filterTypeAdapter)

        // This line should now correctly resolve 'filterType'
        (binding.tilFilterType.editText as? AutoCompleteTextView)?.setText(filterType, false)
        (binding.tilFilterType.editText as? AutoCompleteTextView)?.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                filterType = parent?.getItemAtPosition(position).toString()
                viewModel.setFilterTypeAndFilter(filterType) // Use ViewModel to re-filter
            }

        // Initialize adapter with both click and long click listeners
        adapter = ContactAdapter(
            onItemClick = { contact ->
                val intent = Intent(this, ContactDetailActivity::class.java).apply {
                    putExtra("contact", contact)
                }
                startActivity(intent)
            },
            onItemLongClick = { contact ->
                showContactOptionsDialog(contact)
                true // IMPORTANT: Return true to consume the long click event
            }
        )
        // Ensure your activity_view_contacts.xml has a RecyclerView with id="@+id/recyclerViewContacts"
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContacts.adapter = adapter

        binding.etFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setFilterQuery(s.toString()) // Use ViewModel to set filter query
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe filtered contacts from ViewModel
        viewModel.filteredContacts.observe(this) { contacts ->
            adapter.updateContacts(contacts)
            binding.tvEmptyList.visibility = if (contacts.isEmpty() && binding.etFilter.text.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun showContactOptionsDialog(contact: Contact) {
        MaterialAlertDialogBuilder(this)
            .setTitle(contact.name)
            .setItems(arrayOf("Delete Contact", "View on Map")) { dialog, which ->
                when (which) {
                    0 -> // Delete Contact
                        confirmDeleteContact(contact)
                    1 -> // View on Map
                        viewContactOnMap(contact)
                }
            }
            .show()
    }

    private fun confirmDeleteContact(contact: Contact) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete ${contact.name}?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteContact(contact)
                Snackbar.make(binding.root, "${contact.name} deleted.", Snackbar.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun viewContactOnMap(contact: Contact) {
        if (contact.latitude != 0.0 || contact.longitude != 0.0) {
            val locationUri = Uri.parse("geo:${contact.latitude},${contact.longitude}?q=${Uri.encode(contact.address.ifBlank { contact.name })}")
            val mapIntent = Intent(Intent.ACTION_VIEW, locationUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Explicitly open in Google Maps

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val generalMapUri = Uri.parse("geo:${contact.latitude},${contact.longitude}?q=${Uri.encode(contact.address.ifBlank { contact.name })}")
                val generalMapIntent = Intent(Intent.ACTION_VIEW, generalMapUri)
                if (generalMapIntent.resolveActivity(packageManager) != null) {
                    startActivity(generalMapIntent)
                } else {
                    Snackbar.make(binding.root, "No map application found.", Snackbar.LENGTH_LONG).show()
                }
            }
        } else if (contact.address.isNotBlank()) {
            val searchUri = Uri.parse("geo:0,0?q=${Uri.encode(contact.address)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, searchUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val generalMapUri = Uri.parse("geo:0,0?q=${Uri.encode(contact.address)}")
                val generalMapIntent = Intent(Intent.ACTION_VIEW, generalMapUri)
                if (generalMapIntent.resolveActivity(packageManager) != null) {
                    startActivity(generalMapIntent)
                } else {
                    Snackbar.make(binding.root, "No map application found for address.", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        else {
            Snackbar.make(binding.root, "No location data (coordinates or address) available for this contact.", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContacts() // Reload contacts when returning to this activity
    }

    override fun finish() {
        super.finish()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
