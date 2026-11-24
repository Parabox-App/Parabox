package com.ojhdtapp.parabox.core.util

import android.content.Context
import android.net.Uri
import android.util.Log
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.imageLoader
import com.ojhdtapp.parabox.core.HiltApplication
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.buffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.util.zip.GZIPInputStream

enum class AnimatedMediaKind {
    IMAGE,
    TGS_LOTTIE,
    LOOPING_VIDEO,
}

@OptIn(ExperimentalCoilApi::class)
class CachedMediaHandle(
    val file: File,
    private val snapshot: DiskCache.Snapshot? = null,
) : Closeable {
    override fun close() {
        runCatching { snapshot?.close() }
    }
}

/**
 * Exposure-driven loader for animated extension media (`.tgs` Lottie stickers, `.webm` video
 * stickers, and `.mp4` looping animations).
 *
 * Reuses Coil's bounded 50 MB LRU [DiskCache] so all images, avatars, stickers, and looping
 * animations share a single capped cache directory with zero extra disk bloat.
 */
@OptIn(ExperimentalCoilApi::class)
object ExtensionMediaLoader {
    private const val TAG = "ExtensionMediaLoader"
    private const val MAX_STREAM_BYTES = 20L * 1024L * 1024L
    private const val MAX_DECOMPRESSED_TGS_BYTES = 10 * 1024 * 1024
    private const val ACTIVE_WAIT_TIMEOUT_MS = 8_000L

    fun detectAnimatedMediaKind(fileName: String?): AnimatedMediaKind {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return when (ext) {
            "tgs" -> AnimatedMediaKind.TGS_LOTTIE
            "webm", "mp4", "m4v" -> AnimatedMediaKind.LOOPING_VIDEO
            else -> AnimatedMediaKind.IMAGE
        }
    }

    /**
     * Decompresses Telegram `.tgs` (gzipped Lottie JSON) bytes into a UTF-8 JSON string.
     * Also accepts plain uncompressed Lottie JSON starting with `{`.
     */
    fun decompressTgsBytes(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val isGzip = bytes.size >= 2 &&
            bytes[0] == 0x1F.toByte() &&
            bytes[1] == 0x8B.toByte()
        if (!isGzip) {
            val text = bytes.toString(Charsets.UTF_8).trimStart()
            return if (text.startsWith("{")) text else null
        }
        return runCatching {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = gzip.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_DECOMPRESSED_TGS_BYTES) {
                        return@runCatching null
                    }
                    out.write(buf, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
        }.getOrNull()
    }

    /**
     * Loads and decompresses a `.tgs` sticker into a Lottie JSON string on demand.
     */
    suspend fun loadTgsJson(context: Context, model: Any?): String? = withContext(Dispatchers.IO) {
        val handle = openCachedMedia(context, model) ?: return@withContext null
        handle.use {
            val bytes = runCatching { it.file.readBytes() }.getOrNull() ?: return@withContext null
            decompressTgsBytes(bytes)
        }
    }

    /**
     * Resolves a media model (`parabox-ext://`, `File`, or local `file://` URI) to a readable
     * local [CachedMediaHandle]. For `parabox-ext://`, checks Coil's bounded [DiskCache] first
     * and streams from the extension on demand on a cache miss.
     *
     * Callers MUST close the returned [CachedMediaHandle] when done (e.g., in `DisposableEffect.onDispose`).
     */
    suspend fun openCachedMedia(context: Context, model: Any?): CachedMediaHandle? = withContext(Dispatchers.IO) {
        when (model) {
            is File -> if (model.exists()) CachedMediaHandle(model) else null
            is Uri -> openFromUriString(context, model.toString())
            is String -> openFromUriString(context, model)
            else -> null
        }
    }

    private suspend fun openFromUriString(context: Context, uriString: String): CachedMediaHandle? {
        if (uriString.startsWith("file://")) {
            val path = Uri.parse(uriString).path ?: return null
            val file = File(path)
            return if (file.exists()) CachedMediaHandle(file) else null
        }

        val parsed = ExtensionResourceUri.parse(uriString) ?: return null
        val diskCache = context.imageLoader.diskCache ?: return null

        diskCache.openSnapshot(uriString)?.let { snapshot ->
            return CachedMediaHandle(snapshot.data.toFile(), snapshot)
        }

        val extensionManager = (context.applicationContext as? HiltApplication)?.extensionManager
            ?: return null
        val conn = resolveActiveConnection(extensionManager, parsed.connectionId) ?: return null
        val stream = conn.streamResource(parsed.resourceId)

        val editor = diskCache.openEditor(uriString) ?: return null
        var totalBytes = 0L
        return try {
            diskCache.fileSystem.sink(editor.data).buffer().use { sink ->
                stream.collect { chunk ->
                    val bytes = chunk.data.toByteArray()
                    if (bytes.isNotEmpty()) {
                        totalBytes += bytes.size
                        if (totalBytes > MAX_STREAM_BYTES) {
                            throw IllegalStateException("Media exceeded $MAX_STREAM_BYTES bytes: $uriString")
                        }
                        sink.write(bytes)
                    }
                }
            }
            if (totalBytes <= 0L) {
                runCatching { editor.abort() }
                return null
            }
            val snapshot = editor.commitAndOpenSnapshot() ?: return null
            CachedMediaHandle(snapshot.data.toFile(), snapshot)
        } catch (e: Exception) {
            runCatching { editor.abort() }
            Log.w(TAG, "Failed to stream extension media: $uriString", e)
            null
        }
    }

    private suspend fun resolveActiveConnection(
        extensionManager: ExtensionManager,
        connectionId: Long,
    ): Connection.ConnectionSuccess? {
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
}
