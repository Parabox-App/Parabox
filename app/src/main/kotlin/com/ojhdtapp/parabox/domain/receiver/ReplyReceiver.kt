package com.ojhdtapp.parabox.domain.receiver

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var notificationUtil: NotificationUtil

    override fun onReceive(context: Context, intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(NotificationUtil.KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (replyText.isEmpty()) return

        val chatIdFromExtra = intent.getLongExtra(NotificationUtil.EXTRA_CHAT_ID, -1L)
        val fallbackChat = if (chatIdFromExtra <= 0L) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("chat", Chat::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("chat")
            }
        } else {
            null
        }
        val chatId = if (chatIdFromExtra > 0L) chatIdFromExtra else (fallbackChat?.chatId ?: -1L)
        if (chatId <= 0L) {
            Log.w(TAG, "ReplyReceiver ignored: missing chatId")
            return
        }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                handleDirectReply(context, chatId, replyText)
            } catch (e: Exception) {
                Log.e(TAG, "Direct reply failed unexpectedly", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleDirectReply(context: Context, chatId: Long, replyText: String) {
        val chat = appDatabase.chatDao.getChatByIdWithoutObserve(chatId)?.toChat() ?: run {
            Log.w(TAG, "Direct reply aborted: chat $chatId not found")
            return
        }
        val extensionId = chat.extensionId ?: run {
            Log.w(TAG, "Direct reply aborted: chat $chatId has no extensionId")
            return
        }

        val connection = extensionManager.connectionFlow.value
            .firstOrNull { it.connectionId == extensionId }
        val connectionInfo = connection?.toExtensionInfo()
            ?: appDatabase.connectionInfoDao.getConnectionInfoById(extensionId)?.toConnectionInfo()

        val sendMessage = SendMessage(
            contents = listOf(ParaboxPlainText(replyText)),
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = chat.name,
                    avatar = chat.avatar,
                ),
                type = chat.type,
                uid = chat.uid,
            ),
            timestamp = System.currentTimeMillis(),
            uuid = UUID.randomUUID().toString(),
        )

        val localMessageId = mainRepository.insertOutgoingMessage(
            msg = sendMessage,
            pkg = chat.pkg,
            extensionId = extensionId,
            chatId = chat.chatId,
        )

        val result = when (connection) {
            is Connection.ConnectionSuccess -> {
                try {
                    connection.sendMessage(sendMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "connection.sendMessage failed in ReplyReceiver", e)
                    ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: "发送异常")
                }
            }
            is Connection.ConnectionFail -> {
                val detail = connection.reason.ifBlank { "未知原因" }
                ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "扩展连接异常：$detail")
            }
            is Connection.ConnectionPending -> {
                ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "扩展正在连接中，请稍后重试")
            }
            null -> {
                ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "未找到对应的扩展连接")
            }
        }

        if (result.code == ParaboxResult.SUCCESS) {
            if (localMessageId > 0L) {
                mainRepository.markMessageSendSuccess(localMessageId, result.messageUuid)
            }
            if (chat.unreadMessageNum > 0) {
                appDatabase.chatDao.updateUnreadMessageNum(
                    ChatUnreadMessagesNumUpdate(chat.chatId, 0)
                )
                val currentBadge = context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0)
                context.dataStore.edit { prefs ->
                    prefs[DataStoreKeys.MESSAGE_BADGE_NUM] = (currentBadge - chat.unreadMessageNum).coerceAtLeast(0)
                }
            }
            if (connectionInfo != null) {
                notificationUtil.postDirectReplyResultNotification(
                    chatId = chat.chatId,
                    messageId = localMessageId,
                    connectionInfo = connectionInfo,
                    errorText = null,
                )
            } else {
                notificationUtil.clearNotification(chat.chatId.toInt())
            }
        } else {
            val errorReason = result.message.ifBlank { "未知错误 (${result.code})" }
            if (connectionInfo != null) {
                notificationUtil.postDirectReplyResultNotification(
                    chatId = chat.chatId,
                    messageId = localMessageId,
                    connectionInfo = connectionInfo,
                    errorText = errorReason,
                )
            } else {
                notificationUtil.clearNotification(chat.chatId.toInt())
            }
        }
    }

    companion object {
        private const val TAG = "ReplyReceiver"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
