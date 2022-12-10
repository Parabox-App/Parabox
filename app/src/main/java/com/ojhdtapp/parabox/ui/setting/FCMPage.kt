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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.CloudPageDestination
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
                    modifier = Modifier.animateContentSize(),
                    title = stringResource(id = R.string.fcm_token),
                    subtitle = token.value.ifBlank { stringResource(R.string.fcm_token_unavailable) },
                    enabled = enabled.value,
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
                        is FcmConstants.Status.Success -> stringResource(R.string.fcm_server_status_connected , (state.value as FcmConstants.Status.Success).version)
                        is FcmConstants.Status.Loading -> stringResource(R.string.fcm_server_status_connecting)
                        is FcmConstants.Status.Failure -> stringResource(R.string.fcm_server_status_failed)
                    },
                    enabled = enabled.value,
                ) {
                    viewModel.checkFcmState()
                }
            }
            item {
                PreferencesCategory(text = stringResource(R.string.fcm_connection_settings))
            }
            item {
                SwitchPreference(title = stringResource(R.string.fcm_custom_host), checked = customUrlEnabled.value, onCheckedChange = {
                    viewModel.setEnableFcmCustomUrl(it)
                },
                    enabled = enabled.value)
            }
            item {
                AnimatedVisibility(visible = customUrlEnabled.value, enter = expandVertically(), exit = shrinkVertically()) {
                    NormalPreference(
                        title = stringResource(R.string.fcm_server),
                        subtitle = fcmUrl.value.ifBlank { stringResource(R.string.not_set) },
                        enabled = enabled.value,
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
            item {
                SimpleMenuPreference(
                    title = stringResource(R.string.fcm_role),
                    optionsMap = mapOf(
                        FcmConstants.Role.SENDER.ordinal to stringResource(R.string.fcm_role_sender),
                        FcmConstants.Role.RECEIVER.ordinal to stringResource(R.string.fcm_role_receiver)
                    ),
                    selectedKey = role.value,
                    onSelect = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.restart_app_to_active),
                                actionLabel = context.getString(R.string.restart_app_now),
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
                                title = stringResource(R.string.fcm_send_target_token),
                                subtitle = when {
                                    targetTokens.value.isEmpty() -> stringResource(R.string.not_set)
                                    else -> stringResource(R.string.fcm_send_target_token_set, targetTokens.value.size)
                                },
                                enabled = enabled.value,
                            ) {
                                showEditTokensDialog = true
                            }
                        }

                        FcmConstants.Role.RECEIVER.ordinal -> {
                            NormalPreference(
                                title = stringResource(id = R.string.fcm_callback_target_token),
                                subtitle = loopbackToken.value.ifBlank { stringResource(R.string.not_set) },
                                enabled = enabled.value,
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
                Crossfade(targetState = selectableService.size <= 1) {
                    if(it){
                        NormalPreference(title = stringResource(R.string.object_storage), subtitle = stringResource(
                                                    R.string.object_storage_none),enabled = enabled.value) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(CloudPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.CLOUD)
                            }
                        }
                    }else{
                        SimpleMenuPreference(
                            title = stringResource(R.string.object_storage),
                            enabled = enabled.value,
                            optionsMap = selectableService,
                            selectedKey = cloudStorage.value,
                            onSelect = viewModel::setFCMCloudStorage)
                    }
                }
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.fcm_limited_contact),
                    subtitle = stringResource(id = R.string.fcm_limited_contact_subtitle),
                    enabled = enabled.value,
                ) {
                    showContactDialog = true
                }
            }
//            item {
//                PreferencesCategory(text = stringResource(R.string.fcm_server_settings))
//            }
//            item {
//                NormalPreference(
//                    title = "强制执行未完成的发送",
//                    subtitle = stringResource(R.string.not_set),
//                    enabled = enabled.value
//                ) {}
//            }
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