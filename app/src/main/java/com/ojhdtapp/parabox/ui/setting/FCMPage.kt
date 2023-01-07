package com.ojhdtapp.parabox.ui.setting

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.CloudPageDestination
import com.ojhdtapp.parabox.ui.destinations.ModePageDestination
import com.ojhdtapp.parabox.ui.util.*
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

    val workingMode by viewModel.workingModeFlow.collectAsState(initial = WorkingMode.NORMAL.ordinal)
    val enabled = viewModel.enableFCMStateFlow.collectAsState(initial = false)
    val token = viewModel.fcmTokenFlow.collectAsState(initial = "")
    val state = viewModel.fcmStateFlow.collectAsState()
    val customUrlEnabled = viewModel.enableFcmCustomUrlFlow.collectAsState(initial = false)
    val fcmUrl = viewModel.fcmUrlFlow.collectAsState(initial = "")
    val useHttps = viewModel.fcmHttpsFlow.collectAsState(initial = false)
//    val role = viewModel.fcmRoleFlow.collectAsState(initial = FcmConstants.Role.SENDER.ordinal)
    val targetTokens = viewModel.fcmTargetTokensFlow.collectAsState(initial = emptySet())
    val loopbackToken = viewModel.fcmLoopbackTokenFlow.collectAsState(initial = "")

    val cloudStorage =
        viewModel.fcmCloudStorageFlow.collectAsState(initial = FcmConstants.CloudStorage.NONE.ordinal)

    val tencentCOSSecretId = viewModel.tencentCOSSecretIdFlow.collectAsState(initial = "")
    val tencentCOSSecretKey = viewModel.tencentCOSSecretKeyFlow.collectAsState(initial = "")
    val tencentCOSBucket = viewModel.tencentCOSBucketFlow.collectAsState(initial = "")
    val tencentCOSRegion = viewModel.tencentCOSRegionFlow.collectAsState(initial = "")

    val qiniuKODOAccessKey = viewModel.qiniuKODOAccessKeyFlow.collectAsState(initial = "")
    val qiniuKODOSecretKey = viewModel.qiniuKODOSecretKeyFlow.collectAsState(initial = "")
    val qiniuKODOBucket = viewModel.qiniuKODOBucketFlow.collectAsState(initial = "")
    val qiniuKODODomain = viewModel.qiniuKODODomainFlow.collectAsState(initial = "")

    LaunchedEffect(key1 = Unit) {
        if (enabled.value)
            viewModel.checkFcmState()
    }

    var showRoleDescription by remember {
        mutableStateOf(false)
    }

    if (showRoleDescription) {
        AlertDialog(
            onDismissRequest = {
                showRoleDescription = false
            },
            title = {
                Text(text = "模式说明")
            },
            text = {
                Text(text = "作为转发端工作时，该功能将从已安装扩展获取消息，转发至目标接收设备。\n作为接收端工作时，该功能将待发送消息回送至目标转发端，由其处理后发送。\n默认以转发模式工作。如需切换至接收模式，请前往工作模式设置更改。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRoleDescription = false
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        mainNavController.navigate(ModePageDestination)
                    } else {
                        viewModel.setSelectedSetting(SettingPageState.MODE)
                    }
                }) {
                    Text(text = stringResource(id = R.string.redirect_to_setting))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRoleDescription = false
                }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
        )
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
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(R.string.fcm_server))
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
                        label = { Text(text = stringResource(R.string.fcm_server_host)) },
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
                        label = { Text(text = stringResource(R.string.fcm_server_port)) },
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
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { tempTokens = "" }) {
                    Text(text = stringResource(R.string.clear_input))
                }
            },
            title = {
                Text(text = stringResource(R.string.fcm_send_target_token))
            },
            text = {
                OutlinedTextField(
                    value = tempTokens, onValueChange = { tempTokens = it },
                    label = { Text(text = stringResource(R.string.fcm_token)) },
                    supportingText = { Text(text = stringResource(R.string.fcm_send_target_token_supporting_text)) },
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
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { tempToken = "" }) {
                    Text(text = stringResource(id = R.string.clear_input))
                }
            },
            title = {
                Text(text = stringResource(R.string.fcm_callback_target_token))
            },
            text = {
                OutlinedTextField(
                    value = tempToken, onValueChange = { tempToken = it },
                    label = { Text(text = stringResource(id = R.string.fcm_token)) },
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

    var showFcmObjectStorageDialog by remember {
        mutableStateOf(false)
    }
    var showTencentCosDialog by remember {
        mutableStateOf(false)
    }
    var showQiniuDialog by remember {
        mutableStateOf(false)
    }
    // FCM Object Storage Dialog
    if (showFcmObjectStorageDialog) {
        AlertDialog(
            onDismissRequest = { showFcmObjectStorageDialog = false },
            confirmButton = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Storage,
                    contentDescription = "select object storage"
                )
            },
            title = { Text(text = "对象存储服务") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            showTencentCosDialog = true
                        }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "腾讯云 COS",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            showQiniuDialog = true
                        }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "七牛云 KODO",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        onClick = {

                        }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Google Drive（实验性）",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        )
    }

    if (showTencentCosDialog) {
        var tempSecretId by remember {
            mutableStateOf(tencentCOSSecretId.value)
        }
        var tempSecretKey by remember {
            mutableStateOf(tencentCOSSecretKey.value)
        }
        var tempRegion by remember {
            mutableStateOf(tencentCOSRegion.value)
        }
        var tempBucket by remember {
            mutableStateOf(tencentCOSBucket.value)
        }
        AlertDialog(
            onDismissRequest = { showTencentCosDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTencentCosDialog = false
                    showFcmObjectStorageDialog = false
                    viewModel.setTencentCOSSecretId(tempSecretId)
                    viewModel.setTencentCOSSecretKey(tempSecretKey)
                    viewModel.setTencentCOSRegion(tempRegion)
                    viewModel.setTencentCOSBucket(tempBucket)
                    viewModel.setFCMCloudStorage(FcmConstants.CloudStorage.TENCENT_COS.ordinal)
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTencentCosDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = "腾讯云 COS")
            },
            text = {
                Column {
                    Text(text = "将在存储桶根目录下新建“ParaboxTemp”目录，存放待发送的媒体文件（图片，语音等）。文件传输完毕后不会自动删除，请自行按需清理。")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempSecretId,
                        onValueChange = { tempSecretId = it },
                        label = { Text(text = "SecretId") },
                        singleLine = true,
                        supportingText = { Text(text = "永久密钥 secretId") },
                    )
                    OutlinedTextField(
                        value = tempSecretKey,
                        onValueChange = { tempSecretKey = it },
                        label = { Text(text = "SecretKey") },
                        singleLine = true,
                        supportingText = { Text(text = "永久密钥 secretKey") },
                    )
                    OutlinedTextField(
                        value = tempRegion,
                        onValueChange = { tempRegion = it },
                        label = { Text(text = "Region") },
                        singleLine = true,
                        supportingText = { Text(text = "存储桶所在地域简称，如 ap-guangzhou") }
                    )
                    OutlinedTextField(
                        value = tempBucket,
                        onValueChange = { tempBucket = it },
                        label = { Text(text = "Bucket") },
                        singleLine = true,
                        supportingText = { Text(text = "存储桶名称，由 bucketname-appid 组成，如 examplebucket-1250000000") },
                    )
                }
            },
        )
    }

    if (showQiniuDialog) {
        var tempAccessKey by remember {
            mutableStateOf(qiniuKODOAccessKey.value)
        }
        var tempSecretKey by remember {
            mutableStateOf(qiniuKODOSecretKey.value)
        }
        var tempBucket by remember {
            mutableStateOf(qiniuKODOBucket.value)
        }
        var tempDomain by remember {
            mutableStateOf(qiniuKODODomain.value)
        }
        AlertDialog(
            onDismissRequest = { showQiniuDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showQiniuDialog = false
                    showFcmObjectStorageDialog = false
                    viewModel.setQiniuKODOAccessKey(tempAccessKey)
                    viewModel.setQiniuKODOSecretKey(tempSecretKey)
                    viewModel.setQiniuKODOBucket(tempBucket)
                    viewModel.setQiniuKODODomain(tempDomain)
                    viewModel.setFCMCloudStorage(FcmConstants.CloudStorage.QINIU_KODO.ordinal)
                }) {
                    Text(text = stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQiniuDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = "七牛云 KODO")
            },
            text = {
                Column {
                    Text(text = "将在存储桶根目录下新建“ParaboxTemp”目录，存放待发送的媒体文件（图片，语音等）。文件传输完毕后不会自动删除，请自行按需清理。")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempAccessKey,
                        onValueChange = { tempAccessKey = it },
                        label = { Text(text = "AccessKey") },
                        singleLine = true,
                        supportingText = { Text(text = "永久密钥 accessKey") },
                    )
                    OutlinedTextField(
                        value = tempSecretKey,
                        onValueChange = { tempSecretKey = it },
                        label = { Text(text = "SecretKey") },
                        singleLine = true,
                        supportingText = { Text(text = "永久密钥 secretKey") },
                    )
                    OutlinedTextField(
                        value = tempBucket,
                        onValueChange = { tempBucket = it },
                        label = { Text(text = "Bucket") },
                        singleLine = true,
                        supportingText = { Text(text = "空间名称") },
                    )
                    OutlinedTextField(
                        value = tempDomain,
                        onValueChange = { tempDomain = it },
                        label = { Text(text = "Domain") },
                        singleLine = true,
                        supportingText = { Text(text = "域名，不含http/https") },
                    )
                }
            },
        )
    }


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
                title = { Text(stringResource(R.string.fcm)) },
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
                    title = stringResource(R.string.enable_fcm),
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
                        text = stringResource(R.string.fcm_info),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.fcm_status))
            }
            item {
                NormalPreference(
                    title = "运行模式",
                    subtitle = if (workingMode == WorkingMode.NORMAL.ordinal)
                        stringResource(id = R.string.fcm_role_sender) else
                        stringResource(id = R.string.fcm_role_receiver),
                ) {
                    showRoleDescription = true
                }
            }
            item {
                NormalPreference(
                    modifier = Modifier.animateContentSize(),
                    title = stringResource(id = R.string.fcm_token),
                    subtitle = token.value.ifBlank { stringResource(R.string.fcm_token_unavailable) },
                ) {
                    if (token.value.isNotBlank()) {
                        clipboardManager.setText(AnnotatedString(token.value))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.save_to_clipboard))
                        }
                    }
                }
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.fcm_server_status),
                    subtitle = when (state.value) {
                        is FcmConstants.Status.Success -> stringResource(
                            R.string.fcm_server_status_connected,
                            (state.value as FcmConstants.Status.Success).version
                        )
                        is FcmConstants.Status.Loading -> stringResource(R.string.fcm_server_status_connecting)
                        is FcmConstants.Status.Failure -> stringResource(R.string.fcm_server_status_failed)
                    },
                ) {
                    viewModel.checkFcmState()
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.fcm_connection_settings))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.fcm_custom_host),
                    checked = customUrlEnabled.value,
                    onCheckedChange = {
                        viewModel.setEnableFcmCustomUrl(it)
                    },
                )
            }
            item {
                AnimatedVisibility(
                    visible = customUrlEnabled.value,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    NormalPreference(
                        title = stringResource(R.string.fcm_server),
                        subtitle = fcmUrl.value.ifBlank { stringResource(R.string.not_set) },
                    ) {
                        showEditUrlDialog = true
                    }
                }
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.fcm_enable_https),
                    checked = useHttps.value,
                    onCheckedChange = viewModel::setFCMHttps,
                    enabled = false
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
                            title = stringResource(R.string.fcm_pem),
                            subtitle = stringResource(R.string.not_set),
                        ) {}
                        NormalPreference(
                            title = stringResource(R.string.fcm_cert),
                            subtitle = stringResource(R.string.not_set),
                        ) {}
                    }
                }
            }
