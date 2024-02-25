package com.ojhdtapp.parabox.domain.model

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxBridge
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension

sealed class Extension private constructor(
    override val alias: String,
    override val name: String,
    override val pkg: String,
    override val version: String,
    override val versionCode: Long,
    override val extra: String,
    override val extensionId: Long,
) : ExtensionInfo(alias, pkg, name, version, versionCode, extra, extensionId) {

    class ExtensionPending(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: String,
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
    }

    class ExtensionFail(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: String,
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

    class ExtensionSuccess private constructor(
        alias: String,
        name: String,
        pkgName: String,
        versionName: String,
        versionCode: Long,
        extra: String,
        val ext: ParaboxExtension,
        extensionId: Long
    ) : DefaultLifecycleObserver, Extension(alias, name, pkgName, versionName, versionCode, extra, extensionId) {
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

        constructor(extensionPending: ExtensionPending) : this(
            extensionPending.alias,
            extensionPending.name,
            extensionPending.pkg,
            extensionPending.version,
            extensionPending.versionCode,
            extensionPending.extra,
            extensionPending.ext,
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

        fun init(context: Context, bridge: ParaboxBridge) {
            ext.init(context, bridge)
        }

        fun isLoaded(): Boolean {
            return ext.isLoaded.value
        }

        override fun onCreate(owner: LifecycleOwner) {
            ext.onCreate(owner)
        }

        override fun onStart(owner: LifecycleOwner) {
            ext.onStart(owner)
        }

        override fun onResume(owner: LifecycleOwner) {
            ext.onResume(owner)
        }

        override fun onPause(owner: LifecycleOwner) {
            ext.onPause(owner)
        }

        override fun onStop(owner: LifecycleOwner) {
            ext.onStop(owner)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            ext.onDestroy(owner)
        }
    }
}