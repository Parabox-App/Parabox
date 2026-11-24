package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.ui.common.visibleChatPanelHeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInputLayoutTest {
    @Test
    fun compactAndHeaderThresholdsUseVisiblePaneHeight() {
        assertTrue(isCompactChatInput(239.dp))
        assertFalse(isCompactChatInput(240.dp))
        assertFalse(showChatHeader(159.dp, imeVisible = true))
        assertTrue(showChatHeader(160.dp, imeVisible = true))
        assertTrue(showChatHeader(159.dp, imeVisible = false))
    }

    @Test
    fun panelRetainsPreferredHeightOrShrinksToAvailableSpace() {
        assertEquals(320.dp, chatPanelHeight(500.dp, 320.dp, compact = false))
        assertEquals(78.dp, chatPanelHeight(190.dp, 320.dp, compact = true))
        assertEquals(135.dp, chatPanelHeight(300.dp, 320.dp, compact = true))
        assertEquals(0.dp, chatPanelHeight(100.dp, 160.dp, compact = true))
    }

    @Test
    fun panelResizePreservesItsOpenFraction() {
        assertEquals(80, visibleChatPanelHeight(160, 0.5f))
        assertEquals(40, visibleChatPanelHeight(80, 0.5f))
        assertEquals(80, visibleChatPanelHeight(80, 1f))
    }
}
