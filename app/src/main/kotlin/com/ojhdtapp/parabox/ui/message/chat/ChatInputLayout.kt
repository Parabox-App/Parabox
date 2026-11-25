package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun isCompactChatInput(visibleHeight: Dp): Boolean = visibleHeight < 240.dp

internal fun showChatHeader(visibleHeight: Dp, imeVisible: Boolean): Boolean =
    !imeVisible || visibleHeight >= 160.dp

internal fun chatPanelHeight(bodyHeight: Dp, preferredHeight: Dp, compact: Boolean): Dp {
    val remainingHeight = (bodyHeight - 112.dp).coerceAtLeast(0.dp)
    return if (compact) {
        minOf(preferredHeight, bodyHeight * 0.45f, remainingHeight)
    } else {
        minOf(preferredHeight, remainingHeight)
    }
}
