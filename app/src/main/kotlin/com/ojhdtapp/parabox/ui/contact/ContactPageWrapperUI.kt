package com.ojhdtapp.parabox.ui.contact

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.navigation.DefaultMenuComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent
import com.ojhdtapp.parabox.ui.common.LayoutType

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
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val layoutType by remember {
        derivedStateOf {
            if(scaffoldNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                LayoutType.NORMAL
            } else {
                LayoutType.SPLIT
            }
        }
    }
    ListDetailPaneScaffold(
        modifier = modifier.padding(
            horizontal = if (layoutType == LayoutType.NORMAL) 0.dp else 16.dp,
        ),
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(352.dp)) {
                ContactPage(
                    viewModel = viewModel,
                    scaffoldNavigator = scaffoldNavigator,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onMainSharedEvent = mainSharedViewModel::sendEvent
                )
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier) {
                ContactDetailPage(
                    viewModel = viewModel,
                    scaffoldNavigator = scaffoldNavigator,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onMainSharedEvent = mainSharedViewModel::sendEvent
                )
            }
        }
    )
}