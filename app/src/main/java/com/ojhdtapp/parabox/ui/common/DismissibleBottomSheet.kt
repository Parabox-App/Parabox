package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.swipeable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DismissibleBottomSheet(
    sheetContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: MyDrawerState = rememberMyDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    sheetHeight: Dp,
    content: @Composable () -> Unit
) {
    val sheetHeightPx = with(LocalDensity.current) { sheetHeight.toPx() }
    val minValue = -sheetHeightPx
    val maxValue = 0f

    val scope = rememberCoroutineScope()
    val anchors = mapOf(maxValue to DrawerValue.Closed, minValue to DrawerValue.Open)
    Box(modifier = modifier.swipeable(
        state = sheetState.swipeableState,
        anchors = anchors,
        thresholds = { _, _ -> androidx.compose.material.FractionalThreshold(0.5f) },
        orientation = Orientation.Vertical,
        enabled = gesturesEnabled,
        reverseDirection = false,
        velocityThreshold = 400.dp,
        resistance = null
    )){
        Layout(content = {
            Box {
                content()
            }
            Box(Modifier.semantics {
                paneTitle = "sheet_menu"
                if (sheetState.isOpen) {
                    dismiss {
                        scope.launch { sheetState.close() }; true
                    }
                }
            }) {
                sheetContent()
            }
        }) { measurables, constraints ->
            val contentPlaceable = measurables[0].measure(constraints)
            val sheetPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.placeRelative(
                    0,
                    sheetState.offset.value.roundToInt()
                )
                sheetPlaceable.placeRelative(0, contentPlaceable.height + sheetState.offset.value.roundToInt())
            }
        }
    }
}