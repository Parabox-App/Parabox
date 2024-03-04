package com.ojhdtapp.parabox.ui.navigation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator

fun slideWithOffset(
    animationSpec: FiniteAnimationSpec<Float> = tween(),
    orientation: Orientation = Orientation.Horizontal,
    offset: Float = 0f
): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction, content ->
        content(
            when (orientation) {
                Orientation.Horizontal -> Modifier.offsetXFactor(factor, direction, offset)
                Orientation.Vertical -> Modifier.offsetYFactor(factor, direction, offset)
            }
        )
    }

private fun Modifier.offsetXFactor(factor: Float, direction: Direction, offset: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = (offset * factor).toInt(), y = 0)
        }
    }

private fun Modifier.offsetYFactor(factor: Float, direction: Direction, offset: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = 0, y = if (direction == Direction.ENTER_FRONT) (offset * factor).toInt() else 0)
        }
    }
