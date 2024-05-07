package com.ojhdtapp.parabox.ui.setting.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GeneralSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler(enabled = layoutType != SettingLayoutType.SPLIT) {
        scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
    }
    if (layoutType == SettingLayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = "通用",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Content(
                    modifier = Modifier.weight(1f),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent,
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "通用",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest) }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                state = state,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) =
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = "消息页")
        }
        item {
            SettingItem(
                title = "滚动名称",
                subTitle = "在会话名称超出屏幕宽度时滚动展示",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.enableMarqueeEffectOnChatName, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_ENABLE_MARQUEE_EFFECT_ON_CHAT_NAME, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_ENABLE_MARQUEE_EFFECT_ON_CHAT_NAME,
                    !mainSharedState.datastore.enableMarqueeEffectOnChatName))
            }
        }
        item {
            SettingItem(
                title = "允许水平滑动",
                subTitle = "水平滑动会话项来快速完成或归档",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.enableSwipeToDismiss, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS,
                    !mainSharedState.datastore.enableSwipeToDismiss))
            }
        }
        item {
            SettingHeader(text = "聊天页")
        }
        item {
            SettingItem(title = "展示头像",
                subTitle = "在顶栏展示当前会话头像",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.displayAvatarOnTopAppBar, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_DISPLAY_AVATAR_ON_TOP_APPBAR, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_DISPLAY_AVATAR_ON_TOP_APPBAR,
                    !mainSharedState.datastore.displayAvatarOnTopAppBar))
            }
        }
        item {
            SettingItem(title = "展示消息接收时间",
                subTitle = "在每条消息上展示接收时间",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.displayTimeOnEachMsg, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_DISPLAY_TIME_ON_EACH_MSG, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_DISPLAY_TIME_ON_EACH_MSG,
                    !mainSharedState.datastore.displayTimeOnEachMsg))
            }
        }
        item {
            SettingItem(title = "使用回车键发送消息",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.sendViaEnter, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_SEND_VIA_ENTER, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_SEND_VIA_ENTER,
                    !mainSharedState.datastore.sendViaEnter))
            }
        }
        item {
            SettingHeader(text = "全局")
        }
        item {
            SettingItem(title = "应用内浏览器",
                subTitle = "使用应用内浏览器访问链接",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.enableInnerBrowser, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_ENABLE_INNER_BROWSER, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_ENABLE_INNER_BROWSER,
                    !mainSharedState.datastore.enableInnerBrowser))
            }
        }
    }