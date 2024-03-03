package com.ojhdtapp.parabox.ui.setting

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditNotifications
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.ThreePaneScaffoldRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    viewModel: SettingPageViewModel,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultRootComponent.Config>,
    stackState: ChildStack<*, RootComponent.Child>,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    BackHandler {
        navigation.pop()
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigation.pop() }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                    }
                },
                scrollBehavior = if (layoutType == SettingLayoutType.SPLIT) scrollBehavior else null
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(if (layoutType == SettingLayoutType.SPLIT) 24.dp else 16.dp))
            }
            item {
                SettingCategoryItem(
                    title = "通用",
                    icon = Icons.Outlined.Settings,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.GENERAL
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.GENERAL))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.GENERAL)
                }
            }
            item {
                SettingCategoryItem(
                    title = "扩展",
                    icon = Icons.Outlined.LibraryAdd,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.ADDONS
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.ADDONS))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.ADDONS)
                }
            }
            item {
                SettingCategoryItem(
                    title = "标签",
                    icon = Icons.AutoMirrored.Outlined.Label,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.LABELS
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.LABELS))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.LABELS)
                }
            }
            item {
                SettingCategoryItem(
                    title = "界面",
                    icon = Icons.Outlined.Palette,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.APPEARANCE
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.APPEARANCE))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.APPEARANCE)
                }
            }
            item {
                SettingCategoryItem(
                    title = "通知",
                    icon = Icons.Outlined.EditNotifications,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.NOTIFICATION
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.NOTIFICATION))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.NOTIFICATION)
                }
            }
            item {
                SettingCategoryItem(
                    title = "存储空间",
                    icon = Icons.Outlined.Storage,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.STORAGE
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.STORAGE))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.STORAGE)
                }
            }
            item {
                SettingCategoryItem(
                    title = "试验性",
                    icon = Icons.Outlined.Science,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.EXPERIMENTAL
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.EXPERIMENTAL))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.EXPERIMENTAL)
                }
            }
            item {
                SettingCategoryItem(
                    title = "帮助与支持",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.HELP
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.HELP))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Secondary, Setting.HELP)
                }
            }
        }
    }
}