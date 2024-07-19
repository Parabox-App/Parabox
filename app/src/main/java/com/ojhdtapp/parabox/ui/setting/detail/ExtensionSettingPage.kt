package com.ojhdtapp.parabox.ui.setting.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.ojhdtapp.parabox.domain.model.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.paraboxdevelopmentkit.extension.ParaboxConnectionStatus
import kotlinx.coroutines.flow.collectLatest
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExtensionSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler(enabled = layoutType != LayoutType.SPLIT) {
        scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
    }
    if (layoutType == LayoutType.SPLIT) {
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
                    navigation = navigation,
                    stackState = stackState,
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
                navigation = navigation,
                stackState = stackState,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(text = "建立新连接")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                items(items = state.extensionList, key = { it.key }) {
                    ConnectionCard(
                        model = it,
                        onClick = {
                            if (it is Extension.Success) {
                                onEvent(SettingPageEvent.InitNewConnection(it))
                                navigation.pushNew(DefaultSettingComponent.SettingConfig.ExtensionAddSetting)
                            }
                        }
                    )
                }
            }
        }
        item {
            SettingItem(title = "刷新可用连接", selected = false, layoutType = layoutType,
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "refresh", tint = MaterialTheme.colorScheme.onSurface)
                }) {
                onEvent(SettingPageEvent.ReloadExtension)
            }
        }
        item {
            SettingHeader(text = "已建立的连接")
        }
        items(state.connectionList, key = { "${it.extensionId}#${it.alias}" }) {
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
                    if (it is Connection.ConnectionSuccess) {
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
                        is Connection.ConnectionPending -> {
                            status = "等待实例化"
                            statusIcon = Icons.Outlined.Pending
                        }

                        is Connection.ConnectionFail -> {
                            status = "实例化失败"
                            statusIcon = Icons.Outlined.ErrorOutline
                        }

                        is Connection.ConnectionSuccess -> {
                            it.getStatus().collectLatest {
                                when (it) {
                                    is ParaboxConnectionStatus.Pending -> {
                                        status = "等待初始化"
                                        statusIcon = Icons.Outlined.Pending
                                    }

                                    is ParaboxConnectionStatus.Initializing -> {
                                        status = "正在初始化"
                                        statusIcon = Icons.Outlined.Pending
                                    }

                                    is ParaboxConnectionStatus.Active -> {
                                        status = "运行中"
                                        statusIcon = Icons.Outlined.CheckCircle
                                    }

                                    is ParaboxConnectionStatus.Error -> {
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
        if (state.connectionList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无连接",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    modifier: Modifier = Modifier,
    model: Extension,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = model is Extension.Success
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.width(144.dp)) {
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (model.icon == null) {
                        Icon(imageVector = Icons.Outlined.Extension, contentDescription = "icon")
                    } else {
                        if (model.icon is ImageBitmap) {
                            Image(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                bitmap = model.icon as ImageBitmap, contentDescription = "icon")
                        } else {
                            AsyncImage(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                model = model.icon, contentDescription = "icon")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = model.name, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.basicMarquee(
                    delayMillis = 5000,
                    initialDelayMillis = 2000
                ), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                when(model) {
                    is Extension.Error -> {
                        val primaryColor = MaterialTheme.colorScheme.error
                        val text = remember {
                            buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = primaryColor
                                    )
                                ) {
                                    append("错误 ")
                                }
                                append(model.errMsg)
                            }
                        }
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(
                                delayMillis = 5000,
                                initialDelayMillis = 2000
                            )
                        )
                    }
                    is Extension.Success.BuiltIn -> {
                        Text(text = model.des?: "无说明文本", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.basicMarquee(
                                delayMillis = 5000,
                        initialDelayMillis = 2000
                        ))
                    }
                    is Extension.Success.External -> {
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val text = remember {
                            buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = primaryColor
                                    )
                                ) {
                                    append("外部扩展 ")
                                }
                                append(model.pkg)
                            }
                        }
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(
                                delayMillis = 5000,
                                initialDelayMillis = 2000
                            )
                        )
                    }
                }
            }
            Icon(imageVector = Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = "")
        }
    }
}