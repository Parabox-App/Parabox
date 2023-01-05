package com.ojhdtapp.parabox.ui.guide

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuideFCMReceiverPageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.GuideNavGraph
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.WorkingMode
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideModePage(
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
    val selectedWorkingMode by viewModel.workingModeFlow.collectAsState(initial = WorkingMode.NORMAL.ordinal)
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
                Button(onClick = {
                    when (selectedWorkingMode) {
                        WorkingMode.NORMAL.ordinal -> {
                            mainNavController.navigate(GuideExtensionPageDestination)
                        }
                        WorkingMode.RECEIVER.ordinal -> {
                            mainNavController.navigate(GuideFCMReceiverPageDestination)
                        }
                        WorkingMode.FCM.ordinal -> {}
                    }
                }) {
                    Text(text = "继续")
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(32.dp)) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = Icons.Outlined.BusinessCenter,
                        contentDescription = "mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "工作模式",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                NormalPreference(
                    title = "扩展模式",
                    subtitle = "默认使用的模式。将尝试常驻一个扩展连接服务，与本机已安装的扩展建立通信。",
                    leadingIcon = {
                        RadioButton(
                            selected = selectedWorkingMode == WorkingMode.NORMAL.ordinal,
                            onClick = {
                                if (selectedWorkingMode != WorkingMode.NORMAL.ordinal) {
                                    viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                                    coroutineScope.launch {
                                        delay(200)
                                        onEvent(ActivityEvent.StartExtension)
                                        snackBarHostState.currentSnackbarData?.dismiss()
                                        snackBarHostState.showSnackbar(
                                            message = context.getString(R.string.start_extension_connection_success),
                                            duration = SnackbarDuration.Short
                                        )

                                    }
                                }
                            })
                    }
                ) {
                    if (selectedWorkingMode != WorkingMode.NORMAL.ordinal) {
                        viewModel.setWorkingMode(WorkingMode.NORMAL.ordinal)
                        coroutineScope.launch {
                            delay(200)
                            onEvent(ActivityEvent.StartExtension)
                            snackBarHostState.currentSnackbarData?.dismiss()
                            snackBarHostState.showSnackbar(
                                message = context.getString(R.string.start_extension_connection_success),
                                duration = SnackbarDuration.Short
                            )

                        }
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
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    onEvent(ActivityEvent.StopExtension)
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
                                duration = SnackbarDuration.Short
                            )
                        }
                        onEvent(ActivityEvent.StopExtension)
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
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    onEvent(ActivityEvent.StopExtension)
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
                                duration = SnackbarDuration.Short
                            )
                        }
                        onEvent(ActivityEvent.StopExtension)
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