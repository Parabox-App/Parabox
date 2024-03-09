package com.ojhdtapp.parabox.ui.message

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import calculateMyStandardPaneScaffoldDirective
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ojhdtapp.parabox.ui.navigation.DefaultMenuComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MessageAndChatPageWrapperUI(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    viewModel: MessagePageViewModel,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>
) {
//    val viewModel = hiltViewModel<MessagePageViewModel>()
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = calculateMyStandardPaneScaffoldDirective(
            windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity),
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    val layoutType by remember{
        derivedStateOf {
            if (scaffoldNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                MessageLayoutType.NORMAL
            } else {
                MessageLayoutType.SPLIT
            }
        }
    }

    BackHandler(state.chatDetail.shouldDisplay == true) {
        viewModel.sendEvent(MessagePageEvent.LoadMessage(null))
    }
    LaunchedEffect(state.chatDetail.shouldDisplay) {
        if (layoutType == MessageLayoutType.NORMAL && state.chatDetail.shouldDisplay == true) {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(false))
        }
        if (layoutType == MessageLayoutType.NORMAL && state.chatDetail.shouldDisplay == false) {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.List)
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(true))
        }
    }

    ListDetailPaneScaffold(
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        modifier = modifier,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(352.dp)) {
                MessagePage(
                    viewModel = viewModel,
                    mainSharedViewModel = mainSharedViewModel,
                    layoutType = layoutType,
                )
            }
        }
    ) {
        AnimatedPane(modifier = Modifier) {
            ChatPage(
                viewModel = viewModel,
                state = state,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                onEvent = viewModel::sendEvent,
                onMainSharedEvent = mainSharedViewModel::sendEvent
            )
        }
    }
}