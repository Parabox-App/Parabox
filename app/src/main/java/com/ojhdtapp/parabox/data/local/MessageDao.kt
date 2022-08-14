package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactPinnedStateUpdate
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.MessageVerifyStateUpdate
import com.ojhdtapp.parabox.domain.model.Message

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM message_entity " +
    "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
    "WHERE contact_message_cross_ref.contactId IN (:contactIds) " + "ORDER BY message_entity.timestamp DESC")
    fun getMessagesPagingSource(contactIds: List<Long>): PagingSource<Int, MessageEntity>

    @Update(entity = MessageEntity::class)
    fun updateVerifiedState(obj: MessageVerifyStateUpdate)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM message_entity " +
            "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
            "WHERE contact_message_cross_ref.contactId IN (:contactIds) " +
            "ORDER BY message_entity.timestamp")
    fun getMessagesPagingSourceTest(contactIds: List<Long>): List<MessageEntity>
}