package com.ojhdtapp.parabox.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.navigation.AppNavTransitions
import com.ojhdtapp.parabox.ui.navigation.SceneKeys
import com.ojhdtapp.parabox.ui.navigation.SettingRoute
import com.ojhdtapp.parabox.ui.navigation.rememberAppPaneScaffoldDirective
import com.ojhdtapp.parabox.ui.navigation.toLayoutType
import com.ojhdtapp.parabox.ui.navigation.toSetting
import com.ojhdtapp.parabox.ui.navigation.toSettingRoute
import com.ojhdtapp.parabox.ui.setting.detail.AppearanceSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.CloudSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExperimentalSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionAddSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionConfigSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.ExtensionSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.GeneralSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.HelpAndSupportSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelDetailSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.LabelSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.NotificationSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.OpenSourceLicenseSettingPage
import com.ojhdtapp.parabox.ui.setting.detail.StorageSettingPage
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingPageWrapperUi(
    modifier: Modifier = Modifier,
    initialCategory: Setting? = null,
    mainSharedViewModel: MainSharedViewModel,
    viewModel: SettingPageViewModel,
    onExitSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    val paneDirective = rememberAppPaneScaffoldDirective()
    val layoutType = paneDirective.toLayoutType()

    val settingBackStack = rememberNavBackStack(
        *(if (initialCategory != null) {
            arrayOf<NavKey>(SettingRoute.Home, initialCategory.toSettingRoute())
        } else {
            arrayOf<NavKey>(SettingRoute.Home)
        }),
    )

    fun selectCategory(category: Setting) {
        while (settingBackStack.size > 1) {
            settingBackStack.removeLastOrNull()
        }
        settingBackStack.add(category.toSettingRoute())
    }

    fun pushSubSetting(route: SettingRoute) {
        if (settingBackStack.lastOrNull() != route) {
            settingBackStack.add(route)
        }
    }

    fun popSetting() {
        if (settingBackStack.size > 1) {
            settingBackStack.removeLastOrNull()
        } else {
            onExitSettings()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        mainSharedViewModel.uiEffect
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest { effect ->
                if (effect is MainSharedEffect.SettingNavigate) {
                    selectCategory(effect.target.toSetting())
                }
            }
    }

    val selectedCategory = (settingBackStack.lastOrNull() as? SettingRoute)?.category ?: Setting.GENERAL
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
        directive = paneDirective,
    )

    NavDisplay(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = if (layoutType == LayoutType.NORMAL) 0.dp else 16.dp),
        backStack = settingBackStack,
        sceneStrategy = listDetailStrategy,
        onBack = { popSetting() },
        transitionSpec = { AppNavTransitions.forwardTransition() },
        popTransitionSpec = { AppNavTransitions.popTransition() },
        predictivePopTransitionSpec = { swipeEdge -> AppNavTransitions.predictivePopTransition(swipeEdge) },
        entryProvider = entryProvider {
            entry<SettingRoute.Home>(
                metadata = ListDetailSceneStrategy.listPane(
                    sceneKey = SceneKeys.SETTING,
                    detailPlaceholder = {
                        GeneralSettingPage(
                            state = state,
                            mainSharedState = mainSharedState,
                            layoutType = layoutType,
                            onNavigateBack = ::popSetting,
                            onEvent = viewModel::sendEvent,
                            onMainSharedEvent = mainSharedViewModel::sendEvent,
                        )
                    },
                ),
            ) {
                SettingPage(
                    viewModel = viewModel,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    selectedCategory = selectedCategory,
                    onSelectCategory = ::selectCategory,
                    onExitSettings = onExitSettings,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.General>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                GeneralSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Extension>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                ExtensionSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onNavigateTo = ::pushSubSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.ExtensionAdd>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                ExtensionAddSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.ExtensionConfig>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                ExtensionConfigSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Label>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                LabelSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onNavigateTo = ::pushSubSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.LabelDetail>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                LabelDetailSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Appearance>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                AppearanceSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Notification>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                NotificationSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Storage>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                StorageSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Cloud>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                CloudSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Experimental>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                ExperimentalSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.Help>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                HelpAndSupportSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onNavigateTo = ::pushSubSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }

            entry<SettingRoute.OpenSourceLicense>(
                metadata = ListDetailSceneStrategy.detailPane(sceneKey = SceneKeys.SETTING),
            ) {
                OpenSourceLicenseSettingPage(
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onNavigateBack = ::popSetting,
                    onEvent = viewModel::sendEvent,
                    onMainSharedEvent = mainSharedViewModel::sendEvent,
                )
            }
        },
    )
}
