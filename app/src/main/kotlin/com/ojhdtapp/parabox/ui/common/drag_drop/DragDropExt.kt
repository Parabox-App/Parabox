package com.ojhdtapp.parabox.ui.common.drag_drop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    itemsCountBeforeDrag: Int,
    dragItemCount: Int,
    onSwap: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState, itemsCountBeforeDrag, dragItemCount) {
        DragDropState(
            state = lazyListState,
            itemsCountBeforeDrag = itemsCountBeforeDrag,
            dragItemCount = dragItemCount,
            onSwap = onSwap,
            scope = scope
        )
    }
    return state
}

fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? {
    return this
        .layoutInfo
        .visibleItemsInfo
        .getOrNull(absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index)
}

val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size

@ExperimentalFoundationApi
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.currentIndexOfDraggedItem
    val draggingTranslationY: Float by animateFloatAsState(dragDropState.draggedDistance)
    val otherTranslationY: Float by animateFloatAsState(
        targetValue = if (dragging || dragDropState.currentIndexOfDraggedItem == null) 0f
        else {
            if (index > dragDropState.currentIndexOfDraggedItem!!) {
                if (dragDropState.targetIndex != null && dragDropState.targetIndex!! >= index) {
                    -dragDropState.draggingItemSize.toFloat()
                } else {
                    0f
                }
            } else if (index < dragDropState.currentIndexOfDraggedItem!!) {
                if (dragDropState.targetIndex != null && dragDropState.targetIndex!! <= index) {
                    dragDropState.draggingItemSize.toFloat()
                } else {
                    0f
                }
            } else {
                0f
            }
        },
        animationSpec = if (dragDropState.targetIndex == null) snap(0) else spring<Float>()
    )

    val draggingModifier = if (dragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer {
                translationY = draggingTranslationY
            }
    } else {
        Modifier
            .graphicsLayer {
                translationY = otherTranslationY
            }
    }
    Column(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}