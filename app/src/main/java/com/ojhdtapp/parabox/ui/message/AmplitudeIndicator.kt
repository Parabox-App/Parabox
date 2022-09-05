package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AmplitudeIndicator(modifier: Modifier = Modifier, amplitudeList: SnapshotStateList<Int>) {
    val density = LocalDensity.current
    var height by remember {
        mutableStateOf(0.dp)
    }
    Box(modifier = modifier.onSizeChanged {
        height = with(density) { it.height.toDp() }
    }) {
        val state = rememberLazyListState()
        LaunchedEffect(key1 = amplitudeList.size){
            state.animateScrollToItem(amplitudeList.lastIndex)
        }
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = true
        ) {
            items(items = amplitudeList) {
                val itemHeight = remember {
                    Animatable(4.dp, Dp.Companion.VectorConverter)
                }
                LaunchedEffect(true) {
                    itemHeight.animateTo(height * it.coerceIn(2000, 10000) / 10000)
                }
//                var itemHeight by remember {
//                    mutableStateOf(8.dp)
//                }
//                LaunchedEffect(true){
//                    itemHeight = height * it.coerceIn(200, 10000) / 10000
//                }
//                var itemHeight by remember {
//                    mutableStateOf(height * it.coerceIn(2000, 10000) / 10000)
//                }
                Box(
                    modifier = Modifier
                        .size(width = 2.dp, height = itemHeight.value)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
//            item(key = "blank") {
//                Spacer(modifier = Modifier.width(4.dp))
//            }
        }
    }
}