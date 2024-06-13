package com.ojhdtapp.parabox.ui.setting.detail

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.launchNotificationSetting
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NotificationSettingPage(
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
                        text = "通知",
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
                            text = "通知",
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val context = LocalContext.current
    val notificationPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {

    }
    LazyColumn(modifier = modifier) {
        item{
            SettingHeader(
                text = "行为"
            )
        }
        item {
            SettingItem(
                title = "允许前台发送",
                subTitle = "应用前台运行时也照常发送通知",
                selected = false,
                layoutType = layoutType,
                trailingIcon = {
                    Switch(checked = mainSharedState.datastore.enableForegroundNotification, onCheckedChange = {
                        onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION, it))
                    })
                }) {
                onMainSharedEvent(MainSharedEvent.UpdateSettingSwitch(
                    DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION,
                    !mainSharedState.datastore.enableForegroundNotification))
            }
        }
        item{
            SettingHeader(
                text = "系统设置"
            )
        }
        item {
            SettingItem(
                title = stringResource(R.string.request_notification_permission),
                subTitle = if (notificationPermissionState.status.isGranted) stringResource(R.string.notification_permission_granted)
                else stringResource(R.string.notification_permission_not_granted),
                selected = false,
                layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = "channel",
                        tint = if (notificationPermissionState.status.isGranted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                }) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        item {
            SettingItem(
                title = stringResource(R.string.system_notification_settings_title),
                subTitle = stringResource(R.string.system_notification_settings_subtitle),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = "channel"
                    )
                },
                selected = false, layoutType = layoutType
            ) {
                context.launchNotificationSetting()
            }
        }
        item{
            SettingHeader(
                text = "禁用通知的会话"
            )
        }
        when (state.notificationState.loadState) {
            LoadState.LOADING -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            LoadState.ERROR -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "加载时发生错误", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            LoadState.SUCCESS -> {
                if (state.notificationState.chatList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有会话", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(items = state.notificationState.chatList) { chat ->
                    SettingItem(
                        title = chat.name,
                        selected = false,
                        layoutType = layoutType,
                        leadingIcon = {
                                CommonAvatar(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    model = CommonAvatarModel(chat.avatar.getModel(), chat.name))
                        }
                    ) {}
                }
            }
        }
        item {
            SettingItem(title = "添加会话", selected = false, layoutType = layoutType,
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.AddComment, contentDescription = "添加会话")
                }) {
                onMainSharedEvent(MainSharedEvent.PickChat(
                    onDone = { chat ->
                        chat?.let {
                            onEvent(SettingPageEvent.UpdateChatNotificationEnabled(chat.chatId, false))
                        }
                    }
                ))
            }
        }
    }
}