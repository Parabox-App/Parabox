package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import dalvik.system.PathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.lang.ClassCastException

object ExtensionLoader {
    private const val EXTENSION_CLASS = "parabox.extension.class"
    private const val EXTENSION_FEATURE = "parabox.extension"
    private const val PACKAGE_FLAGS =
        PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES


    fun getExtensionPkgInfo(context: Context): List<PackageInfo> {
        val pkgManager = context.packageManager

        @Suppress("DEPRECATION")
        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }
        return installedPkgs.filter { isPackageAnExtension(it) }
    }

    fun createExtension(context: Context, extensionInfo: ExtensionInfo): Extension {
        val pkgManager = context.packageManager
        val appInfo = try {
            pkgManager.getApplicationInfo(extensionInfo.pkg, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return Extension.ExtensionFail(extensionInfo)
        }
        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        return try {
            val ext = appInfo.metaData.getString(EXTENSION_CLASS)?.let { extClass ->
                val fullExtClass = extClass.takeUnless { it.startsWith(".") } ?: (extensionInfo.pkg + extClass)
                val clazz = Class.forName(fullExtClass, false, classLoader)
                clazz.newInstance()
            } as ParaboxExtension
            Extension.ExtensionPending(
                extensionInfo, ext
            )
        } catch (e: ClassCastException) {
            e.printStackTrace()
            Extension.ExtensionFail(extensionInfo)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Extension.ExtensionFail(extensionInfo)
        }
    }

    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }
}