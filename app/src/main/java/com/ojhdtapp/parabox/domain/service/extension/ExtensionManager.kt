package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.entity.ExtensionInfoEntity
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.repository.ExtensionInfoRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExtensionManager(
    val context: Context,
    val extensionInfoRepository: ExtensionInfoRepository
) {
    // Pkg
    private val _extensionPkgFlow = MutableStateFlow(emptyList<PackageInfo>())
    val extensionPkgFlow = _extensionPkgFlow.asStateFlow()

    // Init
    private val _extensionFlow = MutableStateFlow(emptyList<Extension>())
    val extensionFlow = _extensionFlow.asStateFlow()

    fun addPendingExtension(alias: String, packageInfo: PackageInfo, extra: String): Long {
        val pkgManager = context.packageManager
        val appInfo = try {
            pkgManager.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return -1L
        }
        val extName = pkgManager.getApplicationLabel(appInfo).toString()
        val versionName = packageInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        if (versionName.isNullOrEmpty()) {
            return -1L
        }
        return extensionInfoRepository.insertExtensionInfo(
            ExtensionInfoEntity(
                alias, packageInfo.packageName, extName, versionName, versionCode, extra
            )
        )
    }

    fun removePendingExtension(extensionId: Long): Boolean {
        return extensionInfoRepository.deleteExtensionInfoById(extensionId) > -1
    }

    fun createAndTryAppendExtension(extensionInfo: ExtensionInfo): Extension {
        val extension = ExtensionLoader.createExtension(context, extensionInfo)
        _extensionFlow.update {
            it + extension
        }
        return extension
    }

    fun updateExtension(newExtension: Extension) {
        _extensionFlow.update {
            it.toMutableList().apply {
                val index = indexOfFirst { it.extensionId == newExtension.extensionId }
                if (index > -1) {
                    set(index, newExtension)
                } else {
                    Log.e("parabox", "extension state update error")
                }
            }
        }
    }

    fun refreshExtensionPkg() {
        Log.d("bbb", "refreshExtensionPkg")
        _extensionPkgFlow.value = ExtensionLoader.getExtensionPkgInfo(context)
    }
}