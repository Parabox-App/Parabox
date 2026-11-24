package com.ojhdt.parabox.extension.tdlib.core

import org.json.JSONObject
import java.io.File

data class TdAccountMeta(
    val accountId: String,
    val userId: Long = 0L,
    val displayName: String = "",
    val phoneNumber: String = "",
    val username: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("accountId", accountId)
        put("userId", userId)
        put("displayName", displayName)
        put("phoneNumber", phoneNumber)
        put("username", username)
        put("createdAtMs", createdAtMs)
    }

    companion object {
        fun fromJson(json: JSONObject): TdAccountMeta = TdAccountMeta(
            accountId = json.optString("accountId"),
            userId = json.optLong("userId", 0L),
            displayName = json.optString("displayName"),
            phoneNumber = json.optString("phoneNumber"),
            username = json.optString("username"),
            createdAtMs = json.optLong("createdAtMs", 0L),
        )
    }
}

data class TdAccountDirectories(
    val rootDir: File,
    val dbDir: File,
    val filesDir: File,
    val metaFile: File,
)

class TdAccountStorage(
    private val baseDir: File,
) {
    private val tdlibRoot: File
        get() = File(baseDir, "tdlib").apply { if (!exists()) mkdirs() }

    fun getDirectories(accountId: String): TdAccountDirectories {
        val safeId = sanitizeId(accountId)
        val root = File(tdlibRoot, safeId).apply { if (!exists()) mkdirs() }
        val db = File(root, "db").apply { if (!exists()) mkdirs() }
        val files = File(root, "files").apply { if (!exists()) mkdirs() }
        val meta = File(root, "meta.json")
        return TdAccountDirectories(rootDir = root, dbDir = db, filesDir = files, metaFile = meta)
    }

    fun saveMeta(meta: TdAccountMeta) {
        val dirs = getDirectories(meta.accountId)
        runCatching {
            dirs.metaFile.writeText(meta.toJson().toString())
        }
    }

    fun readMeta(accountId: String): TdAccountMeta? {
        val dirs = getDirectories(accountId)
        if (!dirs.metaFile.exists()) return null
        return runCatching {
            TdAccountMeta.fromJson(JSONObject(dirs.metaFile.readText()))
        }.getOrNull()
    }

    fun listAccounts(): List<TdAccountMeta> {
        val children = tdlibRoot.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return children.mapNotNull { dir ->
            val metaFile = File(dir, "meta.json")
            if (metaFile.exists()) {
                runCatching { TdAccountMeta.fromJson(JSONObject(metaFile.readText())) }.getOrNull()
            } else {
                TdAccountMeta(accountId = dir.name)
            }
        }.sortedByDescending { it.createdAtMs }
    }

    fun clearMediaCache(accountId: String): Long {
        val dirs = getDirectories(accountId)
        val before = computeDirBytes(dirs.filesDir)
        dirs.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        return before
    }

    fun deleteAccount(accountId: String): Boolean {
        val safeId = sanitizeId(accountId)
        val root = File(tdlibRoot, safeId)
        return if (root.exists()) root.deleteRecursively() else true
    }

    fun computeAccountBytes(accountId: String): Long {
        val safeId = sanitizeId(accountId)
        val root = File(tdlibRoot, safeId)
        return if (root.exists()) computeDirBytes(root) else 0L
    }

    private fun computeDirBytes(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { computeDirBytes(it) } ?: 0L
    }

    private fun sanitizeId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "default" }
}
