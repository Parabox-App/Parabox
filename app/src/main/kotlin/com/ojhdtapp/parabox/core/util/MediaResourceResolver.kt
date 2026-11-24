package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Protocol-agnostic media resource resolver for non-image media (audio, future video/files).
 *
 * Resolution order:
 * 1. Valid local URI ([ParaboxResourceInfo.ParaboxLocalInfo] / [ParaboxResourceInfo.ParaboxSyncedInfo.local]),
 *    skipping expired cross-process TDLib FileProvider URIs.
 * 2. PEP v2 on-demand extension stream (`parabox-ext://<sessionId>/<resourceId>`) backed by a
 *    bounded LRU disk cache under `cacheDir/ext_audio_cache/`.
 * 3. Standard remote URL / cloud drive resources via [CloudService].
 */
@Singleton
class MediaResourceResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extensionManager: ExtensionManager,
    private val fileUtil: FileUtil,
    private val cloudService: ParaboxCloudService,
) {
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Result<File>>>()

    suspend fun resolveAudioUri(
        resourceInfo: ParaboxResourceInfo,
        suggestedFileName: String? = null,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        // 1. Local info (excluding expired TDLib FileProvider URIs)
        val localInfo = when (resourceInfo) {
            is ParaboxResourceInfo.ParaboxSyncedInfo -> resourceInfo.local
            is ParaboxResourceInfo.ParaboxLocalInfo -> resourceInfo
            else -> null
        }
        if (localInfo != null && !ExtensionResourceUri.isExpiredTdlibFileProviderUri(localInfo)) {
            val model = localInfo.getModel()
            if (fileUtil.checkResourceModelAvailable(model)) {
                val uri = fileUtil.getUriFromResourceModel(model)
                if (uri != null) {
                    return@withContext Result.success(uri)
                }
            }
        }

        // 2. Remote info (either parabox-ext:// PEP v2 stream or standard URL/Drive)
        val remoteInfo = when (resourceInfo) {
            is ParaboxResourceInfo.ParaboxSyncedInfo -> resourceInfo.remote
            is ParaboxResourceInfo.ParaboxRemoteInfo -> resourceInfo
            else -> null
        } ?: return@withContext Result.failure(IOException("音频资源不可用"))

        if (remoteInfo is ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo) {
            val rawUrl = remoteInfo.url.trim()
            if (rawUrl.startsWith("${ExtensionResourceUri.SCHEME}://")) {
                val uri = Uri.parse(rawUrl)
                val parsed = ExtensionResourceUri.parse(uri)
                    ?: return@withContext Result.failure(IOException("无效的扩展资源地址"))
                val fileResult = streamAndCacheExtensionResource(
                    cacheKey = rawUrl,
                    suggestedFileName = suggestedFileName,
                    cacheDir = cacheDir,
                    inFlight = inFlight,
                    openStream = {
                        val conn = resolveActiveConnection(parsed.connectionId)
                        conn?.streamResource(parsed.resourceId)
                    },
                )
                return@withContext fileResult.map { Uri.fromFile(it) }
            }
        }

        val synced = runCatching {
            cloudService.download(remoteInfo).awaitUntilSuccess(REMOTE_DOWNLOAD_TIMEOUT_MS)
        }.getOrNull()
        val localUri = synced?.localUri
        if (localUri != null) {
            return@withContext Result.success(localUri)
        }
        Result.failure(IOException("音频下载失败"))
    }

    private suspend fun resolveActiveConnection(connectionId: Long): Connection.ConnectionSuccess? {
        val immediate = extensionManager.connectionFlow.value
            .filterIsInstance<Connection.ConnectionSuccess>()
            .firstOrNull { it.connectionId == connectionId }
        if (immediate != null && immediate.getStatus().value is ParaboxConnectionStatus.Active) {
            return immediate
        }
        return withTimeoutOrNull(ACTIVE_WAIT_TIMEOUT_MS) {
            val conn = extensionManager.connectionFlow
                .first { list ->
                    list.any { it is Connection.ConnectionSuccess && it.connectionId == connectionId }
                }
                .filterIsInstance<Connection.ConnectionSuccess>()
                .first { it.connectionId == connectionId }
            conn.getStatus().first { it is ParaboxConnectionStatus.Active }
            conn
        }
    }

    companion object {
        private const val TAG = "MediaResourceResolver"
        private const val CACHE_DIR_NAME = "ext_audio_cache"
        private const val MAX_CACHE_BYTES = 100L * 1024L * 1024L
        private const val MAX_STREAM_BYTES = 50L * 1024L * 1024L
        private const val ACTIVE_WAIT_TIMEOUT_MS = 8_000L
        private const val REMOTE_DOWNLOAD_TIMEOUT_MS = 15_000L

        internal suspend fun streamAndCacheExtensionResource(
            cacheKey: String,
            suggestedFileName: String?,
            cacheDir: File,
            inFlight: ConcurrentHashMap<String, CompletableDeferred<Result<File>>>,
            openStream: suspend () -> Flow<ResourceChunk>?,
        ): Result<File> {
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val hash = sha256Hex(cacheKey)
            findCachedFile(cacheDir, hash)?.let { existing ->
                existing.setLastModified(System.currentTimeMillis())
                return Result.success(existing)
            }

            val createdDeferred = CompletableDeferred<Result<File>>()
            val existingDeferred = inFlight.putIfAbsent(hash, createdDeferred)
            if (existingDeferred != null) {
                return existingDeferred.await()
            }

            return try {
                // Double-check after acquiring in-flight slot
                findCachedFile(cacheDir, hash)?.let { existing ->
                    existing.setLastModified(System.currentTimeMillis())
                    val ok = Result.success(existing)
                    createdDeferred.complete(ok)
                    return ok
                }

                val stream = openStream()
                    ?: throw IOException("扩展连接未就绪，无法拉取音频")

                val tempFile = File(cacheDir, "$hash.tmp_${System.nanoTime()}")
                var detectedMime: String? = null
                var totalBytes = 0L

                try {
                    FileOutputStream(tempFile).use { output ->
                        stream.collect { chunk ->
                            if (detectedMime == null && chunk.mimeType.isNotBlank() &&
                                chunk.mimeType != "application/octet-stream"
                            ) {
                                detectedMime = chunk.mimeType
                            }
                            val bytes = chunk.data.toByteArray()
                            if (bytes.isNotEmpty()) {
                                totalBytes += bytes.size
                                if (totalBytes > MAX_STREAM_BYTES) {
                                    throw IOException("音频文件超过最大限制")
                                }
                                output.write(bytes)
                            }
                        }
                    }

                    if (totalBytes <= 0L) {
                        tempFile.delete()
                        throw IOException("扩展返回空音频流")
                    }

                    val ext = resolveAudioExtension(detectedMime, suggestedFileName)
                    val targetFile = File(cacheDir, "$hash.$ext")
                    if (targetFile.exists()) targetFile.delete()
                    if (!tempFile.renameTo(targetFile)) {
                        tempFile.copyTo(targetFile, overwrite = true)
                        tempFile.delete()
                    }
                    targetFile.setLastModified(System.currentTimeMillis())
                    trimCacheIfNeeded(cacheDir, MAX_CACHE_BYTES)

                    val result = Result.success(targetFile)
                    createdDeferred.complete(result)
                    result
                } catch (e: Exception) {
                    runCatching { tempFile.delete() }
                    throw e
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stream extension audio resource: $cacheKey", e)
                val failure = Result.failure<File>(e)
                createdDeferred.complete(failure)
                failure
            } finally {
                inFlight.remove(hash, createdDeferred)
            }
        }

        internal fun resolveAudioExtension(mimeType: String?, suggestedFileName: String?): String {
            val fromMime = when (mimeType?.lowercase()?.substringBefore(';')?.trim()) {
                "audio/ogg", "audio/opus", "application/ogg" -> "ogg"
                "audio/mpeg", "audio/mp3" -> "mp3"
                "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac" -> "m4a"
                "audio/wav", "audio/x-wav" -> "wav"
                "audio/flac" -> "flac"
                "audio/amr" -> "amr"
                else -> null
            }
            if (fromMime != null) return fromMime

            val fromName = suggestedFileName
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in setOf("ogg", "oga", "opus", "mp3", "m4a", "aac", "wav", "flac", "amr") }
            return fromName ?: "ogg"
        }

        private fun findCachedFile(cacheDir: File, hash: String): File? {
            return cacheDir.listFiles()?.firstOrNull { file ->
                file.isFile &&
                    file.length() > 0L &&
                    !file.name.contains(".tmp_") &&
                    file.name.startsWith("$hash.")
            }
        }

        private fun trimCacheIfNeeded(cacheDir: File, maxBytes: Long) {
            val files = cacheDir.listFiles()?.filter { it.isFile && !it.name.contains(".tmp_") } ?: return
            var total = files.sumOf { it.length() }
            if (total <= maxBytes) return
            files.sortedBy { it.lastModified() }.forEach { file ->
                if (total <= maxBytes) return@forEach
                val len = file.length()
                if (file.delete()) {
                    total -= len
                }
            }
        }

        private fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }
    }
}
