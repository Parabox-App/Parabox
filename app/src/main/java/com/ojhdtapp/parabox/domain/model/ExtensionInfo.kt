package com.ojhdtapp.parabox.domain.model

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmapOrNull

sealed interface ExtensionInfo {
    val name: String
    val icon: Any?
    fun getKey() : String
    data class BuiltInExtensionInfo(
        override val name: String,
        override val icon: Any?,
        val description: String,
        val builtInKey: String,
    ) : ExtensionInfo {
        override fun getKey(): String {
            return builtInKey
        }
    }

    data class ExtendExtensionInfo(
        override val name: String,
        override val icon: ImageBitmap?,
        val version: String,
        val packageInfo: PackageInfo,
    ) : ExtensionInfo {
        override fun getKey(): String {
            return packageInfo.packageName
        }
    }
}



fun PackageInfo.toConnection(context: Context) : ExtensionInfo.ExtendExtensionInfo{
    val pm = context.packageManager
    val name = applicationInfo.loadLabel(pm).toString()
    val iconBm = applicationInfo.loadIcon(pm).toBitmapOrNull()?.asImageBitmap()
    val version = buildString {
            append(" ${versionName}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                append("(${longVersionCode})")
            } else {
                append("(${versionCode})")
            }
        }
    return ExtensionInfo.ExtendExtensionInfo(
        name, iconBm, version, this
    )
}