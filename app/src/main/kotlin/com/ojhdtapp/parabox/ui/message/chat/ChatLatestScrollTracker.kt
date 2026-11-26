package com.ojhdtapp.parabox.ui.message.chat

internal data class ChatScrollObservation(
    val latestItemId: Long?,
    val latestSentByMe: Boolean,
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val isUserDragging: Boolean,
    val isAutoScrolling: Boolean,
)

/**
 * Tracks whether the chat list was pinned to the latest message (index 0 in reverseLayout)
 * BEFORE a new message arrives, avoiding measure-pass races and multi-item prepend shifts
 * (such as time dividers or burst messages).
 */
internal class ChatLatestScrollTracker(
    private val atLatestOffsetThresholdPx: Int = 64,
) {
    private var lastSeenLatestItemId: Long? = null
    private var wasAtLatest: Boolean = true

    fun reset() {
        lastSeenLatestItemId = null
        wasAtLatest = true
    }

    fun onObservation(observation: ChatScrollObservation): Boolean {
        val latestId = observation.latestItemId ?: return false
        val isCurrentlyAtLatest = observation.firstVisibleItemIndex == 0 &&
            observation.firstVisibleItemScrollOffset <= atLatestOffsetThresholdPx

        val previousLatestId = lastSeenLatestItemId
        if (previousLatestId == null) {
            lastSeenLatestItemId = latestId
            wasAtLatest = isCurrentlyAtLatest
            return false
        }

        if (latestId == previousLatestId) {
            if (!observation.isAutoScrolling || observation.isUserDragging) {
                wasAtLatest = isCurrentlyAtLatest
            }
            return false
        }

        // Latest item at index 0 changed (new message(s) prepended).
        lastSeenLatestItemId = latestId
        val shouldScroll = !observation.isUserDragging &&
            (wasAtLatest || observation.isAutoScrolling || observation.latestSentByMe)

        wasAtLatest = if (shouldScroll) {
            true
        } else {
            isCurrentlyAtLatest
        }
        return shouldScroll
    }
}
