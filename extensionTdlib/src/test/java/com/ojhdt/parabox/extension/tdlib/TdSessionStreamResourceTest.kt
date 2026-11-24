package com.ojhdt.parabox.extension.tdlib

import com.ojhdt.parabox.extension.tdlib.core.TdAccountDirectories
import com.ojhdt.parabox.extension.tdlib.core.TdAccountStorage
import com.ojhdt.parabox.extension.tdlib.core.TdClientApi
import com.ojhdt.parabox.extension.tdlib.core.TdClientCoordinator
import com.ojhdt.parabox.extension.tdlib.core.TdClientFactory
import com.ojhdt.parabox.extension.tdlib.core.TdProxyConfig
import com.ojhdt.parabox.extension.tdlib.mapper.TdMediaBridge
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TdSessionStreamResourceTest {

    private class FakeStreamTdClient(
        override val accountId: String,
        private val onSendFunction: (TdApi.Function<*>) -> TdApi.Object,
    ) : TdClientApi {
        val deletedFileIds = mutableListOf<Int>()
        val downloadedPriorities = mutableListOf<Int>()
        override val updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 64)
        override val authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(TdApi.AuthorizationStateReady())
        override val connectionState = MutableStateFlow<TdApi.ConnectionState?>(TdApi.ConnectionStateReady())

        override suspend fun sendRaw(function: TdApi.Function<*>): TdApi.Object {
            when (function) {
                is TdApi.DeleteFile -> {
                    deletedFileIds += function.fileId
                    return TdApi.Ok()
                }
                is TdApi.DownloadFile -> {
                    downloadedPriorities += function.priority
                }
                is TdApi.GetMe -> return TdApi.User().apply {
                    id = 100L
                    firstName = "Me"
                }
            }
            return onSendFunction(function)
        }

        override suspend fun applyProxy(proxyConfig: TdProxyConfig) = Unit
        override suspend fun closeAndWait(timeoutMs: Long) = Unit
    }

    private val stubMediaBridge = object : TdMediaBridge {
        override suspend fun downloadAndShareFile(
            client: TdClientApi,
            tdFile: TdApi.File,
            hostPackageName: String,
            maxSizeBytes: Long,
            timeoutMs: Long,
        ): ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo

        override suspend fun stageResourceForUpload(
            accountId: String,
            resourceInfo: ParaboxResourceInfo,
            suggestedName: String?,
        ): File? = null
    }

    private fun createSession(client: TdClientApi): Pair<TdSession, CoroutineScope> {
        val root = Files.createTempDirectory("td_stream_test_").toFile()
        val storage = TdAccountStorage(root)
        val factory = object : TdClientFactory {
            override fun create(
                accountId: String,
                directories: TdAccountDirectories,
                apiId: Int,
                apiHash: String,
                proxyConfig: TdProxyConfig,
            ): TdClientApi = client
        }
        val coordinator = TdClientCoordinator(storage, factory, 12345, "hash")
        val session = TdSession(
            sessionId = "conn-7",
            extra = JSONObject().put(TdExtensionProvider.KEY_ACCOUNT_ID, "acct-1"),
            coordinator = coordinator,
            mediaBridge = stubMediaBridge,
            hostPackageName = "com.ojhdtapp.parabox",
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val field = ParaboxSession::class.java.getDeclaredField("_sessionScope")
        field.isAccessible = true
        field.set(session, scope)
        return session to scope
    }

    private fun completedTdFile(id: Int, file: File): TdApi.File = TdApi.File().apply {
        this.id = id
        this.size = file.length()
        this.local = TdApi.LocalFile().apply {
            this.path = file.absolutePath
            this.isDownloadingCompleted = true
        }
    }

    @Test
    fun streamUserAvatarDownloadsStreamsDetectsMimeAndDeletesCache() = runBlocking {
        val jpegBytes = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
        )
        val tempFile = Files.createTempFile("td_avatar_", ".bin").toFile().apply {
            writeBytes(jpegBytes)
            deleteOnExit()
        }

        val client = FakeStreamTdClient("acct-1") { fn ->
            when (fn) {
                is TdApi.GetUser -> TdApi.User().apply {
                    id = fn.userId
                    firstName = "Alice"
                    profilePhoto = TdApi.ProfilePhoto().apply {
                        small = TdApi.File().apply { id = 501 }
                    }
                }
                is TdApi.DownloadFile -> completedTdFile(fn.fileId, tempFile)
                else -> TdApi.Ok()
            }
        }
        val (session, scope) = createSession(client)
        try {
            assertTrue(session.onInitialize())
            val chunks = session.onStreamResource("avatar/user/42", 0L).toList()
            assertEquals(1, chunks.size)
            assertEquals("image/jpeg", chunks[0].mimeType)
            assertTrue(chunks[0].eof)
            assertArrayEquals(jpegBytes, chunks[0].data.toByteArray())
            assertEquals(listOf(32), client.downloadedPriorities)
            assertEquals(listOf(501), client.deletedFileIds)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun streamChatAvatarAndRemoteWebpFileCleanUpTdlibCache() = runBlocking {
        val webpBytes = "RIFF\u0010\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)
        val tempFile = Files.createTempFile("td_sticker_", ".bin").toFile().apply {
            writeBytes(webpBytes)
            deleteOnExit()
        }

        val client = FakeStreamTdClient("acct-1") { fn ->
            when (fn) {
                is TdApi.GetChat -> TdApi.Chat().apply {
                    id = fn.chatId
                    title = "Group"
                    photo = TdApi.ChatPhotoInfo().apply {
                        small = TdApi.File().apply { id = 601 }
                    }
                }
                is TdApi.GetRemoteFile -> TdApi.File().apply { id = 701 }
                is TdApi.DownloadFile -> completedTdFile(fn.fileId, tempFile)
                else -> TdApi.Ok()
            }
        }
        val (session, scope) = createSession(client)
        try {
            assertTrue(session.onInitialize())
            val chatChunks = session.onStreamResource("avatar/chat/-10099", 0L).toList()
            assertEquals(1, chatChunks.size)
            assertArrayEquals(webpBytes, chatChunks[0].data.toByteArray())

            val remoteChunks = session.onStreamResource("remote/rem-sticker-1", 0L).toList()
            assertEquals(1, remoteChunks.size)
            assertEquals("image/webp", remoteChunks[0].mimeType)
            assertArrayEquals(webpBytes, remoteChunks[0].data.toByteArray())

            assertEquals(listOf(601, 701), client.deletedFileIds)
        } finally {
            scope.cancel()
        }
    }
}
