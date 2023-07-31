package com.ojhdtapp.parabox.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.ChatArchiveUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ChatHideUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatWithLatestMessageEntity
import com.ojhdtapp.parabox.data.local.entity.ChatLatestMessageIdUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatPinUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertChat(chat: ChatEntity): Long

    @Transaction
    @Query(
        "SELECT chat_entity.* FROM chat_entity " +
                "INNER JOIN message_entity ON chat_entity.latestMessageId = message_entity.messageId " +
                "ORDER BY message_entity.timestamp DESC"
    )
    fun getChatPagingSource(): PagingSource<Int, ChatWithLatestMessageEntity>

    @Transaction
    @Query(
        "SELECT chat_entity.* FROM chat_entity " +
                "INNER JOIN message_entity ON chat_entity.latestMessageId = message_entity.messageId " +
                "WHERE isPinned " +
                "ORDER BY message_entity.timestamp DESC"
    )
    fun getPinnedChatPagingSource(): PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chat_entity WHERE chatId = :id LIMIT 1")
    suspend fun getChatById(id: Long): ChatEntity?

    @Query("SELECT * FROM chat_entity WHERE chatId IN (:ids)")
    suspend fun getChatsByIds(ids: List<Long>): List<ChatEntity>

    @Query("SELECT * FROM chat_entity")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE isArchived")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE isHidden")
    fun getAllHiddenChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE NOT isHidden AND NOT isArchived")
    fun getAllUnhiddenChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chat_entity WHERE name LIKE '%' || :query || '%'")
    fun queryChat(query: String): List<ChatEntity>

    @Query("SELECT * FROM chat_entity WHERE name LIKE '%' || :query || '%' LIMIT :limit")
    fun queryChatWithLimit(query: String, limit: Int): List<ChatEntity>

    @Transaction
    @Query(
        "SELECT chat_entity.* FROM chat_entity " +
                "INNER JOIN message_entity ON chat_entity.latestMessageId = message_entity.messageId " +
                "ORDER BY message_entity.timestamp DESC " +
                "LIMIT :limit"
    )
    fun getChatWithLimit(limit: Int): List<ChatEntity>

    @Update(entity = ChatEntity::class)
    fun updateLatestMessageId(obj: ChatLatestMessageIdUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateUnreadMessageNum(obj: ChatUnreadMessagesNumUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateIsHidden(obj: ChatHideUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateIsPinned(obj: ChatPinUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateArchived(obj: ChatArchiveUpdate): Int

    @Update(entity = ChatEntity::class)
    fun updateTags(obj: ChatTagsUpdate): Int
}