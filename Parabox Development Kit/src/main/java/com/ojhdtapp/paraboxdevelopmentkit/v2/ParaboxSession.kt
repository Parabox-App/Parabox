package com.ojhdtapp.paraboxdevelopmentkit.v2

import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONObject

interface ParaboxSessionEmitter {
    suspend fun emitMessage(message: ReceiveMessage): ParaboxResult
    suspend fun emitRecall(messageUuid: String): ParaboxResult
    fun emitStatus(status: ParaboxConnectionStatus)
}

abstract class ParaboxSession(
    val sessionId: String,
    val extra: JSONObject,
    val hostPackageName: String,
) {
    private var emitter: ParaboxSessionEmitter? = null
    private lateinit var _sessionScope: CoroutineScope
    val sessionScope: CoroutineScope
        get() = _sessionScope

    private val _status = MutableStateFlow<ParaboxConnectionStatus>(ParaboxConnectionStatus.Pending)
    val status: StateFlow<ParaboxConnectionStatus> = _status.asStateFlow()

    internal fun attach(scope: CoroutineScope, sessionEmitter: ParaboxSessionEmitter) {
        _sessionScope = scope
        emitter = sessionEmitter
    }

    internal fun detach() {
        emitter = null
    }

    fun updateStatus(newStatus: ParaboxConnectionStatus) {
        _status.value = newStatus
        emitter?.emitStatus(newStatus)
    }

    suspend fun emitMessage(message: ReceiveMessage): ParaboxResult {
        val currentEmitter = emitter ?: return ParaboxResult(
            code = ParaboxResult.ERROR_UNINITIALIZED,
            message = ParaboxResult.ERROR_UNINITIALIZED_MSG,
        )
        return currentEmitter.emitMessage(message)
    }

    suspend fun emitRecall(messageUuid: String): ParaboxResult {
        val currentEmitter = emitter ?: return ParaboxResult(
            code = ParaboxResult.ERROR_UNINITIALIZED,
            message = ParaboxResult.ERROR_UNINITIALIZED_MSG,
        )
        return currentEmitter.emitRecall(messageUuid)
    }

    abstract suspend fun onInitialize(): Boolean

    abstract suspend fun onSendMessage(message: SendMessage): ParaboxResult

    open suspend fun onRecallMessage(messageUuid: String): ParaboxResult {
        return ParaboxResult(ParaboxResult.ERROR_UNKNOWN, "Recall not supported")
    }

    open suspend fun onGetContacts(): List<ParaboxContact> = emptyList()

    open suspend fun onGetChats(): List<ParaboxChat> = emptyList()

    open suspend fun onQueryMessageHistory(uuid: String): List<ReceiveMessage> = emptyList()

    open suspend fun onGetGroupBasicInfo(groupId: String): ParaboxBasicInfo? = null

    open suspend fun onGetUserBasicInfo(userId: String): ParaboxBasicInfo? = null

    open suspend fun onQueryForwardNode(nodeId: String): ParaboxForward.ForwardNode? = null

    open fun onStreamResource(resourceId: String, offset: Long): Flow<ResourceChunk> = emptyFlow()

    open suspend fun onLifecycleEvent(event: LifecycleEventProto) {}

    open suspend fun onClose() {}
}
