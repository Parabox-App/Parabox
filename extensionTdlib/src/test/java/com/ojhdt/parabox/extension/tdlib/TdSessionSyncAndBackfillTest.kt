package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdErrorException
import com.ojhdt.parabox.extension.tdlib.core.TdNetworkMonitor
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.core.TdSyncStateStore
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSessionEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList

class TdSessionSyncAndBackfillTest {

    private class ChannelBackedFakeTdClient(
        override val accountId: String = "acct-backfill",
        private val onFunction: (TdApi.Function<*>) -> TdApi.Object,
    ) : TdClientApi {
        val updateChannel = Channel<TdApi.Object>(Channel.UNLIMITED)
        override val updates: Flow<TdApi.Object> = updateChannel.receiveAsFlow()
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(TdApi.AuthorizationStateReady())
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())
        val recordedFunctions = CopyOnWriteArrayList<TdApi.Function<*>>()

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object {
            recordedFunctions.add(function)
            return when (function) {
                is TdApi.GetMe -> TdApi.User().apply {
                    id = 100L
                    firstName = "Me"
                }
                is TdApi.GetUser -> TdApi.User().apply {
                    id = function.userId
                    firstName = "User-${function.userId}"
                }
                else -> onFunction(function)
            }
        }

        override suspend fun applyProxy(proxyConfig: TdProxyConfig) = Unit
        override suspend fun closeAndWait(timeoutMs: Long) {
            updateChannel.close()
        }
    }

    private val stubMediaBridge = object : TdMediaBridge {
        override suspend fun downloadAndShareFile(
            client: TdClientApi,
            tdFile: TdApi.File,
            hostPackageName: String,
            maxSizeBytes: Long,
            timeoutMs: Long,
        ): ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo

        override suspend fun stageResourceForUpload(
            accountId: String,
            resourceInfo: ParaboxResourceInfo,
            suggestedName: String?,
        ): File? = null
    }

    private fun makeTextMessage(
        chatId: Long,
        msgId: Long,
        dateSec: Int,
        text: String = "msg-$msgId",
        senderUserId: Long = 200L,
        isOutgoing: Boolean = false,
    ): TdApi.Message = TdApi.Message().apply {
        this.id = msgId
        this.chatId = chatId
        this.date = dateSec
        this.isOutgoing = isOutgoing
        this.sendingState = null
        this.senderId = TdApi.MessageSenderUser(senderUserId)
        this.content = TdApi.MessageText(TdApi.FormattedText(text, emptyArray()), null, null)
    }

    private fun makeChat(chatId: Long, lastMsg: TdApi.Message? = null): TdApi.Chat = TdApi.Chat().apply {
        id = chatId
        title = "Chat-$chatId"
        type = TdApi.ChatTypeSupergroup(chatId, false)
        lastMessage = lastMsg
    }

    private fun attachSession(
        session: TdSession,
        emitted: MutableList<Pair<ReceiveMessage, Boolean>>,
        ackProvider: (ReceiveMessage, Boolean) -> ParaboxResult = { _, _ ->
            ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
        },
    ): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val emitter = object : ParaboxSessionEmitter {
            override suspend fun emitMessage(message: ReceiveMessage, isBackfill: Boolean): ParaboxResult {
                emitted.add(message to isBackfill)
                return ackProvider(message, isBackfill)
            }

            override suspend fun emitRecall(messageUuid: String): ParaboxResult =
                ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)

            override fun emitStatus(status: ParaboxConnectionStatus) = Unit
        }
        val attachMethod = ParaboxSession::class.java.getDeclaredMethod(
            "attach\$Parabox_Development_Kit_debug",
            CoroutineScope::class.java,
            ParaboxSessionEmitter::class.java,
        )
        attachMethod.isAccessible = true
        attachMethod.invoke(session, scope, emitter)
        return scope
    }

    private fun createEnv(
        client: TdClientApi,
        root: File = Files.createTempDirectory("td_sync_backfill_").toFile(),
        lastMessageDebounceMs: Long = 20L,
        reconnectThresholdMs: Long = 0L,
    ): Pair<TdAccountStorage, TdSession> {
        val storage = TdAccountStorage(root)
        val factory = object : TdClientFactory {
            override fun create(
                accountId: String,
                directories: TdAccountDirectories,
                apiId: Int,
                apiHash: String,
                proxyConfig: TdProxyConfig,
            ): TdClientApi = client
        }
        val coordinator = TdClientCoordinator(storage, factory, 12345, "hash")
        val session = TdSession(
            sessionId = "conn-1",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, client.accountId),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = coordinator,
            mediaBridge = stubMediaBridge,
            catchUpChatLimit = 30,
            catchUpMessagesPerChat = 30,
            catchUpChatThrottleMs = 0L,
            lastMessageFallbackDebounceMs = lastMessageDebounceMs,
            reconnectCatchUpThresholdMs = reconnectThresholdMs,
            periodicSyncForegroundIntervalMs = 0L,
            periodicSyncBackgroundIntervalMs = 0L,
            periodicSyncProbePacingMs = 0L,
        )
        return storage to session
    }

    @Test
    fun channelBackedEarlyUpdatesAndBurstAreNotDropped() = runBlocking {
        val totalMessages = 300
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(fn.chatId)
                else -> TdApi.Ok()
            }
        }
        // Enqueue 300 UpdateNewMessage events BEFORE TdSession.onInitialize() even subscribes!
        for (i in 1..totalMessages) {
            client.updateChannel.trySend(
                TdApi.UpdateNewMessage(makeTextMessage(200L, i.toLong(), 1700000000 + i))
            )
        }

        val (_, session) = createEnv(client)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            val deadline = System.currentTimeMillis() + 5_000L
            while (emitted.size < totalMessages && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertEquals(totalMessages, emitted.size)
            assertTrue(emitted.all { !it.second })
            val uuids = emitted.map { it.first.uuid }.toSet()
            assertEquals(totalMessages, uuids.size)
            assertTrue("200:1" in uuids)
            assertTrue("200:$totalMessages" in uuids)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun syncChatLatestMessagesPaginatesSingleMessageChunksAndOrdersAscending() = runBlocking {
        // Server has messages 50, 49, 48, 47, 46 (newest -> oldest)
        val serverHistory = listOf(
            makeTextMessage(200L, 50L, 1700000050),
            makeTextMessage(200L, 49L, 1700000049),
            makeTextMessage(200L, 48L, 1700000048),
            makeTextMessage(200L, 47L, 1700000047),
            makeTextMessage(200L, 46L, 1700000046),
        )
        var historyCalls = 0
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(fn.chatId, serverHistory.first())
                is TdApi.GetChatHistory -> {
                    historyCalls += 1
                    // Simulate TDLib returning only 1 message on round 1 (local cache), then 2 per round
                    val startIdx = if (fn.fromMessageId == 0L) {
                        0
                    } else {
                        serverHistory.indexOfFirst { it.id == fn.fromMessageId }.let { idx ->
                            if (idx >= 0) idx + 1 else serverHistory.size
                        }
                    }
                    val chunkSize = if (historyCalls == 1) 1 else 2
                    val slice = serverHistory.drop(startIdx).take(minOf(chunkSize, fn.limit))
                    TdApi.Messages(slice.size, slice.toTypedArray())
                }
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            val latest = session.onSyncChatLatestMessages("200", 5)
            assertEquals(listOf("200:46", "200:47", "200:48", "200:49", "200:50"), latest.map { it.uuid })
            assertEquals(3, historyCalls)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun startupCatchUpBackfillsMissedMessagesAfterProcessKillAndRespectsAck() = runBlocking {
        val root = Files.createTempDirectory("td_catchup_test_").toFile()
        val storage = TdAccountStorage(root)
        // Simulate previous session persisted watermark 100 for chat 200 before process was killed
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(200L to 100L), nowMs = 1_700_000_000_000L)

        val missedMessages = listOf(
            makeTextMessage(200L, 103L, 1700000103),
            makeTextMessage(200L, 102L, 1700000102),
            makeTextMessage(200L, 101L, 1700000101),
            makeTextMessage(200L, 100L, 1700000100), // already synced
        )
        var loadChatsCalls = 0
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> {
                    loadChatsCalls += 1
                    if (loadChatsCalls == 1) TdApi.Ok() else throw TdErrorException(404, "Not Found")
                }
                is TdApi.GetChats -> TdApi.Chats(1, longArrayOf(200L))
                is TdApi.GetChat -> makeChat(200L, missedMessages.first())
                is TdApi.GetChatHistory -> TdApi.Messages(missedMessages.size, missedMessages.toTypedArray())
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client, root = root)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            val deadline = System.currentTimeMillis() + 3_000L
            while (emitted.size < 3 && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertEquals(listOf("200:101", "200:102", "200:103"), emitted.map { it.first.uuid })
            assertTrue("Startup catch-up must emit with isBackfill=true", emitted.all { it.second })
            assertTrue("LoadChats must be called on startup", loadChatsCalls >= 1)
        } finally {
            session.onClose()
            scope.cancel()
        }

        // Verify persisted watermark advanced to 103 on close
        val reloaded = TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
        assertEquals(103L, reloaded.getWatermark(200L))
    }

    @Test
    fun freshLoginSeedsWatermarksWithoutHistoryFlood() = runBlocking {
        val root = Files.createTempDirectory("td_fresh_seed_").toFile()
        val oldMsg = makeTextMessage(200L, 500L, 1600000000)
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(1, longArrayOf(200L))
                is TdApi.GetChat -> makeChat(200L, oldMsg)
                else -> TdApi.Ok()
            }
        }

        val (storage, session) = createEnv(client, root = root)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(150)
            assertTrue("Fresh login should not dump old history", emitted.isEmpty())
        } finally {
            session.onClose()
            scope.cancel()
        }

        val reloaded = TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
        assertTrue(reloaded.isInitialized())
        assertEquals(500L, reloaded.getWatermark(200L))
    }

    @Test
    fun updateChatLastMessageFallbackRecoversSkippedChannelUpdates() = runBlocking {
        val root = Files.createTempDirectory("td_channel_gap_").toFile()
        val storage = TdAccountStorage(root)
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(300L to 200L), nowMs = 1_700_000_000_000L)

        val gapMessages = listOf(
            makeTextMessage(300L, 202L, 1700000202),
            makeTextMessage(300L, 201L, 1700000201),
            makeTextMessage(300L, 200L, 1700000200),
        )
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(300L, gapMessages.first())
                is TdApi.GetChatHistory -> TdApi.Messages(gapMessages.size, gapMessages.toTypedArray())
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client, root = root, lastMessageDebounceMs = 30L)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(80)
            // TDLib skips UpdateNewMessage and only sends UpdateChatLastMessage
            client.updateChannel.trySend(TdApi.UpdateChatLastMessage(300L, gapMessages.first(), emptyArray()))

            val deadline = System.currentTimeMillis() + 3_000L
            while (emitted.size < 2 && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertEquals(listOf("300:201", "300:202"), emitted.map { it.first.uuid })
            assertTrue("Live channel fallback should emit with isBackfill=false so notifications fire", emitted.all { !it.second })
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun chatFocusLifecycleCallsOpenCloseAndKeepsOnlineTrueOnPause() = runBlocking {
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(fn.chatId)
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            session.onChatFocusChanged("200", opened = true)
            session.onChatFocusChanged("300", opened = true)
            session.onLifecycleEvent(LifecycleEventProto.LIFECYCLE_RESUME)
            session.onLifecycleEvent(LifecycleEventProto.LIFECYCLE_PAUSE)
            session.onChatFocusChanged("300", opened = false)

            val focusOps = client.recordedFunctions.filter { it is TdApi.OpenChat || it is TdApi.CloseChat }
            assertEquals(
                listOf("open:200", "close:200", "open:300", "open:300", "close:300"),
                focusOps.map {
                    when (it) {
                        is TdApi.OpenChat -> "open:${it.chatId}"
                        is TdApi.CloseChat -> "close:${it.chatId}"
                        else -> ""
                    }
                }
            )

            val onlineOps = client.recordedFunctions
                .filterIsInstance<TdApi.SetOption>()
                .filter { it.name == "online" }
                .map { (it.value as TdApi.OptionValueBoolean).value }
            assertTrue("Expected online=true on init/resume", onlineOps.isNotEmpty() && onlineOps.all { it })
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun exceptionInOneUpdateDoesNotKillObserveLoop() = runBlocking {
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(fn.chatId)
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted) { msg, _ ->
            if (msg.uuid == "200:1") {
                throw IllegalStateException("simulated transient failure on msg 1")
            }
            ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
        }
        try {
            assertTrue(session.onInitialize())
            client.updateChannel.trySend(TdApi.UpdateNewMessage(makeTextMessage(200L, 1L, 1700000001)))
            client.updateChannel.trySend(TdApi.UpdateNewMessage(makeTextMessage(200L, 2L, 1700000002)))

            val deadline = System.currentTimeMillis() + 3_000L
            while (emitted.none { it.first.uuid == "200:2" } && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertTrue("Message 2 must still be delivered after msg 1 throws", emitted.any { it.first.uuid == "200:2" })
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun startupCatchUpWaitsForConnectionStateReadyBeforeQueryingServer() = runBlocking {
        val root = Files.createTempDirectory("td_wait_ready_").toFile()
        val storage = TdAccountStorage(root)
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(200L to 100L), nowMs = 1_700_000_000_000L)

        var isConnected = false
        val missed = listOf(
            makeTextMessage(200L, 102L, 1700000102),
            makeTextMessage(200L, 101L, 1700000101),
            makeTextMessage(200L, 100L, 1700000100),
        )
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> {
                    assertTrue("LoadChats must not run before ConnectionStateReady", isConnected)
                    throw TdErrorException(404, "Not Found")
                }
                is TdApi.GetChats -> TdApi.Chats(1, longArrayOf(200L))
                is TdApi.GetChat -> makeChat(200L, if (isConnected) missed.first() else makeTextMessage(200L, 100L, 1700000100))
                is TdApi.GetChatHistory -> TdApi.Messages(missed.size, missed.toTypedArray())
                else -> TdApi.Ok()
            }
        }
        client.connectionState.value = TdApi.ConnectionStateConnecting()

        val (_, session) = createEnv(client, root = root)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(80)
            assertTrue("Must not emit before ConnectionStateReady", emitted.isEmpty())

            // Now transition to ConnectionStateReady
            isConnected = true
            client.connectionState.value = TdApi.ConnectionStateReady()

            val deadline = System.currentTimeMillis() + 3_000L
            while (emitted.size < 2 && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertEquals(listOf("200:101", "200:102"), emitted.map { it.first.uuid })
            assertTrue(emitted.all { it.second })
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun networkMonitorDeduplicatesSameHandleAndType() {
        assertFalse(
            TdNetworkMonitor.shouldEmitTransition(
                previousHandle = 100L,
                previousTypeClass = TdApi.NetworkTypeWiFi::class.java,
                newHandle = 100L,
                newTypeClass = TdApi.NetworkTypeWiFi::class.java,
            )
        )
        assertTrue(
            TdNetworkMonitor.shouldEmitTransition(
                previousHandle = 100L,
                previousTypeClass = TdApi.NetworkTypeWiFi::class.java,
                newHandle = 101L,
                newTypeClass = TdApi.NetworkTypeWiFi::class.java,
            )
        )
            assertTrue(
                TdNetworkMonitor.shouldEmitTransition(
                    previousHandle = 100L,
                    previousTypeClass = TdApi.NetworkTypeWiFi::class.java,
                    newHandle = 100L,
                    newTypeClass = TdApi.NetworkTypeMobile::class.java,
                )
            )
        }

    @Test
    fun periodicSupergroupSyncProbesUnopenedSupergroupsAndNeverClosesFocusedChat() = runBlocking {
        val root = Files.createTempDirectory("td_periodic_sync_").toFile()
        val storage = TdAccountStorage(root)
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(200L to 100L, 300L to 500L), nowMs = 1_700_000_000_000L)

        var serverHasNewMessages = false
        val newMsgsChat200 = listOf(
            makeTextMessage(200L, 102L, 1700000102, "supergroup-new-2"),
            makeTextMessage(200L, 101L, 1700000101, "supergroup-new-1"),
            makeTextMessage(200L, 100L, 1700000100, "old"),
        )
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(2, longArrayOf(200L, 300L))
                is TdApi.GetChat -> makeChat(fn.chatId, makeTextMessage(fn.chatId, if (fn.chatId == 200L) 100L else 500L, 1700000100))
                is TdApi.GetChatHistory -> {
                    if (fn.chatId == 200L && serverHasNewMessages) {
                        TdApi.Messages(newMsgsChat200.size, newMsgsChat200.toTypedArray())
                    } else {
                        TdApi.Messages(0, emptyArray())
                    }
                }
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client, root = root)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(100)

            // User focuses chat 300; chat 200 is unopened and receives new messages on server
            session.onChatFocusChanged("300", opened = true)
            serverHasNewMessages = true

            val delivered = session.runPeriodicSupergroupSyncCycle(client)
            assertEquals(2, delivered)
            assertEquals(listOf("200:101", "200:102"), emitted.map { it.first.uuid })
            assertTrue("Periodic supergroup sync must deliver as live notification (isBackfill=false)", emitted.all { !it.second })

            val focusOps = client.recordedFunctions.filter { it is TdApi.OpenChat || it is TdApi.CloseChat }
            val closedIds = focusOps.filterIsInstance<TdApi.CloseChat>().map { it.chatId }
            assertTrue("Unopened probed chat 200 must be closed after probe", 200L in closedIds)
            assertFalse("Focused chat 300 must never be closed by periodic probe", 300L in closedIds)
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun updateChatLastMessageRecoversFreshMessageForUnseededChat() = runBlocking {
        val root = Files.createTempDirectory("td_unseeded_chat_").toFile()
        val storage = TdAccountStorage(root)
        // Only chat 200 was in the initial top-30 seed; chat 400 has watermark 0L
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(200L to 100L), nowMs = System.currentTimeMillis() - 10_000L)

        val nowSec = (System.currentTimeMillis() / 1000L).toInt()
        val freshMsg = makeTextMessage(400L, 901L, nowSec, "brand-new-chat-msg")
        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(0, longArrayOf())
                is TdApi.GetChat -> makeChat(400L, freshMsg)
                is TdApi.GetChatHistory -> TdApi.Messages(1, arrayOf(freshMsg))
                else -> TdApi.Ok()
            }
        }

        val (_, session) = createEnv(client, root = root, lastMessageDebounceMs = 20L)
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(80)
            client.updateChannel.trySend(TdApi.UpdateChatLastMessage(400L, freshMsg, emptyArray()))

            val deadline = System.currentTimeMillis() + 3_000L
            while (emitted.isEmpty() && System.currentTimeMillis() < deadline) {
                delay(20)
            }
            assertEquals(listOf("400:901"), emitted.map { it.first.uuid })
            assertTrue(emitted.all { !it.second })
        } finally {
            session.onClose()
            scope.cancel()
        }
    }

    @Test
    fun periodicSupergroupSyncCanBeDisabledViaConfigSwitch() = runBlocking {
        val root = Files.createTempDirectory("td_periodic_toggle_").toFile()
        val storage = TdAccountStorage(root)
        TdSyncStateStore(storage.getSyncStateFile("acct-backfill"), flushDebounceMs = 0L)
            .markInitialized(mapOf(200L to 100L), nowMs = 1_700_000_000_000L)

        val client = ChannelBackedFakeTdClient { fn ->
            when (fn) {
                is TdApi.LoadChats -> throw TdErrorException(404, "Not Found")
                is TdApi.GetChats -> TdApi.Chats(1, longArrayOf(200L))
                is TdApi.GetChat -> makeChat(200L, makeTextMessage(200L, 100L, 1700000100))
                is TdApi.GetChatHistory -> TdApi.Messages(1, arrayOf(makeTextMessage(200L, 101L, 1700000101)))
                else -> TdApi.Ok()
            }
        }
        val factory = object : TdClientFactory {
            override fun create(
                accountId: String,
                directories: TdAccountDirectories,
                apiId: Int,
                apiHash: String,
                proxyConfig: TdProxyConfig,
            ): TdClientApi = client
        }
        val coordinator = TdClientCoordinator(storage, factory, 12345, "hash")
        val provider = TdExtensionProvider(coordinator, stubMediaBridge)

        // Verify switch exists in extension config schema with defaultValue = true
        val configItems = provider.getConfigItems(JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-backfill"))
        val toggle = configItems
            .filterIsInstance<ParaboxConfigItem.SwitchConfigItem>()
            .firstOrNull { it.key == TdExtensionProvider.KEY_SUPERGROUP_PERIODIC_SYNC }
        assertTrue("Expected supergroup_periodic_sync switch in config schema", toggle != null && toggle.defaultValue)

        // Verify disabled switch suppresses periodic loop even with short interval
        val session = TdSession(
            sessionId = "conn-1",
            extra = JSONObject()
                .put(TdExtensionProvider.KEY_ACCOUNT_ID, client.accountId)
                .put(TdExtensionProvider.KEY_SUPERGROUP_PERIODIC_SYNC, false),
            hostPackageName = "com.ojhdtapp.parabox",
            coordinator = coordinator,
            mediaBridge = stubMediaBridge,
            catchUpChatThrottleMs = 0L,
            periodicSyncForegroundIntervalMs = 50L,
            periodicSyncBackgroundIntervalMs = 50L,
            periodicSyncProbePacingMs = 0L,
        )
        val emitted = CopyOnWriteArrayList<Pair<ReceiveMessage, Boolean>>()
        val scope = attachSession(session, emitted)
        try {
            assertTrue(session.onInitialize())
            delay(180)
            assertTrue("Periodic sync must not run when disabled in settings", emitted.isEmpty())
            assertTrue(
                "No OpenChat probe should be sent when periodic sync is disabled",
                client.recordedFunctions.none { it is TdApi.OpenChat }
            )
        } finally {
            session.onClose()
            scope.cancel()
        }
    }
}
