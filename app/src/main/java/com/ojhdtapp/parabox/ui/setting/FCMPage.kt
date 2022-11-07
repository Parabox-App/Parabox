package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.CloudPageDestination
import com.ojhdtapp.parabox.ui.destinations.FCMPageDestination
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.MainSwitch
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SimpleMenuPreference
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun FCMPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val enabled = viewModel.enableFCMStateFlow.collectAsState(initial = false)
    val token = viewModel.fcmTokenFlow.collectAsState(initial = "")
    val state = viewModel.fcmStateFlow.collectAsState()
    val customUrlEnabled = viewModel.enableFcmCustomUrlFlow.collectAsState(initial = false)
    val fcmUrl = viewModel.fcmUrlFlow.collectAsState(initial = "")
    val useHttps = viewModel.fcmHttpsFlow.collectAsState(initial = false)
    val role = viewModel.fcmRoleFlow.collectAsState(initial = FcmConstants.Role.SENDER.ordinal)
    val targetTokens = viewModel.fcmTargetTokensFlow.collectAsState(initial = emptySet())
    val loopbackToken = viewModel.fcmLoopbackTokenFlow.collectAsState(initial = "")

    val gDriveLogin by viewModel.googleLoginFlow.collectAsState(initial = false)

    val selectableService by remember {
        derivedStateOf {
            buildMap<Int, String> {
                put(FcmConstants.CloudStorage.NONE.ordinal, "无")
                if (gDriveLogin) put(FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal, "Google Drive")
            }
        }
    }
    val cloudStorage = viewModel.fcmCloudStorageFlow.collectAsState(initial = FcmConstants.CloudStorage.NONE.ordinal)

    LaunchedEffect(key1 = Unit) {
        if (enabled.value)
            viewModel.checkFcmState()
    }

    var showEditUrlDialog by remember {
        mutableStateOf(false)
    }

    if (showEditUrlDialog) {
        var tempUrl by remember {
            mutableStateOf(fcmUrl.value.split(":").getOrNull(0) ?: "")
        }
        var tempPort by remember {
            mutableStateOf(fcmUrl.value.split(":").getOrNull(1) ?: "")
        }
        var editUrlError by remember {
            mutableStateOf(false)
        }
        var editPortError by remember {
            mutableStateOf(false)
        }
        AlertDialog(onDismissRequest = { showEditUrlDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (tempUrl.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex())
                        && tempPort.matches("\\d{1,5}".toRegex())
                    ) {
                        viewModel.setFCMUrl(buildString {
                            append(tempUrl)
                            append(":")
                            append(tempPort)
                        })
                        showEditUrlDialog = false
                    } else {
                        if (!tempUrl.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}\$".toRegex()))
                            editUrlError = true
                        if (!tempPort.matches("\\d{1,5}".toRegex()))
                            editPortError = true
                    }
                }) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text(text = "取消")
                }
            },
            title = {
                Text(text = "服务器地址")
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = tempUrl,
                        onValueChange = {
                            editUrlError = false
                            tempUrl = it
                        },
                        isError = editUrlError,
                        label = { Text(text = "地址") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = null
                        ),
                        singleLine = true,
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = ":",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        modifier = Modifier.width(80.dp),
                        value = tempPort,
                        onValueChange = {
                            editUrlError = false
                            tempPort = it
                        },
                        isError = editPortError,
                        label = { Text(text = "端口") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = null
                        ),
                        singleLine = true,
                    )
                }
            }
        )
    }

    var showEditTokensDialog by remember {
        mutableStateOf(false)
    }
    if (showEditTokensDialog) {
        var tempTokens by remember {
            mutableStateOf(buildString {
                targetTokens.value.forEachIndexed { index, s ->
                    append(s)
                    if (index != targetTokens.value.size - 1)
                        append(",\n")
                }
            })
        }
        AlertDialog(
            onDismissRequest = { showEditTokensDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showEditTokensDialog = false
                    viewModel.setFcmTargetTokens(
                        if (tempTokens.isBlank()) emptySet<String>()
                        else tempTokens.split(",").map { it.trim() }.toSet()
                    )
                }) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { tempTokens = "" }) {
                    Text(text = "清空输入")
                }
            },
            title = {
                Text(text = "转发目标设备 Token")
            },
            text = {
                OutlinedTextField(
                    value = tempTokens, onValueChange = { tempTokens = it },
                    label = { Text(text = "Token") },
                    supportingText = { Text(text = "多个 Token 请用英式逗号分隔") },
                )
            },
        )
    }

    var showEditLoopbackTokenDialog by remember {
        mutableStateOf(false)
    }
    if (showEditLoopbackTokenDialog) {
        var tempToken by remember {
            mutableStateOf(loopbackToken.value)
        }
        AlertDialog(
            onDismissRequest = { showEditLoopbackTokenDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showEditLoopbackTokenDialog = false
                    viewModel.setFcmLoopbackToken(tempToken)
                }) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { tempToken = "" }) {
                    Text(text = "清空输入")
                }
            },
            title = {
                Text(text = "回送目标设备 Token")
            },
            text = {
                OutlinedTextField(
                    value = tempToken, onValueChange = { tempToken = it },
                    label = { Text(text = "Token") },
                )
            },
        )
    }

    var showContactDialog by remember {
        mutableStateOf(false)
    }
    ContactListDialog(
        modifier = Modifier,
        showDialog = showContactDialog,
        contactList = viewModel.contactStateFlow.collectAsState().value.filter { it.contactId == it.senderId },
        contactCheck = { it.disableFCM },
        onValueChange = { target, value ->
            viewModel.onContactDisableFCMChange(target, value)
        },
        loading = viewModel.contactLoadingState.value,
        sizeClass = sizeClass,
        onDismiss = {
            showContactDialog = false
        }
    )


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
                title = { Text("Firebase 云消息传递") },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        LazyColumn(
            contentPadding = it
        ) {
            item {
                MainSwitch(
                    modifier = Modifier.padding(vertical = 24.dp),
                    title = "启用 FCM",
                    checked = enabled.value,
                    onCheckedChange = viewModel::setEnableFCM,
                    enabled = true
                )
            }
            item {
                Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "使用该功能前，请确保设备具有稳定的 Google 服务连接。",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                PreferencesCategory(text = "连接状态")
            }
            item {
                NormalPreference(
                    modifier = Modifier.animateContentSize(),
                    title = "Token",
                    subtitle = token.value.ifBlank { "未获取" },
                    enabled = enabled.value,
                ) {
                    if (token.value.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(token.value))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已复制到剪贴板")
                        }
                    }
                }
            }
            item {
                NormalPreference(
                    title = "服务器状态",
                    subtitle = when (state.value) {
                        is FcmConstants.Status.Success -> "已连接（${(state.value as FcmConstants.Status.Success).version}）"
                        is FcmConstants.Status.Loading -> "正在连接"
                        is FcmConstants.Status.Failure -> "连接失败"
                    },
                    enabled = enabled.value,
                ) {
                    viewModel.checkFcmState()
                }
            }
            item {
                PreferencesCategory(text = "连接配置")
            }
            item {
                SwitchPreference(title = "使用自定义服务器", checked = customUrlEnabled.value, onCheckedChange = {
                    viewModel.setEnableFcmCustomUrl(it)
                })
            }
            item {
                AnimatedVisibility(visible = customUrlEnabled.value, enter = expandVertically(), exit = shrinkVertically()) {
                    NormalPreference(
                        title = "服务器地址",
                        subtitle = fcmUrl.value.ifBlank { "未设置" },
                        enabled = enabled.value,
                    ) {
                        showEditUrlDialog = true
                    }
                }
            }
            item {
                SwitchPreference(
                    title = "使用 HTTPS",
                    checked = useHttps.value,
                    onCheckedChange = viewModel::setFCMHttps,
                    enabled = false && enabled.value
                )
            }
            item {
                AnimatedVisibility(
                    visible = useHttps.value,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        NormalPreference(
                            title = "证书 PEM 文件",
                            subtitle = "未选择",
                        ) {}
                        NormalPreference(
                            title = "证书 CERT 文件",
                            subtitle = "未选择",
                        ) {}
                    }
                }
            }
            item {
                SimpleMenuPreference(
                    title = "角色",
                    optionsMap = mapOf(
                        FcmConstants.Role.SENDER.ordinal to "转发端",
                        FcmConstants.Role.RECEIVER.ordinal to "接收端"
                    ),
                    selectedKey = role.value,
                    onSelect = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "重启应用后生效",
                                actionLabel = "立即重启",
                                withDismissAction = true
                            ).also {
                                if (it == SnackbarResult.ActionPerformed) {
                                    onEvent(ActivityEvent.RestartApp)
                                }
                            }
                        }
                        viewModel.setFCMRole(it)
                    },
                    enabled = enabled.value
                )
            }
            item {
                Crossfade(targetState = role.value) {
                    when (it) {
                        FcmConstants.Role.SENDER.ordinal -> {
                            NormalPreference(
                                title = "转发目标设备 Token",
                                subtitle = when {
                                    targetTokens.value.isEmpty() -> "未设置"
                                    else -> "已设置 ${targetTokens.value.size} 个 token"
                                },
                                enabled = enabled.value,
                            ) {
                                showEditTokensDialog = true
                            }
                        }

                        FcmConstants.Role.RECEIVER.ordinal -> {
                            NormalPreference(
                                title = "回送目标设备 Token",
                                subtitle = loopbackToken.value.ifBlank { "未设置" },
                                enabled = enabled.value,
                            ) {
                                showEditLoopbackTokenDialog = true
                            }
                        }
                    }
                }
            }
            item {
                PreferencesCategory(text = "功能配置")
            }
            item {
                Crossfade(targetState = selectableService.size <= 1) {
                    if(it){
                        NormalPreference(title = "对象存储服务", subtitle = "暂无可用，请新增云端服务") {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(CloudPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.CLOUD)
                            }
                        }
                    }else{
                        SimpleMenuPreference(
                            title = "对象存储服务",
                            enabled = enabled.value,
                            optionsMap = selectableService,
                            selectedKey = cloudStorage.value,
                            onSelect = viewModel::setFCMCloudStorage)
                    }
                }
            }
            item {
                NormalPreference(
                    title = "受限会话",
                    subtitle = "对选定会话限制推送\n" +
                            "因 FCM 存在消息限额（4000条/小时/设备），可手动对消息量大而重要性低的会话应用推送限制。",
                    enabled = enabled.value,
                ) {
                    showContactDialog = true
                }
            }
            item {
                PreferencesCategory(text = "服务器操作")
            }
            item {
                NormalPreference(
                    title = "强制执行未完成的发送",
                    subtitle = "未选择",
                    enabled = enabled.value
                ) {}
            }
            item {
                PreferencesCategory(text = "其他")
            }
            item {
                NormalPreference(
                    title = "寻求帮助",
                    subtitle = "查看文档，获取使用指引",
                ) {}
            }
        }
    }
}