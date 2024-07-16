package com.ojhdtapp.parabox.domain.model

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.ExtensionInfoType
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

sealed interface Connection {
    val alias: String
    val name: String
    val extra: Bundle
    val extensionId: Long

    fun toExtensionInfo() : ExtensionInfo
    sealed interface ConnectionPending : Connection {
        val connection: ParaboxConnection

        fun toFail() : ConnectionFail
        fun toSuccess(job: Job): ConnectionSuccess
        class BuiltInConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val key: String
        ) : ConnectionPending, BuiltInConnection{
            constructor(extensionInfo: ExtensionInfo, ext: ParaboxConnection) : this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                ext,
                extensionInfo.builtInKey
            )

            override fun toFail(): ConnectionFail {
                return ConnectionFail.BuiltInConnectionFail(
                    alias, name, extra, extensionId, key
                )
            }

            override fun toSuccess(job: Job): ConnectionSuccess {
                return ConnectionSuccess.BuiltInConnectionSuccess(
                    alias, name, extra, extensionId, connection, job, key
                )
            }

            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.BuiltIn,
                    extra = extra,
                    builtInKey = key,
                    extensionId = extensionId
                )
            }
        }

        class ExtendConnectionPending(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ConnectionPending, ExternalConnection {
            constructor(extensionInfo: ExtensionInfo, connection: ParaboxConnection) : this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                connection,
                extensionInfo.pkg,
                extensionInfo.version,
                extensionInfo.versionCode
            )

            override fun toFail(): ConnectionFail {
                return ConnectionFail.ExtendConnectionFail(
                    alias, name, extra, extensionId, pkg, version, versionCode
                )
            }

            override fun toSuccess(job: Job): ConnectionSuccess {
                return ConnectionSuccess.ExtendConnectionSuccess(
                    alias, name, extra, extensionId, connection, job, pkg, version, versionCode
                )
            }

            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    extensionId = extensionId
                )
            }
        }
    }

    sealed interface ConnectionFail : Connection{
        class BuiltInConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val key: String
        ) : ConnectionFail, BuiltInConnection {
            constructor(extensionInfo: ExtensionInfo): this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                extensionInfo.builtInKey
            )

            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.BuiltIn,
                    extra = extra,
                    builtInKey = key,
                    extensionId = extensionId
                )
            }
        }

        class ExtendConnectionFail(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ConnectionFail, ExternalConnection {
            constructor(extensionInfo: ExtensionInfo) : this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                extensionInfo.pkg,
                extensionInfo.version,
                extensionInfo.versionCode
            )
            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    extensionId = extensionId
                )
            }
        }
    }

    sealed class ConnectionSuccess : DefaultLifecycleObserver, Connection {
        abstract val realConnection: ParaboxConnection
        abstract val job: Job

        abstract fun toPending() : ConnectionPending

        class BuiltInConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val realConnection: ParaboxConnection,
            override val job: Job,
            override val key: String
        ) : ConnectionSuccess(), BuiltInConnection {
            override fun toPending(): ConnectionPending {
                return ConnectionPending.BuiltInConnectionPending(
                    alias, name, extra, extensionId, realConnection, key
                )
            }

            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.BuiltIn,
                    extra = extra,
                    builtInKey = key,
                    extensionId = extensionId
                )
            }

        }

        class ExtendConnectionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val realConnection: ParaboxConnection,
            override val job: Job,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ConnectionSuccess(), ExternalConnection {
            override fun toPending(): ConnectionPending {
                return ConnectionPending.ExtendConnectionPending(
                    alias, name, extra, extensionId, realConnection, pkg, version, versionCode
                )
            }
            override fun toExtensionInfo(): ExtensionInfo {
                return ExtensionInfo(
                    alias = alias,
                    name = name,
                    type = ExtensionInfoType.Extend,
                    extra = extra,
                    pkg = pkg,
                    version = version,
                    versionCode = versionCode,
                    extensionId = extensionId
                )
            }
        }

        suspend fun init(context: Context, bridge: ParaboxBridge, extra: Bundle) {
            realConnection.init(context, bridge, extra)
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
    val key: String
}

interface ExternalConnection {
    val pkg: String
    val version: String
    val versionCode: Long
}