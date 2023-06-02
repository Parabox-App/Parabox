package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.common.MessageNavGraph
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@MessageNavGraph(start = true)
@Composable
fun MessageAndChatPageWrapperUI(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    drawerState: DrawerState,
    bottomSheetState: SheetState,
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
    Row(modifier = modifier) {
        MessagePage(
            modifier =
            if (layoutType == MessageLayoutType.SPLIT)
                modifier.width(400.dp) else modifier.weight(1f),
            mainNavController = mainNavController,
            mainSharedViewModel = mainSharedViewModel,
            listState = listState,
            drawerState = drawerState,
            bottomSheetState = bottomSheetState,
            layoutType = layoutType
        )
        AnimatedVisibility(visible = layoutType == MessageLayoutType.SPLIT) {
            ChatPage()
        }
    }
}