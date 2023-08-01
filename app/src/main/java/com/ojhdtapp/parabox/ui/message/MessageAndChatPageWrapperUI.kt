package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.common.MessageNavGraph
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@MessageNavGraph(start = true)
@Composable
fun MessageAndChatPageWrapperUI(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
) {
    val layoutType: MessageLayoutType
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            layoutType = MessageLayoutType.NORMAL
        }

        WindowWidthSizeClass.Medium -> {
            layoutType = MessageLayoutType.NORMAL
        }

        WindowWidthSizeClass.Expanded -> {
            layoutType = if (devicePosture is DevicePosture.BookPosture) {
                MessageLayoutType.NORMAL
            } else {
                MessageLayoutType.SPLIT
            }
        }

        else -> {
            layoutType = MessageLayoutType.NORMAL
        }
    }
    Row() {
        MessagePage(
            modifier =
            if (layoutType == MessageLayoutType.SPLIT)
                modifier.width(400.dp) else modifier
                .weight(1f),
            mainNavController = mainNavController,
            mainSharedViewModel = mainSharedViewModel,
            listState = listState,
            layoutType = layoutType,
            windowSize = windowSize
        )
        AnimatedVisibility(visible = layoutType == MessageLayoutType.SPLIT) {
            ChatPage()
        }
    }
}