//            item {
//                SimpleMenuPreference(
//                    title = stringResource(R.string.fcm_role),
//                    optionsMap = mapOf(
//                        FcmConstants.Role.SENDER.ordinal to stringResource(R.string.fcm_role_sender),
//                        FcmConstants.Role.RECEIVER.ordinal to stringResource(R.string.fcm_role_receiver)
//                    ),
//                    selectedKey = role.value,
//                    onSelect = {
//                        coroutineScope.launch {
//                            snackbarHostState.showSnackbar(
//                                message = context.getString(R.string.restart_app_to_active),
//                                actionLabel = context.getString(R.string.restart_app_now),
//                                withDismissAction = true
//                            ).also {
//                                if (it == SnackbarResult.ActionPerformed) {
//                                    onEvent(ActivityEvent.RestartApp)
//                                }
//                            }
//                        }
//                        viewModel.setFCMRole(it)
//                    },
//                    enabled = enabled.value
//                )
//            }
            item {
                Crossfade(targetState = workingMode) {
                    when (it) {
                        WorkingMode.NORMAL.ordinal, WorkingMode.FCM.ordinal -> {
                            NormalPreference(
                                title = stringResource(R.string.fcm_send_target_token),
                                subtitle = when {
                                    targetTokens.value.isEmpty() -> stringResource(R.string.not_set)
                                    else -> stringResource(
                                        R.string.fcm_send_target_token_set,
                                        targetTokens.value.size
                                    )
                                },
                            ) {
                                showEditTokensDialog = true
                            }
                        }

                        WorkingMode.RECEIVER.ordinal -> {
                            NormalPreference(
                                title = stringResource(id = R.string.fcm_callback_target_token),
                                subtitle = loopbackToken.value.ifBlank { stringResource(R.string.not_set) },
                            ) {
                                showEditLoopbackTokenDialog = true
                            }
                        }
                    }
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.fcm_feat_settings))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.object_storage),
                    subtitle = when (cloudStorage.value) {
                        FcmConstants.CloudStorage.NONE.ordinal -> stringResource(R.string.not_set)
                        FcmConstants.CloudStorage.TENCENT_COS.ordinal -> "腾讯云 COS"
                        FcmConstants.CloudStorage.QINIU_KODO.ordinal -> "七牛云 KODO"
                        FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> "Google Drive（实验性）"
                        else -> stringResource(R.string.not_set)
                    },
                ) {
                    showFcmObjectStorageDialog = true
                }
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.fcm_limited_contact),
                    subtitle = stringResource(id = R.string.fcm_limited_contact_subtitle),
                ) {
                    showContactDialog = true
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.fcm_other_settings))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.fcm_support_title),
                    subtitle = stringResource(R.string.fcm_support_subtitle),
                ) {}
            }
        }
    }
}