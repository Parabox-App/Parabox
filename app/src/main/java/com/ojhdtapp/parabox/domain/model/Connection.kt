package com.ojhdtapp.parabox.domain.model

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.data.local.ConnectionInfoType
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

sealed interface Connection {
    val alias: String
    val name: String
    val extra: JSONObject
    val connectionId: Long
    val key: String

    fun toExtensionInfo() : ConnectionInfo
    sealed interface ConnectionPending : Connection {
        val connection: ParaboxConnection
        val needReloadExtra: Boolean

        fun toFail() : ConnectionFail
        fun toSuccess(job: Job, updateExtra: JSONObject? = null): ConnectionSuccess
        class BuiltInConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val connection: ParaboxConnection,
            override val key: String,
            override val needReloadExtra: Boolean = false
        ) : ConnectionPending, BuiltInConnection{
            constructor(connectionInfo: ConnectionInfo, ext: ParaboxConnection) : this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                ext,
                connectionInfo.key
            )

            override fun toFail(): ConnectionFail {
                return ConnectionFail.BuiltInConnectionFail(
                    alias, name, extra, connectionId, key
                )
            }

            override fun toSuccess(job: Job, updateExtra: JSONObject?): ConnectionSuccess {
                return ConnectionSuccess.BuiltInConnectionSuccess(
                    alias, name, updateExtra ?: extra, connectionId, connection, job, key
                )
            }

            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.BuiltIn,
                    extra = extra,
                    key = key,
                    connectionId = connectionId
                )
            }
        }

        class ExtendConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val connection: ParaboxConnection,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String,
            override val needReloadExtra: Boolean = false
        ) : ConnectionPending, ExternalConnection {
            constructor(connectionInfo: ConnectionInfo, connection: ParaboxConnection) : this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                connection,
                connectionInfo.pkg,
                connectionInfo.version,
                connectionInfo.versionCode,
                connectionInfo.key
            )

            override fun toFail(): ConnectionFail {
                return ConnectionFail.ExtendConnectionFail(
                    alias, name, extra, connectionId, pkg, version, versionCode, key
                )
            }

            override fun toSuccess(job: Job, updateExtra: JSONObject?): ConnectionSuccess {
                return ConnectionSuccess.ExtendConnectionSuccess(
                    alias, name, updateExtra ?: extra, connectionId, connection, job, pkg, version, versionCode, key
                )
            }

            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    connectionId = connectionId
                )
            }
        }
    }

    sealed interface ConnectionFail : Connection{
        class BuiltInConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val key: String
        ) : ConnectionFail, BuiltInConnection {
            constructor(connectionInfo: ConnectionInfo): this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                connectionInfo.key
            )

            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.BuiltIn,
                    extra = extra,
                    key = key,
                    connectionId = connectionId
                )
            }
        }

        class ExtendConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String
        ) : ConnectionFail, ExternalConnection {
            constructor(connectionInfo: ConnectionInfo) : this(
                connectionInfo.alias,
                connectionInfo.name,
                connectionInfo.extra,
                connectionInfo.connectionId,
                connectionInfo.pkg,
                connectionInfo.version,
                connectionInfo.versionCode,
                connectionInfo.key
            )
            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    connectionId = connectionId
                )
            }
        }
    }

    sealed class ConnectionSuccess : DefaultLifecycleObserver, Connection {
        abstract val realConnection: ParaboxConnection
        abstract val job: Job

        abstract fun toPending(needReloadExtra: Boolean = false) : ConnectionPending

        class BuiltInConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val realConnection: ParaboxConnection,
            override val job: Job,
            override val key: String
        ) : ConnectionSuccess(), BuiltInConnection {
            override fun toPending(needReloadExtra: Boolean): ConnectionPending {
                return ConnectionPending.BuiltInConnectionPending(
                    alias, name, extra, connectionId, realConnection, key, needReloadExtra
                )
            }

            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.BuiltIn,
                    extra = extra,
                    key = key,
                    connectionId = connectionId
                )
            }

        }

        class ExtendConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: JSONObject,
            override val connectionId: Long,
            override val realConnection: ParaboxConnection,
            override val job: Job,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long,
            override val key: String
        ) : ConnectionSuccess(), ExternalConnection {
            override fun toPending(needReloadExtra: Boolean): ConnectionPending {
                return ConnectionPending.ExtendConnectionPending(
                    alias, name, extra, connectionId, realConnection, pkg, version, versionCode, key, needReloadExtra
                )
            }
            override fun toExtensionInfo(): ConnectionInfo {
                return ConnectionInfo(
                    alias = alias,
                    name = name,
                    type = ConnectionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    connectionId = connectionId
                )
            }
        }

        suspend fun init(context: Context, bridge: ParaboxBridge) {
            realConnection.init(context, job, bridge, extra)
        }

        fun getStatus(): StateFlow<ParaboxConnectionStatus> {
            return realConnection.status
        }

        fun updateStatus(status: ParaboxConnectionStatus) {
            realConnection.updateStatus(status)
        }

        override fun onCreate(owner: LifecycleOwner) {
            realConnection.onCreate()
        }

        override fun onStart(owner: LifecycleOwner) {
            realConnection.onStart()
        }

        override fun onResume(owner: LifecycleOwner) {
            realConnection.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            realConnection.onPause()
        }

        override fun onStop(owner: LifecycleOwner) {
            realConnection.onStop()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            realConnection.onDestroy()
        }
    }
}

interface BuiltInConnection {
}

interface ExternalConnection {
    val pkg: String
    val version: String
    val versionCode: Long
}