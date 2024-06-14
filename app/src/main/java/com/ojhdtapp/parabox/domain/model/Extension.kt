package com.ojhdtapp.parabox.domain.model

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.ExtensionInfoType
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed interface Extension {
    val alias: String
    val name: String
    val extra: Bundle
    val extensionId: Long

    fun toExtensionInfo() : ExtensionInfo
    sealed interface ExtensionPending : Extension {
        val ext: ParaboxExtension

        fun toFail() : ExtensionFail
        fun toSuccess(job: Job): ExtensionSuccess
        class BuiltInExtensionPending(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val ext: ParaboxExtension,
            override val key: String
        ) : ExtensionPending, BuiltInExtension{
            constructor(extensionInfo: ExtensionInfo, ext: ParaboxExtension) : this(
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
                    alias, name, extra, extensionId, ext, job, key
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
            override val ext: ParaboxExtension,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ExtensionPending, ExtendExtension {
            constructor(extensionInfo: ExtensionInfo, ext: ParaboxExtension) : this(
                extensionInfo.alias,
                extensionInfo.name,
                extensionInfo.extra,
                extensionInfo.extensionId,
                ext,
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
                    alias, name, extra, extensionId, ext, job, pkg, version, versionCode
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
        abstract val ext: ParaboxExtension
        abstract val job: Job

        abstract fun toPending() : ExtensionPending

        class BuiltInExtensionSuccess(
            override val alias: String,
            override val name: String,
            override val extra: Bundle,
            override val extensionId: Long,
            override val ext: ParaboxExtension,
            override val job: Job,
            override val key: String
        ) : ExtensionSuccess(), BuiltInExtension {
            override fun toPending(): ExtensionPending {
                return ExtensionPending.BuiltInExtensionPending(
                    alias, name, extra, extensionId, ext, key
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
            override val ext: ParaboxExtension,
            override val job: Job,
            override val pkg: String,
            override val version: String,
            override val versionCode: Long
        ) : ExtensionSuccess(), ExtendExtension {
            override fun toPending(): ExtensionPending {
                return ExtensionPending.ExtendExtensionPending(
                    alias, name, extra, extensionId, ext, pkg, version, versionCode
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
            ext.init(context, bridge, extra)
        }

        fun getStatus(): StateFlow<ParaboxExtensionStatus> {
            return ext.status
        }

        fun updateStatus(status: ParaboxExtensionStatus) {
            ext.updateStatus(status)
        }

        override fun onCreate(owner: LifecycleOwner) {
            ext.onCreate()
        }

        override fun onStart(owner: LifecycleOwner) {
            ext.onStart()
        }

        override fun onResume(owner: LifecycleOwner) {
            ext.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            ext.onPause()
        }

        override fun onStop(owner: LifecycleOwner) {
            ext.onStop()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            ext.onDestroy()
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