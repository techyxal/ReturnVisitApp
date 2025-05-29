package com.xa.rv0.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Import Glide
import com.xa.rv0.R
import com.xa.rv0.databinding.ItemContactBinding
import com.xa.rv0.viewmodel.Contact

class ContactAdapter(
    private val onItemClick: (Contact) -> Unit,
    private val onItemLongClick: (Contact) -> Boolean
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var contacts: List<Contact> = emptyList()

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phoneNumber ?: "No phone number" // Handle nullable phone number

            // Display Location (Address or Coordinates)
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

            // Display Callback Days
            if (contact.callbackDays.isNotBlank()) {
                binding.tvCallbackDays.text = binding.root.context.getString(
                    R.string.callback_days_format,
                    contact.callbackDays
                )
            } else {
                binding.tvCallbackDays.text = binding.root.context.getString(R.string.no_callback_days)
            }

            binding.tvSubject.text = contact.subject

            // NEW: Load image using Glide
            contact.imageUri?.let { uriString ->
                Glide.with(binding.ivContactIcon.context)
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.ic_contact_avatar_placeholder)
                    .error(R.drawable.ic_contact_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivContactIcon)
            } ?: run {
                // If no image URI, load the default placeholder
                Glide.with(binding.ivContactIcon.context)
                    .load(R.drawable.ic_contact_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivContactIcon)
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
                    oldContact.phoneNumber == newContact.phoneNumber && // Compare nullable phone number
                    oldContact.address == newContact.address &&
                    oldContact.latitude == newContact.latitude &&
                    oldContact.longitude == newContact.longitude &&
                    oldContact.subject == newContact.subject &&
                    oldContact.callbackDays == newContact.callbackDays &&
                    oldContact.callbackTime == newContact.callbackTime &&
                    oldContact.remindersEnabled == newContact.remindersEnabled &&
                    oldContact.imageUri == newContact.imageUri && // Compare imageUri
                    oldContact.creationTimestamp == newContact.creationTimestamp
        }
    }
}
