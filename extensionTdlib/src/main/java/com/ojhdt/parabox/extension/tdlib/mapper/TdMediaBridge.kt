package com.ojhdt.parabox.extension.tdlib.mapper

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.send
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxMediaHelper
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.UUID

interface TdMediaBridge {
    suspend fun downloadAndShareFile(
        client: TdClientApi,
        tdFile: TdApi.File,
        hostPackageName: String,
        maxSizeBytes: Long,
        timeoutMs: Long = 20_000L,
    ): ParaboxResourceInfo

    suspend fun stageResourceForUpload(
        accountId: String,
        resourceInfo: ParaboxResourceInfo,
        suggestedName: String? = null,
    ): File?
}

class AndroidTdMediaBridge(
    private val context: Context,
) : TdMediaBridge {

    private val authority: String = "${context.packageName}.fileprovider"

    override suspend fun downloadAndShareFile(
        client: TdClientApi,
        tdFile: TdApi.File,
        hostPackageName: String,
        maxSizeBytes: Long,
        timeoutMs: Long,
    ): ParaboxResourceInfo {
        val existingPath = tdFile.local?.path.orEmpty()
        if (tdFile.local?.isDownloadingCompleted == true && existingPath.isNotBlank()) {
            val file = File(existingPath)
            if (file.exists()) {
                return shareLocalFile(file, hostPackageName)
            }
        }

        val expectedSize = tdFile.expectedSize.takeIf { it > 0L } ?: tdFile.size
        if (maxSizeBytes > 0L && expectedSize > maxSizeBytes) {
            return ParaboxResourceInfo.ParaboxEmptyInfo
        }

        val downloaded = withTimeoutOrNull(timeoutMs) {
            runCatching {
                client.send<TdApi.File>(
                    TdApi.DownloadFile(tdFile.id, 16, 0, 0, true)
                )
            }.getOrNull()
        } ?: return ParaboxResourceInfo.ParaboxEmptyInfo

        val path = downloaded.local?.path.orEmpty()
        if (path.isBlank()) return ParaboxResourceInfo.ParaboxEmptyInfo
        val file = File(path)
        if (!file.exists()) return ParaboxResourceInfo.ParaboxEmptyInfo
        return shareLocalFile(file, hostPackageName)
    }

    private fun shareLocalFile(file: File, hostPackageName: String): ParaboxResourceInfo {
        return try {
            val uri = ParaboxMediaHelper.shareFileWithHost(
                context = context,
                authority = authority,
                file = file,
                hostPackageName = hostPackageName,
            )
            ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(uri)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to share TDLib file via FileProvider: ${file.absolutePath}", e)
            ParaboxResourceInfo.ParaboxEmptyInfo
        }
    }

    override suspend fun stageResourceForUpload(
        accountId: String,
        resourceInfo: ParaboxResourceInfo,
        suggestedName: String?,
    ): File? {
        val uri: Uri = when (resourceInfo) {
            is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> resourceInfo.uri
            is ParaboxResourceInfo.ParaboxSyncedInfo ->
                (resourceInfo.local as? ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo)?.uri
            else -> null
        } ?: return null
        return try {
            val uploadDir = File(context.cacheDir, "upload/$accountId").apply {
                if (!exists()) mkdirs()
            }
            val safeName = suggestedName?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                ?.takeIf { it.isNotBlank() }
                ?: "upload_${UUID.randomUUID()}"
            val target = File(uploadDir, safeName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            target
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage host URI for TDLib upload: $uri", e)
            null
        }
    }

    companion object {
        private const val TAG = "AndroidTdMediaBridge"
    }
}
