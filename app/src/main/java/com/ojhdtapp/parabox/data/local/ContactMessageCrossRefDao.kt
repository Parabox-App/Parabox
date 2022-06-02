package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity

@Dao
interface ContactMessageCrossRefDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNewContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Delete
    fun deleteNewContactMessageCrossRef(crossRef: ContactMessageCrossRef)

    @Transaction
    @Query("SELECT * FROM contactentity")
    fun getAllContactWithMessages() : List<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contactentity WHERE contactid = :contactId")
    fun getSpecifiedContactWithMessages(contactId: Int) : List<ContactWithMessagesEntity>

    @Transaction
    @Query("SELECT * FROM contactentity WHERE contactid IN (:contactIds)")
    fun getSpecifiedListOfContactWithMessages(contactIds: List<Int>) : List<ContactWithMessagesEntity>
}