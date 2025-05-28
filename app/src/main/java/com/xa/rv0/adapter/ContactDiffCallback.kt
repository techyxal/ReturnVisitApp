package com.xa.rv0.adapter // Or your relevant package

import androidx.recyclerview.widget.DiffUtil
import com.xa.rv0.model.Contact // Make sure this import is correct

class ContactDiffCallback(
    private val oldList: List<Contact>,
    private val newList: List<Contact>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Implement logic to check if items are the same, typically using a unique ID.
        // For example, if your Contact model has an 'id' field:
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Implement logic to check if the content of items is the same.
        // This is called only if areItemsTheSame() returns true.
        // If Contact is a data class, you can often just compare them directly.
        return oldList[oldItemPosition] == newList[newItemPosition]
        // If Contact is not a data class, you'll need to compare relevant fields:
        // return oldList[oldItemPosition].name == newList[newItemPosition].name &&
        //        oldList[oldItemPosition].phoneNumber == newList[newItemPosition].phoneNumber // etc.
    }

    // Optional: You can also override getChangePayload for more granular updates
    // override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
    //     // Return a payload if you want to perform partial updates,
    //     // otherwise, return null for a full rebind.
    //     return super.getChangePayload(oldItemPosition, newItemPosition)
    // }
}