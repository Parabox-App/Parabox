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
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.ContactNavGraph
import com.ojhdtapp.parabox.ui.menu.calculateMyPaneScaffoldDirective
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Destination
@ContactNavGraph(start = true)
@Composable
fun ContactPageWrapperUI(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
){
    val viewModel = hiltViewModel<ContactPageViewModel>()
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
                    mainNavController = mainNavController,
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
                mainNavController = mainNavController,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                onMainSharedEvent = mainSharedViewModel::sendEvent
            )
        }
    }
}