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
import kotlinx.coroutines.delay
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
                title = { Text(
                    text = stringResource(id = R.string.working_mode)) },
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
    }
}