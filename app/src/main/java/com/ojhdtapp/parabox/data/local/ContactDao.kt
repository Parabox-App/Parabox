package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.domain.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("SELECT * FROM contact_entity WHERE contactId = :id LIMIT 1")
    suspend fun getContactById(id:Int):ContactEntity?

    @Query("SELECT * FROM contact_entity")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE content IS NOT NULL")
    fun getMessagedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE isHidden")
    fun getAllHiddenContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contact_entity WHERE NOT isHidden")
    fun getAllUnhiddenContacts(): Flow<List<ContactEntity>>
}