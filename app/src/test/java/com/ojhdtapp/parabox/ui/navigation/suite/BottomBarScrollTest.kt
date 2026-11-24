package com.ojhdtapp.parabox.ui.navigation.suite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomBarScrollTest {
    @Test
    fun collapsesAndExpandsAfterEnoughConsumedScroll() {
        val scroll = BottomBarScroll()

        assertNull(scroll.onScroll(-10f, 24f, collapsed = false))
        assertEquals(true, scroll.onScroll(-14f, 24f, collapsed = false))
        assertNull(scroll.onScroll(-60f, 24f, collapsed = true))
        assertNull(scroll.onScroll(20f, 24f, collapsed = true))
        assertEquals(false, scroll.onScroll(4f, 24f, collapsed = true))
    }

    @Test
    fun reversalDiscardsPartialProgress() {
        val scroll = BottomBarScroll()

        assertNull(scroll.onScroll(-20f, 24f, collapsed = false))
        assertNull(scroll.onScroll(2f, 24f, collapsed = false))
        assertNull(scroll.onScroll(-20f, 24f, collapsed = false))
        assertNull(scroll.onScroll(0f, 24f, collapsed = false))
        assertEquals(true, scroll.onScroll(-4f, 24f, collapsed = false))
    }
}
