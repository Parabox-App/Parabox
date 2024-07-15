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

sealed interface Extension {
    val alias: String
    val name: String
    val extra: Bundle
    val extensionId: Long

    fun toExtensionInfo() : ExtensionInfo
    sealed interface ExtensionPending : Extension {
        val connection: ParaboxConnection

        fun toFail() : ExtensionFail
        fun toSuccess(job: Job): ExtensionSuccess
        class BuiltInExtensionPending(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val key: String
        ) : ExtensionPending, BuiltInExtension{
            constructor(extensionInfo: ExtensionInfo, ext: ParaboxConnection) : this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                ext,
                extensionInfo.builtInKey
            )

            override fun toFail(): ExtensionFail {
                return ExtensionFail.BuiltInExtensionFail(
                    alias, name, extra, extensionId, key
                )
            }

            override fun toSuccess(job: Job): ExtensionSuccess {
                return ExtensionSuccess.BuiltInExtensionSuccess(
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

        class ExtendExtensionPending(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ExtensionPending, ExtendExtension {
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

            override fun toFail(): ExtensionFail {
                return ExtensionFail.ExtendExtensionFail(
                    alias, name, extra, extensionId, pkg, version, versionCode
                )
            }

            override fun toSuccess(job: Job): ExtensionSuccess {
                return ExtensionSuccess.ExtendExtensionSuccess(
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

    sealed interface ExtensionFail : Extension{
        class BuiltInExtensionFail(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val key: String
        ) : ExtensionFail, BuiltInExtension {
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

        class ExtendExtensionFail(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ExtensionFail, ExtendExtension {
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

    sealed class ExtensionSuccess : DefaultLifecycleObserver, Extension {
        abstract val connection: ParaboxConnection
        abstract val job: Job

        abstract fun toPending() : ExtensionPending

        class BuiltInExtensionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val job: Job,
            override val key: String
        ) : ExtensionSuccess(), BuiltInExtension {
            override fun toPending(): ExtensionPending {
                return ExtensionPending.BuiltInExtensionPending(
                    alias, name, extra, extensionId, connection, key
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

        class ExtendExtensionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val connection: ParaboxConnection,
            override val job: Job,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ExtensionSuccess(), ExtendExtension {
            override fun toPending(): ExtensionPending {
                return ExtensionPending.ExtendExtensionPending(
                    alias, name, extra, extensionId, connection, pkg, version, versionCode
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
            connection.init(context, bridge, extra)
        }

        fun getStatus(): StateFlow<ParaboxConnectionStatus> {
            return connection.status
        }

        fun updateStatus(status: ParaboxConnectionStatus) {
            connection.updateStatus(status)
        }

        override fun onCreate(owner: LifecycleOwner) {
            connection.onCreate()
        }

        override fun onStart(owner: LifecycleOwner) {
            connection.onStart()
        }

        override fun onResume(owner: LifecycleOwner) {
            connection.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            connection.onPause()
        }

        override fun onStop(owner: LifecycleOwner) {
            connection.onStop()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            connection.onDestroy()
        }
    }
}

interface BuiltInExtension {
    val key: String
}

interface ExtendExtension {
    val pkg: String
    val version: String
    val versionCode: Long
}