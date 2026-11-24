package com.ojhdt.parabox.extension.tdlib

import android.util.Log
import com.google.protobuf.ByteString
import com.ojhdt.parabox.extension.tdlib.core.TdAccountMeta
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.send
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdt.parabox.extension.tdlib.mapper.TdMessageMapper
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

class TdSession(
    sessionId: String,
    extra: JSONObject,
    hostPackageName: String,
    private val coordinator: TdClientCoordinator,
    private val mediaBridge: TdMediaBridge,
    private val initTimeoutMs: Long = 25_000L,
) : ParaboxSession(sessionId, extra, hostPackageName) {

    private val accountId: String = extra.optString(TdExtensionProvider.KEY_ACCOUNT_ID)
    private val autoDownloadMedia: Boolean = extra.optBoolean(TdExtensionProvider.KEY_AUTO_DOWNLOAD_MEDIA, true)
    private val maxDownloadBytes: Long = (extra.optString(TdExtensionProvider.KEY_MAX_DOWNLOAD_MB)
        .toLongOrNull() ?: TdExtensionProvider.DEFAULT_MAX_DOWNLOAD_MB.toLong())
        .coerceIn(1L, 500L) * 1024L * 1024L

    @Volatile
    private var client: TdClientApi? = null

    private val userCache = ConcurrentHashMap<Long, TdApi.User>()
    private val chatCache = ConcurrentHashMap<Long, TdApi.Chat>()

    override suspend fun onInitialize(): Boolean {
        if (accountId.isBlank()) {
            updateStatus(ParaboxConnectionStatus.Error("缺少账号标识 account_id，请删除后重新新建连接"))
            return false
        }

        val proxyConfig = TdProxyConfig.fromExtra(extra)
        val tdClient = try {
            coordinator.openSessionClient(accountId, proxyConfig)
        } catch (e: Exception) {
            updateStatus(ParaboxConnectionStatus.Error("打开 TDLib 账号失败：${e.message}"))
            return false
        }
        client = tdClient

        val authState = withTimeoutOrNull(initTimeoutMs) {
            tdClient.authorizationState.first { state ->
                state != null && state !is TdApi.AuthorizationStateWaitTdlibParameters
            }
        }

        if (authState !is TdApi.AuthorizationStateReady) {
            val detail = authState?.javaClass?.simpleName ?: "Timeout"
            updateStatus(ParaboxConnectionStatus.Error("Telegram 登录态失效（$detail），请重新新建连接登录"))
            return false
        }

        // Apply latest proxy config in case user edited proxy settings after initial login
        tdClient.applyProxy(proxyConfig)

        // Refresh saved metadata for account manager UI
        runCatching { tdClient.send<TdApi.User>(TdApi.GetMe()) }.getOrNull()?.let { me ->
            userCache[me.id] = me
            coordinator.storage.saveMeta(
                TdAccountMeta(
                    accountId = accountId,
                    userId = me.id,
                    displayName = TdMessageMapper.formatUserDisplayName(me),
                    phoneNumber = me.phoneNumber.orEmpty(),
                    username = me.usernames?.editableUsername.orEmpty(),
                )
            )
        }

        updateStatus(ParaboxConnectionStatus.Active)
        observeTdUpdates(tdClient)
        return true
    }

    private fun observeTdUpdates(tdClient: TdClientApi) {
        sessionScope.launch {
            tdClient.updates.collect { update ->
                when (update) {
                    is TdApi.UpdateAuthorizationState -> handleAuthUpdate(update.authorizationState)
                    is TdApi.UpdateConnectionState -> handleConnectionUpdate(update.state)
                    is TdApi.UpdateUser -> userCache[update.user.id] = update.user
                    is TdApi.UpdateNewChat -> chatCache[update.chat.id] = update.chat
                    is TdApi.UpdateNewMessage -> {
                        launch { handleNewMessage(tdClient, update.message) }
                    }
                    is TdApi.UpdateDeleteMessages -> {
                        if (update.isPermanent && !update.fromCache) {
                            launch {
                                update.messageIds.forEach { msgId ->
                                    emitRecall(TdMessageMapper.formatMessageUuid(update.chatId, msgId))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAuthUpdate(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateReady -> updateStatus(ParaboxConnectionStatus.Active)
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosed,
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                updateStatus(ParaboxConnectionStatus.Error("Telegram 账号已登出或登录态失效"))
            }
            else -> Unit
        }
    }

    private fun handleConnectionUpdate(state: TdApi.ConnectionState) {
        when (state) {
            is TdApi.ConnectionStateReady -> updateStatus(ParaboxConnectionStatus.Active)
            is TdApi.ConnectionStateUpdating,
            is TdApi.ConnectionStateConnecting,
            is TdApi.ConnectionStateConnectingToProxy -> updateStatus(ParaboxConnectionStatus.Initializing)
            is TdApi.ConnectionStateWaitingForNetwork -> {
                updateStatus(ParaboxConnectionStatus.Reconnecting(1, "等待网络恢复"))
            }
        }
    }

    private suspend fun handleNewMessage(tdClient: TdClientApi, message: TdApi.Message) {
        if (message.isOutgoing) return
        val receiveMessage = buildReceiveMessage(tdClient, message) ?: return
        emitMessage(receiveMessage)
    }

    private suspend fun buildReceiveMessage(
        tdClient: TdClientApi,
        message: TdApi.Message,
    ): ReceiveMessage? {
        val chat = getOrFetchChat(tdClient, message.chatId)
        val chatType = TdMessageMapper.mapChatType(chat?.type)
        val chatName = chat?.title?.ifBlank { null }

        val senderContact = resolveSender(tdClient, message.senderId)
        val elements = TdMessageMapper.mapMessageContentToElements(
            message = message,
            client = tdClient,
            mediaBridge = mediaBridge,
            hostPackageName = hostPackageName,
            autoDownloadMedia = autoDownloadMedia,
            maxDownloadBytes = maxDownloadBytes,
        )

        return ReceiveMessage(
            contents = elements,
            sender = senderContact,
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = chatName,
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = chatType,
                uid = message.chatId.toString(),
            ),
            timestamp = message.date.toLong() * 1000L,
            uuid = TdMessageMapper.formatMessageUuid(message.chatId, message.id),
        )
    }

    private suspend fun resolveSender(
        tdClient: TdClientApi,
        sender: TdApi.MessageSender?,
    ): ParaboxContact {
        return when (sender) {
            is TdApi.MessageSenderUser -> {
                val user = getOrFetchUser(tdClient, sender.userId)
                TdMessageMapper.buildContact(
                    uid = sender.userId.toString(),
                    name = user?.let { TdMessageMapper.formatUserDisplayName(it) },
                )
            }
            is TdApi.MessageSenderChat -> {
                val chat = getOrFetchChat(tdClient, sender.chatId)
                TdMessageMapper.buildContact(
                    uid = sender.chatId.toString(),
                    name = chat?.title?.ifBlank { null },
                )
            }
            else -> TdMessageMapper.buildContact(uid = "0", name = "Unknown")
        }
    }

    override suspend fun onSendMessage(message: SendMessage): ParaboxResult {
        val tdClient = client
            ?: return ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, ParaboxResult.ERROR_UNINITIALIZED_MSG)
        val chatId = message.chat.uid.toLongOrNull()
            ?: return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "无效的 Telegram Chat ID: ${message.chat.uid}")

        val outbound = TdMessageMapper.mapSendMessageToOutbound(
            sendMessage = message,
            accountId = accountId,
            mediaBridge = mediaBridge,
        )
        if (outbound.contents.isEmpty()) {
            return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "消息内容为空或媒体不可读")
        }

        val replyTo = outbound.replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null) }
        return try {
            outbound.contents.forEachIndexed { index, inputContent ->
                tdClient.send<TdApi.Message>(
                    TdApi.SendMessage(
                        chatId,
                        0L,
                        if (index == 0) replyTo else null,
                        null,
                        null,
                        inputContent,
                    )
                )
            }
            ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
        } catch (e: Exception) {
            Log.e(TAG, "[$accountId] onSendMessage failed", e)
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
        }
    }

    override suspend fun onRecallMessage(messageUuid: String): ParaboxResult {
        val tdClient = client
            ?: return ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, ParaboxResult.ERROR_UNINITIALIZED_MSG)
        val (chatId, msgId) = TdMessageMapper.parseMessageUuid(messageUuid)
            ?: return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "无法解析消息 ID: $messageUuid")
        return try {
            tdClient.send<TdApi.Ok>(TdApi.DeleteMessages(chatId, longArrayOf(msgId), true))
            ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
        }
    }

    override suspend fun onGetContacts(): List<ParaboxContact> {
        val tdClient = client ?: return emptyList()
        val users = runCatching { tdClient.send<TdApi.Users>(TdApi.GetContacts()) }.getOrNull() ?: return emptyList()
        return users.userIds.toList().mapNotNull { uid ->
            val user = getOrFetchUser(tdClient, uid) ?: return@mapNotNull null
            ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = TdMessageMapper.formatUserDisplayName(user),
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                uid = uid.toString(),
            )
        }
    }

    override suspend fun onGetChats(): List<ParaboxChat> {
        val tdClient = client ?: return emptyList()
        val chats = runCatching {
            tdClient.send<TdApi.Chats>(TdApi.GetChats(TdApi.ChatListMain(), 50))
        }.getOrNull() ?: return emptyList()
        return chats.chatIds.toList().mapNotNull { cid ->
            val chat = getOrFetchChat(tdClient, cid) ?: return@mapNotNull null
            ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = chat.title.ifBlank { null },
                    avatar = ParaboxResourceInfo.ParaboxEmptyInfo,
                ),
                type = TdMessageMapper.mapChatType(chat.type),
                uid = cid.toString(),
            )
        }
    }

    override suspend fun onQueryMessageHistory(uuid: String): List<ReceiveMessage> {
        val tdClient = client ?: return emptyList()
        val (chatId, fromMsgId) = TdMessageMapper.parseMessageUuid(uuid) ?: return emptyList()
        val history = runCatching {
            tdClient.send<TdApi.Messages>(TdApi.GetChatHistory(chatId, fromMsgId, 0, 20, false))
        }.getOrNull() ?: return emptyList()
        return history.messages.mapNotNull { msg ->
            buildReceiveMessage(tdClient, msg)
        }
    }

    override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
        val tdClient = client ?: return null
        val uid = userId.toLongOrNull() ?: return null
        val user = getOrFetchUser(tdClient, uid) ?: return null
        val avatarFile = user.profilePhoto?.small
        val avatar = if (avatarFile != null) {
            mediaBridge.downloadAndShareFile(
                client = tdClient,
                tdFile = avatarFile,
                hostPackageName = hostPackageName,
                maxSizeBytes = AVATAR_MAX_BYTES,
                timeoutMs = 10_000L,
            )
        } else {
            ParaboxResourceInfo.ParaboxEmptyInfo
        }
        return ParaboxBasicInfo(
            name = TdMessageMapper.formatUserDisplayName(user),
            avatar = avatar,
        )
    }

    override suspend fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo? {
        val tdClient = client ?: return null
        val chatId = groupId.toLongOrNull() ?: return null
        val chat = getOrFetchChat(tdClient, chatId) ?: return null
        val avatarFile = chat.photo?.small
        val avatar = if (avatarFile != null) {
            mediaBridge.downloadAndShareFile(
                client = tdClient,
                tdFile = avatarFile,
                hostPackageName = hostPackageName,
                maxSizeBytes = AVATAR_MAX_BYTES,
                timeoutMs = 10_000L,
            )
        } else {
            ParaboxResourceInfo.ParaboxEmptyInfo
        }
        return ParaboxBasicInfo(
            name = chat.title.ifBlank { null },
            avatar = avatar,
        )
    }

    override fun onStreamResource(resourceId: String, offset: Long): Flow<ResourceChunk> = flow {
        val tdClient = client ?: return@flow
        val fileId = resourceId.toIntOrNull() ?: return@flow
        val tdFile = runCatching {
            tdClient.send<TdApi.File>(TdApi.DownloadFile(fileId, 16, 0, 0, true))
        }.getOrNull() ?: return@flow
        val path = tdFile.local?.path.orEmpty()
        val file = File(path)
        if (path.isBlank() || !file.exists()) return@flow

        val totalSize = file.length()
        val startOffset = offset.coerceIn(0L, totalSize)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startOffset)
            val buffer = ByteArray(STREAM_CHUNK_BYTES)
            var remaining = totalSize - startOffset
            while (remaining > 0) {
                val read = raf.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read <= 0) break
                remaining -= read
                emit(
                    ResourceChunk.newBuilder()
                        .setData(ByteString.copyFrom(buffer, 0, read))
                        .setTotalSize(totalSize)
                        .setMimeType("application/octet-stream")
                        .setEof(remaining <= 0)
                        .build()
                )
            }
        }
    }

    override suspend fun onClose() {
        val active = client
        client = null
        if (active != null && accountId.isNotBlank()) {
            coordinator.closeSessionClient(accountId, expectedClient = active)
        }
        userCache.clear()
        chatCache.clear()
    }

    private suspend fun getOrFetchUser(tdClient: TdClientApi, userId: Long): TdApi.User? {
        userCache[userId]?.let { return it }
        return runCatching { tdClient.send<TdApi.User>(TdApi.GetUser(userId)) }.getOrNull()?.also {
            userCache[userId] = it
        }
    }

    private suspend fun getOrFetchChat(tdClient: TdClientApi, chatId: Long): TdApi.Chat? {
        chatCache[chatId]?.let { return it }
        return runCatching { tdClient.send<TdApi.Chat>(TdApi.GetChat(chatId)) }.getOrNull()?.also {
            chatCache[chatId] = it
        }
    }

    companion object {
        private const val TAG = "TdSession"
        private const val AVATAR_MAX_BYTES = 2L * 1024L * 1024L
        private const val STREAM_CHUNK_BYTES = 64 * 1024
    }
}
