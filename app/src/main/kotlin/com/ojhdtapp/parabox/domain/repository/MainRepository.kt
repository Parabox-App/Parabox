package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun receiveMessage(
        msg: ReceiveMessage,
        ext: Connection.ConnectionSuccess,
        isBackfill: Boolean = false,
    ): ParaboxResult

    suspend fun syncChatMessages(
        messages: List<ReceiveMessage>,
        ext: Connection.ConnectionSuccess,
        suppressNotification: Boolean = true,
        incrementUnread: Boolean = false,
    ): Int

    suspend fun insertOutgoingMessage(
        msg: SendMessage,
        pkg: String,
        extensionId: Long,
        chatId: Long,
    ): Long

    suspend fun markMessageSendSuccess(messageId: Long, remoteUid: String?)

    suspend fun resetMessageForRetry(messageId: Long, timestamp: Long)

    suspend fun deleteMessage(messageId: Long)

    fun getRecentQuery(): Flow<Resource<List<RecentQuery>>>

    suspend fun submitRecentQuery(value: String): Boolean

    suspend fun deleteRecentQuery(id: Long): Boolean
}