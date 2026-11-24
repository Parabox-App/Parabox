package com.ojhdtapp.parabox.core.util

import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.service.extension.ExtensionManager
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okio.Buffer
import okio.buffer
import java.io.IOException

object ExtensionResourceUri {
    const val SCHEME = "parabox-ext"
    private const val TDLIB_PKG = "com.ojhdt.parabox.extension.tdlib"

    data class Parsed(
        val sessionId: String,
        val connectionId: Long,
        val resourceId: String,
    )

    fun parse(uri: Uri): Parsed? = parse(uri.toString())

    fun parse(uriString: String): Parsed? {
        val prefix = "$SCHEME://"
        if (!uriString.startsWith(prefix)) return null
        val rest = uriString.removePrefix(prefix)
        val slashIndex = rest.indexOf('/')
        if (slashIndex <= 0) return null
        val sessionId = rest.substring(0, slashIndex).takeIf { it.isNotBlank() } ?: return null
        val connectionId = sessionId.removePrefix("conn-").toLongOrNull() ?: return null
        val resourceId = rest.substring(slashIndex + 1).trimStart('/').takeIf { it.isNotBlank() } ?: return null
        return Parsed(
            sessionId = sessionId,
            connectionId = connectionId,
            resourceId = resourceId,
        )
    }

    fun isExpiredTdlibFileProviderUri(resourceInfo: ParaboxResourceInfo?): Boolean {
        val uriStr = when (resourceInfo) {
            is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> resourceInfo.uri.toString()
            is ParaboxResourceInfo.ParaboxSyncedInfo -> resourceInfo.local.getModel()?.toString()
            else -> null
        } ?: return false
        return uriStr.startsWith("content://$TDLIB_PKG.fileprovider/")
    }

    /**
     * Resolves a [ParaboxResourceInfo] for display, automatically falling back to an on-demand
     * `parabox-ext://` avatar URI for legacy TDLib rows that have empty or expired
     * FileProvider avatars.
     */
    fun resolveDisplayAvatarInfo(
        avatar: ParaboxResourceInfo,
        pkg: String?,
        extensionId: Long?,
        uid: String?,
        isChat: Boolean,
    ): ParaboxResourceInfo {
        val rawModel = avatar.getModel()
        if (rawModel != null && !isExpiredTdlibFileProviderUri(avatar)) {
            return avatar
        }
        if (pkg == TDLIB_PKG && extensionId != null && extensionId > 0L && !uid.isNullOrBlank() && uid != "__parabox_self__") {
            val kind = if (isChat) "chat" else "user"
            return ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(
                url = "$SCHEME://conn-$extensionId/avatar/$kind/$uid"
            )
        }
        return avatar
    }

    /**
     * Resolves a display model for Coil, automatically falling back to an on-demand
     * `parabox-ext://` avatar URI for legacy TDLib rows that have empty or expired
     * FileProvider avatars.
     */
    fun resolveDisplayAvatarModel(
        avatar: ParaboxResourceInfo?,
        pkg: String?,
        extensionId: Long?,
        uid: String?,
        isChat: Boolean,
    ): Any? {
        if (avatar == null) {
            return if (pkg == TDLIB_PKG && extensionId != null && extensionId > 0L && !uid.isNullOrBlank() && uid != "__parabox_self__") {
                val kind = if (isChat) "chat" else "user"
                "$SCHEME://conn-$extensionId/avatar/$kind/$uid"
            } else {
                null
            }
        }
        return resolveDisplayAvatarInfo(avatar, pkg, extensionId, uid, isChat).getModel()
    }
}

/**
 * Exposure-driven Coil [Fetcher] for `parabox-ext://<sessionId>/<resourceId>` URIs.
 *
 * Only streams media/avatars over PEP v2 `StreamResource` when a Composable is actually
 * exposed in the viewport, stores results in Coil's bounded LRU [DiskCache], and
 * cancels automatically when the Composable leaves composition.
 */
