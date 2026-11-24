package com.ojhdtapp.parabox.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ExtensionMediaLoaderTest {

    @Test
    fun detectAnimatedMediaKindByExtension() {
        assertEquals(AnimatedMediaKind.TGS_LOTTIE, ExtensionMediaLoader.detectAnimatedMediaKind("tg_sticker_1.tgs"))
        assertEquals(AnimatedMediaKind.TGS_LOTTIE, ExtensionMediaLoader.detectAnimatedMediaKind("STICKER.TGS"))
        assertEquals(AnimatedMediaKind.LOOPING_VIDEO, ExtensionMediaLoader.detectAnimatedMediaKind("tg_sticker_2.webm"))
        assertEquals(AnimatedMediaKind.LOOPING_VIDEO, ExtensionMediaLoader.detectAnimatedMediaKind("clip.mp4"))
        assertEquals(AnimatedMediaKind.IMAGE, ExtensionMediaLoader.detectAnimatedMediaKind("sticker.webp"))
        assertEquals(AnimatedMediaKind.IMAGE, ExtensionMediaLoader.detectAnimatedMediaKind("anim.gif"))
        assertEquals(AnimatedMediaKind.IMAGE, ExtensionMediaLoader.detectAnimatedMediaKind(null))
    }

    @Test
    fun decompressTgsHandlesGzipAndRawJsonAndRejectsInvalidBytes() {
        val sampleJson = """{"v":"5.5.2","fr":60,"ip":0,"op":60,"w":512,"h":512,"layers":[]}"""
        val gzipped = ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { gzip ->
                gzip.write(sampleJson.toByteArray(Charsets.UTF_8))
            }
            out.toByteArray()
        }

        assertEquals(sampleJson, ExtensionMediaLoader.decompressTgsBytes(gzipped))
        assertEquals(sampleJson, ExtensionMediaLoader.decompressTgsBytes(sampleJson.toByteArray(Charsets.UTF_8)))
        assertNull(ExtensionMediaLoader.decompressTgsBytes(byteArrayOf()))
        assertNull(ExtensionMediaLoader.decompressTgsBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03)))
    }
}
