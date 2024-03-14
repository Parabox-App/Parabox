package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
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
import calculateMyDensePaneScaffoldDirective
import calculateMyStandardPaneScaffoldDirective
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.GeneralSettingPage

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SettingPageWrapperUi(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    viewModel: SettingPageViewModel,
    navigation: StackNavigation<DefaultRootComponent.RootConfig>,
    stackState: ChildStack<*, RootComponent.RootChild>
) {
//    val viewModel = hiltViewModel<SettingPageViewModel>()
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Setting>(
        scaffoldDirective = calculateMyDensePaneScaffoldDirective(
            windowSizeClass = calculateWindowSizeClass(activity = LocalContext.current as Activity),
            windowAdaptiveInfo = currentWindowAdaptiveInfo()
        )
    )
    val layoutType by remember {
        derivedStateOf {
            if (scaffoldNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                SettingLayoutType.NORMAL
            } else {
                SettingLayoutType.SPLIT
            }
        }
    }

    ListDetailPaneScaffold(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
        windowInsets = WindowInsets(0.dp),
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(352.dp)) {
                SettingPage(
                    viewModel = viewModel,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    scaffoldNavigator = scaffoldNavigator,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                    navigation = navigation,
                    stackState = stackState
                )
            }
        }
    ) {
        AnimatedPane(modifier = Modifier) {
            when (state.selected) {
                Setting.GENERAL -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.ADDONS -> {
                    ExtensionSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.LABELS -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.APPEARANCE -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.NOTIFICATION -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.STORAGE -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.EXPERIMENTAL -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }

                Setting.HELP -> {
                    GeneralSettingPage(
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        scaffoldNavigator = scaffoldNavigator,
                        onEvent = viewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }
            }
        }
    }
}