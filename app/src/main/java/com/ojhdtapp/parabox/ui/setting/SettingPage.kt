package com.ojhdtapp.parabox.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.EditNotifications
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
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    navigation: StackNavigation<DefaultRootComponent.RootConfig>,
    stackState: ChildStack<*, RootComponent.RootChild>,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (layoutType != SettingLayoutType.SPLIT) MaterialTheme.colorScheme.surface else Color.Transparent,
        topBar = {
            MaterialTheme(
                typography = MaterialTheme.typography.copy(
                    headlineMedium = MaterialTheme.typography.displaySmall
                )
            ) {
                LargeTopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
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
                    scrollBehavior = if (layoutType == SettingLayoutType.SPLIT) scrollBehavior else null,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = if (layoutType != SettingLayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = if (layoutType != SettingLayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(if (layoutType == SettingLayoutType.SPLIT) 24.dp else 16.dp))
            }
            item {
                SettingItem(
                    title = "通用",
                    leadingIcon = Icons.Outlined.Settings,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.GENERAL,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.GENERAL))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.GENERAL)
                }
            }
            item {
                SettingItem(
                    title = "扩展",
                    leadingIcon = Icons.Outlined.LibraryAdd,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.ADDONS,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.ADDONS))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.ADDONS)
                }
            }
            item {
                SettingItem(
                    title = "标签",
                    leadingIcon = Icons.AutoMirrored.Outlined.Label,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.LABELS,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.LABELS))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.LABELS)
                }
            }
            item {
                SettingItem(
                    title = "界面",
                    leadingIcon = Icons.Outlined.Palette,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.APPEARANCE,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.APPEARANCE))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.APPEARANCE)
                }
            }
            item {
                SettingItem(
                    title = "通知",
                    leadingIcon = Icons.Outlined.EditNotifications,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.NOTIFICATION,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.NOTIFICATION))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.NOTIFICATION)
                }
            }
            item {
                SettingItem(
                    title = "存储空间",
                    leadingIcon = Icons.Outlined.Storage,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.STORAGE,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.STORAGE))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.STORAGE)
                }
            }
            item {
                SettingItem(
                    title = "试验性",
                    leadingIcon = Icons.Outlined.Science,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.EXPERIMENTAL,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.EXPERIMENTAL))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.EXPERIMENTAL)
                }
            }
            item {
                SettingItem(
                    title = "帮助与支持",
                    leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline,
                    selected = layoutType == SettingLayoutType.SPLIT && state.selected == Setting.HELP,
                    layoutType = layoutType
                ) {
                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.HELP))
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.HELP)
                }
            }
        }
    }
}