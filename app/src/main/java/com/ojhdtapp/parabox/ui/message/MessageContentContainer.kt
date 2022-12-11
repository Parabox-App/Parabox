package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max

@Composable
fun MessageContentContainer(
    modifier: Modifier = Modifier,
    shouldBreak: List<Boolean>,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        val positionList = mutableListOf<IntOffset>()
        var xPosition = 0
        var xSpace = constraints.maxWidth
        var yPosition = 0
        var currentLineMaxHeight = 0
        var layoutWidth = 0
        var layoutHeight = 0
        placeables.forEachIndexed { index, placeable ->
            if (xSpace >= placeable.width && !shouldBreak.getOrElse(index) { false } && !shouldBreak.getOrElse(index - 1) { false }) {
                positionList.add(IntOffset(xPosition, yPosition))
                xPosition += placeable.width
                xSpace -= placeable.width
                currentLineMaxHeight = max(currentLineMaxHeight, placeable.height)
                layoutWidth = max(layoutWidth, xPosition)
                layoutHeight = max(layoutHeight, yPosition + currentLineMaxHeight)
            } else {
                xPosition = 0
                xSpace = constraints.maxWidth
                yPosition = layoutHeight
                positionList.add(IntOffset(xPosition, yPosition))
                xPosition += placeable.width
                xSpace -= placeable.width
                layoutWidth = max(layoutWidth, xPosition)
                currentLineMaxHeight = placeable.height
                layoutHeight = max(layoutHeight, yPosition + currentLineMaxHeight)
            }
        }
        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(positionList[index])
            }
        }
    }
}