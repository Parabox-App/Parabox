package com.ojhdtapp.parabox.domain.service.extension

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionTrustStoreTest {

    @Test
    fun trustVerifyAndRevokeLifecycle() = runBlocking {
        val store = InMemoryExtensionTrustStore()
        val pkg = "com.example.ext"
        val sigA = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
        val sigB = "FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210FEDCBA9876543210"

        assertNull(store.getTrustedSignature(pkg))
        assertFalse(store.isTrusted(pkg, sigA))

        store.trust(pkg, sigA.lowercase())
        assertEquals(sigA, store.getTrustedSignature(pkg))
        assertTrue(store.isTrusted(pkg, sigA))
        assertTrue(store.isTrusted(pkg, sigA.lowercase()))
        assertFalse(store.isTrusted(pkg, sigB))

        store.revoke(pkg)
        assertNull(store.getTrustedSignature(pkg))
        assertFalse(store.isTrusted(pkg, sigA))
    }

    @Test
    fun signatureHexConversionAndFormatting() {
        val raw = byteArrayOf(0x0A, 0x1B, 0xFF.toByte(), 0x00)
        val hex = SignatureVerifier.bytesToHex(raw)
        assertEquals("0A1BFF00", hex)
        assertArrayEquals(raw, SignatureVerifier.hexToBytes(hex))
        assertEquals("0A:1B:FF:00", SignatureVerifier.formatFingerprint(hex))
    }
}
