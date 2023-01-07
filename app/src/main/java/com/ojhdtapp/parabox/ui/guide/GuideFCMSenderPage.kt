package com.ojhdtapp.parabox.ui.guide

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.HyperlinkText
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideCloudPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuideExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuidePersonalisePageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideFCMSenderPage(
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
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var currentStep by remember {
        mutableStateOf(1)
    }

    val fcmEnabled by viewModel.enableFCMStateFlow.collectAsState(initial = false)
    val token by viewModel.fcmTokenFlow.collectAsState(initial = "")
    val targetTokens = viewModel.fcmTargetTokensFlow.collectAsState(initial = emptySet())

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
                Button(
                    onClick = {
                        mainNavController.navigate(GuideCloudPageDestination)
                    },
                    enabled = !fcmEnabled || targetTokens.value.isNotEmpty()
                ) {
                    if (!fcmEnabled) {
                        Text(text = "稍后再说")
                    } else if (targetTokens.value.isNotEmpty()) {
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
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "fcm sender",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    text = "配置 FCM 转发",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "Firebase云消息传递（FCM）是谷歌推出的系统级消息推送服务。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "该功能允许您将本机作为中继，将接收到的消息通过 FCM 转发至目标设备，并以同一方式接收来自目标设备的消息发送请求。这将大幅减少目标设备的性能开销。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = "通常需要您拥有两台及以上运行 Parabox 的设备，且具有稳定的 Google 服务连接。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                SwitchPreference(
                    title = "启用",
                    checked = fcmEnabled,
                    onCheckedChange = viewModel::setEnableFCM,
                    enabled = token.isNotBlank(),
                    horizontalPadding = 32.dp
                )
            }
            if (token.isBlank()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.padding(start = 32.dp),
                            text = "无法连接至 FCM 网络。",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { onEvent(ActivityEvent.QueryFCMToken) }) {
                            Text(text = "刷新")
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = fcmEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1
                        Row() {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "1", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "检查连接状态",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 1) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "在您的其他设备上运行“配置 FCM 回送”引导，并检查 FCM 网络连接状态。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row() {
                                            Button(onClick = { currentStep = 2 }) {
                                                Text(text = "下一步")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2
                        Row() {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "2", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "填入目标 Token",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 2) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        var tempTokens by remember {
                                            mutableStateOf(buildString {
                                                targetTokens.value.forEachIndexed { index, s ->
                                                    append(s)
                                                    if (index != targetTokens.value.size - 1)
                                                        append(",\n")
                                                }
                                            })
                                        }
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = "从您的其他设备复制 Token，粘贴到下面的输入框中。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        OutlinedTextField(
                                            value = tempTokens, onValueChange = { tempTokens = it },
                                            label = { Text(text = stringResource(R.string.fcm_token)) },
                                            supportingText = { Text(text = stringResource(R.string.fcm_send_target_token_supporting_text)) },
                                        )
                                        Row() {
                                            OutlinedButton(onClick = { currentStep = 1 }) {
                                                Text(text = "上一步")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                viewModel.setFcmTargetTokens(
                                                    if (tempTokens.isBlank()) emptySet<String>()
                                                    else tempTokens.split(",").map { it.trim() }
                                                        .toSet()
                                                )
                                                currentStep = 3
                                            }) {
                                                Text(text = "下一步")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 3
                        Row() {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "3", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "复制回送 Token",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 3) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = "复制本设备 Token，并依次输入到其他设备对应输入框中。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                if (token.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(token))
                                                    coroutineScope.launch {
                                                        snackBarHostState.showSnackbar(
                                                            context.getString(
                                                                R.string.save_to_clipboard
                                                            )
                                                        )
                                                    }
                                                }
                                            }) {
                                            Icon(
                                                modifier = Modifier.size(18.dp),
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "copy to clipboard"
                                            )
                                            Text(
                                                modifier = Modifier.padding(start = 8.dp),
                                                text = "复制到剪贴板"
                                            )
                                        }
                                        Row() {
                                            OutlinedButton(onClick = { currentStep = 2 }) {
                                                Text(text = "上一步")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                currentStep = 4
                                            }) {
                                                Text(text = "下一步")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 4
                        Row() {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "4", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "配置媒体文件传输方式",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 4) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = "由于 FCM 不允许传输媒体文件，您需要为此类文件另外配置所有设备皆可用的传输方式。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // Tab
                                        Row() {
                                            OutlinedButton(onClick = { currentStep = 3 }) {
                                                Text(text = "上一步")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                currentStep = 5
                                            }) {
                                                Text(text = "下一步")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 5
                        Row() {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "5", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "大功告成！",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 5) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = "您已完成 FCM 转发端的所有配置，请继续完成其他引导。",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        HyperlinkText(
                                            fullText = "要获取有关 FCM 使用的其他提示，请浏览用户文档。",
                                            hyperLinks = mapOf("用户文档" to "https://docs.parabox.ojhdt.dev/"),
                                            linkTextColor = MaterialTheme.colorScheme.primary,
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            textColor = MaterialTheme.colorScheme.onSurface

                                        )
                                        Row() {
                                            OutlinedButton(onClick = { currentStep = 4 }) {
                                                Text(text = "上一步")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}