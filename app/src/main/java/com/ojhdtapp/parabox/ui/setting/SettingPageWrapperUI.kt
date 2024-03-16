package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.navigation.slideWithOffset
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.GeneralSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelDetailSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelSettingPage

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalDecomposeApi::class
)
@Composable
fun SettingPageWrapperUi(
    modifier: Modifier = Modifier,
    component: SettingComponent,
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
    val settingStackState by component.settingStack.subscribeAsState()

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
                    navigation = component.settingNav,
                    stackState = settingStackState,
                    rootNavigation = navigation,
                    rootStackState = stackState
                )
            }
        }
    ) {
        AnimatedPane(modifier = Modifier) {
            Children(
                stack = component.settingStack,
                animation = predictiveBackAnimation(
                    backHandler = component.backHandler,
                    fallbackAnimation = stackAnimation(fade() + slideWithOffset(tween(), Orientation.Horizontal, 300f)),
                    selector = { backEvent, _, _ -> androidPredictiveBackAnimatable(backEvent) },
                    onBack = {
                        component.settingNav.pop()
                    },
                ),
            ) { child ->
                when (val instance = child.instance) {
                    is SettingComponent.SettingChild.GeneralSetting -> {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.ExtensionSetting -> {
                        ExtensionSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            navigation = component.settingNav,
                            stackState = settingStackState,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.ExtensionAddSetting -> {
                        ExtensionSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            navigation = component.settingNav,
                            stackState = settingStackState,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.LabelSetting -> {
                        LabelSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            navigation = component.settingNav,
                            stackState = settingStackState,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.LabelDetailSetting -> {
                        LabelDetailSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            navigation = component.settingNav,
                            stackState = settingStackState,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.AppearanceSetting -> {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.NotificationSetting -> {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.StorageSetting -> {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.ExperimentalSetting -> {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            scaffoldNavigator = scaffoldNavigator,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent
                        )
                    }
                    is SettingComponent.SettingChild.HelpAndSupportSetting -> {
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
//            when (state.selected) {
//                Setting.GENERAL -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.ADDONS -> {
//                    ExtensionSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.LABELS -> {
//                    LabelSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.APPEARANCE -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.NOTIFICATION -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.STORAGE -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.EXPERIMENTAL -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//
//                Setting.HELP -> {
//                    GeneralSettingPage(
//                        state = state,
//                        mainSharedState = mainSharedState,
//                        layoutType = layoutType,
//                        scaffoldNavigator = scaffoldNavigator,
//                        onEvent = viewModel::sendEvent,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
//            }
        }
    }
}