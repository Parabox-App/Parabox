package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.flowlayout.FlowRow
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.FCMPageDestination
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
fun ExtensionPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var expandedExtension by remember {
        mutableStateOf<String?>(null)
    }
    val workingMode = viewModel.workingModeFlow.collectAsState(initial = WorkingMode.NORMAL.ordinal)
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
                title = { Text(stringResource(R.string.extension)) },
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
        // Plugin List State
        val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
        LazyColumn(
            contentPadding = it,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (workingMode.value != WorkingMode.NORMAL.ordinal) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(vertical = 16.dp),
                            text = stringResource(R.string.extension_banned),
                            style = MaterialTheme.typography.labelLarge
                        )
                        FilledTonalButton(
                            onClick = {
                                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                    mainNavController.navigate(ModePageDestination)
                                } else {
                                    viewModel.setSelectedSetting(SettingPageState.MODE)
                                }
                            }) {
                            Text(text = stringResource(R.string.redirect_to_setting))
                        }
                    }
                }
            } else {
                item {
                    Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.extension_msg),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item() {
                    if (pluginList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier.padding(vertical = 16.dp),
                                text = stringResource(R.string.no_available_extension),
                                style = MaterialTheme.typography.labelLarge
                            )
                            FilledTonalButton(
                                onClick = { }) {
                                FaIcon(
                                    modifier = Modifier.padding(end = 8.dp),
                                    faIcon = FaIcons.GooglePlay,
                                    size = ButtonDefaults.IconSize,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(text = stringResource(R.string.get_from_appstore))
                            }
                        }
                    }
                }
                items(
                    items = pluginList,
                    key = { it.packageName }) {
                    ExtensionCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        appModel = it,
                        expanded = expandedExtension == it.packageName,
                        onClick = {
                            if (expandedExtension == it.packageName) {
                                expandedExtension = null
                            } else {
                                expandedExtension = it.packageName
                            }
                        },
                        onLaunch = {
                            it.launchIntent?.let {
                                onEvent(ActivityEvent.LaunchIntent(it))
                            }
                        })
                }
                item {
                        NormalPreference(
                            title = stringResource(R.string.reset_extension_connection_title),
                            subtitle = stringResource(R.string.reset_extension_connection_subtitle),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.LinkOff,
                                    contentDescription = "reset link"
                                )
                            },
                            enabled = true
                        ) {
                            onEvent(ActivityEvent.ResetExtension)
                        }
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
    expanded: Boolean,
    onClick: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
//                3.dp
//            )
//        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
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
                        tint = MaterialTheme.colorScheme.primary
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
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = appModel.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(
                        text = stringResource(R.string.extension_support_msg_type),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(mainAxisSpacing = 4.dp) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_plain_text)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.plainTextSupport)
                            },
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_image)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.imageSupport)
                            },
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_audio)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.audioSupport)
                            },
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_file)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.fileSupport)
                            },
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_at)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.atSupport)
                            },
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(text = stringResource(R.string.msg_type_quote_reply)) },
                            leadingIcon = {
                                SupportLeadingIcon(supportType = appModel.quoteReplySupport)
                            },
                        )
                    }
                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = {
                            onLaunch()
                        }) {
                        Text(text = stringResource(R.string.settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportLeadingIcon(supportType: Int) {
    when (supportType) {
        AppModel.SUPPORT_NULL -> Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = "null",
            tint = MaterialTheme.colorScheme.error
        )
        AppModel.SUPPORT_RECEIVE -> Icon(
            imageVector = Icons.Outlined.RemoveCircleOutline,
            contentDescription = "receive only",
            tint = MaterialTheme.colorScheme.primary
        )
        AppModel.SUPPORT_ALL -> Icon(
            imageVector = Icons.Outlined.CheckCircleOutline,
            contentDescription = "all",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}