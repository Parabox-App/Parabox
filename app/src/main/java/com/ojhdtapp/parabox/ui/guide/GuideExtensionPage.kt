package com.ojhdtapp.parabox.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.ojhdtapp.parabox.ui.common.GuideNavGraph
import com.ramcosta.composedestinations.annotation.Destination

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
                Text(text = stringResource(R.string.skip_extension_check_title))
            },
            text = {
                Text(text = stringResource(R.string.skip_extension_check_text))
            }
        )
    }

    Column(modifier = Modifier.systemBarsPadding()){
        LazyColumn(
            modifier = Modifier.weight(1f),
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
                    text = stringResource(R.string.setup_extension_check_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_extension_check_text_a),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_extension_check_text_b),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                FilledTonalButton(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    onClick = {
                        onEvent(ActivityEvent.ResetExtension)
                    }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "reset extension connection",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.reset_extension_connection_title))
                    }
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    text = stringResource(R.string.extension_installled),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.fontSize.title
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.extension_installled_text, pluginList.size, pluginList.count { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }),
//                    text = "检测到 ${pluginList.size} 个已安装的扩展，其中 ${pluginList.count { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }} 个扩展运行正常。",
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
                    text = stringResource(R.string.suggestion),
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
                        fullText = stringResource(R.string.extension_suggestion_a),
                        hyperLinks = mapOf("Google Play" to "https://play.google.com/store/apps/developer?id=Ojhdt+Apps"),
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
                        fullText = stringResource(R.string.extension_suggestion_b),
                        hyperLinks = mapOf("Github" to "https://github.com/topics/parabox-extension"),
                        linkTextColor = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp),
                    text = stringResource(R.string.notice),
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
                        text = stringResource(R.string.setup_extension_check_text_c),
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
                        text = stringResource(R.string.setup_extension_check_text_d),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                Text(text = stringResource(id = R.string.back))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!pluginList.any { it.runningStatus == AppModel.RUNNING_STATUS_RUNNING }) {
                TextButton(onClick = {
                    showSkipGuideDialog = true
                }) {
                    Text(text = stringResource(R.string.continue_anyway))
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
                    Text(text = stringResource(id = R.string.cont))
                } else {
                    Text(text = stringResource(R.string.unfinished))
                }
            }
        }
    }

//    BottomSheetScaffold(
//        modifier = Modifier
//            .systemBarsPadding(),
//        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
//        sheetContent = {
//
//        },
//        sheetElevation = 0.dp,
//        sheetBackgroundColor = Color.Transparent,
//        backgroundColor = Color.Transparent
//    ) { paddingValues ->
//
//    }
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