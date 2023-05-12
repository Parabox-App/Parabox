package com.ojhdtapp.parabox.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuideFCMReceiverPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuideFCMServerPageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.common.GuideNavGraph
import com.ojhdtapp.parabox.ui.common.NormalPreference
import com.ramcosta.composedestinations.annotation.Destination
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
    Column(modifier = Modifier.systemBarsPadding()){
        LazyColumn(
            modifier = Modifier.weight(1f),
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
                        text = stringResource(R.string.working_mode),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.working_mode_normal_title),
                    subtitle = stringResource(R.string.working_mode_normal_text),
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
                    title = stringResource(R.string.working_mode_receiver_title),
                    subtitle = stringResource(R.string.working_mode_receiver_text),
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
                    title = stringResource(R.string.working_mode_fcm_title),
                    subtitle = stringResource(R.string.working_mode_fcm_text),
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
                        text = stringResource(R.string.working_mode_info_a),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ClickableText(text = AnnotatedString(stringResource(R.string.working_mode_info_b)),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        onClick = {})
                }
            }
        }
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
                Text(text = stringResource(R.string.back))
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
                    WorkingMode.FCM.ordinal -> {
                        mainNavController.navigate(GuideFCMServerPageDestination)
                    }
                }
            }) {
                Text(text = stringResource(id = R.string.cont))
            }
        }
    }
}