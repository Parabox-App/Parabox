package com.ojhdtapp.parabox.ui.common

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Pure Foundation + Material 3 state for an inline dismissible bottom panel (e.g. chat toolbar / location picker)
 * with built-in Predictive Back gesture support.
 */
@Stable
class DismissibleBottomSheetState(
    initialValue: DrawerValue = DrawerValue.Closed,
) {
    private val offsetAnimatable = Animatable(0f)

    internal var sheetHeightPx by mutableFloatStateOf(0f)

    var currentValue: DrawerValue by mutableStateOf(initialValue)
        private set

    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /** Current vertical offset in pixels: 0f = closed, -sheetHeightPx = open. */
    val offsetPx: Float
        get() = offsetAnimatable.value

    /** Normalized open fraction in [0f, 1f]: 0f = fully closed, 1f = fully open. */
    val openFraction: Float
        get() = if (sheetHeightPx > 0f) {
            (-offsetAnimatable.value / sheetHeightPx).coerceIn(0f, 1f)
        } else {
            if (isOpen) 1f else 0f
        }

    internal suspend fun updateSheetHeight(newHeightPx: Float) {
        if (newHeightPx <= 0f) return
        val wasOpen = isOpen
        sheetHeightPx = newHeightPx
        val target = if (wasOpen) -newHeightPx else 0f
        if (!offsetAnimatable.isRunning) {
            offsetAnimatable.snapTo(target)
        }
    }

    internal suspend fun dragBy(deltaPx: Float) {
        if (sheetHeightPx <= 0f) return
        val next = (offsetAnimatable.value + deltaPx).coerceIn(-sheetHeightPx, 0f)
        offsetAnimatable.snapTo(next)
    }

    internal suspend fun settle(velocityPx: Float) {
        if (sheetHeightPx <= 0f) return
        val shouldOpen = when {
            velocityPx < -800f -> true
            velocityPx > 800f -> false
            else -> openFraction >= 0.5f
        }
        if (shouldOpen) open() else close()
    }

    internal suspend fun snapPredictiveDismiss(progress: Float) {
        if (sheetHeightPx <= 0f) return
        val eased = PredictiveBackDefaults.Easing.transform(progress.coerceIn(0f, 1f))
        offsetAnimatable.snapTo(-sheetHeightPx * (1f - eased))
    }

    suspend fun open() {
        currentValue = DrawerValue.Open
        if (sheetHeightPx > 0f) {
            offsetAnimatable.animateTo(-sheetHeightPx, animationSpec = tween(durationMillis = 240))
        }
    }

    suspend fun close() {
        currentValue = DrawerValue.Closed
        offsetAnimatable.animateTo(0f, animationSpec = tween(durationMillis = 220))
    }

    suspend fun cancelDismiss() {
        if (sheetHeightPx > 0f) {
            offsetAnimatable.animateTo(
                targetValue = -sheetHeightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
        }
    }

    companion object {
        val Saver: Saver<DismissibleBottomSheetState, DrawerValue> = Saver(
            save = { it.currentValue },
            restore = { DismissibleBottomSheetState(it) },
        )
    }
}

@Composable
fun rememberDismissibleBottomSheetState(
    initialValue: DrawerValue = DrawerValue.Closed,
): DismissibleBottomSheetState {
    return rememberSaveable(saver = DismissibleBottomSheetState.Saver) {
        DismissibleBottomSheetState(initialValue)
    }
}

@Composable
fun DismissibleBottomSheet(
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: DismissibleBottomSheetState = rememberDismissibleBottomSheetState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    predictiveBackEnabled: Boolean = true,
    sheetHeight: Dp,
    content: @Composable () -> Unit,
) {
    val sheetHeightPx = with(LocalDensity.current) { sheetHeight.toPx() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sheetHeightPx) {
        sheetState.updateSheetHeight(sheetHeightPx)
    }

    PredictiveBackHandler(enabled = predictiveBackEnabled && sheetState.isOpen) { backEvents ->
        try {
            backEvents.collect { event ->
                sheetState.snapPredictiveDismiss(event.progress)
            }
            sheetState.close()
        } catch (e: CancellationException) {
            sheetState.cancelDismiss()
        }
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            sheetState.dragBy(delta)
        }
    }

    Box(
        modifier = modifier.draggable(
            state = draggableState,
            orientation = Orientation.Vertical,
            enabled = gesturesEnabled,
            onDragStopped = { velocity ->
                scope.launch {
                    sheetState.settle(velocity)
                }
            },
        ),
    ) {
        Layout(content = {
            Box {
                content()
            }
            Box(
                Modifier.semantics {
                    paneTitle = "sheet_menu"
                    if (sheetState.isOpen) {
                        dismiss {
                            scope.launch { sheetState.close() }
                            true
                        }
                    }
                },
            ) {
                sheetContent()
            }
        }) { measurables, constraints ->
            val contentPlaceable = measurables[0].measure(constraints)
            val sheetPlaceable = measurables[1].measure(constraints)
            val currentOffset = sheetState.offsetPx.roundToInt()
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.placeRelative(0, currentOffset)
                sheetPlaceable.placeRelative(0, contentPlaceable.height + currentOffset)
            }
        }
    }
}
