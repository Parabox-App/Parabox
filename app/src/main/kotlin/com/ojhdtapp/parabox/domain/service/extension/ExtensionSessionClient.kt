package com.ojhdtapp.parabox.domain.service.extension

import android.net.Uri
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionTransport
import com.ojhdtapp.parabox.protocol.v2.BasicInfoRequest
import com.ojhdtapp.parabox.protocol.v2.ExtensionSessionFrame
import com.ojhdtapp.parabox.protocol.v2.ForwardNodeRequest
import com.ojhdtapp.parabox.protocol.v2.HeartbeatFrame
import com.ojhdtapp.parabox.protocol.v2.HostSessionFrame
import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
import com.ojhdtapp.parabox.protocol.v2.MessageAckFrame
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.parabox.protocol.v2.QueryHistoryRequest
import com.ojhdtapp.parabox.protocol.v2.RecallMessageRequest
import com.ojhdtapp.parabox.protocol.v2.SendMessageRequest
import com.ojhdtapp.parabox.protocol.v2.SessionInitFrame
import com.ojhdtapp.parabox.protocol.v2.SessionScopedRequest
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toProto
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

interface SessionInboundHandler {
    suspend fun onReceiveMessage(message: ReceiveMessage): ParaboxResult
    suspend fun onRecallMessage(messageUuid: String): ParaboxResult
}

