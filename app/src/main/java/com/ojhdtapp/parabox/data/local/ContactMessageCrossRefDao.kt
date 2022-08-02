package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactMessageCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Delete
    fun deleteContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Transaction
    @Query("SELECT * FROM contact_entity")
    fun getAllContactWithMessages() : List<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId = :contactId")
    fun getSpecifiedContactWithMessages(contactId: Long) : Flow<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId IN (:contactIds)")
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>) : Flow<List<ContactWithMessagesEntity>>

    @Transaction
    @Query("SELECT * FROM contact_entity WHERE contactId IN (:contactIds)")
    fun getSpecifiedListOfContactWithMessagesPagingSource(contactIds: List<Long>) : PagingSource<Int, ContactWithMessagesEntity>
}