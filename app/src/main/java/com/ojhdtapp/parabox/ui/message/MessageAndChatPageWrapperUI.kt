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
import androidx.compose.material3.adaptive.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.calculateDensePaneScaffoldDirective
import androidx.compose.material3.adaptive.calculatePosture
import androidx.compose.material3.adaptive.calculateStandardPaneScaffoldDirective
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
) {
    val viewModel = hiltViewModel<MessagePageViewModel>()
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = calculateMyPaneScaffoldDirective(
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    val layoutType by remember{
        derivedStateOf {
            if (scaffoldNavigator.scaffoldState.scaffoldDirective.maxHorizontalPartitions == 1) {
                MessageLayoutType.NORMAL
            } else {
                MessageLayoutType.SPLIT
            }
        }
    }

    BackHandler(state.chatDetail.shouldDisplay) {
        viewModel.sendEvent(MessagePageEvent.LoadMessage(null))
    }
    LaunchedEffect(state.chatDetail.shouldDisplay) {
        if (layoutType == MessageLayoutType.NORMAL && state.chatDetail.shouldDisplay) {
            scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary)
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(false))
        }
        if (layoutType == MessageLayoutType.NORMAL && !state.chatDetail.shouldDisplay) {
            scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary)
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(true))
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        scaffoldState = scaffoldNavigator.scaffoldState,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(352.dp)) {
                MessagePage(
                    viewModel = viewModel,
                    mainNavController = mainNavController,
                    mainSharedState = mainSharedState,
                    listState = listState,
                    layoutType = layoutType,
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
                onEvent = viewModel::sendEvent,
                onMainSharedEvent = mainSharedViewModel::sendEvent
            )
        }
    }
}