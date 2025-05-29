package com.xa.rv0.viewmodel

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
    val phoneNumber: String?, // CHANGED: Made nullable
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val subject: String,
    val callbackDays: String,
    val callbackTime: String,
    val remindersEnabled: Boolean = true,
    val imageUri: String? = null, // For storing an image URI
    val creationTimestamp: Long = System.currentTimeMillis()
) : Parcelable
