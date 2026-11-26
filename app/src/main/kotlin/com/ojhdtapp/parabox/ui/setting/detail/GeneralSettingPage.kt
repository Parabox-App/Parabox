package com.ojhdtapp.parabox.ui.setting.detail

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.requestIgnoringBatteryOptimizationSetting
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onNavigateBack: () -> Unit,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    if (layoutType == LayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
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
                        IconButton(onClick = onNavigateBack) {
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
    layoutType: LayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) =
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = "消息页")
        }
        item {
            SettingItem(
                title = "允许水平滑动",
                subTitle = "水平滑动会话项来快速完成或归档",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(
                        thumbContent = {
                            if (mainSharedState.datastore.enableSwipeToDismiss) {
                                Icon(
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "check"
                                )
                            }
                        },
                        checked = mainSharedState.datastore.enableSwipeToDismiss, onCheckedChange = {
                        onMainSharedEvent(
                            MainSharedEvent.UpdateSettingSwitch(
                                DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS,
                                it
                            )
                        )
                    })
                }) {
                onMainSharedEvent(
                    MainSharedEvent.UpdateSettingSwitch(
                        DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS,
                        !mainSharedState.datastore.enableSwipeToDismiss
                    )
                )
            }
        }
        item {
            SettingHeader(text = "聊天页")
        }
        item {
            SettingItem(title = "使用回车键发送消息",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(
                        thumbContent = {
                            if (mainSharedState.datastore.sendViaEnter) {
                                Icon(
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "check"
                                )
                            }
                        },
                        checked = mainSharedState.datastore.sendViaEnter, onCheckedChange = {
                        onMainSharedEvent(
                            MainSharedEvent.UpdateSettingSwitch(
                                DataStoreKeys.SETTINGS_SEND_VIA_ENTER,
                                it
                            )
                        )
                    })
                }) {
                onMainSharedEvent(
                    MainSharedEvent.UpdateSettingSwitch(
                        DataStoreKeys.SETTINGS_SEND_VIA_ENTER,
                        !mainSharedState.datastore.sendViaEnter
                    )
                )
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
                    Switch(
                        thumbContent = {
                            if (mainSharedState.datastore.enableInnerBrowser) {
                                Icon(
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "check"
                                )
                            }
                        },
                        checked = mainSharedState.datastore.enableInnerBrowser, onCheckedChange = {
                        onMainSharedEvent(
                            MainSharedEvent.UpdateSettingSwitch(
                                DataStoreKeys.SETTINGS_ENABLE_INNER_BROWSER,
                                it
                            )
                        )
                    })
                }) {
                onMainSharedEvent(
                    MainSharedEvent.UpdateSettingSwitch(
                        DataStoreKeys.SETTINGS_ENABLE_INNER_BROWSER,
                        !mainSharedState.datastore.enableInnerBrowser
                    )
                )
            }
        }
        item {
            val context = LocalContext.current
            val pm = remember {
                context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            }
            SettingItem(
                title = "关闭电池优化",
                subTitle = "帮助保持服务后台运行，可能增加耗电",
                selected = false,
                layoutType = layoutType,
                disabled = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            ) {
                context.requestIgnoringBatteryOptimizationSetting()
            }
        }
    }