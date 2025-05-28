package com.xa.rv0.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.xa.rv0.R
import com.xa.rv0.databinding.ItemContactBinding
import com.xa.rv0.model.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactAdapter(
    private val onItemClick: (Contact) -> Unit,
    private val onItemLongClick: (Contact) -> Boolean
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var contacts: List<Contact> = emptyList()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    // NEW: Define a SimpleDateFormat for 12-hour format (hh:mm a) for callback time
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())


    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phoneNumber

            if (contact.address.isNotBlank()) {
                binding.tvLocation.text = contact.address
            } else if (contact.latitude != 0.0 || contact.longitude != 0.0) {
                binding.tvLocation.text = binding.root.context.getString(
                    R.string.coordinates_format_short,
                    contact.latitude,
                    contact.longitude
                )
            } else {
                binding.tvLocation.text = binding.root.context.getString(R.string.location_not_set)
            }

            if (contact.callbackDays.isNotBlank()) {
                binding.tvCallbackDays.text = binding.root.context.getString(
                    R.string.callback_days_format,
                    contact.callbackDays
                )
            } else {
                binding.tvCallbackDays.text = binding.root.context.getString(R.string.no_callback_days)
            }

            binding.tvSubject.text = contact.subject

            // Display timestamp
            binding.tvTimestamp.text = binding.root.context.getString(
                R.string.contact_added_timestamp_format,
                dateFormatter.format(Date(contact.creationTimestamp))
            )

            // NEW: Display callback time in AM/PM format
            if (contact.callbackTime.isNotBlank()) {
                try {
                    // Parse the stored time (assuming it was saved in 24-hour format initially if not changed)
                    // Or if it's already in hh:mm a, just display it.
                    // For robustness, if you stored 24hr, you might need to parse and then reformat.
                    // For now, assuming it's stored in a way that can be directly displayed or will be consistent.
                    val parsedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).parse(contact.callbackTime)
                    parsedTime?.let {
                        binding.tvCallbackTime.text = timeFormatter.format(it)
                    } ?: run {
                        binding.tvCallbackTime.text = contact.callbackTime // Fallback if parsing fails
                    }
                } catch (e: Exception) {
                    binding.tvCallbackTime.text = contact.callbackTime // Fallback if parsing fails
                }
            } else {
                binding.tvCallbackTime.text = binding.root.context.getString(R.string.no_callback_time_set)
            }


            binding.root.setOnClickListener {
                onItemClick(contact)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(contact)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<Contact>) {
        val diffResult = DiffUtil.calculateDiff(ContactDiffCallback(this.contacts, newContacts))
        this.contacts = newContacts
        diffResult.dispatchUpdatesTo(this)
    }

    private class ContactDiffCallback(
        private val oldList: List<Contact>,
        private val newList: List<Contact>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldContact = oldList[oldItemPosition]
            val newContact = newList[newItemPosition]
            return oldContact.name == newContact.name &&
                    oldContact.phoneNumber == newContact.phoneNumber &&
                    oldContact.address == newContact.address &&
                    oldContact.latitude == newContact.latitude &&
                    oldContact.longitude == newContact.longitude &&
                    oldContact.subject == newContact.subject &&
                    oldContact.callbackDays == newContact.callbackDays &&
                    oldContact.callbackTime == newContact.callbackTime &&
                    oldContact.imageUri == newContact.imageUri &&
                    oldContact.creationTimestamp == newContact.creationTimestamp
        }
    }
}
