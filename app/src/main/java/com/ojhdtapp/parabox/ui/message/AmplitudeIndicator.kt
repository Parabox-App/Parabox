package com.ojhdtapp.parabox.ui.message

import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarttoolfactory.gesture.pointerMotionEvents
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AmplitudeIndicator(
    modifier: Modifier = Modifier,
    amplitudeList: SnapshotStateList<Int>,
    progressFraction: Float,
    onPause: () -> Unit,
    onResumeAtFraction: (progressFraction: Float) -> Unit,
) {
    val density = LocalDensity.current
    var height by remember {
        mutableStateOf(0.dp)
    }
    val state = rememberLazyListState()
    val widthPx by remember {
        derivedStateOf {
            val itemLengthInPx = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            state.layoutInfo.totalItemsCount * itemLengthInPx
        }
    }
    Box(modifier = modifier
        .onSizeChanged {
            height = with(density) { it.height.toDp() }
        }
        .pointerMotionEvents(
            onDown = {
                Log.d("parabox", "down")
                onPause()
                it.consume()
            },
            onUp = {
                state.firstVisibleItemIndex
                val itemLengthInPx = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                val fra =
                    (state.firstVisibleItemScrollOffset + state.firstVisibleItemIndex * itemLengthInPx).toFloat() / widthPx
                onResumeAtFraction(fra)
                Log.d("parabox", "up")
            }
        )) {

//        val nestedScrollConnection = remember {
//            object : NestedScrollConnection {
//                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
//                    // called when you scroll the content
//                    return Offset.Zero
//                }
//            }
//        }
        LaunchedEffect(key1 = progressFraction) {
            if (!state.isScrollInProgress) {
                if (progressFraction <= 1f) {
//                    Log.d("parabox", "${progressFraction} * ${widthPx} = ${widthPx * progressFraction}")
                    state.scrollToItem(0, (widthPx * progressFraction).roundToInt())
                } else {
                    state.animateScrollToItem(amplitudeList.lastIndex)
                }
            }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxSize(),
//                .nestedScroll(nestedScrollConnection)
            state = state,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(items = amplitudeList) {
                val itemHeight = remember {
                    Animatable(4.dp, Dp.Companion.VectorConverter)
                }
                LaunchedEffect(true) {
                    itemHeight.animateTo(height * it.coerceIn(2000, 10000) / 10000)
                }
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(width = 2.dp, height = itemHeight.value)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}