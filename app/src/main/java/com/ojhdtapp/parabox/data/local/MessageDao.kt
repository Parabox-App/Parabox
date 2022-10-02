package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactPinnedStateUpdate
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.MessageVerifyStateUpdate
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM message_entity " +
    "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
    "WHERE contact_message_cross_ref.contactId IN (:contactIds) " + "ORDER BY message_entity.timestamp DESC")
    fun getMessagesPagingSource(contactIds: List<Long>): PagingSource<Int, MessageEntity>

    @Query("DELETE FROM message_entity WHERE messageId = :messageId")
    fun deleteMessageById(messageId: Long)
    @Query("DELETE FROM message_entity WHERE messageId IN (:messageIdList)")
    fun deleteMessageById(messageIdList: List<Long>)

    @Update(entity = MessageEntity::class)
    fun updateVerifiedState(obj: MessageVerifyStateUpdate)

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM message_entity " +
            "INNER JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
            "WHERE contact_message_cross_ref.contactId IN (:contactIds) " +
            "ORDER BY message_entity.timestamp")
    fun getMessagesPagingSourceTest(contactIds: List<Long>): List<MessageEntity>

    @Query("SELECT * FROM message_entity WHERE name LIKE '%' || :query || '%' OR contentString LIKE '%' || :query || '%'")
    fun queryMessage(query: String): List<MessageEntity>

    @Query("SELECT * FROM message_entity " +
            "JOIN contact_message_cross_ref ON contact_message_cross_ref.messageId = message_entity.messageId " +
            "JOIN contact_entity ON contact_entity.contactId = contact_message_cross_ref.contactId " +
            "WHERE message_entity.name LIKE '%' || :query || '%' OR message_entity.contentString LIKE '%' || :query || '%' ")
    fun queryContactWithMessages(query: String) : Map<ContactEntity, List<MessageEntity>>

}