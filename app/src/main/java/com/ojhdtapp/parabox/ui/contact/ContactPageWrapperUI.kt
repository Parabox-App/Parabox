package com.ojhdtapp.parabox.ui.contact

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.menu.calculateMyPaneScaffoldDirective
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ContactPageWrapperUI(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    viewModel: ContactPageViewModel,
    navigation: StackNavigation<DefaultRootComponent.Config>,
    stackState: ChildStack<*, RootComponent.Child>
){
//    val viewModel = hiltViewModel<ContactPageViewModel>()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = calculateMyPaneScaffoldDirective(
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    val layoutType by remember {
        derivedStateOf {
            if(scaffoldNavigator.scaffoldState.scaffoldDirective.maxHorizontalPartitions == 1) {
                ContactLayoutType.NORMAL
            } else {
                ContactLayoutType.SPLIT
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        scaffoldState = scaffoldNavigator.scaffoldState,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(352.dp)) {
                ContactPage(
                    viewModel = viewModel,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onMainSharedEvent = mainSharedViewModel::sendEvent
                )
            }
        }
    ) {
        AnimatedPane(modifier = Modifier) {
            ContactDetailPage(
                viewModel = viewModel,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                onMainSharedEvent = mainSharedViewModel::sendEvent
            )
        }
    }
}