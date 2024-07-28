package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
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
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.SettingNavigateTarget
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.navigation.slideWithOffset
import com.ojhdtapp.parabox.ui.setting.detail.AppearanceSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.CloudSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExperimentalSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionAddSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.GeneralSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.HelpAndSupportSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelDetailSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.NotificationSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.OpenSourceLicenseSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.StorageSettingPage
import kotlinx.coroutines.flow.collectLatest

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class,
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
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Setting>()
    val layoutType by remember {
        derivedStateOf {
            if (scaffoldNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                LayoutType.NORMAL
            } else {
                LayoutType.SPLIT
            }
        }
    }
    val settingStackState by component.settingStack.subscribeAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        mainSharedViewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    is MainSharedEffect.SettingNavigate -> {
                        when(it.target) {
                            SettingNavigateTarget.GENERAL -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.GeneralSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.GeneralSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.GENERAL)
                            }
                            SettingNavigateTarget.ADDONS -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.ExtensionSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.ExtensionSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.ADDONS)
                            }
                            SettingNavigateTarget.LABELS -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.LabelSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.LabelSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.LABELS)
                            }
                            SettingNavigateTarget.APPEARANCE -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.AppearanceSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.AppearanceSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.APPEARANCE)
                            }
                            SettingNavigateTarget.NOTIFICATION -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.NotificationSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.NotificationSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.NOTIFICATION)
                            }
                            SettingNavigateTarget.STORAGE -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.StorageSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.StorageSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.STORAGE)
                            }
                            SettingNavigateTarget.CLOUD -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.CloudSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.CloudSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.CLOUD)
                            }
                            SettingNavigateTarget.EXPERIMENTAL -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.ExperimentalSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.ExperimentalSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.EXPERIMENTAL)
                            }
                            SettingNavigateTarget.HELP -> {
                                component.settingNav.bringToFront(DefaultSettingComponent.SettingConfig.HelpAndSupportSetting) {
                                    component.settingNav.replaceAll(DefaultSettingComponent.SettingConfig.HelpAndSupportSetting)
                                }
                                scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.HELP)
                            }
                        }
                    }
                    else -> {}
                }
            }
    }
    ListDetailPaneScaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(
                horizontal = if (layoutType == LayoutType.NORMAL) 0.dp else 16.dp,
            ),
        directive = scaffoldNavigator.scaffoldDirective,
        value = scaffoldNavigator.scaffoldValue,
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
        },
        detailPane = {
            AnimatedPane(modifier = Modifier) {
                Children(
                    stack = component.settingStack,
                    animation = predictiveBackAnimation(
                        backHandler = component.backHandler,
                        fallbackAnimation = stackAnimation(
                            fade() + slideWithOffset(
                                tween(),
                                Orientation.Horizontal,
                                300f
                            )
                        ),
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
                            ExtensionAddSettingPage(
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
                            AppearanceSettingPage(
                                state = state,
                                mainSharedState = mainSharedState,
                                layoutType = layoutType,
                                scaffoldNavigator = scaffoldNavigator,
                                onEvent = viewModel::sendEvent,
                                onMainSharedEvent = mainSharedViewModel::sendEvent
                            )
                        }

                        is SettingComponent.SettingChild.NotificationSetting -> {
                            NotificationSettingPage(
                                state = state,
                                mainSharedState = mainSharedState,
                                layoutType = layoutType,
                                scaffoldNavigator = scaffoldNavigator,
                                onEvent = viewModel::sendEvent,
                                onMainSharedEvent = mainSharedViewModel::sendEvent
                            )
                        }

                        is SettingComponent.SettingChild.StorageSetting -> {
                            StorageSettingPage(
                                state = state,
                                mainSharedState = mainSharedState,
                                layoutType = layoutType,
                                scaffoldNavigator = scaffoldNavigator,
                                onEvent = viewModel::sendEvent,
                                onMainSharedEvent = mainSharedViewModel::sendEvent
                            )
                        }

                        is SettingComponent.SettingChild.CloudSetting -> {
                            CloudSettingPage(
                                state = state,
                                mainSharedState = mainSharedState,
                                layoutType = layoutType,
                                scaffoldNavigator = scaffoldNavigator,
                                onEvent = viewModel::sendEvent,
                                onMainSharedEvent = mainSharedViewModel::sendEvent
                            )
                        }

                        is SettingComponent.SettingChild.ExperimentalSetting -> {
                            ExperimentalSettingPage(
                                state = state,
                                mainSharedState = mainSharedState,
                                layoutType = layoutType,
                                scaffoldNavigator = scaffoldNavigator,
                                onEvent = viewModel::sendEvent,
                                onMainSharedEvent = mainSharedViewModel::sendEvent
                            )
                        }

                        is SettingComponent.SettingChild.HelpAndSupportSetting -> {
                            HelpAndSupportSettingPage(
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

                        is SettingComponent.SettingChild.OpenSourceLicenseSetting -> {
                            OpenSourceLicenseSettingPage(
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
                    }
                }
            }
        })
}