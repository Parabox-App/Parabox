package com.ojhdtapp.parabox.core.util

import com.google.protobuf.ByteString
import com.ojhdtapp.parabox.protocol.v2.ResourceChunk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MediaResourceResolverTest {

    @Test
    fun streamAndCacheWritesAudioFileAndReusesDiskCache() = runBlocking {
        val cacheDir = Files.createTempDirectory("audio_resolver_test_").toFile()
        try {
            val inFlight = ConcurrentHashMap<String, CompletableDeferred<Result<File>>>()
            val streamCalls = AtomicInteger(0)
            val payload = "OggS-mock-audio-bytes".toByteArray()

            val first = MediaResourceResolver.streamAndCacheExtensionResource(
                cacheKey = "parabox-ext://conn-7/remote/voice1",
                suggestedFileName = "tg_voice_1.ogg",
                cacheDir = cacheDir,
                inFlight = inFlight,
                openStream = {
                    streamCalls.incrementAndGet()
                    flowOf(
                        ResourceChunk.newBuilder()
                            .setData(ByteString.copyFrom(payload, 0, 8))
                            .setTotalSize(payload.size.toLong())
                            .setMimeType("audio/ogg")
                            .setEof(false)
                            .build(),
                        ResourceChunk.newBuilder()
                            .setData(ByteString.copyFrom(payload, 8, payload.size - 8))
                            .setTotalSize(payload.size.toLong())
                            .setMimeType("audio/ogg")
                            .setEof(true)
                            .build(),
                    )
                },
            )
            assertTrue(first.isSuccess)
            val cachedFile = first.getOrThrow()
            assertEquals("ogg", cachedFile.extension)
            assertArrayEquals(payload, cachedFile.readBytes())
            assertEquals(1, streamCalls.get())

            // Second call hits disk cache without opening stream
            val second = MediaResourceResolver.streamAndCacheExtensionResource(
                cacheKey = "parabox-ext://conn-7/remote/voice1",
                suggestedFileName = "tg_voice_1.ogg",
                cacheDir = cacheDir,
                inFlight = inFlight,
                openStream = {
                    streamCalls.incrementAndGet()
                    null
                },
            )
            assertTrue(second.isSuccess)
            assertEquals(cachedFile.absolutePath, second.getOrThrow().absolutePath)
            assertEquals(1, streamCalls.get())
        } finally {
            cacheDir.deleteRecursively()
        }
        Unit
    }

    @Test
    fun resolveAudioExtensionPrefersMimeThenFileName() {
        assertEquals("ogg", MediaResourceResolver.resolveAudioExtension("audio/ogg", "foo.bin"))
        assertEquals("mp3", MediaResourceResolver.resolveAudioExtension("audio/mpeg", null))
        assertEquals("m4a", MediaResourceResolver.resolveAudioExtension("application/octet-stream", "note.m4a"))
        assertEquals("ogg", MediaResourceResolver.resolveAudioExtension(null, null))
    }
}
