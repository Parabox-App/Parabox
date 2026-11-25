package com.ojhdt.parabox.extension.tdlib.mapper

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.send
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxMediaHelper
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.UUID

data class MediaMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val durationSeconds: Int = 0,
    val sizeBytes: Long = 0L,
)

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

    fun probeImageMetadata(file: File): MediaMetadata = MediaMetadata(sizeBytes = file.length())

    fun probeVideoMetadata(file: File): MediaMetadata = MediaMetadata(sizeBytes = file.length())

    fun probeAudioDurationSeconds(file: File): Int = 0
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
            val uploadDir = File(context.cacheDir, "upload/$accountId/${UUID.randomUUID()}").apply {
                if (!exists()) mkdirs()
            }
            val cleaned = suggestedName
                ?.substringAfterLast('/')
                ?.replace(Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]"), "_")
                ?.takeIf { it.isNotBlank() }
            val resolvedName = when {
                cleaned != null && cleaned.contains('.') -> cleaned
                else -> {
                    val mimeExt = runCatching {
                        context.contentResolver.getType(uri)?.let { mime ->
                            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                        }
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                    val base = cleaned ?: "upload_${UUID.randomUUID().toString().take(8)}"
                    if (mimeExt != null) "$base.$mimeExt" else base
                }
            }
            val target = File(uploadDir, resolvedName)
            val inputStream = if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).takeIf { it.exists() }?.inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return null
            inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!target.exists() || target.length() == 0L) {
                target.delete()
                return null
            }
            target
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage host URI for TDLib upload: $uri", e)
            null
        }
    }

    override fun probeImageMetadata(file: File): MediaMetadata {
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            MediaMetadata(
                width = opts.outWidth.coerceAtLeast(0),
                height = opts.outHeight.coerceAtLeast(0),
                sizeBytes = file.length(),
            )
        }.getOrDefault(MediaMetadata(sizeBytes = file.length()))
    }

    override fun probeVideoMetadata(file: File): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            MediaMetadata(
                width = width.coerceAtLeast(0),
                height = height.coerceAtLeast(0),
                durationSeconds = ((durationMs + 500L) / 1000L).toInt().coerceAtLeast(0),
                sizeBytes = file.length(),
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to probe video metadata: ${file.absolutePath}", e)
            MediaMetadata(sizeBytes = file.length())
        } finally {
            runCatching { retriever.release() }
        }
    }

    override fun probeAudioDurationSeconds(file: File): Int {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            ((durationMs + 500L) / 1000L).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to probe audio duration: ${file.absolutePath}", e)
            0
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private const val TAG = "AndroidTdMediaBridge"
    }
}
