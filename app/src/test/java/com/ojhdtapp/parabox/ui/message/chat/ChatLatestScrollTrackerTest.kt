package com.ojhdtapp.parabox.ui.message.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLatestScrollTrackerTest {

    private fun obs(
        latestId: Long?,
        sentByMe: Boolean = false,
        index: Int = 0,
        offset: Int = 0,
        dragging: Boolean = false,
        autoScrolling: Boolean = false,
    ) = ChatScrollObservation(
        latestItemId = latestId,
        latestSentByMe = sentByMe,
        firstVisibleItemIndex = index,
        firstVisibleItemScrollOffset = offset,
        isUserDragging = dragging,
        isAutoScrolling = autoScrolling,
    )

    @Test
    fun initialLoadDoesNotTriggerScroll() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        assertFalse(tracker.onObservation(obs(latestId = null)))
        assertFalse(tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0)))
    }

    @Test
    fun newMessageWhileAtLatestTriggersScrollEvenWhenIndexHasNotShiftedYet() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        assertFalse(tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0)))

        // Same-frame race: latestId changed before LazyColumn shifted firstVisibleItemIndex
        assertTrue(tracker.onObservation(obs(latestId = 101L, index = 0, offset = 0)))
    }

    @Test
    fun newMessageWithTimeDividerOrBurstShiftTriggersScroll() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        assertFalse(tracker.onObservation(obs(latestId = 100L, index = 0, offset = 20)))

        // New message + TimeDivider shifted previous index 0 to index 2
        assertTrue(tracker.onObservation(obs(latestId = 102L, index = 2, offset = 0)))

        // Auto-scroll finishes back at index 0
        assertFalse(tracker.onObservation(obs(latestId = 102L, index = 0, offset = 0, autoScrolling = false)))

        // Burst of 5 messages shifts previous index 0 to index 5
        assertTrue(tracker.onObservation(obs(latestId = 107L, index = 5, offset = 0)))
    }

    @Test
    fun burstArrivalDuringInFlightAutoScrollTriggersFollowUpScroll() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))
        assertTrue(tracker.onObservation(obs(latestId = 101L, index = 1, offset = 0)))

        // Intermediate animation frame while auto-scrolling should not lose pinned state
        assertFalse(tracker.onObservation(obs(latestId = 101L, index = 1, offset = 120, autoScrolling = true)))

        // Another message arrives while still auto-scrolling
        assertTrue(tracker.onObservation(obs(latestId = 102L, index = 2, offset = 120, autoScrolling = true)))
    }

    @Test
    fun readingOlderHistoryDoesNotAutoScrollOnIncomingMessage() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))

        // User scrolls up to read older messages
        assertFalse(tracker.onObservation(obs(latestId = 100L, index = 4, offset = 0)))

        // Incoming message arrives from someone else
        assertFalse(tracker.onObservation(obs(latestId = 101L, index = 5, offset = 0, sentByMe = false)))

        // User scrolls back to index 0, then receives another message -> resumes auto-scroll
        assertFalse(tracker.onObservation(obs(latestId = 101L, index = 0, offset = 0)))
        assertTrue(tracker.onObservation(obs(latestId = 102L, index = 1, offset = 0)))
    }

    @Test
    fun loadingOlderHistoryWithSameLatestIdDoesNotTriggerScroll() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))
        assertFalse(tracker.onObservation(obs(latestId = 100L, index = 15, offset = 300)))
    }

    @Test
    fun ownSentMessageTriggersScrollEvenWhenScrolledUp() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))
        tracker.onObservation(obs(latestId = 100L, index = 6, offset = 0))

        assertTrue(tracker.onObservation(obs(latestId = 101L, sentByMe = true, index = 7, offset = 0)))
    }

    @Test
    fun userDraggingPreventsAutoScroll() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))

        assertFalse(tracker.onObservation(obs(latestId = 101L, index = 1, offset = 0, dragging = true)))
    }

    @Test
    fun resetClearsStateForChatSwitch() {
        val tracker = ChatLatestScrollTracker(atLatestOffsetThresholdPx = 50)
        tracker.onObservation(obs(latestId = 100L, index = 0, offset = 0))
        tracker.reset()

        // First observation of the newly opened chat should not spuriously scroll
        assertFalse(tracker.onObservation(obs(latestId = 200L, index = 0, offset = 0)))
    }
}
