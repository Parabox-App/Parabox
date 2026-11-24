package com.ojhdtapp.paraboxdevelopmentkit.v2

import com.google.protobuf.ByteString
import com.ojhdtapp.parabox.protocol.v2.ActionResult
import com.ojhdtapp.parabox.protocol.v2.BasicInfoRequest
import com.ojhdtapp.parabox.protocol.v2.BasicInfoResponse
import com.ojhdtapp.parabox.protocol.v2.ChatListResponse
import com.ojhdtapp.parabox.protocol.v2.ConfigSchemaResponse
import com.ojhdtapp.parabox.protocol.v2.ContactListResponse
import com.ojhdtapp.parabox.protocol.v2.ExecuteConfigActionRequest
import com.ojhdtapp.parabox.protocol.v2.ExtensionDescriptor
import com.ojhdtapp.parabox.protocol.v2.ExtensionManifest
import com.ojhdtapp.parabox.protocol.v2.ExtensionSessionFrame
import com.ojhdtapp.parabox.protocol.v2.ForwardNodeRequest
import com.ojhdtapp.parabox.protocol.v2.ForwardNodeResponse
import com.ojhdtapp.parabox.protocol.v2.GetConfigSchemaRequest
import com.ojhdtapp.parabox.protocol.v2.GetManifestRequest
import com.ojhdtapp.parabox.protocol.v2.HeartbeatFrame
import com.ojhdtapp.parabox.protocol.v2.HostSessionFrame
import com.ojhdtapp.parabox.protocol.v2.InitStepRequest
import com.ojhdtapp.parabox.protocol.v2.InitStepResponse
import com.ojhdtapp.parabox.protocol.v2.MessageListResponse
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.parabox.protocol.v2.QueryHistoryRequest
import com.ojhdtapp.parabox.protocol.v2.RecallMessageEventFrame
import com.ojhdtapp.parabox.protocol.v2.RecallMessageRequest
import com.ojhdtapp.parabox.protocol.v2.ReceiveMessageEventFrame
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.parabox.protocol.v2.ResourceStreamRequest
import com.ojhdtapp.parabox.protocol.v2.SendMessageRequest
import com.ojhdtapp.parabox.protocol.v2.SessionScopedRequest
import com.ojhdtapp.parabox.protocol.v2.ValidateConfigRequest
import com.ojhdtapp.parabox.protocol.v2.ValidationResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomain
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toDomainValue
import com.ojhdtapp.paraboxdevelopmentkit.protocol.ParaboxProtoMapper.toProto
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ParaboxExtensionGrpcEngine(
    providers: List<ParaboxExtensionProvider>,
    private val versionName: String = "1.0.0",
    private val versionCode: Long = 1L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val ackTimeoutMs: Long = 30_000L,
) : ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineImplBase() {

    private val providerMap: Map<String, ParaboxExtensionProvider> =
        providers.associateBy { it.descriptor.key }

    private val activeSessions = ConcurrentHashMap<String, ActiveSessionHolder>()

    private data class ActiveSessionHolder(
        val sessionId: String,
        val session: ParaboxSession,
        val scope: CoroutineScope,
        val pendingAcks: ConcurrentHashMap<String, CompletableDeferred<ParaboxResult>>,
    )

    override suspend fun getManifest(request: GetManifestRequest): ExtensionManifest {
        val descriptors = providerMap.values.map { provider ->
            val d = provider.descriptor
            ExtensionDescriptor.newBuilder()
                .setKey(d.key)
                .setName(d.name)
                .setDescription(d.description)
                .apply {
                    if (d.iconPng != null) {
                        setIconPng(ByteString.copyFrom(d.iconPng))
                    }
                    addAllCapabilities(d.capabilities)
                }
                .build()
        }
        return ExtensionManifest.newBuilder()
            .setProtocolVersion(ParaboxExtensionConstants.PROTOCOL_VERSION)
            .setVersionName(versionName)
            .setVersionCode(versionCode)
            .addAllExtensions(descriptors)
            .build()
    }

    override suspend fun nextInitStep(request: InitStepRequest): InitStepResponse {
        val provider = providerMap[request.extensionKey]
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Unknown extension: ${request.extensionKey}"))
        val extra = parseJsonObject(request.accumulatedExtraJson)
        val submittedKey = request.submittedStepKey.ifEmpty { null }
        val submittedValue = if (request.hasSubmittedValue()) request.submittedValue.toDomainValue() else null

        val result = provider.onInitStep(
            wizardSessionId = request.wizardSessionId,
            currentStepIndex = request.currentStepIndex,
            submittedKey = submittedKey,
            submittedValue = submittedValue,
            accumulatedExtra = extra,
        )
        return InitStepResponse.newBuilder()
            .addAllSteps(result.steps.map { it.toProto() })
            .setNextStepIndex(result.nextStepIndex)
            .setValidationError(result.validationError)
            .setCompleted(result.completed)
            .setUpdatedExtraJson(result.updatedExtra.toString())
            .build()
    }

    override suspend fun getConfigSchema(request: GetConfigSchemaRequest): ConfigSchemaResponse {
        val provider = providerMap[request.extensionKey]
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Unknown extension: ${request.extensionKey}"))
        val extra = parseJsonObject(request.currentExtraJson)
        val items = provider.getConfigItems(extra)
        return ConfigSchemaResponse.newBuilder()
            .addAllItems(items.map { it.toProto() })
            .build()
    }

    override suspend fun validateConfig(request: ValidateConfigRequest): ValidationResult {
        val provider = providerMap[request.extensionKey]
            ?: throw StatusException(Status.NOT_FOUND.withDescription("Unknown extension: ${request.extensionKey}"))
        val extra = parseJsonObject(request.currentExtraJson)
        val value = if (request.hasValue()) request.value.toDomainValue() else null
        return when (val res = provider.validateConfig(request.configKey, value, extra)) {
            is ParaboxInitActionResult.Done -> ValidationResult.newBuilder()
                .setValid(true)
                .build()
            is ParaboxInitActionResult.Error -> ValidationResult.newBuilder()
                .setValid(false)
                .setErrorMessage(res.message)
                .build()
        }
    }

    override suspend fun executeConfigAction(request: ExecuteConfigActionRequest): ActionResult {
        val provider = providerMap[request.extensionKey]
            ?: return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "Unknown extension: ${request.extensionKey}").toProto()
        val extra = parseJsonObject(request.currentExtraJson)
        return try {
            provider.onConfigAction(
                actionKey = request.actionKey,
                sessionId = request.sessionId,
                currentExtra = extra,
            ).toProto()
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG).toProto()
        }
    }

    override fun openSession(requests: Flow<HostSessionFrame>): Flow<ExtensionSessionFrame> = channelFlow {
        var boundSessionId: String? = null
        try {
            requests.collect { frame ->
                when (frame.payloadCase) {
                    HostSessionFrame.PayloadCase.INIT -> {
                        val init = frame.init
                        val sessionId = init.sessionId
                        val provider = providerMap[init.extensionKey]
                            ?: throw StatusException(
                                Status.NOT_FOUND.withDescription("Unknown extension: ${init.extensionKey}")
                            )
                        // Clean up any stale session with the same ID
                        activeSessions.remove(sessionId)?.let { cleanupHolder(it) }

                        val extra = parseJsonObject(init.extraJson)
                        val session = provider.createSession(
                            sessionId = sessionId,
                            extra = extra,
                            hostPackageName = init.hostPackageName,
                        )
                        val pendingAcks = ConcurrentHashMap<String, CompletableDeferred<ParaboxResult>>()
                        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                            session.updateStatus(
                                ParaboxConnectionStatus.Error(throwable.message ?: "Session coroutine error")
                            )
                        }
                        val sessionScope = CoroutineScope(SupervisorJob() + ioDispatcher + exceptionHandler)
                        val emitter = object : ParaboxSessionEmitter {
                            override suspend fun emitMessage(message: ReceiveMessage): ParaboxResult {
                                val ackId = UUID.randomUUID().toString()
                                val deferred = CompletableDeferred<ParaboxResult>()
                                pendingAcks[ackId] = deferred
                                val eventFrame = ExtensionSessionFrame.newBuilder()
                                    .setReceiveMessage(
                                        ReceiveMessageEventFrame.newBuilder()
                                            .setAckId(ackId)
                                            .setMessage(message.toProto())
                                            .build()
                                    )
                                    .build()
                                send(eventFrame)
                                return withTimeoutOrNull(ackTimeoutMs) {
                                    deferred.await()
                                } ?: run {
                                    pendingAcks.remove(ackId)
                                    ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "Host ACK timeout")
                                }
                            }

                            override suspend fun emitRecall(messageUuid: String): ParaboxResult {
                                val ackId = UUID.randomUUID().toString()
                                val deferred = CompletableDeferred<ParaboxResult>()
                                pendingAcks[ackId] = deferred
                                val eventFrame = ExtensionSessionFrame.newBuilder()
                                    .setRecallMessage(
                                        RecallMessageEventFrame.newBuilder()
                                            .setAckId(ackId)
                                            .setMessageUuid(messageUuid)
                                            .build()
                                    )
                                    .build()
                                send(eventFrame)
                                return withTimeoutOrNull(ackTimeoutMs) {
                                    deferred.await()
                                } ?: run {
                                    pendingAcks.remove(ackId)
                                    ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "Host ACK timeout")
                                }
                            }

                            override fun emitStatus(status: ParaboxConnectionStatus) {
                                trySend(
                                    ExtensionSessionFrame.newBuilder()
                                        .setStatusChanged(status.toProto())
                                        .build()
                                )
                            }
                        }

                        session.attach(sessionScope, emitter)
                        val holder = ActiveSessionHolder(sessionId, session, sessionScope, pendingAcks)
                        activeSessions[sessionId] = holder
                        boundSessionId = sessionId

                        sessionScope.launch {
                            try {
                                session.updateStatus(ParaboxConnectionStatus.Initializing)
                                val ok = session.onInitialize()
                                if (!ok && session.status.value is ParaboxConnectionStatus.Initializing) {
                                    session.updateStatus(ParaboxConnectionStatus.Error("Initialization failed"))
                                }
                            } catch (e: Exception) {
                                session.updateStatus(ParaboxConnectionStatus.Error(e.message ?: "Initialization error"))
                            }
                        }
                    }

                    HostSessionFrame.PayloadCase.PING -> {
                        send(
                            ExtensionSessionFrame.newBuilder()
                                .setPong(
                                    HeartbeatFrame.newBuilder()
                                        .setTimestampMs(frame.ping.timestampMs)
                                        .build()
                                )
                                .build()
                        )
                    }

                    HostSessionFrame.PayloadCase.LIFECYCLE -> {
                        val sid = boundSessionId ?: return@collect
                        activeSessions[sid]?.let { holder ->
                            holder.scope.launch {
                                runCatching { holder.session.onLifecycleEvent(frame.lifecycle) }
                            }
                        }
                    }

                    HostSessionFrame.PayloadCase.ACK -> {
                        val sid = boundSessionId ?: return@collect
                        activeSessions[sid]?.pendingAcks
                            ?.remove(frame.ack.ackId)
                            ?.complete(frame.ack.result.toDomain())
                    }

                    HostSessionFrame.PayloadCase.PAYLOAD_NOT_SET, null -> Unit
                }
            }
        } finally {
            boundSessionId?.let { sid ->
                activeSessions.remove(sid)?.let { cleanupHolder(it) }
            }
        }
    }

    override suspend fun sendMessage(request: SendMessageRequest): ActionResult {
        val holder = activeSessions[request.sessionId]
            ?: return ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, ParaboxResult.ERROR_UNINITIALIZED_MSG).toProto()
        return try {
            holder.session.onSendMessage(request.message.toDomain()).toProto()
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG).toProto()
        }
    }

    override suspend fun recallMessage(request: RecallMessageRequest): ActionResult {
        val holder = activeSessions[request.sessionId]
            ?: return ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, ParaboxResult.ERROR_UNINITIALIZED_MSG).toProto()
        return try {
            holder.session.onRecallMessage(request.messageUuid).toProto()
        } catch (e: Exception) {
            ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG).toProto()
        }
    }

    override suspend fun getContacts(request: SessionScopedRequest): ContactListResponse {
        val holder = activeSessions[request.sessionId] ?: return ContactListResponse.getDefaultInstance()
        val contacts = runCatching { holder.session.onGetContacts() }.getOrDefault(emptyList())
        return ContactListResponse.newBuilder()
            .addAllContacts(contacts.map { it.toProto() })
            .build()
    }

    override suspend fun getChats(request: SessionScopedRequest): ChatListResponse {
        val holder = activeSessions[request.sessionId] ?: return ChatListResponse.getDefaultInstance()
        val chats = runCatching { holder.session.onGetChats() }.getOrDefault(emptyList())
        return ChatListResponse.newBuilder()
            .addAllChats(chats.map { it.toProto() })
            .build()
    }

    override suspend fun queryMessageHistory(request: QueryHistoryRequest): MessageListResponse {
        val holder = activeSessions[request.sessionId] ?: return MessageListResponse.getDefaultInstance()
        val messages = runCatching { holder.session.onQueryMessageHistory(request.messageUuid) }.getOrDefault(emptyList())
        return MessageListResponse.newBuilder()
            .addAllMessages(messages.map { it.toProto() })
            .build()
    }

    override suspend fun getGroupBasicInfo(request: BasicInfoRequest): BasicInfoResponse {
        val holder = activeSessions[request.sessionId] ?: return BasicInfoResponse.getDefaultInstance()
        val info = runCatching { holder.session.onGetGroupBasicInfo(request.targetId) }.getOrNull()
            ?: return BasicInfoResponse.newBuilder().setFound(false).build()
        return BasicInfoResponse.newBuilder()
            .setFound(true)
            .setBasicInfo(info.toProto())
            .build()
    }

    override suspend fun getUserBasicInfo(request: BasicInfoRequest): BasicInfoResponse {
        val holder = activeSessions[request.sessionId] ?: return BasicInfoResponse.getDefaultInstance()
        val info = runCatching { holder.session.onGetUserBasicInfo(request.targetId) }.getOrNull()
            ?: return BasicInfoResponse.newBuilder().setFound(false).build()
        return BasicInfoResponse.newBuilder()
            .setFound(true)
            .setBasicInfo(info.toProto())
            .build()
    }

    override suspend fun queryForwardNode(request: ForwardNodeRequest): ForwardNodeResponse {
        val holder = activeSessions[request.sessionId] ?: return ForwardNodeResponse.getDefaultInstance()
        val node = runCatching { holder.session.onQueryForwardNode(request.nodeId) }.getOrNull()
            ?: return ForwardNodeResponse.newBuilder().setFound(false).build()
        return ForwardNodeResponse.newBuilder()
            .setFound(true)
            .setNode(node.toProto())
            .build()
    }

    override fun streamResource(request: ResourceStreamRequest): Flow<ResourceChunk> {
        val holder = activeSessions[request.sessionId] ?: return emptyFlow()
        return holder.session.onStreamResource(request.resourceId, request.offset)
    }

    suspend fun shutdownAllSessions() {
        val holders = activeSessions.values.toList()
        activeSessions.clear()
        holders.forEach { cleanupHolder(it) }
    }

    private suspend fun cleanupHolder(holder: ActiveSessionHolder) {
        holder.pendingAcks.values.forEach { deferred ->
            deferred.complete(ParaboxResult(ParaboxResult.ERROR_UNINITIALIZED, "Session closed"))
        }
        holder.pendingAcks.clear()
        holder.session.detach()
        runCatching { holder.session.onClose() }
        holder.scope.cancel()
    }

    private fun parseJsonObject(raw: String): JSONObject {
        return if (raw.isBlank()) {
            JSONObject()
        } else {
            runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        }
    }
}
