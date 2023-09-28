package com.ojhdtapp.parabox.ui.message

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.common.MessageNavGraph
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate

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
    val viewModel = hiltViewModel<MessagePageViewModel>()
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
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
    BackHandler(state.chatDetail.chat != null) {
        viewModel.sendEvent(MessagePageEvent.LoadMessage(null))
    }
    LaunchedEffect(state.chatDetail.chat) {
        if (layoutType == MessageLayoutType.NORMAL && state.chatDetail.chat != null) {
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(false))
        }
        if (layoutType == MessageLayoutType.NORMAL && state.chatDetail.chat == null) {
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(true))
        }
    }
    AnimatedContent(targetState = state.chatDetail.chat, label = "",
        transitionSpec = {
            if (layoutType == MessageLayoutType.NORMAL && targetState != null) {
                (fadeIn(tween(300)) + scaleIn(tween(300), 0.9f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(300), 1.1f))
//                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)) togetherWith slideOutOfContainer(
//                    AnimatedContentTransitionScope.SlideDirection.Start
//                )
            } else {
                (fadeIn(tween(300)) + scaleIn(tween(300), 1.1f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(300), 0.9f))
//                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) togetherWith slideOutOfContainer(
//                    AnimatedContentTransitionScope.SlideDirection.End
//                ))
            }
        }) {
        if (layoutType == MessageLayoutType.NORMAL && it != null) {
            ChatPage(
                state = state,
                mainNavController = mainNavController,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                windowSize = windowSize,
                onEvent = viewModel::sendEvent,
                onMainSharedEvent = mainSharedViewModel::sendEvent
            )
        } else {
            Row() {
                MessagePage(
                    modifier =
                    if (layoutType == MessageLayoutType.SPLIT)
                        modifier.width(400.dp) else modifier
                        .weight(1f),
                    viewModel = viewModel,
                    mainNavController = mainNavController,
                    mainSharedState = mainSharedState,
                    listState = listState,
                    layoutType = layoutType,
                    windowSize = windowSize,
                    onMainSharedEvent = mainSharedViewModel::sendEvent
                )
                AnimatedVisibility(visible = layoutType == MessageLayoutType.SPLIT) {
                    ChatPage(
                        state = state,
                        mainNavController = mainNavController,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        windowSize = windowSize,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }
            }
        }
    }

}