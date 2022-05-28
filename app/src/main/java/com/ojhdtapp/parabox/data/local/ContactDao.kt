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

    @Query("SELECT * FROM contactentity")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contactentity WHERE latestMessage IS NOT NULL")
    fun getMessagedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contactentity WHERE ishidden = TRUE")
    fun getAllHiddenContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contactentity WHERE ishidden != TRUE")
    fun getAllUnhiddenContacts(): Flow<List<ContactEntity>>
}