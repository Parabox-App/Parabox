package com.ojhdtapp.parabox.domain.model

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class Extension private constructor(
    override val alias: String,
    override val name: String,
    override val pkg: String,
    override val version: String,
    override val versionCode: Long,
    override val extra: Bundle,
    override val extensionId: Long,
) : ExtensionInfo(alias, pkg, name, version, versionCode, extra, extensionId) {

    class ExtensionPending(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: Bundle,
        val ext: ParaboxExtension,
        extensionId: Long,
    ) : Extension(alias, name, pkgName, versionName, versionCode, extra, extensionId) {
        constructor(extensionInfo: ExtensionInfo, ext: ParaboxExtension) : this(
            extensionInfo.alias,
            extensionInfo.name,
            extensionInfo.pkg,
            extensionInfo.version,
            extensionInfo.versionCode,
            extensionInfo.extra,
            ext,
            extensionInfo.extensionId
        )
        constructor(extensionSuccess: ExtensionSuccess) : this(
            extensionSuccess.alias,
            extensionSuccess.name,
            extensionSuccess.pkg,
            extensionSuccess.version,
            extensionSuccess.versionCode,
            extensionSuccess.extra,
            extensionSuccess.ext,
            extensionSuccess.extensionId)
    }

    class ExtensionFail(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: Bundle,
        extensionId: Long
    ) : Extension(alias, name, pkgName, versionName, versionCode, extra, extensionId) {
        constructor(info: ExtensionInfo) : this(
            info.alias,
            info.name,
            info.pkg,
            info.version,
            info.versionCode,
            info.extra,
            info.extensionId
        )
    }

    open class ExtensionSuccess private constructor(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: Bundle,
        val ext: ParaboxExtension,
        val job: Job,
        extensionId: Long
    ) : DefaultLifecycleObserver, Extension(alias, name, pkgName, versionName, versionCode, extra, extensionId) {
        constructor(extensionInfo: ExtensionInfo, ext: ParaboxExtension, job: Job) : this(
            extensionInfo.alias,
            extensionInfo.name,
            extensionInfo.pkg,
            extensionInfo.version,
            extensionInfo.versionCode,
            extensionInfo.extra,
            ext,
            job,
            extensionInfo.extensionId
        )

        constructor(extensionPending: ExtensionPending, job: Job) : this(
            extensionPending.alias,
            extensionPending.name,
            extensionPending.pkg,
            extensionPending.version,
            extensionPending.versionCode,
            extensionPending.extra,
            extensionPending.ext,
            job,
            extensionPending.extensionId
        )

        fun toExtensionInfo(): ExtensionInfo {
            return ExtensionInfo(
                alias = alias,
                pkg = pkg,
                name = name,
                version = version,
                versionCode = versionCode,
                extra = extra,
                extensionId = extensionId
            )
        }

        suspend fun init(context: Context, bridge: ParaboxBridge) {
            ext.init(context, bridge)
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