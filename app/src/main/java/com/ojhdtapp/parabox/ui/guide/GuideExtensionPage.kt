package com.ojhdtapp.parabox.ui.guide

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.HyperlinkText
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideFCMSenderPageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.theme.fontSize
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.GuideNavGraph
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideExtensionPage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()

    var showSkipGuideDialog by remember {
        mutableStateOf(false)
    }
    if (showSkipGuideDialog) {
        AlertDialog(
            onDismissRequest = { showSkipGuideDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSkipGuideDialog = false
                    mainNavController.navigate(GuideFCMSenderPageDestination)
                }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSkipGuideDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "略过扩展检查")
            },
            text = {
                Text(text = "检测到扩展未安装或配置不正确，Parabox 将无法正常使用。\n\n是否继续？")
            }
        )
    }

    BottomSheetScaffold(
        modifier = Modifier
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        sheetContent = {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedButton(onClick = {
                    mainNavController.navigateUp()
                }) {
                    Text(text = "返回")
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!pluginList.any { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }) {
                    TextButton(onClick = {
                        showSkipGuideDialog = true
                    }) {
                        Text(text = "仍然继续")
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        mainNavController.navigate(GuideFCMSenderPageDestination)
                    },
                    enabled = pluginList.any { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }
                ) {
                    if (pluginList.any { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }) {
                        Text(text = "继续")
                    } else {
                        Text(text = "未完成")
                    }
                }
            }
        },
        sheetElevation = 0.dp,
        sheetBackgroundColor = Color.Transparent,
//        sheetPeekHeight = 56.dp,
        backgroundColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Icon(
                    modifier = Modifier
                        .padding(start = 32.dp, end = 32.dp, top = 32.dp)
                        .size(48.dp),
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = "extension",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    text = "扩展检查",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "Parabox 需配合扩展运行。扩展将承担与各即时通讯平台对接的职能，为 Parabox 提供消息源与发送信道。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "如果已安装的扩展未显示于下方，或状态获取出现延迟，请尝试重置扩展连接。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                TextButton(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    onClick = {
                        onEvent(ActivityEvent.ResetExtension)
                    }) {
                    Row {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "reset extension connection",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "重置扩展连接")
                    }
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    text = "已安装的扩展",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.fontSize.title
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "检测到 ${pluginList.size} 个已安装的扩展，其中 ${pluginList.count { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }} 个扩展运行正常。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(
                items = pluginList,
                key = { it.packageName }) {
                ExtensionCard(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    appModel = it,
                    onClick = {
                        it.launchIntent?.let {
                            onEvent(ActivityEvent.LaunchIntent(it))
                        }
                    },
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    text = "建议",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.fontSize.title
                )
            }
            item {
                Row(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Spacer(modifier = Modifier.width(2.dp))
                    FaIcon(faIcon = FaIcons.GooglePlay, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    HyperlinkText(
                        fullText = "前往 Google Play 获取更多由官方维护的扩展。",
                        hyperLinks = mapOf("Google Play" to "https://play.google.com/"),
                        linkTextColor = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = MaterialTheme.colorScheme.onSurface

                    )
                }
            }
            item {
                Row(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Spacer(modifier = Modifier.width(2.dp))
                    FaIcon(faIcon = FaIcons.Github, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    HyperlinkText(
                        fullText = "前往 Github 搜索由社区贡献的扩展。",
                        hyperLinks = mapOf("Github" to "https://github.com/"),
                        linkTextColor = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    text = "注意事项",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.fontSize.title
                )
            }
            item {
                Row(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = "security",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "扩展面向整个开源社区开放，Parabox 不对扩展的来源，真实性等做检验，扩展的预期行为无法受到保证。为了您的数据安全，请不要安装及启用未知来源的扩展。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                Row(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = "memory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "扩展须保持后台运行以正常工作。若扩展因资源回收而被停止，您可能需要手动重启扩展。以前台服务运行，启用后台锁，关闭电池优化等措施可帮助扩展后台留存。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionCard(
    modifier: Modifier = Modifier,
    appModel: AppModel,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                3.dp
            )
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = appModel.icon,
                contentDescription = "icon",
                modifier = Modifier
                    .size(48.dp)
                    .clip(
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appModel.name,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = context.getString(
                        R.string.extension_info,
                        appModel.version,
                        appModel.author
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            when (appModel.runningStatus) {
                AppModel.RUNNING_STATUS_DISABLED -> Icon(
                    imageVector = Icons.Outlined.HighlightOff,
                    contentDescription = "disabled",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppModel.RUNNING_STATUS_ERROR -> Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "error",
                    tint = MaterialTheme.colorScheme.error
                )

                AppModel.RUNNING_STATUS_RUNNING -> Icon(
                    imageVector = Icons.Outlined.CheckCircleOutline,
                    contentDescription = "running",
                    tint = MaterialTheme.colorScheme.primary
                )

                AppModel.RUNNING_STATUS_CHECKING -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}