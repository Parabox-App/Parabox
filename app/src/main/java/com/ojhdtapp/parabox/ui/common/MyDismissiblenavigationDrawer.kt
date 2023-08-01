package com.ojhdtapp.parabox.ui.common

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MyDismissibleNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: MyDrawerState = rememberMyDrawerState(DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    drawerWidth: Dp,
    content: @Composable () -> Unit
) {
    val drawerWidthPx = with(LocalDensity.current) { drawerWidth.toPx() }
    val minValue = -drawerWidthPx
    val maxValue = 0f

    val scope = rememberCoroutineScope()
    val navigationMenu = "navigation_menu"

    val anchors = mapOf(minValue to DrawerValue.Closed, maxValue to DrawerValue.Open)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier.swipeable(
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
        Layout(content = {
            Box(Modifier.semantics {
                paneTitle = navigationMenu
                if (drawerState.isOpen) {
                    dismiss {
                        scope.launch { drawerState.close() }; true
                    }
                }
            }) {
                drawerContent()
            }
            Box {
                content()
            }
        }) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                contentPlaceable.placeRelative(
                    sheetPlaceable.width + drawerState.offset.value.roundToInt(),
                    0
                )
                sheetPlaceable.placeRelative(drawerState.offset.value.roundToInt(), 0)
            }
        }
    }
}

@Suppress("NotCloseable")
@Stable
class MyDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    @OptIn(ExperimentalMaterialApi::class)
    val swipeableState = androidx.compose.material.SwipeableState(
        initialValue = initialValue,
        animationSpec = TweenSpec<Float>(durationMillis = 256),
        confirmStateChange = confirmStateChange
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    val currentValue: DrawerValue
        get() {
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = animateTo(DrawerValue.Open, AnimationSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = animateTo(DrawerValue.Closed, AnimationSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    suspend fun animateTo(targetValue: DrawerValue, anim: AnimationSpec<Float>) {
        swipeableState.animateTo(targetValue, anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    suspend fun snapTo(targetValue: DrawerValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    val targetValue: DrawerValue
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer container.
     */
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    val offset: State<Float>
        get() = swipeableState.offset

    companion object {
        /**
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<MyDrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { MyDrawerState(it, confirmStateChange) }
            )
    }
    private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)
}
@Composable
fun rememberMyDrawerState(
    initialValue: DrawerValue,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): MyDrawerState {
    return rememberSaveable(saver = MyDrawerState.Saver(confirmStateChange)) {
        MyDrawerState(initialValue, confirmStateChange)
    }
}