package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionEndpoint
import com.ojhdtapp.parabox.domain.service.extension.transport.ExtensionTransport
import com.ojhdtapp.parabox.protocol.v2.GetManifestRequest
import com.ojhdtapp.parabox.protocol.v2.ParaboxExtensionServiceGrpcKt
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ExtensionDiscovery(
    private val context: Context,
    private val transport: ExtensionTransport,
    private val trustStore: ExtensionTrustStore,
) {
    suspend fun scanAllExternalExtensions(): List<Extension> = withContext(Dispatchers.IO) {
        val intent = Intent(ParaboxExtensionConstants.ACTION_EXTENSION_SERVICE)
        val services = queryServices(intent)
        services.flatMap { resolveInfo ->
            inspectService(resolveInfo)
        }
    }

    suspend fun scanExternalPackage(packageName: String): List<Extension> = withContext(Dispatchers.IO) {
        val intent = Intent(ParaboxExtensionConstants.ACTION_EXTENSION_SERVICE).apply {
            setPackage(packageName)
        }
        val services = queryServices(intent)
        services.flatMap { resolveInfo ->
            inspectService(resolveInfo)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryServices(intent: Intent): List<ResolveInfo> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            pm.queryIntentServices(intent, flags)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun inspectService(resolveInfo: ResolveInfo): List<Extension> {
        val serviceInfo = resolveInfo.serviceInfo ?: return emptyList()
        val packageName = serviceInfo.packageName
        val serviceClassName = serviceInfo.name
        val pm = context.packageManager

        val previewName = serviceInfo.metaData
            ?.getString(ParaboxExtensionConstants.META_PREVIEW_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: resolveInfo.loadLabel(pm)?.toString()
            ?: packageName
        val previewDesc = serviceInfo.metaData
            ?.getString(ParaboxExtensionConstants.META_PREVIEW_DESC)
        val previewIcon = runCatching {
            resolveInfo.loadIcon(pm)?.toBitmapOrNull()?.asImageBitmap()
        }.getOrNull()

        val pkgInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return listOf(
                Extension.Error(
                    name = previewName,
                    icon = previewIcon,
                    des = previewDesc,
                    key = "$packageName:$serviceClassName",
                    errMsg = "扩展包已被卸载",
                )
            )
        }

        val versionName = pkgInfo.versionName ?: "1.0.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            pkgInfo.versionCode.toLong()
        }

        val declaredProtoVersion = serviceInfo.metaData
            ?.getInt(ParaboxExtensionConstants.META_PROTOCOL_VERSION, ParaboxExtensionConstants.PROTOCOL_VERSION)
            ?: ParaboxExtensionConstants.PROTOCOL_VERSION
        if (declaredProtoVersion != ParaboxExtensionConstants.PROTOCOL_VERSION) {
            return listOf(
                Extension.Error(
                    name = previewName,
                    icon = previewIcon,
                    des = previewDesc,
                    key = "$packageName:$serviceClassName",
                    errMsg = "协议版本不兼容 (扩展 v$declaredProtoVersion, 宿主需 v${ParaboxExtensionConstants.PROTOCOL_VERSION})",
                )
            )
        }

        val signatureSha256 = SignatureVerifier.getPackageSignatureSha256(context, packageName)
            ?: return listOf(
                Extension.Error(
                    name = previewName,
                    icon = previewIcon,
                    des = previewDesc,
                    key = "$packageName:$serviceClassName",
                    errMsg = "无法读取扩展签名信息",
                )
            )

        val trustedSig = trustStore.getTrustedSignature(packageName)
        if (trustedSig == null) {
            return listOf(
                Extension.Untrusted(
                    name = previewName,
                    icon = previewIcon,
                    des = previewDesc ?: packageName,
                    key = "$packageName:$serviceClassName",
                    pkg = packageName,
                    serviceClassName = serviceClassName,
                    version = versionName,
                    versionCode = versionCode,
                    signatureSha256 = signatureSha256,
                    signatureMismatch = false,
                )
            )
        }

        if (!trustedSig.equals(signatureSha256, ignoreCase = true)) {
            return listOf(
                Extension.Untrusted(
                    name = previewName,
                    icon = previewIcon,
                    des = "签名已变更，需重新确认授权",
                    key = "$packageName:$serviceClassName",
                    pkg = packageName,
                    serviceClassName = serviceClassName,
                    version = versionName,
                    versionCode = versionCode,
                    signatureSha256 = signatureSha256,
                    signatureMismatch = true,
                )
            )
        }

        return fetchManifestFromBinderService(
            packageName = packageName,
            serviceClassName = serviceClassName,
            signatureSha256 = signatureSha256,
            fallbackName = previewName,
            fallbackIcon = previewIcon,
            fallbackVersion = versionName,
            fallbackVersionCode = versionCode,
        )
    }

    private suspend fun fetchManifestFromBinderService(
        packageName: String,
        serviceClassName: String,
        signatureSha256: String,
        fallbackName: String,
        fallbackIcon: ImageBitmap?,
        fallbackVersion: String,
        fallbackVersionCode: Long,
    ): List<Extension> {
        val probeEndpoint = ExtensionEndpoint.BinderService(
            packageName = packageName,
            serviceClassName = serviceClassName,
            extensionKey = "",
            expectedSignatureSha256 = signatureSha256,
        )
        return try {
            val channel = transport.acquireChannel(probeEndpoint)
            val stub = ParaboxExtensionServiceGrpcKt.ParaboxExtensionServiceCoroutineStub(channel)
                .withDeadlineAfter(MANIFEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            val manifest = stub.getManifest(
                GetManifestRequest.newBuilder()
                    .setHostProtocolVersion(ParaboxExtensionConstants.PROTOCOL_VERSION)
                    .setHostPackageName(context.packageName)
                    .build()
            )
            if (manifest.protocolVersion != ParaboxExtensionConstants.PROTOCOL_VERSION) {
                return listOf(
                    Extension.Error(
                        name = fallbackName,
                        icon = fallbackIcon,
                        des = null,
                        key = "$packageName:$serviceClassName",
                        errMsg = "协议版本不匹配 (扩展 v${manifest.protocolVersion})",
                    )
                )
            }
            manifest.extensionsList.map { desc ->
                val icon = desc.iconPng.takeIf { !it.isEmpty }?.let { bytes ->
                    runCatching {
                        BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())?.asImageBitmap()
                    }.getOrNull()
                } ?: fallbackIcon
                Extension.Success.External(
                    name = desc.name.ifBlank { fallbackName },
                    icon = icon,
                    des = desc.description.ifBlank { null },
                    key = desc.key,
                    endpoint = ExtensionEndpoint.BinderService(
                        packageName = packageName,
                        serviceClassName = serviceClassName,
                        extensionKey = desc.key,
                        expectedSignatureSha256 = signatureSha256,
                    ),
                    serviceClassName = serviceClassName,
                    version = manifest.versionName.ifBlank { fallbackVersion },
                    versionCode = manifest.versionCode.takeIf { it > 0 } ?: fallbackVersionCode,
                    pkg = packageName,
                    signatureSha256 = signatureSha256,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch manifest from $packageName/$serviceClassName", e)
            transport.invalidateChannel(probeEndpoint)
            listOf(
                Extension.Error(
                    name = fallbackName,
                    icon = fallbackIcon,
                    des = null,
                    key = "$packageName:$serviceClassName",
                    errMsg = e.message ?: "无法连接到扩展服务",
                )
            )
        }
    }

    companion object {
        private const val TAG = "ExtensionDiscovery"
        private const val MANIFEST_TIMEOUT_SECONDS = 8L
    }
}
