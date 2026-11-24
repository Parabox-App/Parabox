package com.ojhdtapp.parabox.domain.service.extension

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest

interface ExtensionTrustStore {
    suspend fun getTrustedSignature(packageName: String): String?
    suspend fun isTrusted(packageName: String, signatureSha256: String): Boolean
    suspend fun trust(packageName: String, signatureSha256: String)
    suspend fun revoke(packageName: String)
    suspend fun getAllTrusted(): Map<String, String>
}

class InMemoryExtensionTrustStore : ExtensionTrustStore {
    private val map = mutableMapOf<String, String>()
    private val mutex = Mutex()

    override suspend fun getTrustedSignature(packageName: String): String? =
        mutex.withLock { map[packageName] }

    override suspend fun isTrusted(packageName: String, signatureSha256: String): Boolean =
        mutex.withLock {
            val saved = map[packageName] ?: return@withLock false
            saved.equals(signatureSha256, ignoreCase = true)
        }

    override suspend fun trust(packageName: String, signatureSha256: String) {
        mutex.withLock {
            map[packageName] = signatureSha256.uppercase()
        }
    }

    override suspend fun revoke(packageName: String) {
        mutex.withLock {
            map.remove(packageName)
        }
    }

    override suspend fun getAllTrusted(): Map<String, String> =
        mutex.withLock { map.toMap() }
}

class DataStoreExtensionTrustStore(
    private val context: Context,
) : ExtensionTrustStore {
    private val mutex = Mutex()

    private suspend fun readMap(): MutableMap<String, String> {
        val raw = context.getDataStoreValue(DataStoreKeys.TRUSTED_EXTENSION_SIGNATURES, "{}")
        val result = mutableMapOf<String, String>()
        runCatching {
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val sig = json.optString(pkg)
                if (sig.isNotBlank()) {
                    result[pkg] = sig.uppercase()
                }
            }
        }
        return result
    }

    private suspend fun writeMap(map: Map<String, String>) {
        val json = JSONObject()
        map.forEach { (pkg, sig) -> json.put(pkg, sig.uppercase()) }
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.TRUSTED_EXTENSION_SIGNATURES] = json.toString()
        }
    }

    override suspend fun getTrustedSignature(packageName: String): String? =
        mutex.withLock { readMap()[packageName] }

    override suspend fun isTrusted(packageName: String, signatureSha256: String): Boolean =
        mutex.withLock {
            val saved = readMap()[packageName] ?: return@withLock false
            saved.equals(signatureSha256, ignoreCase = true)
        }

    override suspend fun trust(packageName: String, signatureSha256: String) {
        mutex.withLock {
            val map = readMap()
            map[packageName] = signatureSha256.uppercase()
            writeMap(map)
        }
    }

    override suspend fun revoke(packageName: String) {
        mutex.withLock {
            val map = readMap()
            if (map.remove(packageName) != null) {
                writeMap(map)
            }
        }
    }

    override suspend fun getAllTrusted(): Map<String, String> =
        mutex.withLock { readMap() }
}

object SignatureVerifier {

    @Suppress("DEPRECATION")
    fun getPackageSignatureSha256(context: Context, packageName: String): String? {
        return try {
            val pm = context.packageManager
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val flags = PackageManager.GET_SIGNING_CERTIFICATES
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    pm.getPackageInfo(packageName, flags)
                }
                val signingInfo = pkgInfo.signingInfo ?: return null
                val signatures = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                signatures?.firstOrNull()?.toByteArray()
            } else {
                val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: return null

            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            bytesToHex(digest)
        } catch (e: Exception) {
            null
        }
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02X".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(":", "").trim()
        require(clean.length % 2 == 0) { "Invalid hex string length: ${clean.length}" }
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    fun formatFingerprint(hex: String): String =
        hex.replace(":", "").uppercase().chunked(2).joinToString(":")
}
