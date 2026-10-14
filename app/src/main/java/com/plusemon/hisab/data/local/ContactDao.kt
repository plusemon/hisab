package com.plusemon.hisab.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.plusemon.hisab.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY name ASC")
    fun getAllContacts(userId: String): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getContactById(id: Long, userId: String): Contact?

    @Query("SELECT * FROM contacts WHERE userId = :userId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getContactByName(name: String, userId: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("SELECT * FROM contacts WHERE userId = :userId")
    suspend fun getAllContactsList(userId: String): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("DELETE FROM contacts WHERE id = :id AND userId = :userId")
    suspend fun deleteContact(id: Long, userId: String)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteAllContactsForUser(userId: String)
}
