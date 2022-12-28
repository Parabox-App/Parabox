package com.ojhdtapp.parabox.ui.setting

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun ModePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackBarHostState = remember { SnackbarHostState() }
    val selectedWorkingMode by viewModel.workingModeFlow.collectAsState(initial = WorkingMode.NORMAL.ordinal)
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
                title = { Text("工作模式") },
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
    ) {
        LazyColumn(
            contentPadding = it
        ) {
            item {
                NormalPreference(
                    title = "扩展模式",
                    subtitle = "默认使用的模式。将尝试常驻一个扩展连接服务，与本机已安装的扩展建立通信。",
                    leadingIcon = {
                        RadioButton(
                            selected = selectedWorkingMode == WorkingMode.NORMAL.ordinal,
                            onClick = {
                                if (selectedWorkingMode != WorkingMode.NORMAL.ordinal) {
                                    val tempSelection = selectedWorkingMode
                                    snackBarHostState.currentSnackbarData?.dismiss()
                                    coroutineScope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = context.getString(R.string.start_extension_connection_success),
                                            actionLabel = context.getString(R.string.cancel),
                                            duration = SnackbarDuration.Short
                                        )
                                            .also { result ->
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        viewModel.setWorkingMode(tempSelection)
                                                    }
                                                    SnackbarResult.Dismissed -> {
                                                        onEvent(ActivityEvent.StartExtension)
                                                    }
                                                    else -> {}
                                                }
                                            }
                                    }
                                    viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                }
                            })
                    }
                ) {
                    if (selectedWorkingMode != WorkingMode.NORMAL.ordinal) {
                        val tempSelection = selectedWorkingMode
                        snackBarHostState.currentSnackbarData?.dismiss()
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.start_extension_connection_success),
                                actionLabel = context.getString(R.string.cancel),
                                duration = SnackbarDuration.Short
                            )
                                .also { result ->
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.setWorkingMode(tempSelection)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            onEvent(ActivityEvent.StartExtension)
                                        }
                                        else -> {}
                                    }
                                }
                        }
                        viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                    }
                }
            }
            item {
                NormalPreference(
                    title = "接收者模式",
                    subtitle = "将尝试组建多设备间 FCM 网络来通信。此模式下扩展功能将禁用。\n这通常需要拥有两台以上运行 Parabox 的设备，且其中一台已以扩展模式运行，并开启 FCM 转发者模式。",
                    leadingIcon = {
                        RadioButton(
                            selected = selectedWorkingMode == WorkingMode.RECEIVER.ordinal,
                            onClick = {
                                if (selectedWorkingMode == WorkingMode.NORMAL.ordinal) {
                                    snackBarHostState.currentSnackbarData?.dismiss()
                                    coroutineScope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = context.getString(R.string.stop_extension_connection_success),
                                            actionLabel = context.getString(R.string.cancel),
                                            duration = SnackbarDuration.Short
                                        )
                                            .also { result ->
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                                    }
                                                    SnackbarResult.Dismissed -> {
                                                        onEvent(ActivityEvent.StopExtension)
                                                    }
                                                    else -> {}
                                                }
                                            }
                                    }
                                }
                                viewModel.setWorkingMode(WorkingMode.RECEIVER.ordinal)
                            })
                    }
                ) {
                    if (selectedWorkingMode == WorkingMode.NORMAL.ordinal) {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.stop_extension_connection_success),
                                actionLabel = context.getString(R.string.cancel),
                                duration = SnackbarDuration.Short
                            )
                                .also { result ->
                                    Log.d("parabox", "snackbar: $result")
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            onEvent(ActivityEvent.StopExtension)
                                        }
                                        else -> {}
                                    }
                                }
                        }
                    }
                    viewModel.setWorkingMode(WorkingMode.RECEIVER.ordinal)
                }
            }
            item {
                NormalPreference(
                    title = "FCM 模式",
                    subtitle = "此模式下消息收发将由 FCM 完全接管，并交由私有服务器进行处理。此模式下扩展功能将禁用。\n这通常需要私有服务器，并遵照教程指引进行相关配置。",
                    leadingIcon = {
                        RadioButton(
                            selected = selectedWorkingMode == WorkingMode.FCM.ordinal,
                            onClick = {
                                if (selectedWorkingMode == WorkingMode.NORMAL.ordinal) {
                                    snackBarHostState.currentSnackbarData?.dismiss()
                                    coroutineScope.launch {
                                        snackBarHostState.showSnackbar(
                                            message = context.getString(R.string.stop_extension_connection_success),
                                            actionLabel = context.getString(R.string.cancel),
                                            duration = SnackbarDuration.Short
                                        )
                                            .also { result ->
                                                when (result) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                                    }
                                                    SnackbarResult.Dismissed -> {
                                                        onEvent(ActivityEvent.StopExtension)
                                                    }
                                                    else -> {}
                                                }
                                            }
                                    }
                                }
                                viewModel.setWorkingMode(WorkingMode.FCM.ordinal)
                            })
                    }
                ) {
                    if (selectedWorkingMode == WorkingMode.NORMAL.ordinal) {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.stop_extension_connection_success),
                                actionLabel = context.getString(R.string.cancel),
                                duration = SnackbarDuration.Short
                            )
                                .also { result ->
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            onEvent(ActivityEvent.StopExtension)
                                        }
                                        else -> {}
                                    }
                                }
                        }
                    }
                    viewModel.setWorkingMode(WorkingMode.FCM.ordinal)
                }
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
                        text = "请结合具体使用场景选择恰当的工作模式。",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ClickableText(text = AnnotatedString("详细了解工作模式"),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        onClick = {})
                }
            }
        }
    }
}