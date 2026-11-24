package com.ojhdtapp.parabox.domain.model

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.ConnectionInfoType
import com.ojhdtapp.parabox.domain.service.extension.ExtensionSessionClient
import com.ojhdtapp.parabox.protocol.v2.LifecycleEventProto
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import kotlinx.coroutines.flow.Flow
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

sealed interface Connection {
    val alias: String
    val name: String
    val extra: JSONObject
    val connectionId: Long
    val key: String

    fun toExtensionInfo(): ConnectionInfo

    sealed interface ConnectionPending : Connection {
        val needReloadExtra: Boolean

        fun toFail(reason: String = ""): ConnectionFail
        fun toSuccess(sessionClient: ExtensionSessionClient, updateExtra: JSONObject? = null): ConnectionSuccess

        class BuiltInConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val key: String,
            override val needReloadExtra: Boolean = false,
        ) : ConnectionPending, BuiltInConnection {
            constructor(connectionInfo: ConnectionInfo, needReloadExtra: Boolean = false) : this(
                alias = connectionInfo.alias,
                name = connectionInfo.name,
                extra = connectionInfo.extra,
                connectionId = connectionInfo.connectionId,
                key = connectionInfo.key,
                needReloadExtra = needReloadExtra,
            )

            override fun toFail(reason: String): ConnectionFail =
                ConnectionFail.BuiltInConnectionFail(alias, name, extra, connectionId, key, reason)

            override fun toSuccess(
                sessionClient: ExtensionSessionClient,
                updateExtra: JSONObject?,
            ): ConnectionSuccess = ConnectionSuccess.BuiltInConnectionSuccess(
                alias = alias,
                name = name,
                extra = updateExtra ?: extra,
                connectionId = connectionId,
                sessionClient = sessionClient,
                key = key,
            )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.BuiltIn,
                extra = extra,
                key = key,
                connectionId = connectionId,
            )
        }

        class ExtendConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val pkg: String,
            val serviceClassName: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String,
            override val needReloadExtra: Boolean = false,
        ) : ConnectionPending, ExternalConnection {
            constructor(connectionInfo: ConnectionInfo, needReloadExtra: Boolean = false) : this(
                alias = connectionInfo.alias,
                name = connectionInfo.name,
                extra = connectionInfo.extra,
                connectionId = connectionInfo.connectionId,
                pkg = connectionInfo.pkg,
                serviceClassName = connectionInfo.connectionClassName,
                version = connectionInfo.version,
                versionCode = connectionInfo.versionCode,
                key = connectionInfo.key,
                needReloadExtra = needReloadExtra,
            )

            override fun toFail(reason: String): ConnectionFail =
                ConnectionFail.ExtendConnectionFail(
                    alias, name, extra, connectionId, pkg, serviceClassName, version, versionCode, key, reason
                )

            override fun toSuccess(
                sessionClient: ExtensionSessionClient,
                updateExtra: JSONObject?,
            ): ConnectionSuccess = ConnectionSuccess.ExtendConnectionSuccess(
                alias = alias,
                name = name,
                extra = updateExtra ?: extra,
                connectionId = connectionId,
                sessionClient = sessionClient,
                pkg = pkg,
                serviceClassName = serviceClassName,
                version = version,
                versionCode = versionCode,
                key = key,
            )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.Extend,
                extra = extra,
                pkg = pkg,
                connectionClassName = serviceClassName,
                version = version,
                versionCode = versionCode,
                key = key,
                connectionId = connectionId,
            )
        }
    }

    sealed interface ConnectionFail : Connection {
        val reason: String

        class BuiltInConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val key: String,
            override val reason: String = "",
        ) : ConnectionFail, BuiltInConnection {
            constructor(connectionInfo: ConnectionInfo, reason: String = "") : this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                connectionInfo.key,
                reason,
            )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.BuiltIn,
                extra = extra,
                key = key,
                connectionId = connectionId,
            )
        }

        class ExtendConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val pkg: String,
            val serviceClassName: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String,
            override val reason: String = "",
        ) : ConnectionFail, ExternalConnection {
            constructor(connectionInfo: ConnectionInfo, reason: String = "") : this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                connectionInfo.pkg,
                connectionInfo.connectionClassName,
                connectionInfo.version,
                connectionInfo.versionCode,
                connectionInfo.key,
                reason,
            )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.Extend,
                extra = extra,
                pkg = pkg,
                connectionClassName = serviceClassName,
                version = version,
                versionCode = versionCode,
                key = key,
                connectionId = connectionId,
            )
        }
    }

    sealed class ConnectionSuccess : DefaultLifecycleObserver, Connection {
        abstract val sessionClient: ExtensionSessionClient

        abstract fun toPending(needReloadExtra: Boolean = false): ConnectionPending

        class BuiltInConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val sessionClient: ExtensionSessionClient,
            override val key: String,
        ) : ConnectionSuccess(), BuiltInConnection {
            override fun toPending(needReloadExtra: Boolean): ConnectionPending =
                ConnectionPending.BuiltInConnectionPending(
                    alias = alias,
                    name = name,
                    extra = extra,
                    connectionId = connectionId,
                    key = key,
                    needReloadExtra = needReloadExtra,
                )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.BuiltIn,
                extra = extra,
                key = key,
                connectionId = connectionId,
            )
        }

        class ExtendConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val sessionClient: ExtensionSessionClient,
            override val pkg: String,
            val serviceClassName: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String,
        ) : ConnectionSuccess(), ExternalConnection {
            override fun toPending(needReloadExtra: Boolean): ConnectionPending =
                ConnectionPending.ExtendConnectionPending(
                    alias = alias,
                    name = name,
                    extra = extra,
                    connectionId = connectionId,
                    pkg = pkg,
                    serviceClassName = serviceClassName,
                    version = version,
                    versionCode = versionCode,
                    key = key,
                    needReloadExtra = needReloadExtra,
                )

            override fun toExtensionInfo(): ConnectionInfo = ConnectionInfo(
                alias = alias,
                name = name,
                type = ConnectionInfoType.Extend,
                extra = extra,
                pkg = pkg,
                connectionClassName = serviceClassName,
                version = version,
                versionCode = versionCode,
                key = key,
                connectionId = connectionId,
            )
        }

        fun getStatus(): StateFlow<ParaboxConnectionStatus> = sessionClient.status

        fun updateStatus(status: ParaboxConnectionStatus) {
            sessionClient.updateLocalStatus(status)
        }

        suspend fun sendMessage(message: SendMessage): ParaboxResult = sessionClient.sendMessage(message)

        suspend fun recallMessage(messageUuid: String): ParaboxResult = sessionClient.recallMessage(messageUuid)

        suspend fun getContacts(): List<ParaboxContact> = sessionClient.getContacts()

        suspend fun getChats(): List<ParaboxChat> = sessionClient.getChats()

        suspend fun queryMessageHistory(uuid: String): List<ReceiveMessage> = sessionClient.queryMessageHistory(uuid)

        suspend fun getGroupBasicInfo(groupId: String): ParaboxBasicInfo? = sessionClient.getGroupBasicInfo(groupId)

        suspend fun getUserBasicInfo(userId: String): ParaboxBasicInfo? = sessionClient.getUserBasicInfo(userId)

        suspend fun queryForwardNode(nodeId: String): ParaboxForward.ForwardNode? = sessionClient.queryForwardNode(nodeId)

        fun streamResource(resourceId: String, offset: Long = 0L): Flow<ResourceChunk> =
            sessionClient.streamResource(resourceId, offset)

        fun closeSession() {
            sessionClient.close()
        }

        override fun onCreate(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_CREATE)
        }

        override fun onStart(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_START)
        }

        override fun onResume(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_RESUME)
        }

        override fun onPause(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_PAUSE)
        }

        override fun onStop(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_STOP)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            sessionClient.sendLifecycleEvent(LifecycleEventProto.LIFECYCLE_DESTROY)
        }
    }
}

interface BuiltInConnection

interface ExternalConnection {
    val pkg: String
    val version: String
    val versionCode: Long
}
