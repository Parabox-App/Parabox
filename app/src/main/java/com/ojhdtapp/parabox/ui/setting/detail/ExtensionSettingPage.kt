package com.ojhdtapp.parabox.ui.setting.detail

import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmapOrNull
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxExtensionStatus
import kotlinx.coroutines.flow.collectLatest
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExtensionSettingPage(
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
                        text = "扩展",
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
                            text = "扩展",
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
) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = "建立新连接")
        }
        items(state.packageInfo, key = { it.packageName }) {
            val label = remember {
                it.applicationInfo.loadLabel(context.packageManager).toString()
            }
            val iconBm = remember {
                it.applicationInfo.loadIcon(context.packageManager).toBitmapOrNull()?.asImageBitmap()
            }
            val subTitle = remember {
                buildString {
                    append("版本: ${it.versionName}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        append("(${it.longVersionCode})")
                    } else {
                        append("(${it.versionCode})")
                    }
                }
            }
            SettingItem(
                title = label,
                subTitle = subTitle,
                leadingIcon = {
                    if (iconBm == null) {
                        Icon(imageVector = Icons.Outlined.Extension, contentDescription = "icon")
                    } else {
                        Image(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape), bitmap = iconBm, contentDescription = "icon"
                        )
                    }
                },
                selected = false,
                layoutType = layoutType
            ) {
            }
        }
        item {
            SettingHeader(text = "已建立的连接")
        }
        items(state.extension, key = { it.extensionId }) {
            Box {
                var isMenuVisible by remember {
                    mutableStateOf(false)
                }
                var status by remember {
                    mutableStateOf("")
                }
                var statusIcon by remember {
                    mutableStateOf(
                        Icons.Outlined.Pending
                    )
                }
                CascadeDropdownMenu(
                    expanded = isMenuVisible,
                    onDismissRequest = { isMenuVisible = false },
                    offset = DpOffset(16.dp, 0.dp),
                    properties = PopupProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        focusable = true
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (it is Extension.ExtensionSuccess) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("重新启动") },
                            onClick = {
                                onEvent(SettingPageEvent.RestartExtensionConnection(it.extensionId))
                                isMenuVisible = false
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.RestartAlt, contentDescription = "restart connection")
                            }
                        )
                    }
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            onEvent(SettingPageEvent.DeleteExtensionInfo(it.extensionId))
                            isMenuVisible = false
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete connection")
                        }
                    )
                }
                LaunchedEffect(Unit) {
                    when (it) {
                        is Extension.ExtensionPending -> {
                            status = "等待实例化"
                            statusIcon = Icons.Outlined.Pending
                        }

                        is Extension.ExtensionFail -> {
                            status = "实例化失败"
                            statusIcon = Icons.Outlined.ErrorOutline
                        }

                        is Extension.ExtensionSuccess -> {
                            it.getStatus().collectLatest {
                                when (it) {
                                    is ParaboxExtensionStatus.Pending -> {
                                        status = "等待初始化"
                                        statusIcon = Icons.Outlined.Pending
                                    }

                                    is ParaboxExtensionStatus.Initializing -> {
                                        status = "正在初始化"
                                        statusIcon = Icons.Outlined.Pending
                                    }

                                    is ParaboxExtensionStatus.Active -> {
                                        status = "运行中"
                                        statusIcon = Icons.Outlined.CheckCircle
                                    }

                                    is ParaboxExtensionStatus.Error -> {
                                        status = "错误（${it.message}）"
                                        statusIcon = Icons.Outlined.ErrorOutline
                                    }
                                }
                            }
                        }
                    }
                }
                SettingItem(
                    title = it.alias,
                    subTitle = status,
                    trailingIcon = {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "status_icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    selected = false,
                    layoutType = layoutType,
                    onLongClick = {
                        isMenuVisible = true
                    }
                ) {
                }
            }

        }
    }
}
