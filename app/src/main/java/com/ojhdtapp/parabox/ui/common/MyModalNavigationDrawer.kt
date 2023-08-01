package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.swipeable
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MyModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: MyDrawerState = rememberMyDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    drawerWidth: Dp,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val navigationMenu = "navigation_menu"
    val minValue = -with(LocalDensity.current) { drawerWidth.toPx() }
    val maxValue = 0f

    val anchors = mapOf(minValue to DrawerValue.Closed, maxValue to DrawerValue.Open)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .fillMaxSize()
            .swipeable(
                state = drawerState.swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> androidx.compose.material.FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
                velocityThreshold = 400.dp,
                resistance = null
            )
    ) {
        Box {
            content()
        }
        Scrim(
            open = drawerState.isOpen,
            onClose = {
                if (
                    gesturesEnabled) {
                    scope.launch { drawerState.close() }
                }
            },
            fraction = {
                calculateFraction(minValue, maxValue, drawerState.offset.value)
            },
            color = scrimColor
        )
        Box(
            Modifier
                .offset { IntOffset(drawerState.offset.value.roundToInt(), 0) }
                .semantics {
                    paneTitle = navigationMenu
                    if (drawerState.isOpen) {
                        dismiss {
                            scope.launch { drawerState.close() }; true
                        }
                    }
                },
        ) {
            drawerContent()
        }
    }
}

@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeDrawer = "close_drawer"
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeDrawer
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissDrawer)
    ) {
        drawRect(color, alpha = fraction())
    }
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)