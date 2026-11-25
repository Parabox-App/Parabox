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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    viewModel: SettingPageViewModel,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    selectedCategory: Setting,
    onSelectCategory: (Setting) -> Unit,
    onExitSettings: () -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else Color.Transparent,
        topBar = {
            MaterialTheme(
                typography = MaterialTheme.typography.copy(
                    headlineMedium = MaterialTheme.typography.displaySmall,
                ),
            ) {
                LargeTopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onExitSettings) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                    scrollBehavior = if (layoutType == LayoutType.SPLIT) scrollBehavior else null,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                        scrolledContainerColor = if (layoutType != LayoutType.SPLIT) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.GENERAL,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.GENERAL)
                }
            }
            item {
                SettingItem(
                    title = "连接与扩展",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.LibraryAdd, contentDescription = "extension settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.ADDONS,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.ADDONS)
                }
            }
            item {
                SettingItem(
                    title = "标签",
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Label, contentDescription = "label settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.LABELS,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.LABELS)
                }
            }
            item {
                SettingItem(
                    title = "界面",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Palette, contentDescription = "interface settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.APPEARANCE,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.APPEARANCE)
                }
            }
            item {
                SettingItem(
                    title = "通知",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.EditNotifications, contentDescription = "notification settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.NOTIFICATION,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.NOTIFICATION)
                }
            }
            item {
                SettingItem(
                    title = "存储",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Storage, contentDescription = "storage settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.STORAGE,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.STORAGE)
                }
            }
            item {
                SettingItem(
                    title = "云服务",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Cloud, contentDescription = "cloud settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.CLOUD,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.CLOUD)
                }
            }
            item {
                SettingItem(
                    title = "试验性",
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Science, contentDescription = "experimental settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.EXPERIMENTAL,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.EXPERIMENTAL)
                }
            }
            item {
                SettingItem(
                    title = "帮助与支持",
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "support settings", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    selected = layoutType == LayoutType.SPLIT && selectedCategory == Setting.HELP,
                    layoutType = layoutType,
                ) {
                    onSelectCategory(Setting.HELP)
                }
            }
        }
    }
}
