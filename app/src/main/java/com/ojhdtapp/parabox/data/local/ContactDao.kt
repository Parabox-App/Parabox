package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.parabox.data.local.entity.ContactEntity

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContact(contact: ContactEntity): Long

    @Query("SELECT * FROM contact_entity " +
            "WHERE contactId = :contactId " +
            "LIMIT 1")
    fun getContactById(contactId: Long): ContactEntity?

    @Query("SELECT * FROM contact_entity WHERE name LIKE '%' || :query || '%'")
    fun queryContact(query: String): List<ContactEntity>
}