package com.ojhdtapp.parabox.ui.common.drag_drop

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DragDropState internal constructor(
    val state: LazyListState,
    val itemsCountBeforeDrag: Int,
    val dragItemCount: Int,
    private val scope: CoroutineScope,
    private val onSwap: (Int, Int) -> Unit
) {
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
    var targetIndex by mutableStateOf<Int?>(null)
    var initialOffsets: Pair<Int, Int>? by mutableStateOf(null)
    var draggedDistance by mutableStateOf(0f)
    internal val draggingItemOffset: Float
        get() = initialOffsets?.first?.plus(draggedDistance) ?: 0f
    internal val draggingItemSize: Int
        get() = initialOffsets?.second?.minus(initialOffsets?.first ?: 0) ?: 0


    fun onDragStart(offset: Offset) {
        Log.d("DragDropState", "onDragStart: $offset")
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                Log.d("DragDropState", "currentIndexOfDraggedItem: ${it.index};draggingItemInitialOffset: ${it.offset}")
                currentIndexOfDraggedItem = it.index
                initialOffsets = it.offset to it.offsetEnd
            }
    }

    fun onDragInterrupted() {
        if (targetIndex == null || initialOffsets == null || state.layoutInfo.visibleItemsInfo.size - 1 < targetIndex!!) {
            draggedDistance = 0f
        } else {
            draggedDistance = (state.layoutInfo.visibleItemsInfo[targetIndex!!].offset - initialOffsets!!.first).toFloat()
        }
        scope.launch {
            delay(200)
            if (currentIndexOfDraggedItem !=null && targetIndex != null){
                onSwap.invoke(currentIndexOfDraggedItem!! - itemsCountBeforeDrag, targetIndex!! - itemsCountBeforeDrag)
            }
            currentIndexOfDraggedItem = null
            draggedDistance = 0f
            initialOffsets = null
            targetIndex = null
        }
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> draggingItemOffset.toInt() + (draggingItemSize / 2) in item.offset..(item.offset + item.size) }
            ?.also {
                Log.d("DragDropState", "onDrag: $draggingItemOffset; currentIndexOfDraggedItem: ${it.index}")
                targetIndex = it.index.coerceIn(itemsCountBeforeDrag, itemsCountBeforeDrag + dragItemCount - 1)
            }
    }

    fun checkForOverScroll(): Float {
        return initialOffsets?.let {
            val startOffset = it.first + draggedDistance
            val endOffset = it.second + draggedDistance
            return@let when {
                draggedDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset + 50f).takeIf { diff -> diff > 0 }
                draggedDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset - 50f).takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}