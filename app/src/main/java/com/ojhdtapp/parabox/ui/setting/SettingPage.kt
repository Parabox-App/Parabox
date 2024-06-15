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
import androidx.compose.material.icons.outlined.Cloud
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
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.replaceAll
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    viewModel: SettingPageViewModel,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    rootNavigation: StackNavigation<DefaultRootComponent.RootConfig>,
    rootStackState: ChildStack<*, RootComponent.RootChild>,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else Color.Transparent,
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
                        IconButton(onClick = { rootNavigation.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                    scrollBehavior = if (layoutType == LayoutType.SPLIT) scrollBehavior else null,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
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
                Spacer(modifier = Modifier.height(if (layoutType == LayoutType.SPLIT) 24.dp else 16.dp))
            }
            item {
                SettingItem(
                    title = "通用",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "general settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.GeneralSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.GENERAL))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.GeneralSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.GeneralSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.GENERAL)
                }
            }
            item {
                SettingItem(
                    title = "扩展",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.LibraryAdd, contentDescription = "extension settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && (stackState.active.instance is SettingComponent.SettingChild.ExtensionSetting || stackState.active.instance is SettingComponent.SettingChild.ExtensionAddSetting),
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.ADDONS))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.ExtensionSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.ExtensionSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.ADDONS)
                }
            }
            item {
                SettingItem(
                    title = "标签",
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = "label settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && (stackState.active.instance is SettingComponent.SettingChild.LabelSetting || stackState.active.instance is SettingComponent.SettingChild.LabelDetailSetting),
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.LABELS))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.LabelSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.LabelSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.LABELS)
                }
            }
            item {
                SettingItem(
                    title = "界面",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Palette, contentDescription = "interface settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.AppearanceSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.APPEARANCE))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.AppearanceSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.AppearanceSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.APPEARANCE)
                }
            }
            item {
                SettingItem(
                    title = "通知",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.EditNotifications, contentDescription = "notification settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.NotificationSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.NOTIFICATION))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.NotificationSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.NotificationSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.NOTIFICATION)
                }
            }
            item {
                SettingItem(
                    title = "存储",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Storage, contentDescription = "storage settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.StorageSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.STORAGE))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.StorageSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.StorageSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.STORAGE)
                }
            }
            item {
                SettingItem(
                    title = "云服务",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Cloud, contentDescription = "cloud settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.CloudSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.STORAGE))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.CloudSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.CloudSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.CLOUD)
                }
            }
            item {
                SettingItem(
                    title = "试验性",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Science, contentDescription = "experimental settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.ExperimentalSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.EXPERIMENTAL))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.ExperimentalSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.ExperimentalSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.EXPERIMENTAL)
                }
            }
            item {
                SettingItem(
                    title = "帮助与支持",
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "support settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && stackState.active.instance is SettingComponent.SettingChild.HelpAndSupportSetting,
                    layoutType = layoutType
                ) {
//                    viewModel.sendEvent(SettingPageEvent.SelectSetting(Setting.HELP))
                    navigation.bringToFront(DefaultSettingComponent.SettingConfig.HelpAndSupportSetting) {
                        navigation.replaceAll(DefaultSettingComponent.SettingConfig.HelpAndSupportSetting)
                    }
                    scaffoldNavigator.navigateTo(ThreePaneScaffoldRole.Primary, Setting.HELP)
                }
            }
        }
    }
}