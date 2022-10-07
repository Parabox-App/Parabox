package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun ExperimentalPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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
                title = { Text("高级") },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {
                            mainNavController.navigateUp()
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
                Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "该页面提供实验性功能，可能导致不可预见的问题。请勿对该页面功能进行反馈。",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                PreferencesCategory(text = "由 ML Kit 提供")
            }
            item {
                SwitchPreference(
                    title = "实体提取",
                    subtitleOn = "启用",
                    subtitleOff = "在静态文本中以及输入时识别特定实体（地址，日期，邮箱等）",
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
            }
            item {
                SwitchPreference(
                    title = "智能回复",
                    subtitleOn = "启用",
                    subtitleOff = "根据对话的完整上下文生成回复建议（仅支持英语）",
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
            }
            item {
                SwitchPreference(
                    title = "翻译",
                    subtitleOn = "启用",
                    subtitleOff = "将会话内容翻译成您的语言",
                    checked = false,
                    enabled = false,
                    onCheckedChange = {}
                )
            }
            item {
                PreferencesCategory(text = "通知及对话泡")
            }
            item {
                SwitchPreference(
                    title = "允许返回主页",
                    subtitleOn = "返回按钮将于对话泡界面显示",
                    subtitleOff = "将在对话泡界面显示主页按钮。开启后可于悬浮窗使用完整应用功能",
                    checked = viewModel.allowBubbleHomeFlow.collectAsState(initial = false).value,
                    onCheckedChange = viewModel::setAllowBubbleHome
                )
            }
            item {
                SwitchPreference(
                    title = "允许前台发送通知",
                    subtitleOn = "无视应用状态发送通知",
                    subtitleOff = "仅当应用进入后台才发送通知",
                    checked = viewModel.allowForegroundNotificationFlow.collectAsState(initial = false).value,
                    onCheckedChange = viewModel::setAllowForegroundNotification
                )
            }
        }
    }
}