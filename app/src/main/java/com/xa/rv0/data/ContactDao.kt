package com.xa.rv0.data

import androidx.room.Dao
import androidx.room.Delete // Import the Delete annotation
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xa.rv0.model.Contact

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>) // For bulk insert during restore

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete // Annotation for deleting a specific entity
    suspend fun deleteContact(contact: Contact) // Function to delete a single contact

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContacts(): List<Contact>

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Int): Contact?
}
