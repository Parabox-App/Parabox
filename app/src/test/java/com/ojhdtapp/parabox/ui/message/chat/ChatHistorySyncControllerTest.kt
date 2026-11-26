package com.ojhdtapp.parabox.ui.message.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatHistorySyncControllerTest {

    @Test
    fun singleFlightBlocksConcurrentFetchesForSameChat() {
        val controller = ChatHistorySyncController()
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m100"))
        assertEquals(HistorySyncStatus.LOADING, controller.getStatus(1L))

        // Concurrent boundary or gap heal on same chat is blocked
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m100"))
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.GAP_HEAL, anchorUid = "m120", anchorTimestamp = 2000L))

        // Independent chat is not blocked
        assertTrue(controller.tryBegin(2L, HistorySyncTrigger.BOUNDARY, anchorUid = "m50"))
    }

    @Test
    fun boundaryReturnsEmptyMarksEndReachedAndPreventsFurtherAutoFetches() {
        val controller = ChatHistorySyncController()
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m1"))

        val status = controller.onSuccess(
            chatId = 1L,
            trigger = HistorySyncTrigger.BOUNDARY,
            anchorUid = "m1",
            anchorTimestamp = 1000L,
            returnedCount = 0,
            insertedCount = 0,
            minReturnedTimestamp = null,
            newOldestUid = "m1",
        )
        assertEquals(HistorySyncStatus.END_REACHED, status)
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m1"))
    }

    @Test
    fun boundaryAdvancesAnchorAllowsNextPageFetch() {
        val controller = ChatHistorySyncController()
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m50"))
        val status = controller.onSuccess(
            chatId = 1L,
            trigger = HistorySyncTrigger.BOUNDARY,
            anchorUid = "m50",
            anchorTimestamp = 5000L,
            returnedCount = 20,
            insertedCount = 20,
            minReturnedTimestamp = 3000L,
            newOldestUid = "m30",
        )
        assertEquals(HistorySyncStatus.IDLE, status)
        // Same anchor is blocked, new anchor succeeds
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m50"))
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m30"))
    }

    @Test
    fun gapHealZeroInsertDoesNotMarkEndReachedAndCachesVerifiedRange() {
        val controller = ChatHistorySyncController()
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.GAP_HEAL, anchorUid = "m120", anchorTimestamp = 12000L))
        // Gap heal does not flip top boundary status to LOADING
        assertEquals(HistorySyncStatus.IDLE, controller.getStatus(1L))

        val status = controller.onSuccess(
            chatId = 1L,
            trigger = HistorySyncTrigger.GAP_HEAL,
            anchorUid = "m120",
            anchorTimestamp = 12000L,
            returnedCount = 20,
            insertedCount = 0,
            minReturnedTimestamp = 10000L,
            newOldestUid = "m1",
        )
        assertEquals(HistorySyncStatus.IDLE, status)

        // Anchor inside verified range [10000, 12000] is skipped
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.GAP_HEAL, anchorUid = "m110", anchorTimestamp = 11000L))

        // Anchor outside verified range is allowed
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.GAP_HEAL, anchorUid = "m95", anchorTimestamp = 9500L))
    }

    @Test
    fun errorCooldownBlocksAutoRetryButForceRetryBypasses() {
        var clock = 10_000L
        val controller = ChatHistorySyncController(errorCooldownMs = 5_000L, nowMs = { clock })

        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m10"))
        val status = controller.onFailure(1L, HistorySyncTrigger.BOUNDARY)
        assertEquals(HistorySyncStatus.ERROR, status)

        // Within cooldown -> blocked
        clock += 2_000L
        assertFalse(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m10"))

        // Manual retry bypasses cooldown
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m10", forceRetry = true))
        controller.onFailure(1L, HistorySyncTrigger.BOUNDARY)

        // After cooldown expires -> auto retry allowed
        clock += 6_000L
        assertTrue(controller.tryBegin(1L, HistorySyncTrigger.BOUNDARY, anchorUid = "m10"))
    }
}
