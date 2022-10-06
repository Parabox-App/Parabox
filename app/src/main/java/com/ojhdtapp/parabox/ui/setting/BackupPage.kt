package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPage(
    modifier: Modifier = Modifier,
    viewModel: SettingPageViewModel,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val cacheSize = viewModel.cacheSizeStateFlow.collectAsState()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colorTransitionFraction = scrollBehavior.state.collapsedFraction
            val appBarContainerColor by rememberUpdatedState(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    FastOutLinearInEasing.transform(colorTransitionFraction)
                )
            )
            LargeTopAppBar(
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                title = { Text("备份与还原") },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {

                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) {
        LazyColumn(
            contentPadding = it
        ) {
            item {
                NormalPreference(
                    title = "备份",
                    subtitle = "将会话，聊天记录导出到文件中（不包含图片）",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = "backup"
                        )
                    },
                    onClick = {
                        onEvent(ActivityEvent.Backup)
                    }
                )
            }
            item {
                NormalPreference(
                    title = "还原",
                    subtitle = "从备份文件还原记录",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = "restore"
                        )
                    },
                    onClick = {
                        onEvent(ActivityEvent.Restore)
                    }
                )
            }
            item {
                PreferencesCategory(text = "空间管理")
            }
            item {
                NormalPreference(
                    title = "清理缓存",
                    subtitle = "应用缓存已占用 ${cacheSize.value}\n缓存清理不影响聊天记录",
                    enabled = !viewModel.cleaningCache.value,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "clean cache",
                            tint = if (viewModel.cleaningCache.value) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        viewModel.clearCache()
                    }
                )
            }
        }
    }
}