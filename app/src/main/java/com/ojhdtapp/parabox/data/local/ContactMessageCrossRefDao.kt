package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactMessageCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNewContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Delete
    fun deleteNewContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Transaction
    @Query("SELECT * FROM contact_entity")
    fun getAllContactWithMessages() : List<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId = :contactId")
    fun getSpecifiedContactWithMessages(contactId: Int) : Flow<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId IN (:contactIds)")
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Int>) : Flow<List<ContactWithMessagesEntity>>
}