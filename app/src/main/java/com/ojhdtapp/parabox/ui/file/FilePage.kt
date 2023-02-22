package com.ojhdtapp.parabox.ui.file

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.WorkInfo
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.DropdownMenuItemEvent
import com.ojhdtapp.parabox.ui.setting.EditUserNameDialog
import com.ojhdtapp.parabox.ui.theme.Theme
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.Integer.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Destination
@FileNavGraph(start = true)
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel: FilePageViewModel = hiltViewModel()
    val mainState by viewModel.fileStateFlow.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showCloudDialog by remember {
        mutableStateOf(false)
    }

    val lazyGridState = rememberLazyGridState()
    val searchLazyListState = rememberLazyListState()

    val hoverSearchBar by remember {
        derivedStateOf {
            (mainState.area == FilePageState.MAIN_AREA && lazyGridState.firstVisibleItemIndex > 1) ||
                    (mainState.area == FilePageState.SEARCH_AREA && searchLazyListState.firstVisibleItemIndex > 1)
        }
    }

    BackHandler(enabled = mainState.area != FilePageState.MAIN_AREA) {
        viewModel.setArea(FilePageState.MAIN_AREA)
    }
    BackHandler(enabled = viewModel.searchBarActivateState.value != SearchAppBar.NONE) {
        viewModel.setSearchBarActivateState(SearchAppBar.NONE)
    }
    LaunchedEffect(key1 = true) {
        viewModel.onSearch("", withoutDelay = true)
        viewModel.updateGoogleDriveFilesStateFlow()
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is FilePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar((it.message))
                }
            }
        }
    }
    val cloudService by mainSharedViewModel.cloudServiceFlow.collectAsState(initial = 0)
    val cloudTotalSpace by mainSharedViewModel.cloudTotalSpaceFlow.collectAsState(initial = 0L)
    val cloudUsedSpace by mainSharedViewModel.cloudUsedSpaceFlow.collectAsState(initial = 0L)
    val cloudUsedSpacePercent = remember {
        derivedStateOf {
            if (cloudTotalSpace == 0L) 0 else (cloudUsedSpace * 100 / cloudTotalSpace).toInt()
        }
    }
    val cloudAppUsedSpace by mainSharedViewModel.cloudAppUsedSpaceFlow.collectAsState(initial = 0L)
    val cloudAppUsedSpacePercent = remember {
        derivedStateOf {
            if (cloudTotalSpace == 0L) 0 else (cloudAppUsedSpace * 100 / cloudTotalSpace).toInt()
        }
    }
    val gDriveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (result.data != null) {
                    val googleSignInAccount = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    googleSignInAccount.addOnCompleteListener { task ->
                        showCloudDialog = false
                        if (task.isSuccessful) {
                            val account = task.result
                            if (account != null) {
                                mainSharedViewModel.saveGoogleDriveAccount(account)
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar(context.getString(R.string.connect_gd_success))
                                }
                            }
                        } else {
                            mainSharedViewModel.saveGoogleDriveAccount(null)
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar(context.getString(R.string.connect_cloud_service_cancel))
                            }
                        }
                    }
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar("设备不支持")
                    }
                }
            } else {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar("设备不支持")
                }
            }
        }
    // Delete File Confirm
    var deleteFileConfirm by remember {
        mutableStateOf(false)
    }
    if (deleteFileConfirm) {
        androidx.compose.material3.AlertDialog(onDismissRequest = {
            deleteFileConfirm = false
        },
            title = { Text(text = stringResource(R.string.delete_confirm)) },
            text = { Text(text = stringResource(R.string.delete_file_confirm_text)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteSelectedFile()
                    deleteFileConfirm = false
                }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deleteFileConfirm = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            })
    }
    // Cloud Dialog
    if (showCloudDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDialog = false },
            confirmButton = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "select cloud storage"
                )
            },
            title = { Text(text = stringResource(R.string.connect_cloud_service)) },
            text = {
                LazyColumn() {
                    item {
                        Surface(onClick = {
                            val signInIntent =
                                (context as MainActivity).getGoogleLoginAuth().signInIntent
                            gDriveLauncher.launch(signInIntent)
                        }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    faIcon = FaIcons.GoogleDrive,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.cloud_service_save_to_gd),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        )
    }
    UserProfileDialog(
        openDialog = mainSharedViewModel.showUserProfileDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
        avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
        pluginList = mainSharedViewModel.pluginListStateFlow.collectAsState().value,
        sizeClass = sizeClass,
        onUpdateName = {
            mainSharedViewModel.setEditUserNameDialogState(true)
        },
        onUpdateAvatar = {
            onEvent(ActivityEvent.SetUserAvatar)
        },
        onDismiss = { mainSharedViewModel.setShowUserProfileDialogState(false) })
    EditUserNameDialog(
        openDialog = mainSharedViewModel.editUserNameDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
        onConfirm = {
            mainSharedViewModel.setEditUserNameDialogState(false)
            mainSharedViewModel.setUserName(it)
        },
        onDismiss = { mainSharedViewModel.setEditUserNameDialogState(false) }
    )
    WorkInfoDialog(
        showDialog = mainSharedViewModel.workInfoDialogState.value,
        workInfoMap = mainSharedViewModel.workInfoMap,
        onCancel = {
            onEvent(ActivityEvent.CancelBackupWork(it, it.toString()))
        },
        sizeClass = sizeClass,
        onDismiss = { mainSharedViewModel.setWorkInfoDialogState(false) }
    )
    Scaffold(modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::onSearch,
                placeholder = stringResource(R.string.search_file),
                fileSelection = mainState.data.filter { it.fileId in viewModel.selectedFilesId },
                activateState = viewModel.searchBarActivateState.value,
                avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
                shouldHover = hoverSearchBar,
                onActivateStateChanged = {
                    viewModel.setSearchBarActivateState(it)
                    when (it) {
                        SearchAppBar.SEARCH -> viewModel.setArea(FilePageState.SEARCH_AREA)
                        SearchAppBar.NONE -> {
                            viewModel.setArea(FilePageState.MAIN_AREA)
                            viewModel.clearSelectedFiles()
                        }
                    }
                },
                sizeClass = sizeClass,
                onMenuClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }
                },
                onAvatarClick = {
                    mainSharedViewModel.setShowUserProfileDialogState(true)
                },
                onDropdownMenuItemEvent = {
                    when (it) {
                        is DropdownMenuItemEvent.DownloadFile -> {
                            viewModel.selectedFilesId.forEach { id ->
                                mainState.data.firstOrNull { it.fileId == id }?.also {
                                    onEvent(ActivityEvent.DownloadFile(it))
                                }
                            }
                            viewModel.setSearchBarActivateState(SearchAppBar.NONE)
                            viewModel.clearSelectedFiles()
                        }

                        is DropdownMenuItemEvent.CloudDownloadFile -> {
                            viewModel.selectedFilesId.forEach { id ->
                                mainState.data.firstOrNull { it.cloudType != null && it.cloudType != 0 && it.cloudId != null && it.fileId == id }
                                    ?.also {
                                        onEvent(ActivityEvent.DownloadFile(it))
                                    }
                            }
                            viewModel.setSearchBarActivateState(SearchAppBar.NONE)
                            viewModel.clearSelectedFiles()
                        }

                        is DropdownMenuItemEvent.SaveToCloud -> {
                            viewModel.selectedFilesId
                                .forEach { id ->
                                    mainState.data.firstOrNull { it.cloudId == null && it.fileId == id }
                                        ?.also {
                                            onEvent(ActivityEvent.SaveToCloud(it))
                                        }
                                }
                        }

                        is DropdownMenuItemEvent.DeleteFile -> {
                            deleteFileConfirm = true
                        }
                        else -> {}
                    }
                }
            )
        },
        bottomBar = {

        }) { paddingValues ->
        AnimatedContent(
            targetState = mainState.area,
//            transitionSpec = {
//                if (targetState == FilePageState.SEARCH_AREA && initialState == FilePageState.MAIN_AREA) {
//                    expandVertically(expandFrom = Alignment.Top).with(
//                        scaleOut(
//                            tween(200),
//                            0.9f
//                        ) + fadeOut(tween(200))
//                    ).apply {
//                        targetContentZIndex = 2f
//                    }
//                } else if (targetState == FilePageState.MAIN_AREA && initialState == FilePageState.SEARCH_AREA) {
//                    (scaleIn(tween(200), 0.9f) + fadeIn(tween(200))).with(
//                        shrinkVertically(
//                            shrinkTowards = Alignment.Top
//                        )
//                    ).apply {
//                        targetContentZIndex = 1f
//                    }
//                } else {
//                    fadeIn() with fadeOut()
//                }
//            }
        ) {
            when (it) {
                FilePageState.MAIN_AREA -> MainArea(
                    mainState = mainState,
                    lazyGridState = lazyGridState,
                    onSetRecentFilter = { type, value -> viewModel.setRecentFilter(type, value) },
                    searchText = viewModel.searchText.value,
                    selectedFileIdList = viewModel.selectedFilesId,
                    paddingValues = paddingValues,
                    onEvent = onEvent,
                    searchAppBarState = viewModel.searchBarActivateState.value,
                    sizeClass = sizeClass,
                    cloudService = cloudService,
                    cloudTotalSpace = cloudTotalSpace,
                    cloudUsedSpace = cloudUsedSpace,
                    cloudUsedSpacePercent = cloudUsedSpacePercent.value,
                    cloudAppUsedSpace = cloudAppUsedSpace,
                    cloudAppUsedSpacePercent = cloudAppUsedSpacePercent.value,
                    gDriveLauncher = gDriveLauncher,
                    workInfoPairList = mainSharedViewModel.workInfoMap.values.toList(),
                    isRefreshing = viewModel.isRefreshing.value,
                    theme = mainSharedViewModel.themeFlow.collectAsState(initial = Theme.WILLOW).value,
                    enableDynamicColor = mainSharedViewModel.enableDynamicColorFlow.collectAsState(
                        initial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ).value,
                    onLogoutGoogleDrive = {
                        mainSharedViewModel.saveGoogleDriveAccount(null)
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(context.getString(R.string.logged_out))
                        }
                    },
                    onChangeSearchAppBarState = {
                        viewModel.setSearchBarActivateState(it)
                    },
                    onChangeArea = { viewModel.setArea(it) },
                    onAddOrRemoveFile = { viewModel.addOrRemoveItemOfSelectedFileList(it.fileId) },
                    onShowWorkInfoDialog = {
                        mainSharedViewModel.setWorkInfoDialogState(true)
                    },
                    onShowCloudDialog = {
                        showCloudDialog = true
                    },
                    onRefresh = {
                        when (cloudService) {
                            GoogleDriveUtil.SERVICE_CODE -> {
                                viewModel.setIsRefreshing(true)
                                viewModel.updateGoogleDriveFilesStateFlow()
                            }
                            else -> {
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar(context.getString(R.string.cloud_service_not_connected))
                                }
                                coroutineScope.launch {
                                    viewModel.setIsRefreshing(true)
                                    delay(500)
                                    viewModel.setIsRefreshing(false)
                                }
                            }
                        }
                    }
                )

                FilePageState.SEARCH_AREA -> SearchArea(
                    mainState = mainState,
                    lazyListState = searchLazyListState,
                    searchText = viewModel.searchText.value,
                    selectedFileIdList = viewModel.selectedFilesId,
                    paddingValues = paddingValues,
                    onEvent = onEvent,
                    searchAppBarState = viewModel.searchBarActivateState.value,
                    onChangeSearchAppBarState = {
                        viewModel.setSearchBarActivateState(it)
                    },
                    onUpdateSizeFilter = viewModel::setFilter,
                    onUpdateExtensionFilter = viewModel::setFilter,
                    onUpdateTimeFilter = viewModel::setFilter,
                    onAddOrRemoveFile = { viewModel.addOrRemoveItemOfSelectedFileList(it.fileId) }
                )

                else -> {
                    AlertDialogDefaults.containerColor
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainArea(
    modifier: Modifier = Modifier,
    lazyGridState: LazyGridState,
    mainState: FilePageState,
    searchText: String,
    selectedFileIdList: List<Long>,
    paddingValues: PaddingValues,
    searchAppBarState: Int,
    sizeClass: WindowSizeClass,
    cloudService: Int,
    cloudTotalSpace: Long,
    cloudUsedSpace: Long,
    cloudUsedSpacePercent: Int,
    cloudAppUsedSpace: Long,
    cloudAppUsedSpacePercent: Int,
    gDriveLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    workInfoPairList: List<Pair<File, List<WorkInfo>>>,
    isRefreshing: Boolean,
    theme: Int,
    enableDynamicColor: Boolean,
    onLogoutGoogleDrive: () -> Unit,
    onChangeSearchAppBarState: (state: Int) -> Unit,
    onEvent: (ActivityEvent) -> Unit,
    onChangeArea: (area: Int) -> Unit,
    onAddOrRemoveFile: (file: File) -> Unit,
    onSetRecentFilter: (type: Int, value: Boolean) -> Unit,
    onShowWorkInfoDialog: () -> Unit,
    onShowCloudDialog: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
        onRefresh = onRefresh,
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                modifier = Modifier.offset(y = paddingValues.calculateTopPadding()),
                state = state, refreshTriggerDistance = trigger,
                scale = true,
                contentColor = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }
    ) {
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 64.dp else 0.dp),
            columns = GridCells.Adaptive(352.dp),
            contentPadding = paddingValues
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recent),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(bottom = 8.dp),
                        mainAxisSpacing = 8.dp
                    ) {
                        MyFilterChip(
                            selected = mainState.enableRecentDocsFilter,
                            label = { Text(text = stringResource(R.string.file_type_docs)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.DOCS,
                                !mainState.enableRecentDocsFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentSlidesFilter,
                            label = { Text(text = stringResource(R.string.file_type_slides)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.SLIDES,
                                !mainState.enableRecentSlidesFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentSheetsFilter,
                            label = { Text(text = stringResource(R.string.file_type_sheets)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.SHEETS,
                                !mainState.enableRecentSheetsFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentVideoFilter,
                            label = { Text(text = stringResource(R.string.file_type_video)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.VIDEO,
                                !mainState.enableRecentVideoFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentAudioFilter,
                            label = { Text(text = stringResource(R.string.file_type_audio)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.AUDIO,
                                !mainState.enableRecentAudioFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentPictureFilter,
                            label = { Text(text = stringResource(R.string.file_type_picture)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.PICTURE,
                                !mainState.enableRecentPictureFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentPDFFilter,
                            label = { Text(text = stringResource(R.string.file_type_pdf)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.PDF,
                                !mainState.enableRecentPDFFilter
                            )
                        }
                        MyFilterChip(
                            selected = mainState.enableRecentCompressedFilter,
                            label = { Text(text = stringResource(R.string.file_type_compressed)) }
                        ) {
                            onSetRecentFilter(
                                ExtensionFilter.COMPRESSED,
                                !mainState.enableRecentCompressedFilter
                            )
                        }
                    }
                    if (mainState.recentFilterData.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 320.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val imageLoader = ImageLoader.Builder(context)
                                .components {
                                    add(SvgDecoder.Factory())
                                }
                                .build()
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(
                                        when {
                                            enableDynamicColor -> R.drawable.empty_2_dynamic
                                            theme == Theme.WILLOW -> R.drawable.empty_2_willow
                                            theme == Theme.PURPLE -> R.drawable.empty_2_purple
                                            theme == Theme.SAKURA -> R.drawable.empty_2_sakura
                                            theme == Theme.GARDENIA -> R.drawable.empty_2_gardenia
                                            theme == Theme.WATER -> R.drawable.empty_2_water
                                            else -> R.drawable.empty_2_willow
                                        }                                    )
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(224.dp)
                                    .padding(bottom = 16.dp)
                            )
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally),
                                text = stringResource(R.string.no_file_text),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        mainState.recentFilterData.take(5)
                            .forEachIndexed { index, file ->
                                FileItem(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .animateItemPlacement(),
                                    file = file,
                                    searchText = searchText,
                                    isFirst = index == 0,
                                    isLast = index == min(mainState.recentFilterData.lastIndex, 4),
                                    isSelected = selectedFileIdList.contains(file.fileId),
                                    onClick = {
                                        if (searchAppBarState == SearchAppBar.FILE_SELECT) {
                                            onAddOrRemoveFile(file)
                                        } else {
                                            if (file.downloadingState is DownloadingState.None || file.downloadingState is DownloadingState.Failure) {
                                                onEvent(ActivityEvent.DownloadFile(file))
                                            } else if (file.downloadingState is DownloadingState.Done) {
                                                onEvent(ActivityEvent.OpenFile(file))
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                                        onAddOrRemoveFile(file)
                                    },
                                    onAvatarClick = {
                                        onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                                        onAddOrRemoveFile(file)
                                    })
                            }
                        TextButton(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = { onChangeArea(FilePageState.SEARCH_AREA) }
                        ) {
                            Text(text = stringResource(R.string.show_full_list))
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cloud_service),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val runningWork by remember(workInfoPairList) {
                        derivedStateOf {
                            workInfoPairList.count { it.second.any { !it.state.isFinished } }
                        }
                    }
                    AnimatedVisibility(
                        visible = workInfoPairList.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Surface(
                            modifier = Modifier.padding(bottom = 16.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            onClick = {
                                onShowWorkInfoDialog()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (runningWork != 0) Icons.Outlined.Backup else Icons.Outlined.CloudDone,
                                    contentDescription = "backup",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = if (runningWork != 0) stringResource(
                                        R.string.backuping_file,
                                        runningWork
                                    )
                                    else stringResource(R.string.no_backuping_file),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(
                                    imageVector = Icons.Outlined.NavigateNext,
                                    contentDescription = "next",
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    Crossfade(
                        targetState = cloudService,
                    ) {
                        when (it) {
                            GoogleDriveUtil.SERVICE_CODE -> {
                                Column() {
                                    var expanded by remember {
                                        mutableStateOf(false)
                                    }
                                    Box(modifier = Modifier.wrapContentSize()) {
                                        OutlinedCard(modifier = Modifier
                                            .fillMaxWidth(), onClick = {
                                            expanded = true
                                        }) {
                                            Row(modifier = Modifier.padding(16.dp)) {
                                                Surface(
                                                    modifier = Modifier.size(48.dp),
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        FaIcon(
                                                            faIcon = FaIcons.GoogleDrive,
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column() {
                                                    Text(
                                                        text = stringResource(id = R.string.cloud_service_gd),
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    LinearProgressIndicator(
                                                        progress = 0.6f,
                                                        modifier = Modifier
                                                            .padding(vertical = 4.dp)
                                                            .clip(CircleShape),
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            id = R.string.cloud_service_used_space,
                                                            cloudUsedSpacePercent,
                                                            FileUtil.getSizeString(
                                                                cloudUsedSpace
                                                            ),
                                                            FileUtil.getSizeString(
                                                                cloudTotalSpace
                                                            )
                                                        ),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = stringResource(
                                                            R.string.cloud_service_app_used_space,
                                                            cloudAppUsedSpacePercent,
                                                            FileUtil.getSizeString(
                                                                cloudAppUsedSpace
                                                            )
                                                        ),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        RoundedCornerDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(R.string.sign_out_cloud_service)) },
                                                onClick = {
                                                    expanded = false
                                                    (context as MainActivity).getGoogleLoginAuth()
                                                        .signOut()
                                                        .addOnCompleteListener {
                                                            onLogoutGoogleDrive()
                                                        }
                                                })
                                        }
                                    }
                                }
                            }
                            else -> {
                                OutlinedCard() {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Box(
                                                modifier = Modifier.size(72.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CloudOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            modifier = Modifier.padding(top = 16.dp),
                                            text = stringResource(R.string.cloud_service_not_connected),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            text = stringResource(R.string.cloud_service_not_connected_text),
                                            style = MaterialTheme.typography.labelLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                onShowCloudDialog()
//                                            val signInIntent =
//                                                (context as MainActivity).getGoogleLoginAuth().signInIntent
//                                            gDriveLauncher.launch(signInIntent)
                                            }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Cloud,
                                                contentDescription = "cloud",
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .size(ButtonDefaults.IconSize),
                                            )
                                            Text(text = stringResource(R.string.connect_cloud_service))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchArea(
    modifier: Modifier = Modifier,
    mainState: FilePageState,
    lazyListState: LazyListState,
    searchText: String,
    selectedFileIdList: List<Long>,
    paddingValues: PaddingValues,
    searchAppBarState: Int,
    onChangeSearchAppBarState: (state: Int) -> Unit,
    onUpdateTimeFilter: (filter: TimeFilter) -> Unit,
    onUpdateExtensionFilter: (filter: ExtensionFilter) -> Unit,
    onUpdateSizeFilter: (filter: SizeFilter) -> Unit,
    onAddOrRemoveFile: (file: File) -> Unit,
    onEvent: (ActivityEvent) -> Unit,
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = lazyListState,
            modifier = modifier
                .fillMaxHeight()
                .widthIn(0.dp, 600.dp),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    var showSizeFilterDropDownMenu by remember {
                        mutableStateOf(false)
                    }
                    var showExtensionFilterDropDownMenu by remember {
                        mutableStateOf(false)
                    }
                    var showTimeFilterDropDownMenu by remember {
                        mutableStateOf(false)
                    }
                    MyFilterChip(
                        selected = mainState.sizeFilter !is SizeFilter.All,
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "expand",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                                RoundedCornerDropdownMenu(
                                    expanded = showSizeFilterDropDownMenu,
                                    onDismissRequest = { showSizeFilterDropDownMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = SizeFilter.All.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.All)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = SizeFilter.TenMB.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.TenMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = SizeFilter.HundredMB.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.HundredMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = SizeFilter.OverHundredMB.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.OverHundredMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                }
                            }
                        },
                        label = { Text(text = stringResource(id = mainState.sizeFilter.labelResId)) },
                        withoutLeadingIcon = true,
                    ) {
                        showSizeFilterDropDownMenu = true
                    }
                    MyFilterChip(
                        selected = mainState.extensionFilter !is ExtensionFilter.All,
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "expand",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                                RoundedCornerDropdownMenu(
                                    expanded = showExtensionFilterDropDownMenu,
                                    onDismissRequest = { showExtensionFilterDropDownMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.All.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.All)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Docs.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Docs)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Slides.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Slides)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Sheets.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Sheets)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Video.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Video)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Audio.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Audio)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Picture.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Picture)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Compressed.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Compressed)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = ExtensionFilter.Pdf.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Pdf)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(id = mainState.extensionFilter.labelResId)
                            )
                        },
                        withoutLeadingIcon = true,
                    ) {
                        showExtensionFilterDropDownMenu = true
                    }
                    MyFilterChip(
                        selected = mainState.timeFilter !is TimeFilter.All,
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "expand",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                                RoundedCornerDropdownMenu(
                                    expanded = showTimeFilterDropDownMenu,
                                    onDismissRequest = { showTimeFilterDropDownMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = TimeFilter.All.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.All)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = TimeFilter.WithinThreeDays.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThreeDays)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = TimeFilter.WithinThisWeek.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThisWeek)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = TimeFilter.WithinThisMonth.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThisMonth)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(id = TimeFilter.MoreThanAMonth.labelResId)
                                            )
                                        },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.MoreThanAMonth)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    val timeRangePicker = rememberDateRangePicker() {
                                        onUpdateTimeFilter(
                                            TimeFilter.Custom(
                                                timestampStart = it.first,
                                                timestampEnd = it.second
                                            )
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.time_range_picker)) },
                                        onClick = {
                                            showTimeFilterDropDownMenu = false
                                            if (!timeRangePicker.isAdded) {
                                                timeRangePicker.show(
                                                    (context as AppCompatActivity).supportFragmentManager,
                                                    "time_range_picker"
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                text = if (mainState.timeFilter is TimeFilter.Custom) {
                                    stringResource(
                                        id = mainState.timeFilter.labelResId,
                                        mainState.timeFilter.timestampStart?.toFormattedDate(context)
                                            ?: context.getString(R.string.time_filter_custom_not_set_label),
                                        mainState.timeFilter.timestampEnd?.toFormattedDate(context)
                                            ?: context.getString(R.string.time_filter_custom_not_set_label),
                                    )
                                } else {
                                    stringResource(id = mainState.timeFilter.labelResId)
                                }
                            )
                        },
                        withoutLeadingIcon = true,
                    ) {
                        showTimeFilterDropDownMenu = true
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            item {
                if (mainState.filterData.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "search result",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_search_result),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            itemsIndexed(
                items = mainState.filterData,
                key = { index, item -> item.fileId }) { index, item ->
                FileItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement(),
                    file = item,
                    searchText = searchText,
                    isFirst = index == 0,
                    isLast = index == mainState.filterData.lastIndex,
                    isSelected = selectedFileIdList.contains(item.fileId),
                    onClick = {
                        if (searchAppBarState == SearchAppBar.FILE_SELECT) {
                            onAddOrRemoveFile(item)
                        } else {
                            if (item.downloadingState is DownloadingState.None || item.downloadingState is DownloadingState.Failure) {
                                onEvent(ActivityEvent.DownloadFile(item))
                            } else {

                            }
                        }
                    },
                    onLongClick = {
                        onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                        onAddOrRemoveFile(item)
                    },
                    onAvatarClick = {
                        onChangeSearchAppBarState(SearchAppBar.FILE_SELECT)
                        onAddOrRemoveFile(item)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    modifier: Modifier = Modifier,
    file: File,
    searchText: String,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current
    val topRadius by animateDpAsState(targetValue = if (isFirst) 24.dp else 0.dp)
    val bottomRadius by animateDpAsState(targetValue = if (isLast) 24.dp else 0.dp)
    Surface(
        modifier = modifier.height(IntrinsicSize.Min),
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        ),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = LocalIndication.current,
                    enabled = true,
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(targetState = isSelected) {
                if (it) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                onAvatarClick()
                            }
                    ) {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (file.downloadingState is DownloadingState.Failure) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                onAvatarClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (file.downloadingState) {
                            is DownloadingState.None -> {
                                Icon(
                                    imageVector = when (file.extension) {
                                        "apk" -> Icons.Outlined.Android
                                        "bmp", "jpeg", "jpg", "png", "tif", "gif", "pcx", "tga", "exif", "fpx", "svg", "psd", "cdr", "pcd", "dxf", "ufo", "eps", "ai", "raw", "webp", "avif", "apng", "tiff" -> Icons.Outlined.Image
                                        "txt", "log", "md", "json", "xml" -> Icons.Outlined.Description
                                        "cd", "wav", "aiff", "mp3", "wma", "ogg", "mpc", "flac", "ape", "3gp" -> Icons.Outlined.AudioFile
                                        "avi", "wmv", "mp4", "mpeg", "mpg", "mov", "flv", "rmvb", "rm", "asf" -> Icons.Outlined.VideoFile
                                        "zip", "rar", "7z", "bz2", "tar", "jar", "gz", "deb" -> Icons.Outlined.FolderZip
                                        else -> Icons.Outlined.FilePresent
                                    }, contentDescription = "type",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            is DownloadingState.Downloading -> {
                                val progress by animateFloatAsState(targetValue = file.downloadingState.progress)
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    progress = progress,
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "download",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            is DownloadingState.Failure -> {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = "warning",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            is DownloadingState.Done -> {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownloadDone,
                                    contentDescription = "download done",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), verticalArrangement = Arrangement.Center
            ) {
//                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildAnnotatedString {
                        file.name.splitKeeping(searchText).forEach {
                            if (it == searchText) {
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(it)
                                }
                            } else {
                                append(it)
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        file.profileName.splitKeeping(searchText).forEach {
                            if (it == searchText) {
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append(it)
                                }
                            } else {
                                append(it)
                            }
                        }
                        append("  ")
                        if (file.downloadingState is DownloadingState.Downloading) {
                            append(FileUtil.getSizeString(file.downloadingState.downloadedBytes.toLong()))
                            append(" ")
                            append("/")
                            append(" ")
                            append(FileUtil.getSizeString(file.downloadingState.totalBytes.toLong()))
                        } else {
                            if(file.size != 0L){
                                append(FileUtil.getSizeString(file.size))
                            } else {
                                append(stringResource(R.string.unknown_file_size))
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = file.timestamp.toTimeUntilNow(context),
                    style = MaterialTheme.typography.labelMedium
                )
                if (file.cloudId != null) {
                    Icon(
                        modifier = Modifier
                            .size(16.dp),
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = "cloud done",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

