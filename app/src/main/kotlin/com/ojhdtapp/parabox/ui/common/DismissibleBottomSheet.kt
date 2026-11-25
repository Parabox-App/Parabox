package com.ojhdtapp.parabox.ui.common

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal fun visibleChatPanelHeight(heightPx: Int, openFraction: Float): Int =
    (heightPx * openFraction.coerceIn(0f, 1f)).roundToInt()

/** Reserves the revealed part of the panel below the content as it animates. */
@Composable
fun DismissibleBottomSheet(
    sheetHeight: Dp,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    imeOccupiesBottom: Boolean = false,
    gesturesEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val openFraction = remember { Animatable(0f) }
    val heightPx = with(LocalDensity.current) { sheetHeight.toPx() }
    val showPanel = expanded && !imeOccupiesBottom
    val latestShowPanel = rememberUpdatedState(showPanel)
    val scope = rememberCoroutineScope()

    LaunchedEffect(showPanel, imeOccupiesBottom) {
        if (imeOccupiesBottom) {
            openFraction.snapTo(0f)
        } else {
            openFraction.animateTo(if (showPanel) 1f else 0f, tween(if (showPanel) 240 else 220))
        }
    }

    PredictiveBackHandler(enabled = predictiveBackEnabled && showPanel) { events ->
        try {
            events.collect { event ->
                openFraction.snapTo(1f - PredictiveBackDefaults.Easing.transform(event.progress))
            }
            onExpandedChange(false)
        } catch (cancelled: CancellationException) {
            scope.launch { openFraction.animateTo(if (latestShowPanel.value) 1f else 0f, tween(220)) }
            throw cancelled
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (heightPx > 0f) {
            scope.launch {
                openFraction.snapTo((openFraction.value - delta / heightPx).coerceIn(0f, 1f))
            }
        }
    }

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box { content() }
            Box(
                Modifier
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = gesturesEnabled && showPanel && sheetHeight > 0.dp,
                        onDragStopped = { velocity ->
                            if (latestShowPanel.value) {
                                val keepOpen = when {
                                    velocity < -800f -> true
                                    velocity > 800f -> false
                                    else -> openFraction.value >= 0.5f
                                }
                                if (keepOpen) openFraction.animateTo(1f, tween(220))
                                else onExpandedChange(false)
                            }
                        },
                    )
                    .semantics {
                        if (showPanel) {
                            dismiss {
                                onExpandedChange(false)
                                true
                            }
                        } else {
                            hideFromAccessibility()
                        }
                    },
            ) { sheetContent() }
        },
    ) { measurables, constraints ->
        val panelHeight = heightPx.roundToInt().coerceIn(0, constraints.maxHeight)
        val revealedHeight = if (imeOccupiesBottom) 0 else
            visibleChatPanelHeight(panelHeight, openFraction.value)
        val contentHeight = constraints.maxHeight - revealedHeight
        val body = measurables[0].measure(
            constraints.copy(minHeight = contentHeight, maxHeight = contentHeight),
        )
        val panel = measurables[1].measure(
            constraints.copy(minHeight = panelHeight, maxHeight = panelHeight),
        )
        layout(body.width, constraints.maxHeight) {
            body.placeRelative(0, 0)
            if (revealedHeight > 0) panel.placeRelative(0, contentHeight)
        }
    }
}
