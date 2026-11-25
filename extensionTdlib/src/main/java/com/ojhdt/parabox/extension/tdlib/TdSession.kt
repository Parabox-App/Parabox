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
import com.ojhdt.parabox.extension.tdlib.mapper.TdResourceUri
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.CompletableDeferred
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
    private val sendAckTimeoutMs: Long = 50_000L,
) : ParaboxSession(sessionId, extra, hostPackageName) {

    private sealed interface SendAck {
        data class Succeeded(val finalMessageId: Long) : SendAck
        data class Failed(val code: Int, val message: String) : SendAck
    }

    private val accountId: String = extra.optString(TdExtensionProvider.KEY_ACCOUNT_ID)
    private val autoDownloadMedia: Boolean = extra.optBoolean(TdExtensionProvider.KEY_AUTO_DOWNLOAD_MEDIA, true)
    private val maxDownloadBytes: Long = (extra.optString(TdExtensionProvider.KEY_MAX_DOWNLOAD_MB)
        .toLongOrNull() ?: TdExtensionProvider.DEFAULT_MAX_DOWNLOAD_MB.toLong())
        .coerceIn(1L, 500L) * 1024L * 1024L

    @Volatile
    private var client: TdClientApi? = null

    private val userCache = ConcurrentHashMap<Long, TdApi.User>()
    private val chatCache = ConcurrentHashMap<Long, TdApi.Chat>()
    private val pendingSends = ConcurrentHashMap<Pair<Long, Long>, CompletableDeferred<SendAck>>()
    private val locallySentMessageIds = ConcurrentHashMap<Pair<Long, Long>, Long>()

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

        if (authState == null) {
            updateStatus(ParaboxConnectionStatus.Reconnecting(1, "TDLib 启动超时，正在重试"))
            return false
        }
        if (authState !is TdApi.AuthorizationStateReady) {
            val detail = authState.javaClass.simpleName
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
                    is TdApi.UpdateMessageSendSucceeded -> {
                        val key = update.message.chatId to update.oldMessageId
                        if (pendingSends.containsKey(key) || isLocallySent(update.message.chatId, update.oldMessageId)) {
                            recordLocallySent(update.message.chatId, update.message.id)
                        }
                        pendingSends.computeIfAbsent(key) { CompletableDeferred() }
                            .complete(SendAck.Succeeded(update.message.id))
                    }
                    is TdApi.UpdateMessageSendFailed -> {
                        val key = update.message.chatId to update.oldMessageId
                        val errCode = update.error?.code ?: -1
                        val errMsg = update.error?.message.orEmpty()
                        pendingSends.computeIfAbsent(key) { CompletableDeferred() }
                            .complete(SendAck.Failed(errCode, errMsg))
                    }
                    is TdApi.UpdateDeleteMessages -> {
                        update.messageIds.forEach { msgId ->
                            pendingSends[update.chatId to msgId]?.complete(
                                SendAck.Failed(-1, "消息在发送过程中被 Telegram 丢弃或删除")
                            )
                        }
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

    internal suspend fun handleNewMessage(tdClient: TdClientApi, message: TdApi.Message) {
        if (!shouldSyncUpdateNewMessage(message)) return
        val receiveMessage = buildReceiveMessage(tdClient, message) ?: return
        emitMessage(receiveMessage)
    }

    internal fun shouldSyncUpdateNewMessage(message: TdApi.Message): Boolean {
        if (!message.isOutgoing) return true
        // Local sends enter TDLib with pending/failed sendingState or are tracked in locallySentMessageIds
        if (message.sendingState != null) return false
        if (isLocallySent(message.chatId, message.id)) return false
        return true
    }

    internal fun recordLocallySent(chatId: Long, messageId: Long) {
        if (chatId == 0L || messageId == 0L) return
        val now = System.currentTimeMillis()
        locallySentMessageIds[chatId to messageId] = now
        if (locallySentMessageIds.size > LOCAL_SENT_MAX_ENTRIES) {
            val cutoff = now - LOCAL_SENT_TTL_MS
            locallySentMessageIds.entries.removeIf { it.value < cutoff }
        }
    }

    private fun isLocallySent(chatId: Long, messageId: Long): Boolean {
        val recordedAt = locallySentMessageIds[chatId to messageId] ?: return false
        if (System.currentTimeMillis() - recordedAt > LOCAL_SENT_TTL_MS) {
            locallySentMessageIds.remove(chatId to messageId, recordedAt)
            return false
        }
        return true
    }

    internal suspend fun buildReceiveMessage(
        tdClient: TdClientApi,
        message: TdApi.Message,
    ): ReceiveMessage? {
        val chat = getOrFetchChat(tdClient, message.chatId)
        val chatType = TdMessageMapper.mapChatType(chat?.type)
        val chatName = chat?.title?.ifBlank { null }

        val senderContact = resolveSender(tdClient, message.senderId)
        val resolvedQuoteReply = resolveQuoteReply(tdClient, message)
        val elements = TdMessageMapper.mapMessageContentToElements(
            message = message,
            client = tdClient,
            mediaBridge = mediaBridge,
            hostPackageName = hostPackageName,
            autoDownloadMedia = autoDownloadMedia,
            maxDownloadBytes = maxDownloadBytes,
            resolvedQuoteReply = resolvedQuoteReply,
            sessionId = sessionId,
        )

        val chatAvatar = if (chat?.photo?.small != null) {
            TdResourceUri.chatAvatar(sessionId, message.chatId)
        } else {
            ParaboxResourceInfo.ParaboxEmptyInfo
        }

        return ReceiveMessage(
            contents = elements,
            sender = senderContact,
            chat = ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = chatName,
                    avatar = chatAvatar,
                ),
                type = chatType,
                uid = message.chatId.toString(),
            ),
            timestamp = message.date.toLong() * 1000L,
            uuid = TdMessageMapper.formatMessageUuid(message.chatId, message.id),
            sentByMe = message.isOutgoing,
        )
    }

    private suspend fun resolveQuoteReply(
        tdClient: TdClientApi,
        message: TdApi.Message,
    ): ParaboxQuoteReply? {
        val reply = message.replyTo as? TdApi.MessageReplyToMessage ?: return null
        val quoteText = reply.quote?.text?.text?.takeIf { it.isNotBlank() }

        val repliedMsg = fetchRepliedMessage(tdClient, message.chatId, message.id, reply)
        if (repliedMsg != null) {
            val sender = resolveSender(tdClient, repliedMsg.senderId)
            val timestamp = repliedMsg.date.takeIf { it > 0 }?.toLong()?.times(1000L)
                ?: reply.originSendDate.takeIf { it > 0 }?.toLong()?.times(1000L)
            val id = when {
                repliedMsg.chatId != 0L && repliedMsg.id != 0L ->
                    TdMessageMapper.formatMessageUuid(repliedMsg.chatId, repliedMsg.id)
                reply.chatId != 0L && reply.messageId != 0L ->
                    TdMessageMapper.formatMessageUuid(reply.chatId, reply.messageId)
                else -> null
            }
            val previewElements = if (quoteText != null) {
                listOf(ParaboxPlainText(quoteText))
            } else {
                TdMessageMapper.mapContentToQuotePreviewElements(repliedMsg.content, repliedMsg.date)
            }
            return ParaboxQuoteReply(
                sender = sender,
                timestamp = timestamp,
                id = id,
                messages = previewElements,
            )
        }

        val fallbackSender = resolveMessageOrigin(tdClient, reply.origin)
        val fallbackTimestamp = reply.originSendDate.takeIf { it > 0 }?.toLong()?.times(1000L)
        val fallbackId = if (reply.chatId != 0L && reply.messageId != 0L) {
            TdMessageMapper.formatMessageUuid(reply.chatId, reply.messageId)
        } else {
            null
        }
        val fallbackMessages = if (quoteText != null) {
            listOf(ParaboxPlainText(quoteText))
        } else {
            TdMessageMapper.mapContentToQuotePreviewElements(reply.content, reply.originSendDate)
        }
        return ParaboxQuoteReply(
            sender = fallbackSender,
            timestamp = fallbackTimestamp,
            id = fallbackId,
            messages = fallbackMessages,
        )
    }

    private suspend fun fetchRepliedMessage(
        tdClient: TdClientApi,
        chatId: Long,
        messageId: Long,
        reply: TdApi.MessageReplyToMessage,
    ): TdApi.Message? {
        val viaReplied = withTimeoutOrNull(QUOTE_FETCH_TIMEOUT_MS) {
            runCatching {
                tdClient.send<TdApi.Message>(TdApi.GetRepliedMessage(chatId, messageId))
            }.getOrNull()
        }
        if (viaReplied != null) return viaReplied

        if (reply.chatId != 0L && reply.messageId != 0L) {
            return withTimeoutOrNull(QUOTE_FETCH_TIMEOUT_MS) {
                runCatching {
                    tdClient.send<TdApi.Message>(TdApi.GetMessage(reply.chatId, reply.messageId))
                }.getOrNull()
            }
        }
        return null
    }

    private suspend fun resolveMessageOrigin(
        tdClient: TdClientApi,
        origin: TdApi.MessageOrigin?,
    ): ParaboxContact? {
        return when (origin) {
            is TdApi.MessageOriginUser -> {
                val user = getOrFetchUser(tdClient, origin.senderUserId)
                TdMessageMapper.buildContact(
                    uid = origin.senderUserId.toString(),
                    name = user?.let { TdMessageMapper.formatUserDisplayName(it) },
                )
            }
            is TdApi.MessageOriginHiddenUser -> {
                TdMessageMapper.buildContact(
                    uid = "0",
                    name = origin.senderName?.ifBlank { null },
                )
            }
            is TdApi.MessageOriginChat -> {
                val chat = getOrFetchChat(tdClient, origin.senderChatId)
                val chatTitle = chat?.title?.ifBlank { null }
                val signature = origin.authorSignature?.ifBlank { null }
                val displayName = when {
                    chatTitle != null && signature != null -> "$chatTitle ($signature)"
                    chatTitle != null -> chatTitle
                    else -> signature
                }
                TdMessageMapper.buildContact(
                    uid = origin.senderChatId.toString(),
                    name = displayName,
                )
            }
            is TdApi.MessageOriginChannel -> {
                val chat = getOrFetchChat(tdClient, origin.chatId)
                val chatTitle = chat?.title?.ifBlank { null }
                val signature = origin.authorSignature?.ifBlank { null }
                val displayName = when {
                    chatTitle != null && signature != null -> "$chatTitle ($signature)"
                    chatTitle != null -> chatTitle
                    else -> signature
                }
                TdMessageMapper.buildContact(
                    uid = origin.chatId.toString(),
                    name = displayName,
                )
            }
            else -> null
        }
    }

    private suspend fun resolveSender(
        tdClient: TdClientApi,
        sender: TdApi.MessageSender?,
    ): ParaboxContact {
        return when (sender) {
            is TdApi.MessageSenderUser -> {
                val user = getOrFetchUser(tdClient, sender.userId)
                val avatar = if (user?.profilePhoto?.small != null) {
                    TdResourceUri.userAvatar(sessionId, sender.userId)
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                TdMessageMapper.buildContact(
                    uid = sender.userId.toString(),
                    name = user?.let { TdMessageMapper.formatUserDisplayName(it) },
                    avatar = avatar,
                )
            }
            is TdApi.MessageSenderChat -> {
                val chat = getOrFetchChat(tdClient, sender.chatId)
                val avatar = if (chat?.photo?.small != null) {
                    TdResourceUri.chatAvatar(sessionId, sender.chatId)
                } else {
                    ParaboxResourceInfo.ParaboxEmptyInfo
                }
                TdMessageMapper.buildContact(
                    uid = sender.chatId.toString(),
                    name = chat?.title?.ifBlank { null },
                    avatar = avatar,
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
            cleanupStagedFiles(outbound.stagedFiles)
            val code = if (outbound.failedMediaCount > 0) {
                ParaboxResult.ERROR_MEDIA_UNREADABLE
            } else {
                ParaboxResult.ERROR_UNKNOWN
            }
            return ParaboxResult(code, outbound.errorReason ?: "消息内容为空或媒体不可读")
        }

        val replyTo = outbound.replyToMessageId?.let { TdApi.InputMessageReplyToMessage(it, null) }
        return try {
            val queuedMessages: List<TdApi.Message> = if (outbound.canSendAsAlbum) {
                val albumResult = tdClient.send<TdApi.Messages>(
                    TdApi.SendMessageAlbum(
                        chatId,
                        0L,
                        replyTo,
                        null,
                        outbound.contents.toTypedArray(),
                    )
                )
                albumResult.messages?.filterNotNull().orEmpty()
            } else {
                outbound.contents.mapIndexed { index, inputContent ->
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
            }

            if (queuedMessages.isEmpty()) {
                return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "Telegram 未返回发送任务")
            }

            var lastConfirmedId: Long? = null
            for (queued in queuedMessages) {
                recordLocallySent(chatId, queued.id)
                when (val sendingState = queued.sendingState) {
                    null -> {
                        lastConfirmedId = queued.id
                    }
                    is TdApi.MessageSendingStateFailed -> {
                        val errCode = sendingState.error?.code ?: -1
                        val errMsg = sendingState.error?.message.orEmpty()
                        return ParaboxResult(
                            ParaboxResult.ERROR_UNKNOWN,
                            formatTdError(errCode, errMsg),
                        )
                    }
                    is TdApi.MessageSendingStatePending -> {
                        val ack = awaitSendAck(chatId, queued.id)
                        when (ack) {
                            is SendAck.Succeeded -> {
                                recordLocallySent(chatId, ack.finalMessageId)
                                lastConfirmedId = ack.finalMessageId
                            }
                            is SendAck.Failed -> {
                                return ParaboxResult(
                                    ParaboxResult.ERROR_UNKNOWN,
                                    formatTdError(ack.code, ack.message),
                                )
                            }
                            null -> {
                                return ParaboxResult(
                                    ParaboxResult.ERROR_TIMEOUT,
                                    "等待 Telegram 服务端确认超时，请检查网络连接",
                                )
                            }
                        }
                    }
                }
            }

            val finalUuid = lastConfirmedId?.let { TdMessageMapper.formatMessageUuid(chatId, it) }
            ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG, messageUuid = finalUuid)
        } catch (e: Exception) {
            Log.e(TAG, "[$accountId] onSendMessage failed", e)
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
        } finally {
            cleanupStagedFiles(outbound.stagedFiles)
        }
    }

    private suspend fun awaitSendAck(chatId: Long, tempMessageId: Long): SendAck? {
        val key = chatId to tempMessageId
        val deferred = pendingSends.computeIfAbsent(key) { CompletableDeferred() }
        return try {
            withTimeoutOrNull(sendAckTimeoutMs) {
                deferred.await()
            }
        } finally {
            pendingSends.remove(key, deferred)
        }
    }

    private fun formatTdError(code: Int, rawMessage: String): String {
        val upper = rawMessage.uppercase()
        return when {
            code == 429 || upper.contains("TOO_MANY_REQUESTS") -> "发送过于频繁，请稍后再试 ($rawMessage)"
            code == 403 || upper.contains("FORBIDDEN") || upper.contains("CHAT_WRITE_FORBIDDEN") ->
                "无权在此会话发送消息 ($rawMessage)"
            upper.contains("CHAT_NOT_FOUND") || upper.contains("PEER_ID_INVALID") ->
                "目标会话不存在或已失效 ($rawMessage)"
            upper.contains("MEDIA_EMPTY") || upper.contains("FILE_") ->
                "媒体文件上传失败 ($rawMessage)"
            rawMessage.isNotBlank() -> "Telegram 错误 ($code): $rawMessage"
            else -> "Telegram 发送失败 (code=$code)"
        }
    }

    private fun cleanupStagedFiles(files: List<File>) {
        files.forEach { file ->
            runCatching {
                val parent = file.parentFile
                file.delete()
                if (parent != null && parent.isDirectory && parent.list()?.isEmpty() == true) {
                    parent.delete()
                }
            }
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
            val avatar = if (user.profilePhoto?.small != null) {
                TdResourceUri.userAvatar(sessionId, uid)
            } else {
                ParaboxResourceInfo.ParaboxEmptyInfo
            }
            ParaboxContact(
                basicInfo = ParaboxBasicInfo(
                    name = TdMessageMapper.formatUserDisplayName(user),
                    avatar = avatar,
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
            val avatar = if (chat.photo?.small != null) {
                TdResourceUri.chatAvatar(sessionId, cid)
            } else {
                ParaboxResourceInfo.ParaboxEmptyInfo
            }
            ParaboxChat(
                basicInfo = ParaboxBasicInfo(
                    name = chat.title.ifBlank { null },
                    avatar = avatar,
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
        val avatar = if (user.profilePhoto?.small != null) {
            TdResourceUri.userAvatar(sessionId, uid)
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
        val avatar = if (chat.photo?.small != null) {
            TdResourceUri.chatAvatar(sessionId, chatId)
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
        val isAvatar = resourceId.startsWith(TdResourceUri.PREFIX_AVATAR_USER) ||
            resourceId.startsWith(TdResourceUri.PREFIX_AVATAR_CHAT)
        val resolvedFile: TdApi.File = when {
            resourceId.startsWith(TdResourceUri.PREFIX_AVATAR_USER) -> {
                val uid = resourceId.removePrefix(TdResourceUri.PREFIX_AVATAR_USER).toLongOrNull() ?: return@flow
                getOrFetchUser(tdClient, uid)?.profilePhoto?.small ?: return@flow
            }
            resourceId.startsWith(TdResourceUri.PREFIX_AVATAR_CHAT) -> {
                val cid = resourceId.removePrefix(TdResourceUri.PREFIX_AVATAR_CHAT).toLongOrNull() ?: return@flow
                getOrFetchChat(tdClient, cid)?.photo?.small ?: return@flow
            }
            resourceId.startsWith(TdResourceUri.PREFIX_REMOTE_FILE) -> {
                val remoteId = resourceId.removePrefix(TdResourceUri.PREFIX_REMOTE_FILE)
                if (remoteId.isBlank()) return@flow
                runCatching {
                    tdClient.send<TdApi.File>(TdApi.GetRemoteFile(remoteId, null))
                }.getOrNull() ?: return@flow
            }
            else -> {
                val fileId = resourceId.toIntOrNull() ?: return@flow
                TdApi.File().apply { id = fileId }
            }
        }

        val maxBytes = if (isAvatar) AVATAR_MAX_BYTES else maxDownloadBytes
        val expectedSize = resolvedFile.expectedSize.takeIf { it > 0L } ?: resolvedFile.size
        if (maxBytes > 0L && expectedSize > maxBytes) return@flow

        val tdFile = withTimeoutOrNull(STREAM_DOWNLOAD_TIMEOUT_MS) {
            runCatching {
                tdClient.send<TdApi.File>(TdApi.DownloadFile(resolvedFile.id, 32, 0, 0, true))
            }.getOrNull()
        } ?: return@flow

        val path = tdFile.local?.path.orEmpty()
        val file = File(path)
        if (path.isBlank() || !file.exists()) return@flow

        val mimeType = detectMimeType(file)
        val totalSize = file.length()
        val startOffset = offset.coerceIn(0L, totalSize)
        try {
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
                            .setMimeType(mimeType)
                            .setEof(remaining <= 0)
                            .build()
                    )
                }
            }
        } finally {
            // Reclaim TDLib local cache immediately after streaming; host Coil LRU holds the bounded cache.
            if (tdFile.id != 0) {
                runCatching {
                    tdClient.send<TdApi.Ok>(TdApi.DeleteFile(tdFile.id))
                }
            }
        }
    }

    private fun detectMimeType(file: File): String {
        val ext = file.extension.lowercase()
        when (ext) {
            "jpg", "jpeg" -> return "image/jpeg"
            "png" -> return "image/png"
            "webp" -> return "image/webp"
            "gif" -> return "image/gif"
            "tgs" -> return "application/x-tgsticker"
            "webm" -> return "video/webm"
            "mp4" -> return "video/mp4"
            "ogg", "oga", "opus" -> return "audio/ogg"
            "mp3" -> return "audio/mpeg"
            "m4a", "aac" -> return "audio/mp4"
            "wav" -> return "audio/wav"
            "flac" -> return "audio/flac"
            "amr" -> return "audio/amr"
        }
        return runCatching {
            val header = ByteArray(12)
            val n = file.inputStream().use { it.read(header) }
            when {
                n >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte() ->
                    "image/jpeg"
                n >= 4 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                    header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() ->
                    "image/png"
                n >= 12 && String(header, 0, 4) == "RIFF" && String(header, 8, 4) == "WEBP" ->
                    "image/webp"
                n >= 12 && String(header, 0, 4) == "RIFF" && String(header, 8, 4) == "WAVE" ->
                    "audio/wav"
                n >= 6 && (String(header, 0, 6) == "GIF87a" || String(header, 0, 6) == "GIF89a") ->
                    "image/gif"
                n >= 4 && String(header, 0, 4) == "OggS" ->
                    "audio/ogg"
                n >= 4 && String(header, 0, 4) == "fLaC" ->
                    "audio/flac"
                n >= 5 && String(header, 0, 5) == "#!AMR" ->
                    "audio/amr"
                n >= 3 && String(header, 0, 3) == "ID3" ->
                    "audio/mpeg"
                n >= 2 && header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0 ->
                    "audio/mpeg"
                n >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte() ->
                    "application/x-tgsticker"
                n >= 4 && header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() &&
                    header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte() ->
                    "video/webm"
                n >= 12 && String(header, 4, 4) == "ftyp" && String(header, 8, 3) == "M4A" ->
                    "audio/mp4"
                n >= 8 && String(header, 4, 4) == "ftyp" ->
                    "video/mp4"
                else -> "application/octet-stream"
            }
        }.getOrDefault("application/octet-stream")
    }

    override suspend fun onClose() {
        val active = client
        client = null
        if (active != null && accountId.isNotBlank()) {
            coordinator.closeSessionClient(accountId, expectedClient = active)
        }
        userCache.clear()
        chatCache.clear()
        pendingSends.values.forEach { deferred ->
            deferred.complete(SendAck.Failed(-2, "Telegram 会话已关闭"))
        }
        pendingSends.clear()
        locallySentMessageIds.clear()
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
        private const val STREAM_DOWNLOAD_TIMEOUT_MS = 20_000L
        private const val QUOTE_FETCH_TIMEOUT_MS = 3_000L
        private const val LOCAL_SENT_TTL_MS = 5 * 60 * 1000L
        private const val LOCAL_SENT_MAX_ENTRIES = 512
    }
}