@OptIn(ExperimentalCoilApi::class)
class ExtensionResourceFetcher(
    private val uri: Uri,
    private val options: Options,
    private val diskCache: DiskCache?,
    private val resourceStreamer: suspend (connectionId: Long, resourceId: String) -> Flow<ResourceChunk>?,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val parsed = ExtensionResourceUri.parse(uri)
            ?: throw IOException("Invalid parabox-ext URI: $uri")
        val cacheKey = options.diskCacheKey ?: uri.toString()

        if (options.diskCachePolicy.readEnabled && diskCache != null) {
            diskCache.openSnapshot(cacheKey)?.let { snapshot ->
                return SourceResult(
                    source = ImageSource(
                        file = snapshot.data,
                        fileSystem = diskCache.fileSystem,
                        diskCacheKey = cacheKey,
                        closeable = snapshot,
                    ),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            }
        }

        val stream = resourceStreamer(parsed.connectionId, parsed.resourceId)
            ?: throw IOException("Extension connection ${parsed.connectionId} unavailable for $uri")

        var detectedMime: String? = null
        var totalBytes = 0L
        val buffer = Buffer()

        stream.collect { chunk ->
            if (detectedMime == null && chunk.mimeType.isNotBlank() && chunk.mimeType != "application/octet-stream") {
                detectedMime = chunk.mimeType
            }
            val bytes = chunk.data.toByteArray()
            if (bytes.isNotEmpty()) {
                totalBytes += bytes.size
                if (totalBytes > MAX_STREAM_BYTES) {
                    throw IOException("Extension resource exceeded ${MAX_STREAM_BYTES} bytes: $uri")
                }
                buffer.write(bytes)
            }
        }

        if (totalBytes <= 0L) {
            throw IOException("Empty extension resource stream: $uri")
        }

        if (options.diskCachePolicy.writeEnabled && diskCache != null) {
            val editor = diskCache.openEditor(cacheKey)
            if (editor != null) {
                try {
                    diskCache.fileSystem.sink(editor.data).buffer().use { sink ->
                        sink.writeAll(buffer.clone())
                    }
                    val snapshot = editor.commitAndOpenSnapshot()
                    if (snapshot != null) {
                        return SourceResult(
                            source = ImageSource(
                                file = snapshot.data,
                                fileSystem = diskCache.fileSystem,
                                diskCacheKey = cacheKey,
                                closeable = snapshot,
                            ),
                            mimeType = detectedMime,
                            dataSource = DataSource.NETWORK,
                        )
                    }
                } catch (e: Exception) {
                    runCatching { editor.abort() }
                    Log.w(TAG, "Failed to write extension resource to Coil DiskCache: $uri", e)
                }
            }
        }

        return SourceResult(
            source = ImageSource(
                source = buffer,
                context = options.context,
            ),
            mimeType = detectedMime,
            dataSource = DataSource.NETWORK,
        )
    }

    class Factory(
        private val extensionManager: ExtensionManager,
        private val activeWaitTimeoutMs: Long = 8_000L,
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != ExtensionResourceUri.SCHEME) return null
            return ExtensionResourceFetcher(
                uri = data,
                options = options,
                diskCache = imageLoader.diskCache,
                resourceStreamer = { connectionId, resourceId ->
                    val conn = resolveActiveConnection(connectionId)
                    conn?.streamResource(resourceId)
                },
            )
        }

        private suspend fun resolveActiveConnection(connectionId: Long): Connection.ConnectionSuccess? {
            val immediate = extensionManager.connectionFlow.value
                .filterIsInstance<Connection.ConnectionSuccess>()
                .firstOrNull { it.connectionId == connectionId }
            if (immediate != null && immediate.getStatus().value is ParaboxConnectionStatus.Active) {
                return immediate
            }
            return withTimeoutOrNull(activeWaitTimeoutMs) {
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

    companion object {
        private const val TAG = "ExtensionResourceFetcher"
        private const val MAX_STREAM_BYTES = 20L * 1024L * 1024L
    }
}
