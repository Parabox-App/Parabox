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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.adaptive.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.calculateDensePaneScaffoldDirective
import androidx.compose.material3.adaptive.calculatePosture
import androidx.compose.material3.adaptive.calculateStandardPaneScaffoldDirective
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.suite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.common.MessageNavGraph
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.menu.calculateMyPaneScaffoldDirective
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.navigate

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
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

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator(
        scaffoldDirective = calculateMyPaneScaffoldDirective(
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    ListDetailPaneScaffold(
        modifier = modifier,
        scaffoldState = scaffoldNavigator.scaffoldState,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(400.dp)) {
//                AnimatedContent(targetState = state.chatDetail.chat, label = "",
//                    transitionSpec = {
//                        if (targetState != null) {
//                            (fadeIn(tween(300)) + scaleIn(
//                                tween(300),
//                                0.9f
//                            )) togetherWith (fadeOut(tween(300)) + scaleOut(
//                                tween(300),
//                                1.1f
//                            ))
//                        } else {
//                            (fadeIn(tween(300)) + scaleIn(
//                                tween(300),
//                                1.1f
//                            )) togetherWith (fadeOut(tween(300)) + scaleOut(
//                                tween(300),
//                                0.9f
//                            ))
//                        }
//                    }) {
//                    if (it != null) {
//                        ChatPage(
//                            viewModel = viewModel,
//                            state = state,
//                            mainNavController = mainNavController,
//                            mainSharedState = mainSharedState,
//                            layoutType = layoutType,
//                            windowSize = windowSize,
//                            onEvent = viewModel::sendEvent,
//                            onMainSharedEvent = mainSharedViewModel::sendEvent
//                        )
//                    } else {
//                        MessagePage(
//                            viewModel = viewModel,
//                            mainNavController = mainNavController,
//                            mainSharedState = mainSharedState,
//                            listState = listState,
//                            layoutType = layoutType,
//                            windowSize = windowSize,
//                            onMainSharedEvent = mainSharedViewModel::sendEvent
//                        )
//                    }
//                }
                MessagePage(
                            viewModel = viewModel,
                            mainNavController = mainNavController,
                            mainSharedState = mainSharedState,
                            listState = listState,
                            layoutType = layoutType,
                            windowSize = windowSize,
                            onMainSharedEvent = {
                                mainSharedViewModel.sendEvent(it)
                            }
                        )
            }
        }
    ) {
        AnimatedPane(modifier = Modifier) {
            ChatPage(
                viewModel = viewModel,
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


//    AnimatedContent(targetState = state.chatDetail.chat, label = "",
//        transitionSpec = {
//            if (layoutType == MessageLayoutType.NORMAL && targetState != null) {
//                (fadeIn(tween(300)) + scaleIn(tween(300), 0.9f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(300), 1.1f))
//            } else {
//                (fadeIn(tween(300)) + scaleIn(tween(300), 1.1f)) togetherWith (fadeOut(tween(300)) + scaleOut(tween(300), 0.9f))
//            }
//        }) {
//        if (layoutType == MessageLayoutType.NORMAL && it != null) {
//            ChatPage(
//                viewModel = viewModel,
//                state = state,
//                mainNavController = mainNavController,
//                mainSharedState = mainSharedState,
//                layoutType = layoutType,
//                windowSize = windowSize,
//                onEvent = viewModel::sendEvent,
//                onMainSharedEvent = mainSharedViewModel::sendEvent
//            )
//        } else {
//            Row() {
//                MessagePage(
//                    modifier =
//                    if (layoutType == MessageLayoutType.SPLIT)
//                        modifier.width(400.dp) else modifier
//                        .weight(1f),
//                    viewModel = viewModel,
//                    mainNavController = mainNavController,
//                    mainSharedState = mainSharedState,
//                    listState = listState,
//                    layoutType = layoutType,
//                    windowSize = windowSize,
//                    onMainSharedEvent = mainSharedViewModel::sendEvent
//                )
//                AnimatedVisibility(visible = layoutType == MessageLayoutType.SPLIT) {
//                    ChatPage(
//                        viewModel = viewModel,
//                        state = state,
//                        mainNavController = mainNavController,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        windowSize = windowSize,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//            }
//        }
//    }
}