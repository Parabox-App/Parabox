package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnection
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtension
import dalvik.system.PathClassLoader
import java.lang.ClassCastException

object ExtensionLoader {
    private const val EXTENSION_CLASS = "parabox.extension.class"
    private const val EXTENSION_FEATURE = "parabox.extension"
    private const val EXTENSION_LIB_VERSION = "parabox.extension.lib_version"
    private const val MIN_LIB_VERSION = 100
    private const val PACKAGE_FLAGS =
        PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNATURES

    fun scanInstalledApp(context: Context): List<Extension> {
        val pkgManager = context.packageManager
        @Suppress("DEPRECATION")
        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }
        return installedPkgs.filter { isPackageAnExtension(it) }.flatMap {
            createExternalExtension(context, it)
        }
    }

    fun scanAppWithPackageName(context: Context, packageName: String): List<Extension> {
        val pkgManager = context.packageManager
        val packageInfo = pkgManager.getPackageInfo(packageName, PACKAGE_FLAGS)
        return if (isPackageAnExtension(packageInfo)) {
            createExternalExtension(context, packageInfo)
        } else {
            emptyList()
        }
    }

    private fun createExternalExtension(context: Context, packageInfo: PackageInfo): List<Extension> {
        val pkgManager = context.packageManager
        val appInfo = try {
            pkgManager.getApplicationInfo(packageInfo.packageName, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return listOf(Extension.Error(
                "Unknown",
                null,
                null,
                packageInfo.packageName,
                error.message ?: "package may have been uninstalled while loading"
            ))
        }

        val libVersion = appInfo.metaData.getString(EXTENSION_LIB_VERSION) ?: "1.0.0"
        val appName = appInfo.loadLabel(pkgManager).toString()
        val appIcon = appInfo.loadIcon(pkgManager).toBitmapOrNull()?.asImageBitmap()
        val version = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        if ((libVersion.replace(".", "").toIntOrNull() ?: 0) < MIN_LIB_VERSION) {
            return listOf(Extension.Error(
                appName,
                appIcon,
                null,
                packageInfo.packageName,
                "Lib version ${libVersion} is lower than require version ${MIN_LIB_VERSION}"
            ))
        }
        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        val classNameList = appInfo.metaData.getString(EXTENSION_CLASS)?.split(";")
        return classNameList?.mapIndexed { index, extClass ->
            val fullExtClass = extClass.takeUnless { it.startsWith(".") } ?: (packageInfo.packageName + extClass)
            try {
                val clazz = Class.forName(fullExtClass, false, classLoader)
                val entrance = clazz.newInstance() as ParaboxExtension
                Extension.Success.External(
                    entrance.getName() ?: appName,
                    appIcon,
                    entrance.getDescription(),
                    entrance.getKey(),
                    entrance.getInitHandler(),
                    entrance.getConnectionClassName(),
                    version,
                    versionCode,
                    packageInfo.packageName
                )
            } catch (e: ClassCastException) {
                e.printStackTrace()
                Extension.Error(
                    appName,
                    appIcon,
                    null,
                    "${packageInfo.packageName}_$index",
                    e.message ?: "target class is not instance of ParaboxExtension"
                )
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                Extension.Error(
                    appName,
                    appIcon,
                    null,
                    "${packageInfo.packageName}_$index",
                    e.message ?: "target class not found"
                )
            }
        } ?: emptyList()
    }

    fun createExternalConnection(context: Context, connectionInfo: ConnectionInfo): Connection {
        val pkgManager = context.packageManager
        val appInfo = try {
            pkgManager.getApplicationInfo(connectionInfo.pkg, PackageManager.GET_META_DATA)
        } catch (error: PackageManager.NameNotFoundException) {
            // Unlikely, but the package may have been uninstalled at this point
            return Connection.ConnectionFail.ExtendConnectionFail(connectionInfo)
        }
        val classLoader = PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        return try {
            val ext = connectionInfo.connectionClassName.let { extClass ->
                val fullExtClass = extClass.takeUnless { it.startsWith(".") } ?: (connectionInfo.pkg + extClass)
                val clazz = Class.forName(fullExtClass, false, classLoader)
                clazz.newInstance()
            } as ParaboxConnection
            Connection.ConnectionPending.ExtendConnectionPending(
                connectionInfo, ext
            )
        } catch (e: ClassCastException) {
            e.printStackTrace()
            Connection.ConnectionFail.ExtendConnectionFail(connectionInfo)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Connection.ConnectionFail.ExtendConnectionFail(connectionInfo)
        }
    }

    fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }
}