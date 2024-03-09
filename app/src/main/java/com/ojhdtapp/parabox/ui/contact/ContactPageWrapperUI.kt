package com.ojhdtapp.parabox.ui.contact

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
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
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.navigation.DefaultMenuComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ContactPageWrapperUI(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    viewModel: ContactPageViewModel,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>
){
//    val viewModel = hiltViewModel<ContactPageViewModel>()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>(
        scaffoldDirective = calculateMyStandardPaneScaffoldDirective(
            windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity),
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    val layoutType by remember {
        derivedStateOf {
            if(scaffoldNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                ContactLayoutType.NORMAL
            } else {
                ContactLayoutType.SPLIT
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier,
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
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