class ExtensionSessionClient(
    val sessionId: String,
    val endpoint: ExtensionEndpoint,
    val extra: JSONObject,
    private val hostPackageName: String,
    private val transport: ExtensionTransport,
    private val inboundHandler: SessionInboundHandler,
    private val uriPermissionGranter: ((targetPackage: String, uri: Uri) -> Unit)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val initialBackoffMs: Long = 1_000L,
    private val maxBackoffMs: Long = 30_000L,
    private val rpcTimeoutSeconds: Long = 15L,
    private val heartbeatIntervalMs: Long = 25_000L,
    private val heartbeatTimeoutMs: Long = 10_000L,
    private val recoverTimeoutMs: Long = 10_000L,
) {
    private val _status = MutableStateFlow<ParaboxConnectionStatus>(ParaboxConnectionStatus.Pending)
    val status: StateFlow<ParaboxConnectionStatus> = _status.asStateFlow()

    private val clientScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var streamSupervisorJob: Job? = null

    @Volatile
    private var activeOutboundChannel: Channel<HostSessionFrame>? = null

    @Volatile
    private var activeStreamJob: Job? = null

    private val reconnectSignal = Channel<Unit>(Channel.CONFLATED)

    fun start() {
        if (streamSupervisorJob?.isActive == true) return
        streamSupervisorJob = clientScope.launch {
            var attempt = 0
            while (isActive) {
                val outbound = Channel<HostSessionFrame>(Channel.UNLIMITED)
                val pongChannel = Channel<Long>(Channel.CONFLATED)
                activeOutboundChannel = outbound
                var forceImmediateReconnect = false
                try {
                    if (attempt == 0) {
                        _status.value = ParaboxConnectionStatus.Initializing
                    } else {
                        _status.value = ParaboxConnectionStatus.Reconnecting(attempt, "正在重新连接扩展服务")
                    }
                    val channel = transport.acquireChannel(endpoint)
                    val stub = ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)

                    outbound.send(
                        HostSessionFrame.newBuilder()
                            .setInit(
                                SessionInitFrame.newBuilder()
                                    .setSessionId(sessionId)
                                    .setExtensionKey(endpoint.extensionKey)
                                    .setExtraJson(extra.toString())
                                    .setHostPackageName(hostPackageName)
                                    .build()
                            )
                            .build()
                    )

                    coroutineScope {
                        val streamJob = launch {
                            val heartbeatJob = if (heartbeatIntervalMs > 0L) {
                                launch {
                                    while (isActive) {
                                        delay(heartbeatIntervalMs)
                                        val sent = outbound.trySend(
                                            HostSessionFrame.newBuilder()
                                                .setPing(
                                                    HeartbeatFrame.newBuilder()
                                                        .setTimestampMs(System.currentTimeMillis())
                                                        .build()
                                                )
                                                .build()
                                        ).isSuccess
                                        if (!sent) break
                                        val pong = withTimeoutOrNull(heartbeatTimeoutMs) {
                                            pongChannel.receive()
                                        }
                                        if (pong == null) {
                                            throw IllegalStateException("扩展心跳超时，触发重连")
                                        }
                                    }
                                }
                            } else {
                                null
                            }

                            try {
                                stub.openSession(outbound.consumeAsFlow()).collect { frame ->
                                    when (frame.payloadCase) {
                                        ExtensionSessionFrame.PayloadCase.STATUS_CHANGED -> {
                                            val nextStatus = frame.statusChanged.toDomain()
                                            if (nextStatus is ParaboxConnectionStatus.Active) {
                                                attempt = 0
                                            }
                                            _status.value = nextStatus
                                        }

                                        ExtensionSessionFrame.PayloadCase.RECEIVE_MESSAGE -> {
                                            val event = frame.receiveMessage
                                            launch {
                                                val result = try {
                                                    inboundHandler.onReceiveMessage(event.message.toDomain())
                                                } catch (e: Exception) {
                                                    ParaboxResult(
                                                        ParaboxResult.ERROR_UNKNOWN,
                                                        e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG,
                                                    )
                                                }
                                                outbound.trySend(
                                                    HostSessionFrame.newBuilder()
                                                        .setAck(
                                                            MessageAckFrame.newBuilder()
                                                                .setAckId(event.ackId)
                                                                .setResult(result.toProto())
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                            }
                                        }

                                        ExtensionSessionFrame.PayloadCase.RECALL_MESSAGE -> {
                                            val event = frame.recallMessage
                                            launch {
                                                val result = try {
                                                    inboundHandler.onRecallMessage(event.messageUuid)
                                                } catch (e: Exception) {
                                                    ParaboxResult(
                                                        ParaboxResult.ERROR_UNKNOWN,
                                                        e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG,
                                                    )
                                                }
                                                outbound.trySend(
                                                    HostSessionFrame.newBuilder()
                                                        .setAck(
                                                            MessageAckFrame.newBuilder()
                                                                .setAckId(event.ackId)
                                                                .setResult(result.toProto())
                                                                .build()
                                                        )
                                                        .build()
                                                )
                                            }
                                        }

                                        ExtensionSessionFrame.PayloadCase.PONG -> {
                                            pongChannel.trySend(frame.pong.timestampMs)
                                        }

                                        ExtensionSessionFrame.PayloadCase.PAYLOAD_NOT_SET,
                                        null -> Unit
                                    }
                                }
                            } finally {
                                heartbeatJob?.cancel()
                            }
                        }
                        activeStreamJob = streamJob
                        streamJob.join()
                    }

                    // Stream completed cleanly without cancellation: reconnect unless closed
                    if (!isActive) break
                    attempt += 1
                    _status.value = ParaboxConnectionStatus.Reconnecting(attempt, "会话流已断开，准备重连")
                } catch (e: ForceReconnectException) {
                    if (!isActive) break
                    attempt = 0
                    forceImmediateReconnect = true
                    _status.value = ParaboxConnectionStatus.Reconnecting(1, "正在重建扩展会话")
                    transport.invalidateChannel(endpoint)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (!isActive) break
                    attempt += 1
                    val reason = e.message ?: "扩展服务不可用"
                    _status.value = ParaboxConnectionStatus.Reconnecting(attempt, reason)
                    transport.invalidateChannel(endpoint)
                } finally {
                    activeStreamJob = null
                    pongChannel.close()
                    outbound.close()
                    if (activeOutboundChannel === outbound) {
                        activeOutboundChannel = null
                    }
                }

                if (!forceImmediateReconnect) {
                    val shift = (attempt - 1).coerceIn(0, 6)
                    val backoff = min(maxBackoffMs, initialBackoffMs * (1L shl shift))
                    withTimeoutOrNull(backoff) {
                        reconnectSignal.receive()
                    }
                }
            }
        }
    }

    fun forceReconnect() {
        _status.value = ParaboxConnectionStatus.Reconnecting(1, "正在重建扩展会话")
        val current = activeStreamJob
        if (current != null && current.isActive) {
            current.cancel(ForceReconnectException())
        }
        reconnectSignal.trySend(Unit)
    }

    private class ForceReconnectException : CancellationException("Force reconnect requested")

    fun sendLifecycleEvent(event: LifecycleEventProto) {
        if (event == LifecycleEventProto.LIFECYCLE_RESUME &&
            (_status.value is ParaboxConnectionStatus.Reconnecting || _status.value is ParaboxConnectionStatus.Error)
        ) {
            reconnectSignal.trySend(Unit)
        }
        activeOutboundChannel?.trySend(
            HostSessionFrame.newBuilder()
                .setLifecycle(event)
                .build()
        )
    }

    fun updateLocalStatus(newStatus: ParaboxConnectionStatus) {
        _status.value = newStatus
    }

    fun close() {
        activeOutboundChannel?.close()
        activeOutboundChannel = null
        streamSupervisorJob?.cancel()
        clientScope.cancel()
    }

    private fun unaryStub(timeoutSeconds: Long = rpcTimeoutSeconds): ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub {
        val channel = transport.acquireChannel(endpoint)
        return ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)
            .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
    }

    suspend fun sendMessage(message: SendMessage): ParaboxResult {
        val firstResult = executeSendMessage(message)
        if (firstResult.code == ParaboxResult.ERROR_UNINITIALIZED ||
            firstResult.code == ParaboxResult.ERROR_DISCONNECTED
        ) {
            forceReconnect()
            val recovered = withTimeoutOrNull(recoverTimeoutMs) {
                _status.first { it is ParaboxConnectionStatus.Active }
            } != null
            if (recovered) {
                return executeSendMessage(message)
            }
            return ParaboxResult(
                ParaboxResult.ERROR_DISCONNECTED,
                "扩展会话已中断且正在重建，请稍后重试",
            )
        }
        return firstResult
    }

    private suspend fun executeSendMessage(message: SendMessage): ParaboxResult {
        return try {
            if (endpoint is ExtensionEndpoint.BinderService && uriPermissionGranter != null) {
                grantUrisForElements(endpoint.packageName, message.contents)
            }
            unaryStub(timeoutSeconds = SEND_MESSAGE_TIMEOUT_SECONDS).sendMessage(
                SendMessageRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setMessage(message.toProto())
                    .build()
            ).toDomain()
        } catch (e: io.grpc.StatusException) {
            when (e.status.code) {
                io.grpc.Status.Code.DEADLINE_EXCEEDED ->
                    ParaboxResult(ParaboxResult.ERROR_TIMEOUT, "发送超时，请检查网络或扩展状态")
                io.grpc.Status.Code.UNAVAILABLE ->
                    ParaboxResult(ParaboxResult.ERROR_DISCONNECTED, "扩展服务连接已断开")
                else ->
                    ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.status.description ?: e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
            }
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
        }
    }

    companion object {
        private const val SEND_MESSAGE_TIMEOUT_SECONDS = 60L
    }

    private fun grantUrisForElements(targetPackage: String, elements: List<ParaboxMessageElement>) {
        elements.forEach { elem ->
            when (elem) {
                is ParaboxImage -> grantResourceUri(targetPackage, elem.resourceInfo)
                is ParaboxAudio -> grantResourceUri(targetPackage, elem.resourceInfo)
                is ParaboxVideo -> grantResourceUri(targetPackage, elem.resourceInfo)
                is ParaboxFile -> grantResourceUri(targetPackage, elem.resourceInfo)
                is ParaboxQuoteReply -> grantUrisForElements(targetPackage, elem.messages)
                is ParaboxForward -> elem.list.forEach { node -> grantUrisForElements(targetPackage, node.messages) }
                else -> Unit
            }
        }
    }

    private fun grantResourceUri(targetPackage: String, resourceInfo: ParaboxResourceInfo) {
        when (resourceInfo) {
            is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> {
                runCatching { uriPermissionGranter?.invoke(targetPackage, resourceInfo.uri) }
            }
            is ParaboxResourceInfo.ParaboxSyncedInfo -> {
                grantResourceUri(targetPackage, resourceInfo.local)
            }
            else -> Unit
        }
    }

    suspend fun recallMessage(messageUuid: String): ParaboxResult {
        return try {
            unaryStub().recallMessage(
                RecallMessageRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setMessageUuid(messageUuid)
                    .build()
            ).toDomain()
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
        }
    }

    suspend fun getContacts(): List<ParaboxContact> {
        return runCatching {
            unaryStub().getContacts(
                SessionScopedRequest.newBuilder().setSessionId(sessionId).build()
            ).contactsList.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    suspend fun getChats(): List<ParaboxChat> {
        return runCatching {
            unaryStub().getChats(
                SessionScopedRequest.newBuilder().setSessionId(sessionId).build()
            ).chatsList.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    suspend fun queryMessageHistory(uuid: String): List<ReceiveMessage> {
        return runCatching {
            unaryStub().queryMessageHistory(
                QueryHistoryRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setMessageUuid(uuid)
                    .build()
            ).messagesList.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    suspend fun getGroupBasicInfo(groupId: String): ParaboxBasicInfo? {
        return runCatching {
            val resp = unaryStub().getGroupBasicInfo(
                BasicInfoRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTargetId(groupId)
                    .build()
            )
            if (resp.found && resp.hasBasicInfo()) resp.basicInfo.toDomain() else null
        }.getOrNull()
    }

    suspend fun getUserBasicInfo(userId: String): ParaboxBasicInfo? {
        return runCatching {
            val resp = unaryStub().getUserBasicInfo(
                BasicInfoRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setTargetId(userId)
                    .build()
            )
            if (resp.found && resp.hasBasicInfo()) resp.basicInfo.toDomain() else null
        }.getOrNull()
    }

    suspend fun queryForwardNode(nodeId: String): ParaboxForward.ForwardNode? {
        return runCatching {
            val resp = unaryStub().queryForwardNode(
                ForwardNodeRequest.newBuilder()
                    .setSessionId(sessionId)
                    .setNodeId(nodeId)
                    .build()
            )
            if (resp.found && resp.hasNode()) resp.node.toDomain() else null
        }.getOrNull()
    }
}
