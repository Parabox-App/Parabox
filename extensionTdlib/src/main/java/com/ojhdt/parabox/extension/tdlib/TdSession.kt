package com.ojhdt.parabox.extension.tdlib

import android.util.Log
import com.google.protobuf.ByteString
import com.ojhdt.parabox.extension.tdlib.core.TdAccountMeta
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdErrorException
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.TdSyncStateStore
import com.ojhdt.parabox.extension.tdlib.core.send
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdt.parabox.extension.tdlib.mapper.TdMessageMapper
import com.ojhdt.parabox.extension.tdlib.mapper.TdResourceUri
import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val catchUpChatLimit: Int = DEFAULT_CATCH_UP_CHAT_LIMIT,
    private val catchUpMessagesPerChat: Int = DEFAULT_CATCH_UP_MESSAGES_PER_CHAT,
    private val catchUpChatThrottleMs: Long = DEFAULT_CATCH_UP_THROTTLE_MS,
    private val lastMessageFallbackDebounceMs: Long = DEFAULT_LAST_MESSAGE_FALLBACK_DEBOUNCE_MS,
    private val reconnectCatchUpThresholdMs: Long = DEFAULT_RECONNECT_CATCH_UP_THRESHOLD_MS,
    private val periodicSyncForegroundIntervalMs: Long = DEFAULT_PERIODIC_SYNC_FOREGROUND_MS,
    private val periodicSyncBackgroundIntervalMs: Long = DEFAULT_PERIODIC_SYNC_BACKGROUND_MS,
    private val periodicSyncProbePacingMs: Long = DEFAULT_PERIODIC_SYNC_PROBE_PACING_MS,
) : ParaboxSession(sessionId, extra, hostPackageName) {

    private sealed interface SendAck {
        data class Succeeded(val finalMessageId: Long) : SendAck
        data class Failed(val code: Int, val message: String) : SendAck
    }

    private val accountId: String = extra.optString(TdExtensionProvider.KEY_ACCOUNT_ID)
    private val autoDownloadMedia: Boolean = extra.optBoolean(TdExtensionProvider.KEY_AUTO_DOWNLOAD_MEDIA, true)
    private val supergroupPeriodicSyncEnabled: Boolean =
        extra.optBoolean(TdExtensionProvider.KEY_SUPERGROUP_PERIODIC_SYNC, true)
    private val maxDownloadBytes: Long = (extra.optString(TdExtensionProvider.KEY_MAX_DOWNLOAD_MB)
        .toLongOrNull() ?: TdExtensionProvider.DEFAULT_MAX_DOWNLOAD_MB.toLong())
        .coerceIn(1L, 500L) * 1024L * 1024L

    @Volatile
    private var client: TdClientApi? = null

    @Volatile
    private var syncState: TdSyncStateStore? = null

    private val userCache = ConcurrentHashMap<Long, TdApi.User>()
    private val chatCache = ConcurrentHashMap<Long, TdApi.Chat>()
    private val pendingSends = ConcurrentHashMap<Pair<Long, Long>, CompletableDeferred<SendAck>>()
    private val locallySentMessageIds = ConcurrentHashMap<Pair<Long, Long>, Long>()
    private val recentlyEmittedMessageIds = ConcurrentHashMap<Pair<Long, Long>, Long>()
    private val lastMessageFallbackJobs = ConcurrentHashMap<Long, Job>()

    private val chatFocusMutex = Mutex()
    private val catchUpMutex = Mutex()

    @Volatile
    private var activeOpenChatId: Long? = null

    @Volatile
    private var selfUserId: Long? = null

    @Volatile
    private var isHostForeground: Boolean = true

    @Volatile
    private var sessionStartedAtMs: Long = 0L

    @Volatile
    private var floodWaitUntilMs: Long = 0L

    @Volatile
    private var lastDisconnectedAtMs: Long = 0L

    @Volatile
    private var lastCatchUpAtMs: Long = 0L

    private var periodicRotateOffset: Int = 0
    private var periodicSyncJob: Job? = null

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

        syncState = TdSyncStateStore(coordinator.storage.getSyncStateFile(accountId))

        // Apply latest proxy & network type config (skip NetworkTypeNone on init so we don't block startup)
        tdClient.applyProxy(proxyConfig)
        coordinator.currentNetworkType()
            ?.takeIf { it !is TdApi.NetworkTypeNone }
            ?.let { netType ->
                runCatching { tdClient.setNetworkType(netType) }
            }

        // Keep TDLib online so Telegram continues pushing live UpdateNewMessage events for
        // supergroups/channels while Parabox's foreground service maintains the persistent socket
        // (Parabox does not use FCM push notifications).
        runCatching {
            tdClient.send<TdApi.Ok>(TdApi.SetOption("online", TdApi.OptionValueBoolean(true)))
        }

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

        sessionStartedAtMs = System.currentTimeMillis()
        Log.i(TAG, "[$accountId] onInitialize ready: sessionId=$sessionId, syncInitialized=${syncState?.isInitialized()}")
        observeTdUpdates(tdClient)
        updateStatus(ParaboxConnectionStatus.Active)
        sessionScope.launch {
            warmUpAndCatchUp(tdClient, reason = "startup")
            startPeriodicSupergroupSync(tdClient)
        }
        return true
    }

    private fun observeTdUpdates(tdClient: TdClientApi) {
        sessionScope.launch {
            Log.i(TAG, "[$accountId] observeTdUpdates started")
            try {
                tdClient.updates.collect { update ->
                    try {
                        when (update) {
                            is TdApi.UpdateAuthorizationState -> handleAuthUpdate(update.authorizationState)
                            is TdApi.UpdateConnectionState -> handleConnectionUpdate(tdClient, update.state)
                            is TdApi.UpdateUser -> userCache[update.user.id] = update.user
                            is TdApi.UpdateNewChat -> chatCache[update.chat.id] = update.chat
                            is TdApi.UpdateChatLastMessage -> handleChatLastMessageUpdate(tdClient, update)
                            is TdApi.UpdateNewMessage -> {
                                // Launch on sessionScope (SupervisorJob) so a failure on one message
                                // can NEVER cancel the parent updates.collect loop.
                                sessionScope.launch {
                                    runCatching {
                                        handleNewMessage(tdClient, update.message)
                                    }.onFailure { err ->
                                        Log.e(TAG, "[$accountId] handleNewMessage failed for chat=${update.message.chatId} msg=${update.message.id}", err)
                                    }
                                }
                            }
                            is TdApi.UpdateMessageSendSucceeded -> {
                                val key = update.message.chatId to update.oldMessageId
                                if (pendingSends.containsKey(key) || isLocallySent(update.message.chatId, update.oldMessageId)) {
                                    recordLocallySent(update.message.chatId, update.message.id)
                                }
                                syncState?.advanceWatermark(update.message.chatId, update.message.id, scope = sessionScope)
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
                                    sessionScope.launch {
                                        runCatching {
                                            update.messageIds.forEach { msgId ->
                                                emitRecall(TdMessageMapper.formatMessageUuid(update.chatId, msgId))
                                            }
                                        }.onFailure { err ->
                                            Log.w(TAG, "[$accountId] emitRecall failed: ${err.message}")
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.e(TAG, "[$accountId] Error processing TDLib update ${update.javaClass.simpleName}", e)
                    }
                }
            } finally {
                Log.i(TAG, "[$accountId] observeTdUpdates ended")
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

    private fun handleConnectionUpdate(tdClient: TdClientApi, state: TdApi.ConnectionState) {
        Log.i(TAG, "[$accountId] ConnectionState -> ${state.javaClass.simpleName}")
        when (state) {
            is TdApi.ConnectionStateReady -> {
                updateStatus(ParaboxConnectionStatus.Active)
                val disconnectedAt = lastDisconnectedAtMs
                lastDisconnectedAtMs = 0L
                val now = System.currentTimeMillis()
                val focusedChatId = activeOpenChatId
                if (focusedChatId != null) {
                    sessionScope.launch {
                        runCatching { tdClient.send<TdApi.Ok>(TdApi.OpenChat(focusedChatId)) }
                        val watermark = syncState?.getWatermark(focusedChatId) ?: 0L
                        if (watermark > 0L) {
                            catchUpSingleChat(
                                tdClient = tdClient,
                                chatId = focusedChatId,
                                watermark = watermark,
                                limit = catchUpMessagesPerChat,
                                emitAsBackfill = false,
                            )
                        }
                    }
                }
                if (disconnectedAt > 0L &&
                    now - disconnectedAt >= reconnectCatchUpThresholdMs &&
                    now - lastCatchUpAtMs >= CATCH_UP_MIN_INTERVAL_MS
                ) {
                    sessionScope.launch {
                        warmUpAndCatchUp(tdClient, reason = "reconnect")
                    }
                }
            }
            is TdApi.ConnectionStateUpdating,
            is TdApi.ConnectionStateConnecting,
            is TdApi.ConnectionStateConnectingToProxy -> {
                if (lastDisconnectedAtMs == 0L) {
                    lastDisconnectedAtMs = System.currentTimeMillis()
                }
                updateStatus(ParaboxConnectionStatus.Initializing)
            }
            is TdApi.ConnectionStateWaitingForNetwork -> {
                if (lastDisconnectedAtMs == 0L) {
                    lastDisconnectedAtMs = System.currentTimeMillis()
                }
                updateStatus(ParaboxConnectionStatus.Reconnecting(1, "等待网络恢复"))
            }
        }
    }

    private fun handleChatLastMessageUpdate(
        tdClient: TdClientApi,
        update: TdApi.UpdateChatLastMessage,
    ) {
        chatCache[update.chatId]?.let { cached ->
            cached.lastMessage = update.lastMessage
        }
        val lastMsg = update.lastMessage ?: return
        if (lastMsg.sendingState != null) return
        if (isLocallySent(update.chatId, lastMsg.id) || wasRecentlyEmitted(update.chatId, lastMsg.id)) return
        val store = syncState ?: return
        if (!store.isInitialized()) return
        val watermark = store.getWatermark(update.chatId)
        val lastMsgTsMs = lastMsg.date.toLong() * 1000L
        if (watermark > 0L) {
            if (lastMsg.id <= watermark) return
        } else {
            // For chats not in the initial top-30 seed, still recover genuinely fresh messages
            // arriving after this session started rather than silently dropping them.
            if (lastMsgTsMs < sessionStartedAtMs - NEW_CHAT_GRACE_MS) return
        }

        // Safety net for Telegram channelDifferenceTooLong (>100 missed channel messages)
        // where TDLib skips individual UpdateNewMessage events and only fires UpdateChatLastMessage.
        // Emit as live messages (emitAsBackfill = false) so notifications fire for live channel updates.
        lastMessageFallbackJobs.remove(update.chatId)?.cancel()
        lastMessageFallbackJobs[update.chatId] = sessionScope.launch {
            if (lastMessageFallbackDebounceMs > 0L) {
                delay(lastMessageFallbackDebounceMs)
            }
            val currentWatermark = store.getWatermark(update.chatId)
            if ((currentWatermark > 0L && lastMsg.id <= currentWatermark) ||
                isLocallySent(update.chatId, lastMsg.id) ||
                wasRecentlyEmitted(update.chatId, lastMsg.id)
            ) {
                return@launch
            }
            val recovered = catchUpSingleChat(
                tdClient = tdClient,
                chatId = update.chatId,
                watermark = currentWatermark,
                limit = FALLBACK_GAP_LIMIT,
                minTimestampMs = if (currentWatermark == 0L) sessionStartedAtMs - NEW_CHAT_GRACE_MS else 0L,
                emitAsBackfill = false,
            )
            if (recovered > 0) {
                Log.i(TAG, "[$accountId] UpdateChatLastMessage fallback recovered $recovered live msgs for chat=${update.chatId}")
            }
        }
    }

    private fun startPeriodicSupergroupSync(tdClient: TdClientApi) {
        if (!supergroupPeriodicSyncEnabled) {
            Log.i(TAG, "[$accountId] Periodic supergroup sync disabled by config")
            return
        }
        if (periodicSyncForegroundIntervalMs <= 0L && periodicSyncBackgroundIntervalMs <= 0L) return
        periodicSyncJob?.cancel()
        periodicSyncJob = sessionScope.launch {
            Log.i(
                TAG,
                "[$accountId] Periodic supergroup sync started (fg=${periodicSyncForegroundIntervalMs}ms, bg=${periodicSyncBackgroundIntervalMs}ms)"
            )
            while (client === tdClient) {
                val intervalMs = if (isHostForeground) {
                    periodicSyncForegroundIntervalMs
                } else {
                    periodicSyncBackgroundIntervalMs
                }.coerceAtLeast(1_000L)
                delay(intervalMs)
                if (client !== tdClient) break
                val now = System.currentTimeMillis()
                if (now < floodWaitUntilMs) {
                    Log.w(TAG, "[$accountId] Skipping periodic sync due to FloodWait (${(floodWaitUntilMs - now) / 1000}s left)")
                    continue
                }
                if (tdClient.connectionState.value !is TdApi.ConnectionStateReady) continue
                runCatching {
                    runPeriodicSupergroupSyncCycle(tdClient)
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    Log.w(TAG, "[$accountId] Periodic supergroup sync cycle error: ${err.message}")
                }
            }
        }
    }

    /**
     * Periodically probes unopened supergroups/channels using a gentle serialized
     * `OpenChat -> GetChatHistory(limit=5) -> CloseChat` sweep.
     *
     * Per TDLib's architecture, Telegram servers withhold real-time `UpdateNewMessage` pushes for
     * unopened large supergroups/channels. Probing top recent supergroups + a rotating window of
     * remaining chats ensures timely message delivery & notifications without keeping dozens of
     * chats permanently open or hitting Telegram FloodWait limits.
     */
    internal suspend fun runPeriodicSupergroupSyncCycle(tdClient: TdClientApi): Int {
        val store = syncState ?: return 0
        if (!store.isInitialized()) return 0

        val chatsResp = runCatching {
            tdClient.send<TdApi.Chats>(TdApi.GetChats(TdApi.ChatListMain(), catchUpChatLimit))
        }.getOrNull() ?: return 0

        val supergroupIds = ArrayList<Long>()
        for (chatId in chatsResp.chatIds) {
            if (chatId == activeOpenChatId) continue
            val chat = getOrFetchChat(tdClient, chatId) ?: continue
            if (chat.type is TdApi.ChatTypeSupergroup) {
                supergroupIds.add(chatId)
            }
        }
        if (supergroupIds.isEmpty()) return 0

        // Always probe top 10 most recent supergroups + a rotating slice of up to 10 from the rest
        val prioritySlice = supergroupIds.take(PERIODIC_PRIORITY_COUNT)
        val tail = if (supergroupIds.size > PERIODIC_PRIORITY_COUNT) {
            supergroupIds.drop(PERIODIC_PRIORITY_COUNT)
        } else {
            emptyList()
        }
        val rotatingSlice = if (tail.isNotEmpty()) {
            val start = periodicRotateOffset % tail.size
            periodicRotateOffset = (start + PERIODIC_ROTATING_COUNT) % tail.size
            List(minOf(PERIODIC_ROTATING_COUNT, tail.size)) { idx ->
                tail[(start + idx) % tail.size]
            }
        } else {
            emptyList()
        }

        val targets = prioritySlice + rotatingSlice
        var totalRecovered = 0
        for (chatId in targets) {
            if (client !== tdClient) break
            if (System.currentTimeMillis() < floodWaitUntilMs) break
            if (chatId == activeOpenChatId) continue

            val recovered = probeSingleSupergroup(tdClient, chatId, store)
            totalRecovered += recovered
            if (periodicSyncProbePacingMs > 0L) {
                delay(periodicSyncProbePacingMs)
            }
        }
        if (totalRecovered > 0) {
            Log.i(TAG, "[$accountId] Periodic supergroup sync probed ${targets.size} chats, delivered $totalRecovered new messages")
        }
        return totalRecovered
    }

    private suspend fun probeSingleSupergroup(
        tdClient: TdClientApi,
        chatId: Long,
        store: TdSyncStateStore,
    ): Int {
        var openedByProbe = false
        try {
            // Only open if the user is not already focusing this chat
            chatFocusMutex.withLock {
                if (activeOpenChatId == chatId) return 0
                tdClient.send<TdApi.Ok>(TdApi.OpenChat(chatId))
                openedByProbe = true
            }

            // Trigger server history check (forces fresh messages even when channelDifferenceTooLong)
            val response = tdClient.send<TdApi.Messages>(
                TdApi.GetChatHistory(chatId, 0L, 0, PERIODIC_PROBE_FETCH_LIMIT, false)
            )
            val batch = response.messages?.filterNotNull().orEmpty()
            if (batch.isEmpty()) return 0

            val watermark = store.getWatermark(chatId)
            val newestId = batch.maxOfOrNull { it.id } ?: 0L
            if (watermark > 0L && newestId <= watermark) {
                return 0
            }

            // If we have a known watermark and the entire 5-message probe batch is still newer than
            // watermark, run a deeper paginated catch-up so no messages in the gap are skipped.
            val oldestInBatch = batch.minOfOrNull { it.id } ?: 0L
            if (watermark > 0L && batch.size >= PERIODIC_PROBE_FETCH_LIMIT && oldestInBatch > watermark) {
                return catchUpSingleChat(
                    tdClient = tdClient,
                    chatId = chatId,
                    watermark = watermark,
                    limit = catchUpMessagesPerChat,
                    emitAsBackfill = false,
                )
            }

            val cutoffMs = if (watermark == 0L) sessionStartedAtMs - NEW_CHAT_GRACE_MS else 0L
            val candidates = batch
                .asSequence()
                .filter { msg -> watermark == 0L || msg.id > watermark }
                .filter { msg -> cutoffMs <= 0L || msg.date.toLong() * 1000L >= cutoffMs }
                .filter { msg -> msg.sendingState == null }
                .filterNot { msg -> isLocallySent(msg.chatId, msg.id) || wasRecentlyEmitted(msg.chatId, msg.id) }
                .sortedWith(compareBy<TdApi.Message> { it.date }.thenBy { it.id })
                .toList()

            if (candidates.isEmpty()) {
                if (newestId > watermark) {
                    store.advanceWatermark(chatId, newestId, scope = sessionScope)
                }
                return 0
            }

            var delivered = 0
            for (msg in candidates) {
                if (wasRecentlyEmitted(msg.chatId, msg.id)) continue
                recordEmitted(msg.chatId, msg.id)
                val receiveMessage = buildReceiveMessage(tdClient, msg) ?: continue
                val ack = emitMessage(receiveMessage, isBackfill = false)
                if (ack.code == ParaboxResult.SUCCESS) {
                    store.advanceWatermark(msg.chatId, msg.id, scope = sessionScope)
                    delivered += 1
                } else {
                    recentlyEmittedMessageIds.remove(msg.chatId to msg.id)
                    break
                }
            }
            return delivered
        } catch (e: TdErrorException) {
            if (e.code == 429) {
                val retrySec = parseFloodWaitSeconds(e.message)
                floodWaitUntilMs = System.currentTimeMillis() + (retrySec + 5L) * 1000L
                Log.w(TAG, "[$accountId] FloodWait 429 during supergroup probe: pausing for ${retrySec + 5}s (${e.message})")
            } else {
                Log.d(TAG, "[$accountId] probeSingleSupergroup chat=$chatId error (${e.code}): ${e.message}")
            }
            return 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "[$accountId] probeSingleSupergroup chat=$chatId failed: ${e.message}")
            return 0
        } finally {
            if (openedByProbe) {
                chatFocusMutex.withLock {
                    // Never close if the user entered this exact chat while the probe was running
                    if (activeOpenChatId != chatId) {
                        runCatching { tdClient.send<TdApi.Ok>(TdApi.CloseChat(chatId)) }
                    }
                }
            }
        }
    }

    private fun parseFloodWaitSeconds(message: String): Long {
        val match = FLOOD_WAIT_REGEX.find(message)
        return match?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceIn(10L, 600L) ?: 60L
    }

    internal suspend fun warmUpAndCatchUp(tdClient: TdClientApi, reason: String) {
        val store = syncState ?: return
        // Wait for TDLib to reach ConnectionStateReady so LoadChats / GetChat / GetChatHistory
        // query live Telegram server state instead of stale local cache during ConnectionStateConnecting.
        if (tdClient.connectionState.value !is TdApi.ConnectionStateReady) {
            Log.i(TAG, "[$accountId] Catch-up ($reason) waiting for ConnectionStateReady (current=${tdClient.connectionState.value?.javaClass?.simpleName})")
            val ready = withTimeoutOrNull(CATCH_UP_READY_TIMEOUT_MS) {
                tdClient.connectionState.first { it is TdApi.ConnectionStateReady }
            }
            if (ready == null) {
                Log.w(TAG, "[$accountId] Catch-up ($reason) timed out waiting for ConnectionStateReady")
                return
            }
        }
        catchUpMutex.withLock {
            loadMainChats(tdClient, targetLimit = maxOf(catchUpChatLimit, 50))
            val chats = runCatching {
                tdClient.send<TdApi.Chats>(TdApi.GetChats(TdApi.ChatListMain(), catchUpChatLimit))
            }.getOrNull() ?: return@withLock

            val nowMs = System.currentTimeMillis()
            if (!store.isInitialized()) {
                val seeds = LinkedHashMap<Long, Long>()
                for (chatId in chats.chatIds) {
                    val chat = fetchFreshChat(tdClient, chatId) ?: continue
                    val lastId = chat.lastMessage?.id ?: 0L
                    if (lastId > 0L) {
                        seeds[chatId] = lastId
                    }
                }
                store.markInitialized(seeds, nowMs = nowMs, scope = sessionScope)
                lastCatchUpAtMs = nowMs
                Log.d(TAG, "[$accountId] Seeded initial sync watermarks for ${seeds.size} chats ($reason)")
                return@withLock
            }

            val previousActiveMs = store.lastActiveMs()
            val newChatCutoffMs = (previousActiveMs - NEW_CHAT_GRACE_MS).coerceAtLeast(0L)
            val emitAsBackfill = reason == "startup"
            var backfilledCount = 0
            for (chatId in chats.chatIds) {
                val chat = fetchFreshChat(tdClient, chatId) ?: continue
                val lastMsg = chat.lastMessage ?: continue
                val watermark = store.getWatermark(chatId)
                val lastMsgTsMs = lastMsg.date.toLong() * 1000L

                if (watermark > 0L) {
                    if (lastMsg.id <= watermark) continue
                    backfilledCount += catchUpSingleChat(
                        tdClient = tdClient,
                        chatId = chatId,
                        watermark = watermark,
                        limit = catchUpMessagesPerChat,
                        emitAsBackfill = emitAsBackfill,
                    )
                } else {
                    if (previousActiveMs > 0L && lastMsgTsMs >= newChatCutoffMs) {
                        backfilledCount += catchUpSingleChat(
                            tdClient = tdClient,
                            chatId = chatId,
                            watermark = 0L,
                            limit = catchUpMessagesPerChat,
                            minTimestampMs = newChatCutoffMs,
                            emitAsBackfill = emitAsBackfill,
                        )
                    } else if (lastMsg.id > 0L) {
                        store.advanceWatermark(chatId, lastMsg.id, nowMs = nowMs, scope = sessionScope)
                    }
                }
                if (catchUpChatThrottleMs > 0L) {
                    delay(catchUpChatThrottleMs)
                }
            }
            store.touchActive(nowMs = nowMs, scope = sessionScope)
            lastCatchUpAtMs = System.currentTimeMillis()
            Log.i(TAG, "[$accountId] Catch-up ($reason) checked ${chats.chatIds.size} chats, synced $backfilledCount messages (isBackfill=$emitAsBackfill)")
        }
    }

    private suspend fun loadMainChats(tdClient: TdClientApi, targetLimit: Int) {
        val batchSize = targetLimit.coerceIn(10, 100)
        repeat(LOAD_CHATS_MAX_ROUNDS) {
            try {
                tdClient.send<TdApi.Ok>(TdApi.LoadChats(TdApi.ChatListMain(), batchSize))
            } catch (e: TdErrorException) {
                if (e.code == 404) return
                Log.w(TAG, "[$accountId] LoadChats returned error (${e.code}): ${e.message}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "[$accountId] LoadChats failed: ${e.message}")
                return
            }
        }
    }

    private suspend fun catchUpSingleChat(
        tdClient: TdClientApi,
        chatId: Long,
        watermark: Long,
        limit: Int,
        minTimestampMs: Long = 0L,
        emitAsBackfill: Boolean = true,
    ): Int {
        val store = syncState ?: return 0
        val history = fetchChatHistoryLoop(
            tdClient = tdClient,
            chatId = chatId,
            fromMessageId = 0L,
            limit = limit,
            stopAtMessageId = watermark,
        )
        if (history.isEmpty()) return 0

        val candidates = history
            .asSequence()
            .filter { msg -> msg.id > watermark }
            .filter { msg -> minTimestampMs <= 0L || msg.date.toLong() * 1000L >= minTimestampMs }
            .filter { msg -> msg.sendingState == null }
            .filterNot { msg -> isLocallySent(msg.chatId, msg.id) || wasRecentlyEmitted(msg.chatId, msg.id) }
            .sortedWith(compareBy<TdApi.Message> { it.date }.thenBy { it.id })
            .toList()

        if (candidates.isEmpty()) {
            val highestId = history.maxOfOrNull { it.id } ?: 0L
            if (highestId > watermark) {
                store.advanceWatermark(chatId, highestId, scope = sessionScope)
            }
            return 0
        }

        var synced = 0
        for (msg in candidates) {
            val receiveMessage = buildReceiveMessage(tdClient, msg) ?: continue
            val ack = emitMessage(receiveMessage, isBackfill = emitAsBackfill)
            if (ack.code == ParaboxResult.SUCCESS) {
                recordEmitted(msg.chatId, msg.id)
                store.advanceWatermark(msg.chatId, msg.id, scope = sessionScope)
                synced += 1
            } else {
                Log.w(TAG, "[$accountId] catchUpSingleChat ACK failed for chat=$chatId msg=${msg.id}: code=${ack.code} ${ack.message}")
                // Stop advancing this chat's watermark if the host rejected or disconnected
                break
            }
        }
        return synced
    }

    internal suspend fun fetchChatHistoryLoop(
        tdClient: TdClientApi,
        chatId: Long,
        fromMessageId: Long,
        limit: Int,
        stopAtMessageId: Long = 0L,
        maxRounds: Int = HISTORY_LOOP_MAX_ROUNDS,
    ): List<TdApi.Message> {
        val target = limit.coerceIn(1, MAX_SYNC_LATEST_LIMIT)
        val collected = ArrayList<TdApi.Message>(target)
        val seenIds = HashSet<Long>(target * 2)
        var cursorId = fromMessageId

        for (round in 0 until maxRounds) {
            val remaining = target - collected.size
            if (remaining <= 0) break
            val response = runCatching {
                tdClient.send<TdApi.Messages>(
                    TdApi.GetChatHistory(chatId, cursorId, 0, remaining, false)
                )
            }.getOrNull() ?: break

            val batch = response.messages?.filterNotNull().orEmpty()
            if (batch.isEmpty()) break

            var reachedStop = false
            var addedInRound = 0
            for (msg in batch) {
                if (stopAtMessageId > 0L && msg.id <= stopAtMessageId) {
                    reachedStop = true
                    break
                }
                if (seenIds.add(msg.id)) {
                    collected.add(msg)
                    addedInRound += 1
                    if (collected.size >= target) break
                }
            }
            if (reachedStop || collected.size >= target) break

            val nextCursor = batch.last().id
            if (nextCursor == 0L || nextCursor == cursorId || addedInRound == 0) break
            cursorId = nextCursor
        }
        return collected
    }

    internal suspend fun handleNewMessage(tdClient: TdClientApi, message: TdApi.Message) {
        if (!shouldSyncUpdateNewMessage(message)) {
            Log.d(TAG, "[$accountId] Skip UpdateNewMessage chat=${message.chatId} msg=${message.id} outgoing=${message.isOutgoing} pending=${message.sendingState != null}")
            return
        }
        if (wasRecentlyEmitted(message.chatId, message.id)) return
        lastMessageFallbackJobs.remove(message.chatId)?.cancel()
        recordEmitted(message.chatId, message.id)
        val receiveMessage = buildReceiveMessage(tdClient, message) ?: return
        val ack = emitMessage(receiveMessage, isBackfill = false)
        Log.i(TAG, "[$accountId] UpdateNewMessage emitted chat=${message.chatId} msg=${message.id} outgoing=${message.isOutgoing} ack=${ack.code}")
        if (ack.code == ParaboxResult.SUCCESS) {
            syncState?.advanceWatermark(message.chatId, message.id, scope = sessionScope)
        } else {
            recentlyEmittedMessageIds.remove(message.chatId to message.id)
        }
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

    private fun recordEmitted(chatId: Long, messageId: Long) {
        if (chatId == 0L || messageId == 0L) return
        val now = System.currentTimeMillis()
        recentlyEmittedMessageIds[chatId to messageId] = now
        if (recentlyEmittedMessageIds.size > RECENTLY_EMITTED_MAX_ENTRIES) {
            val cutoff = now - RECENTLY_EMITTED_TTL_MS
            recentlyEmittedMessageIds.entries.removeIf { it.value < cutoff }
        }
    }

    private fun wasRecentlyEmitted(chatId: Long, messageId: Long): Boolean {
        val recordedAt = recentlyEmittedMessageIds[chatId to messageId] ?: return false
        if (System.currentTimeMillis() - recordedAt > RECENTLY_EMITTED_TTL_MS) {
            recentlyEmittedMessageIds.remove(chatId to messageId, recordedAt)
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

            val finalUuid = lastConfirmedId?.let { confirmedId ->
                syncState?.advanceWatermark(chatId, confirmedId, scope = sessionScope)
                TdMessageMapper.formatMessageUuid(chatId, confirmedId)
            }
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
        val history = fetchChatHistoryLoop(
            tdClient = tdClient,
            chatId = chatId,
            fromMessageId = fromMsgId,
            limit = 20,
        )
        return history
            .filter { it.sendingState == null }
            .mapNotNull { msg -> buildReceiveMessage(tdClient, msg) }
    }

    override suspend fun onSyncChatLatestMessages(chatUid: String, limit: Int): List<ReceiveMessage> {
        val tdClient = client ?: return emptyList()
        val chatId = chatUid.toLongOrNull() ?: return emptyList()
        val safeLimit = limit.coerceIn(1, MAX_SYNC_LATEST_LIMIT)
        // Ensure chat metadata is available before fetching history
        getOrFetchChat(tdClient, chatId)
        val rawMessages = fetchChatHistoryLoop(
            tdClient = tdClient,
            chatId = chatId,
            fromMessageId = 0L,
            limit = safeLimit,
        )
        if (rawMessages.isEmpty()) return emptyList()

        val sorted = rawMessages
            .filter { it.sendingState == null }
            .sortedWith(compareBy<TdApi.Message> { it.date }.thenBy { it.id })

        val mapped = sorted.mapNotNull { msg ->
            buildReceiveMessage(tdClient, msg)
        }
        val highestId = sorted.maxOfOrNull { it.id } ?: 0L
        if (highestId > 0L) {
            syncState?.advanceWatermark(chatId, highestId, scope = sessionScope)
        }
        Log.i(TAG, "[$accountId] onSyncChatLatestMessages chat=$chatId limit=$safeLimit returned=${mapped.size}")
        return mapped
    }

    override suspend fun onChatFocusChanged(chatUid: String, opened: Boolean) {
        val tdClient = client ?: return
        val chatId = chatUid.toLongOrNull() ?: return
        chatFocusMutex.withLock {
            if (opened) {
                val previous = activeOpenChatId
                if (previous != null && previous != chatId) {
                    Log.i(TAG, "[$accountId] CloseChat($previous) before switching to $chatId")
                    runCatching { tdClient.send<TdApi.Ok>(TdApi.CloseChat(previous)) }
                }
                activeOpenChatId = chatId
                Log.i(TAG, "[$accountId] OpenChat($chatId)")
                runCatching { tdClient.send<TdApi.Ok>(TdApi.OpenChat(chatId)) }
            } else if (activeOpenChatId == chatId) {
                activeOpenChatId = null
                Log.i(TAG, "[$accountId] CloseChat($chatId)")
                runCatching { tdClient.send<TdApi.Ok>(TdApi.CloseChat(chatId)) }
            }
        }
    }

    override suspend fun onLifecycleEvent(event: LifecycleEventProto) {
        val tdClient = client ?: return
        Log.d(TAG, "[$accountId] onLifecycleEvent: $event")
        when (event) {
            LifecycleEventProto.LIFECYCLE_RESUME -> {
                isHostForeground = true
                // Keep online=true (do NOT call setNetworkType here as it forces TDLib to tear down
                // and reopen healthy MTProto sockets on every app resume).
                runCatching {
                    tdClient.send<TdApi.Ok>(TdApi.SetOption("online", TdApi.OptionValueBoolean(true)))
                }
                activeOpenChatId?.let { openId ->
                    runCatching { tdClient.send<TdApi.Ok>(TdApi.OpenChat(openId)) }
                }
            }
            LifecycleEventProto.LIFECYCLE_PAUSE,
            LifecycleEventProto.LIFECYCLE_STOP -> {
                isHostForeground = false
                // Do NOT set online=false here: without FCM push notifications, setting online=false
                // puts TDLib into background/idle mode and suppresses/throttles live updates for
                // supergroups and channels.
                syncState?.flushNow()
            }
            else -> Unit
        }
    }

    override suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? {
        val tdClient = client ?: return null
        val user = if (userId == SELF_USER_ID) {
            val cachedSelfId = selfUserId
            if (cachedSelfId != null) {
                getOrFetchUser(tdClient, cachedSelfId)
            } else {
                runCatching { tdClient.send<TdApi.User>(TdApi.GetMe()) }.getOrNull()?.also { me ->
                    selfUserId = me.id
                    userCache[me.id] = me
                }
            }
        } else {
            val uid = userId.toLongOrNull() ?: return null
            getOrFetchUser(tdClient, uid)
        } ?: return null
        val avatar = if (user.profilePhoto?.small != null) {
            TdResourceUri.userAvatar(sessionId, user.id)
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
        periodicSyncJob?.cancel()
        periodicSyncJob = null
        lastMessageFallbackJobs.values.forEach { it.cancel() }
        lastMessageFallbackJobs.clear()
        val active = client
        val openChat = activeOpenChatId
        activeOpenChatId = null
        if (active != null && openChat != null) {
            runCatching { active.send<TdApi.Ok>(TdApi.CloseChat(openChat)) }
        }
        syncState?.flushNow()
        syncState = null
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
        recentlyEmittedMessageIds.clear()
    }

    private suspend fun getOrFetchUser(tdClient: TdClientApi, userId: Long): TdApi.User? {
        userCache[userId]?.let { return it }
        return runCatching { tdClient.send<TdApi.User>(TdApi.GetUser(userId)) }.getOrNull()?.also {
            userCache[userId] = it
        }
    }

    private suspend fun getOrFetchChat(tdClient: TdClientApi, chatId: Long): TdApi.Chat? {
        chatCache[chatId]?.let { return it }
        return fetchFreshChat(tdClient, chatId)
    }

    private suspend fun fetchFreshChat(tdClient: TdClientApi, chatId: Long): TdApi.Chat? {
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
        private const val RECENTLY_EMITTED_TTL_MS = 5 * 60 * 1000L
        private const val RECENTLY_EMITTED_MAX_ENTRIES = 1024
        private const val DEFAULT_CATCH_UP_CHAT_LIMIT = 30
        private const val DEFAULT_CATCH_UP_MESSAGES_PER_CHAT = 30
        private const val DEFAULT_CATCH_UP_THROTTLE_MS = 50L
        private const val DEFAULT_LAST_MESSAGE_FALLBACK_DEBOUNCE_MS = 1_500L
        private const val DEFAULT_RECONNECT_CATCH_UP_THRESHOLD_MS = 15_000L
        private const val CATCH_UP_READY_TIMEOUT_MS = 20_000L
        private const val CATCH_UP_MIN_INTERVAL_MS = 10_000L
        private const val NEW_CHAT_GRACE_MS = 60_000L
        private const val FALLBACK_GAP_LIMIT = 20
        private const val LOAD_CHATS_MAX_ROUNDS = 3
        private const val HISTORY_LOOP_MAX_ROUNDS = 6
        private const val MAX_SYNC_LATEST_LIMIT = 100
        private const val DEFAULT_PERIODIC_SYNC_FOREGROUND_MS = 25_000L
        private const val DEFAULT_PERIODIC_SYNC_BACKGROUND_MS = 60_000L
        private const val DEFAULT_PERIODIC_SYNC_PROBE_PACING_MS = 350L
        private const val PERIODIC_PRIORITY_COUNT = 10
        private const val PERIODIC_ROTATING_COUNT = 10
        private const val PERIODIC_PROBE_FETCH_LIMIT = 5
        private val FLOOD_WAIT_REGEX = Regex("retry after (\\d+)", RegexOption.IGNORE_CASE)
    }
}
