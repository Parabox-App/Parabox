package com.ojhdtapp.parabox.ui.message.chat

enum class HistorySyncStatus {
    IDLE,
    LOADING,
    END_REACHED,
    ERROR,
}

enum class HistorySyncTrigger {
    BOUNDARY,
    GAP_HEAL,
}

/**
 * Per-chat pure state machine coordinating upward-scroll history fetches:
 * - Single-flight concurrency guard per chat
 * - Verified anchor & timestamp range cache to avoid redundant RPCs during gap healing
 * - End-of-history detection for boundary fetches without false positives from gap heals
 * - Error cooldown with explicit forceRetry bypass
 */
class ChatHistorySyncController(
    private val errorCooldownMs: Long = DEFAULT_ERROR_COOLDOWN_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private data class TimestampRange(val startMs: Long, val endMs: Long)

    private data class ChatSyncEntry(
        var status: HistorySyncStatus = HistorySyncStatus.IDLE,
        var inFlightTrigger: HistorySyncTrigger? = null,
        var lastErrorAtMs: Long = 0L,
        val queriedAnchors: MutableSet<String> = HashSet(),
        val verifiedRanges: MutableList<TimestampRange> = mutableListOf(),
    )

    private val entries = HashMap<Long, ChatSyncEntry>()

    @Synchronized
    fun getStatus(chatId: Long): HistorySyncStatus =
        entries[chatId]?.status ?: HistorySyncStatus.IDLE

    @Synchronized
    fun reset(chatId: Long) {
        entries.remove(chatId)
    }

    @Synchronized
    fun tryBegin(
        chatId: Long,
        trigger: HistorySyncTrigger,
        anchorUid: String,
        anchorTimestamp: Long? = null,
        forceRetry: Boolean = false,
    ): Boolean {
        if (anchorUid.isBlank()) return false
        val entry = entries.getOrPut(chatId) { ChatSyncEntry() }

        if (entry.inFlightTrigger != null) return false

        when (trigger) {
            HistorySyncTrigger.BOUNDARY -> {
                if (!forceRetry && entry.status == HistorySyncStatus.END_REACHED) return false
                if (!forceRetry && entry.status == HistorySyncStatus.ERROR) {
                    if (nowMs() - entry.lastErrorAtMs < errorCooldownMs) return false
                }
                if (!forceRetry && anchorUid in entry.queriedAnchors) return false
                entry.status = HistorySyncStatus.LOADING
                entry.inFlightTrigger = trigger
                return true
            }

            HistorySyncTrigger.GAP_HEAL -> {
                if (!forceRetry && entry.lastErrorAtMs > 0L && nowMs() - entry.lastErrorAtMs < errorCooldownMs) {
                    return false
                }
                if (!forceRetry && anchorUid in entry.queriedAnchors) return false
                if (!forceRetry && anchorTimestamp != null && isInsideVerifiedRange(entry, anchorTimestamp)) {
                    return false
                }
                entry.inFlightTrigger = trigger
                return true
            }
        }
    }

    @Synchronized
    fun onSuccess(
        chatId: Long,
        trigger: HistorySyncTrigger,
        anchorUid: String,
        anchorTimestamp: Long?,
        returnedCount: Int,
        insertedCount: Int,
        minReturnedTimestamp: Long?,
        newOldestUid: String?,
    ): HistorySyncStatus {
        val entry = entries.getOrPut(chatId) { ChatSyncEntry() }
        entry.inFlightTrigger = null
        entry.lastErrorAtMs = 0L
        entry.queriedAnchors.add(anchorUid)

        if (anchorTimestamp != null && minReturnedTimestamp != null && minReturnedTimestamp <= anchorTimestamp) {
            addAndMergeRange(entry, TimestampRange(minReturnedTimestamp, anchorTimestamp))
        }

        if (trigger == HistorySyncTrigger.BOUNDARY) {
            val anchorUnchanged = newOldestUid == null || newOldestUid == anchorUid
            entry.status = if (returnedCount == 0 || (insertedCount == 0 && anchorUnchanged)) {
                HistorySyncStatus.END_REACHED
            } else {
                HistorySyncStatus.IDLE
            }
        }
        return entry.status
    }

    @Synchronized
    fun onFailure(
        chatId: Long,
        trigger: HistorySyncTrigger,
    ): HistorySyncStatus {
        val entry = entries.getOrPut(chatId) { ChatSyncEntry() }
        entry.inFlightTrigger = null
        entry.lastErrorAtMs = nowMs()
        if (trigger == HistorySyncTrigger.BOUNDARY) {
            entry.status = HistorySyncStatus.ERROR
        }
        return entry.status
    }

    private fun isInsideVerifiedRange(entry: ChatSyncEntry, timestamp: Long): Boolean =
        entry.verifiedRanges.any { timestamp in it.startMs..it.endMs }

    private fun addAndMergeRange(entry: ChatSyncEntry, incoming: TimestampRange) {
        entry.verifiedRanges.add(incoming)
        if (entry.verifiedRanges.size <= 1) return
        entry.verifiedRanges.sortBy { it.startMs }
        val merged = ArrayList<TimestampRange>(entry.verifiedRanges.size)
        for (range in entry.verifiedRanges) {
            val last = merged.lastOrNull()
            if (last == null || range.startMs > last.endMs) {
                merged.add(range)
            } else {
                merged[merged.lastIndex] = TimestampRange(last.startMs, maxOf(last.endMs, range.endMs))
            }
        }
        entry.verifiedRanges.clear()
        entry.verifiedRanges.addAll(merged)
    }

    companion object {
        const val DEFAULT_ERROR_COOLDOWN_MS = 5_000L
    }
}
