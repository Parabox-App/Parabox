package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.Lifecycle
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import dalvik.system.PathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.lang.ClassCastException

object ExtensionLoader {
    private const val EXTENSION_CLASS = "parabox.extension.class"
    private const val EXTENSION_FEATURE = "parabox.extension"
    private const val PACKAGE_FLAGS =
        PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES

    fun loadExtensions(context: Context): List<LoadResult> {
        val pkgManager = context.packageManager

        @Suppress("DEPRECATION")
        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val extPkgs = installedPkgs.filter { isPackageAnExtension(it) }

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadExtension(context, it.packageName, it) }
            }
            deferred.map { it.await() }
        }
    }

    private fun loadExtension(context: Context, pkgName: String, pkgInfo: PackageInfo): LoadResult {
        Log.d("parabox", "loading:$pkgName")
        val pkgManager = context.packageManager

        val appInfo = try {
            pkgManager.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return LoadResult.Error
        }

        val extName = pkgManager.getApplicationLabel(appInfo).toString()
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            return LoadResult.Error
        }
        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        return try {
            val ext = appInfo.metaData.getString(EXTENSION_CLASS)?.let { extClass ->
                val fullExtClass = extClass.takeUnless { it.startsWith(".") } ?: (pkgInfo.packageName + extClass)
                val clazz = Class.forName(fullExtClass, false, classLoader)
                clazz.newInstance()
            } as ParaboxExtension
            LoadResult.Success(
                Extension(
                    name = extName,
                    pkgName = pkgName,
                    versionName = versionName,
                    versionCode = versionCode,
                    ext = ext
                )
            )
        } catch (e: ClassCastException) {
            e.printStackTrace()
            LoadResult.Error
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            LoadResult.Error
        }
    }

    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
//        Log.d(
//            "parabox",
//            "pkg:${pkgInfo.packageName}, features:${
//                pkgInfo.reqFeatures.orEmpty().map { it.name ?: "null" }.joinToString(",")
//            }"
//        )
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }
}