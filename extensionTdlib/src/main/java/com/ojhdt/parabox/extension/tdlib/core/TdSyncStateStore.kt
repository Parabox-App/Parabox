package com.ojhdt.parabox.extension.tdlib.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Persists per-account, per-chat Host-ACKed TDLib message watermarks (`sync_state.json`).
 *
 * Because TDLib maintains its own SQLite database & `pts/qts` separately from the Parabox host
 * Room database, killing either process mid-delivery can leave messages committed in TDLib's DB
 * without ever reaching the host. Advancing watermarks strictly AFTER host ACK allows startup
 * catch-up sync to detect and backfill those gaps reliably.
 */
class TdSyncStateStore(
    private val stateFile: File,
    private val flushDebounceMs: Long = 500L,
) {
    private val initialized = AtomicBoolean(false)
    private val lastActiveMs = AtomicLong(0L)
    private val watermarks = ConcurrentHashMap<Long, Long>()
    private val writeLock = Any()

    @Volatile
    private var flushJob: Job? = null

    init {
        loadFromDisk()
    }

    fun isInitialized(): Boolean = initialized.get()

    fun lastActiveMs(): Long = lastActiveMs.get()

    fun getWatermark(chatId: Long): Long = watermarks[chatId] ?: 0L

    fun markInitialized(
        seedWatermarks: Map<Long, Long>,
        nowMs: Long = System.currentTimeMillis(),
        scope: CoroutineScope? = null,
    ) {
        seedWatermarks.forEach { (chatId, msgId) ->
            if (chatId != 0L && msgId > 0L) {
                watermarks.merge(chatId, msgId) { old, new -> maxOf(old, new) }
            }
        }
        initialized.set(true)
        lastActiveMs.set(maxOf(lastActiveMs.get(), nowMs))
        if (scope != null) {
            scheduleFlush(scope)
        } else {
            flushNow()
        }
    }

    fun advanceWatermark(
        chatId: Long,
        messageId: Long,
        nowMs: Long = System.currentTimeMillis(),
        scope: CoroutineScope? = null,
    ): Boolean {
        if (chatId == 0L || messageId <= 0L) return false
        var updated = false
        watermarks.compute(chatId) { _, current ->
            if (current == null || messageId > current) {
                updated = true
                messageId
            } else {
                current
            }
        }
        if (updated) {
            initialized.set(true)
            lastActiveMs.set(maxOf(lastActiveMs.get(), nowMs))
            if (scope != null) {
                scheduleFlush(scope)
            } else {
                flushNow()
            }
        }
        return updated
    }

    fun touchActive(nowMs: Long = System.currentTimeMillis(), scope: CoroutineScope? = null) {
        lastActiveMs.set(maxOf(lastActiveMs.get(), nowMs))
        if (scope != null) {
            scheduleFlush(scope)
        } else {
            flushNow()
        }
    }

    fun scheduleFlush(scope: CoroutineScope) {
        if (flushDebounceMs <= 0L) {
            flushNow()
            return
        }
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(flushDebounceMs)
            flushNow()
        }
    }

    fun flushNow() {
        flushJob?.cancel()
        flushJob = null
        synchronized(writeLock) {
            runCatching {
                trimIfNeeded()
                val chatsJson = JSONObject()
                watermarks.forEach { (chatId, msgId) ->
                    chatsJson.put(chatId.toString(), msgId)
                }
                val root = JSONObject().apply {
                    put(KEY_INITIALIZED, initialized.get())
                    put(KEY_LAST_ACTIVE_MS, lastActiveMs.get())
                    put(KEY_CHAT_WATERMARKS, chatsJson)
                }
                stateFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
                val tmp = File(stateFile.parentFile, "${stateFile.name}.tmp")
                tmp.writeText(root.toString())
                if (!tmp.renameTo(stateFile)) {
                    stateFile.writeText(root.toString())
                    tmp.delete()
                }
            }
        }
    }

    private fun trimIfNeeded() {
        if (watermarks.size <= MAX_TRACKED_CHATS) return
        val keep = watermarks.entries
            .sortedByDescending { it.value }
            .take(MAX_TRACKED_CHATS)
            .associate { it.key to it.value }
        watermarks.clear()
        watermarks.putAll(keep)
    }

    private fun loadFromDisk() {
        if (!stateFile.exists()) return
        runCatching {
            val root = JSONObject(stateFile.readText())
            initialized.set(root.optBoolean(KEY_INITIALIZED, false))
            lastActiveMs.set(root.optLong(KEY_LAST_ACTIVE_MS, 0L))
            val chatsJson = root.optJSONObject(KEY_CHAT_WATERMARKS) ?: return
            val keys = chatsJson.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                val chatId = rawKey.toLongOrNull() ?: continue
                val msgId = chatsJson.optLong(rawKey, 0L)
                if (msgId > 0L) {
                    watermarks[chatId] = msgId
                }
            }
        }
    }

    companion object {
        const val FILE_NAME = "sync_state.json"
        private const val KEY_INITIALIZED = "initialized"
        private const val KEY_LAST_ACTIVE_MS = "lastActiveMs"
        private const val KEY_CHAT_WATERMARKS = "chatWatermarks"
        private const val MAX_TRACKED_CHATS = 500
    }
}
