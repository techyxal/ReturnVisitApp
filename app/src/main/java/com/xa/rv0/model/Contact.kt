package com.xa.rv0.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val subject: String = "",
    val callbackDays: String = "",
    val callbackTime: String = "",
    val imageUri: String? = null,
    val creationTimestamp: Long = System.currentTimeMillis() // NEW: Timestamp for creation
) : Parcelable
