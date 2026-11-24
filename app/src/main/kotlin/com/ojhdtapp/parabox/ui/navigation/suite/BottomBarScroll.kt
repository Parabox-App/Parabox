package com.ojhdtapp.parabox.ui.navigation.suite

internal class BottomBarScroll {
    private var distance = 0f

    fun onScroll(deltaY: Float, threshold: Float, collapsed: Boolean): Boolean? {
        val towardTarget = if (collapsed) deltaY else -deltaY
        if (towardTarget < 0f) reset()
        if (towardTarget <= 0f) return null

        distance += towardTarget
        if (distance < threshold) return null

        reset()
        return !collapsed
    }

    private fun reset() {
        distance = 0f
    }
}
