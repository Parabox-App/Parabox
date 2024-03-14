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
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

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

    fun createAndTryAppendExtension(extensionInfo: ExtensionInfo) {
        val extension = ExtensionLoader.createExtension(context, extensionInfo)
        _extensionFlow.update {
            it + extension
        }
        Log.d("parabox", "append extension=${_extensionFlow.value}")
    }

    // replace the old extension with the new one if the extensionId is the same
    fun updateExtensions(newExtensionList: List<Extension>) {
        _extensionFlow.update {
            it.toMutableList().apply {
                replaceAll {
                    newExtensionList.find { newExtension -> it.extensionId == newExtension.extensionId } ?: it
                }
            }
        }
    }

    fun removeExtension(extensionId: Long) {
        _extensionFlow.update {
            it.filter { it.extensionId != extensionId }
        }
    }

    fun refreshExtensionPkg() {
        _extensionPkgFlow.value = ExtensionLoader.getExtensionPkgInfo(context)
    }

    // Only ExtensionSuccess, no Context and bridge changed
    fun restartExtension(extensionId: Long) {
        _extensionFlow.value.find { it.extensionId == extensionId }?.also {
            if (it is Extension.ExtensionSuccess) {
                try {
                    it.ext.updateStatus(ParaboxExtensionStatus.Pending)
                    it.job.cancel(CancellationException("restart"))
                    it.ext.onPause()
                    it.ext.onStop()
                    it.ext.onDestroy()
                    updateExtensions(listOf(Extension.ExtensionPending(it)))
                } catch (e: Exception) {
                    it.ext.updateStatus(ParaboxExtensionStatus.Error(e.message ?: "restart error"))
                    Log.e("parabox", "restartExtension error", e)
                }
            }
        }
    }
}