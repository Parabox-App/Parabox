package com.ojhdtapp.parabox.ui.setting

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.BackupPageDestination
import com.ojhdtapp.parabox.ui.destinations.CloudPageDestination
import com.ojhdtapp.parabox.ui.destinations.ExperimentalPageDestination
import com.ojhdtapp.parabox.ui.destinations.ExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.FCMPageDestination
import com.ojhdtapp.parabox.ui.destinations.InterfacePageDestination
import com.ojhdtapp.parabox.ui.destinations.NotificationPageDestination
import com.ojhdtapp.parabox.ui.destinations.SupportPageDestination
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.RoundedCornerDropdownMenu
import com.ojhdtapp.parabox.ui.util.SettingNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Destination
@SettingNavGraph(start = true)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()

    EditUserNameDialog(
        openDialog = viewModel.editUserNameDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
        onConfirm = {
            viewModel.setEditUserNameDialogState(false)
            mainSharedViewModel.setUserName(it)
        },
        onDismiss = { viewModel.setEditUserNameDialogState(false) }
    )
    BoxWithConstraints() {
        //300 -400 -> 750 -1000
        val savedMaxWidth by remember{
            mutableStateOf(maxWidth)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Left
            val exitUntilCollapsedScrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            val pinnedScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

            val leftModifier = when {
                savedMaxWidth > 1000.dp -> modifier.width(400.dp)
                else -> modifier.weight(2f)
            }

            Scaffold(
                modifier = leftModifier
                    .shadow(8.dp)
                    .zIndex(1f)
                    .nestedScroll(
                        if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) exitUntilCollapsedScrollBehavior.nestedScrollConnection
                        else pinnedScrollBehavior.nestedScrollConnection
                    ),
                topBar = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        val colorTransitionFraction =
                            exitUntilCollapsedScrollBehavior.state.collapsedFraction
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
                            title = { Text(stringResource(R.string.settings)) },
                            actions = {
//                                var expanded by remember {
//                                    mutableStateOf(false)
//                                }
//                                Box(
//                                    modifier = Modifier
//                                        .wrapContentSize(Alignment.TopStart)
//                                ) {
//                                    IconButton(onClick = {
//                                        expanded = !expanded
//                                    }) {
//                                        Icon(
//                                            imageVector = Icons.Outlined.MoreVert,
//                                            contentDescription = "more"
//                                        )
//                                    }
//                                    RoundedCornerDropdownMenu(
//                                        expanded = expanded,
//                                        onDismissRequest = { expanded = false },
//                                        modifier = Modifier.width(192.dp)
//                                    ) {
//                                        DropdownMenuItem(
//                                            text = {
//                                                Text(
//                                                    text = "开放源代码许可",
//                                                    style = MaterialTheme.typography.bodyMedium
//                                                )
//                                            },
//                                            onClick = {
//                                                expanded = false
//                                            })
//                                    }
//                                }
                            },
                            scrollBehavior = exitUntilCollapsedScrollBehavior
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Text(text = stringResource(R.string.settings))
                            },
                            navigationIcon = {
                                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            drawerState.open()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Menu,
                                            contentDescription = "menu"
                                        )
                                    }
                                }
                            },
                            actions = {
//                                var expanded by remember {
//                                    mutableStateOf(false)
//                                }
//                                Box(
//                                    modifier = Modifier
//                                        .wrapContentSize(Alignment.TopStart)
//                                ) {
//                                    IconButton(onClick = {
//                                        expanded = !expanded
//                                    }) {
//                                        Icon(
//                                            imageVector = Icons.Outlined.MoreVert,
//                                            contentDescription = "more"
//                                        )
//                                    }
//                                    RoundedCornerDropdownMenu(
//                                        expanded = expanded,
//                                        onDismissRequest = { expanded = false },
//                                        modifier = Modifier.width(192.dp)
//                                    ) {
//                                        DropdownMenuItem(
//                                            text = {
//                                                Text(
//                                                    text = "开放源代码许可",
//                                                    style = MaterialTheme.typography.bodyMedium
//                                                )
//                                            },
//                                            onClick = {
//                                                expanded = false
//                                            })
//                                    }
//                                }
                            },
                            scrollBehavior = pinnedScrollBehavior
                        )
                    }

                }
                ) { innerPadding ->
                // Plugin List State
                val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
                LazyColumn(
                    contentPadding = innerPadding,
                    //                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                        item {
                            ThemeBlock(
                                modifier = Modifier.fillMaxWidth(),
                                userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
                                version = BuildConfig.VERSION_NAME,
                                onBlockClick = {},
                                onUserNameClick = {
                                    viewModel.setEditUserNameDialogState(true)
                                },
                                onVersionClick = {}
                            )
                        }
                    }
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact || sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
//                        item(key = "extension_status") {
//                            AnimatedVisibility(
//                                visible = pluginList.isNotEmpty(),
//                                enter = expandVertically(),
//                                exit = shrinkVertically()
//                            ) {
//                                PreferencesCategory(text = "扩展")
//                            }
//                        }
//                        items(
//                            items = pluginList,
//                            key = { it.packageName }) {
//                            NormalPreference(
//                                modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
//                                title = it.name,
//                                subtitle = it.packageName,
//                                leadingIcon = {
//                                    AsyncImage(
//                                        model = it.icon,
//                                        contentDescription = "icon",
//                                        modifier = Modifier
//                                            .size(24.dp)
//                                            .clip(
//                                                CircleShape
//                                            )
//                                    )
//                                },
//                                trailingIcon = {
//                                    when (it.runningStatus) {
//                                        AppModel.RUNNING_STATUS_DISABLED -> Icon(
//                                            imageVector = Icons.Outlined.Block,
//                                            contentDescription = "disabled"
//                                        )
//                                        AppModel.RUNNING_STATUS_ERROR -> Icon(
//                                            imageVector = Icons.Outlined.ErrorOutline,
//                                            contentDescription = "error",
//                                            tint = MaterialTheme.colorScheme.error
//                                        )
//                                        AppModel.RUNNING_STATUS_RUNNING -> Icon(
//                                            imageVector = Icons.Outlined.CheckCircleOutline,
//                                            contentDescription = "running",
//                                            tint = MaterialTheme.colorScheme.primary
//                                        )
//                                        AppModel.RUNNING_STATUS_CHECKING -> CircularProgressIndicator(
//                                            modifier = Modifier.size(24.dp),
//                                            strokeWidth = 2.dp
//                                        )
//                                    }
//                                },
//                                onClick = {
//                                    it.launchIntent?.let {
//                                        onEvent(ActivityEvent.LaunchIntent(it))
//                                    }
//                                }
//                            )
//                        }
                    } else {
                        item(key = "info") {
                            NormalPreference(
                                modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                                leadingIcon =
                                if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "application info"
                                        )
                                    }
                                } else null,
                                title = stringResource(R.string.about),
                                subtitle = stringResource(R.string.about_subtitle),
                                selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                        && viewModel.selectedSetting.value == SettingPageState.INFO,
                                roundedCorner = true,
                            ) {
                                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                    mainNavController.navigate(ExtensionPageDestination)
                                } else {
                                    viewModel.setSelectedSetting(SettingPageState.INFO)
                                }
                            }
                        }
                    }
                    item(key = "function") {
                        PreferencesCategory(text = stringResource(R.string.function))
                    }
                    item(key = "extension") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Extension,
                                        contentDescription = "plugin"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.extension),
                            subtitle = stringResource(R.string.extension_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.EXTENSION,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(ExtensionPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.EXTENSION)
                            }
                        }
                    }
                    item(key = "cloud") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Cloud,
                                        contentDescription = "cloud"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.connect_cloud_service),
                            subtitle = stringResource(R.string.connect_cloud_service_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.CLOUD,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(CloudPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.CLOUD)
                            }
                        }
                    }
                    item(key = "fcm") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = "fcm"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.fcm),
                            subtitle = stringResource(R.string.fcm_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.FCM,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(FCMPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.FCM)
                            }
                        }
                    }
                    item(key = "backup") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = "backup and restore"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.backup_and_restore),
                            subtitle = stringResource(R.string.backup_and_restore_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.BACKUP,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(BackupPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.BACKUP)
                            }
                        }
                    }
                    item(key = "personalise") {
                        PreferencesCategory(text = stringResource(R.string.personalise))
                    }
                    item(key = "notification") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "notification"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.notification),
                            subtitle = stringResource(R.string.notification_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.NOTIFICATION,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(NotificationPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.NOTIFICATION)
                            }
                        }
                    }
                    item(key = "interface") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = "theme"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.user_interface),
                            subtitle = stringResource(R.string.user_interface_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.INTERFACE,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(InterfacePageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.INTERFACE)
                            }
                        }
                    }
                    item(key = "additional") {
                        PreferencesCategory(text = stringResource(R.string.other))
                    }
                    item(key = "experimental") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Science,
                                        contentDescription = "experimental"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.experimental),
                            subtitle = stringResource(R.string.experimental_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.EXPERIMENTAL,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(ExperimentalPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.EXPERIMENTAL)
                            }
                        }
                    }
                    item(key = "help") {
                        NormalPreference(
                            modifier = Modifier.padding(horizontal = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 16.dp else 0.dp),
                            leadingIcon =
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.HelpOutline,
                                        contentDescription = "help and support"
                                    )
                                }
                            } else null,
                            title = stringResource(R.string.support),
                            subtitle = stringResource(R.string.support_subtitle),
                            selected = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                                    && viewModel.selectedSetting.value == SettingPageState.SUPPORT,
                            roundedCorner = true,
                        ) {
                            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                                mainNavController.navigate(SupportPageDestination)
                            } else {
                                viewModel.setSelectedSetting(SettingPageState.SUPPORT)
                            }
                        }
                    }
                }
            }
            // Right
            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) {
                AnimatedContent(
                    modifier = Modifier.weight(3f),
                    targetState = viewModel.selectedSetting.value,
                    transitionSpec = {
                        slideInHorizontally { 100 } + fadeIn() with fadeOut()
                    }
                ) {
                    when (it) {
                        SettingPageState.INFO -> InfoPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent,
                        )
                        SettingPageState.EXTENSION -> ExtensionPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent,
                        )
                        SettingPageState.CLOUD -> CloudPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.FCM -> FCMPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.BACKUP -> BackupPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.NOTIFICATION -> NotificationPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.INTERFACE -> InterfacePage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.EXPERIMENTAL -> ExperimentalPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                        SettingPageState.SUPPORT -> SupportPage(
                            navigator = navigator,
                            mainNavController = mainNavController,
                            mainSharedViewModel = mainSharedViewModel,
                            sizeClass = sizeClass,
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeBlock(
    modifier: Modifier = Modifier,
    userName: String,
    version: String,
    onBlockClick: () -> Unit,
    onUserNameClick: () -> Unit,
    onVersionClick: () -> Unit,
    padding: Dp = 16.dp,
) =
    Row(
        modifier = modifier
            .aspectRatio(2f)
            .padding(horizontal = padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        var shapeState by remember {
            mutableStateOf(true)
        }
        val roundedCornerA by animateDpAsState(targetValue = if (shapeState) 72.dp else 24.dp)
        val roundedCornerB by animateDpAsState(targetValue = if (shapeState) 24.dp else 72.dp)

        Surface(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(
                topStart = roundedCornerA,
                topEnd = roundedCornerB,
                bottomStart = roundedCornerB,
                bottomEnd = roundedCornerA,
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        shapeState = !shapeState
                        onBlockClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        add(SvgDecoder.Factory())
                    }
                    .build()
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.drawable.ic_launcher_foreground_dynamic else R.drawable.ic_launcher_foreground)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(padding))
        Column(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .clickable { onUserNameClick() }
                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.user_name_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(padding))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .clickable { onVersionClick() }
                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.version),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
