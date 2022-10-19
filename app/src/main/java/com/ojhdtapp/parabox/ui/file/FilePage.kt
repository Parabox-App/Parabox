package com.ojhdtapp.parabox.ui.file

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.HiltApplication
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.DropdownMenuItemEvent
import com.ojhdtapp.parabox.ui.setting.EditUserNameDialog
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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
    BackHandler(enabled = mainState.area != FilePageState.MAIN_AREA) {
        viewModel.setArea(FilePageState.MAIN_AREA)
    }
    BackHandler(enabled = viewModel.searchBarActivateState.value != SearchAppBar.NONE) {
        viewModel.setSearchBarActivateState(SearchAppBar.NONE)
    }
    LaunchedEffect(key1 = true) {
        viewModel.onSearch("", withoutDelay = true)
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is FilePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar((it.message))
                }
            }
        }
    }
    // Google Drive
    val gDriveLogin by mainSharedViewModel.googleLoginFlow.collectAsState(initial = false)
    val gDriveTotalSpace by mainSharedViewModel.googleTotalSpaceFlow.collectAsState(initial = 0L)
    val gDriveUsedSpace by mainSharedViewModel.googleUsedSpaceFlow.collectAsState(initial = 0L)
    val gDriveUsedSpacePercent = remember {
        derivedStateOf {
            if (gDriveTotalSpace == 0L) 0 else (gDriveUsedSpace * 100 / gDriveTotalSpace).toInt()
        }
    }
    val gDriveAppUsedSpace by mainSharedViewModel.googleAppUsedSpaceFlow.collectAsState(initial = 0L)
    val gDriveAppUsedSpacePercent = remember {
        derivedStateOf {
            if (gDriveTotalSpace == 0L) 0 else (gDriveAppUsedSpace * 100 / gDriveTotalSpace).toInt()
        }
    }
    val gDriveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (result.data != null) {
                    val googleSignInAccount = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    googleSignInAccount.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val account = task.result
                            if (account != null) {
                                mainSharedViewModel.saveGoogleDriveAccount(account)
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar("成功连接 Google Drive")
                                }
                            }
                        } else {
                            mainSharedViewModel.saveGoogleDriveAccount(null)
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar("连接取消")
                            }
                        }
                    }
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
            title = { Text(text = "确认删除") },
            text = { Text(text = "选择项将从文件列表移除，但不会影响所有已下载的本地文件。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteSelectedFile()
                    deleteFileConfirm = false
                }) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    deleteFileConfirm = false
                }) {
                    Text(text = "取消")
                }
            })
    }
    UserProfileDialog(
        openDialog = mainSharedViewModel.showUserProfileDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
        avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
        gDriveLogin = mainSharedViewModel.googleLoginFlow.collectAsState(initial = false).value,
        gDriveTotalSpace = mainSharedViewModel.googleTotalSpaceFlow.collectAsState(initial = 0L).value,
        gDriveUsedSpace = mainSharedViewModel.googleUsedSpaceFlow.collectAsState(initial = 0L).value,
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
    Scaffold(modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::onSearch,
                placeholder = "搜索文件",
                fileSelection = mainState.data.filter { it.fileId in viewModel.selectedFilesId },
                activateState = viewModel.searchBarActivateState.value,
                avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
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
                        }

                        is DropdownMenuItemEvent.SaveToCloud -> {}
                        is DropdownMenuItemEvent.DeleteFile -> {
                            deleteFileConfirm = true
                        }
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
                    onSetRecentFilter = { type, value -> viewModel.setRecentFilter(type, value) },
                    searchText = viewModel.searchText.value,
                    selectedFileIdList = viewModel.selectedFilesId,
                    paddingValues = paddingValues,
                    onEvent = onEvent,
                    searchAppBarState = viewModel.searchBarActivateState.value,
                    sizeClass = sizeClass,
                    gDriveLogin = gDriveLogin,
                    gDriveTotalSpace = gDriveTotalSpace,
                    gDriveUsedSpace = gDriveUsedSpace,
                    gDriveUsedSpacePercent = gDriveUsedSpacePercent.value,
                    gDriveAppUsedSpace = gDriveAppUsedSpace,
                    gDriveAppUsedSpacePercent = gDriveAppUsedSpacePercent.value,
                    gDriveLauncher = gDriveLauncher,
                    onLogoutGoogleDrive = {
                        mainSharedViewModel.saveGoogleDriveAccount(null)
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar("已退出登录")
                        }
                    },
                    onChangeSearchAppBarState = {
                        viewModel.setSearchBarActivateState(it)
                    },
                    onChangeArea = { viewModel.setArea(it) },
                    onAddOrRemoveFile = { viewModel.addOrRemoveItemOfSelectedFileList(it.fileId) }
                )

                FilePageState.SEARCH_AREA -> SearchArea(
                    mainState = mainState,
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
    mainState: FilePageState,
    searchText: String,
    selectedFileIdList: List<Long>,
    paddingValues: PaddingValues,
    searchAppBarState: Int,
    sizeClass: WindowSizeClass,
    gDriveLogin: Boolean,
    gDriveTotalSpace: Long,
    gDriveUsedSpace: Long,
    gDriveUsedSpacePercent: Int,
    gDriveAppUsedSpace: Long,
    gDriveAppUsedSpacePercent: Int,
    gDriveLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onLogoutGoogleDrive: () -> Unit,
    onChangeSearchAppBarState: (state: Int) -> Unit,
    onEvent: (ActivityEvent) -> Unit,
    onChangeArea: (area: Int) -> Unit,
    onAddOrRemoveFile: (file: File) -> Unit,
    onSetRecentFilter: (type: Int, value: Boolean) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
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
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "最近的",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(bottom = 8.dp),
                    mainAxisSpacing = 8.dp
                ) {
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentDocsFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.DOCS,
                                !mainState.enableRecentDocsFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentDocsFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "文档") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentSlidesFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.SLIDES,
                                !mainState.enableRecentSlidesFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentSlidesFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "演示文稿") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentSheetsFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.SHEETS,
                                !mainState.enableRecentSheetsFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentSheetsFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "电子表格") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentVideoFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.VIDEO,
                                !mainState.enableRecentVideoFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentVideoFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "视频") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentAudioFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.AUDIO,
                                !mainState.enableRecentAudioFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentAudioFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "音频") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentPictureFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.PICTURE,
                                !mainState.enableRecentPictureFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentPictureFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "图片") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentPDFFilter,
                        onClick = {
                            onSetRecentFilter(ExtensionFilter.PDF, !mainState.enableRecentPDFFilter)
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentPDFFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "便携式文档") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.enableRecentCompressedFilter,
                        onClick = {
                            onSetRecentFilter(
                                ExtensionFilter.COMPRESSED,
                                !mainState.enableRecentCompressedFilter
                            )
                        },
                        enabled = true,
                        leadingIcon = {
                            if (mainState.enableRecentCompressedFilter)
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = "",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                        },
                        label = { Text(text = "压缩文件") },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                }
                if (mainState.recentFilterData.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val context = LocalContext.current
                        val imageLoader = ImageLoader.Builder(context)
                            .components {
                                add(SvgDecoder.Factory())
                            }
                            .build()
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.drawable.empty_2_dynamic else R.drawable.empty_2)
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
                            text = "暂无可显示的文件",
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
                        Text(text = "查看完整列表")
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
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "云服务",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Crossfade(
                targetState = gDriveLogin
            ) {
                if (it) {
                    Column() {
                        var expanded by remember {
                            mutableStateOf(false)
                        }
                        Box(modifier = Modifier.wrapContentSize()) {
                            OutlinedCard(modifier = Modifier
                                .padding(16.dp)
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
                                            text = "Google Drive",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        LinearProgressIndicator(
                                            progress = 0.6f,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Text(
                                            text = "已使用 ${gDriveUsedSpacePercent}% 的存储空间（${
                                                FileUtil.getSizeString(
                                                    gDriveUsedSpace
                                                )
                                            } / ${FileUtil.getSizeString(gDriveTotalSpace)}）",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "其中应用使用 ${gDriveAppUsedSpacePercent}%（${
                                                FileUtil.getSizeString(
                                                    gDriveAppUsedSpace
                                                )
                                            }）",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            RoundedCornerDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text(text = "退出登录") }, onClick = {
                                    expanded = false
                                    (context as MainActivity).getGoogleLoginAuth().signOut()
                                        .addOnCompleteListener {
                                            onLogoutGoogleDrive()
                                        }
                                })
                            }
                        }
                    }
                } else {
                    OutlinedCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItemPlacement(),
                        shape = CardDefaults.outlinedShape
                    ) {
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
                                text = "未连接云端服务",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                modifier = Modifier.padding(vertical = 16.dp),
                                text = "连接云端服务可将您的会话文件备份至云端",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FilledTonalButton(
                                onClick = {
                                    val signInIntent =
                                        (context as MainActivity).getGoogleLoginAuth().signInIntent
                                    gDriveLauncher.launch(signInIntent)
                                }) {
                                Icon(
                                    imageVector = Icons.Outlined.Cloud,
                                    contentDescription = "cloud",
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(ButtonDefaults.IconSize),
                                )
                                Text(text = "连接云端服务")
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
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.sizeFilter !is SizeFilter.All,
                        onClick = {
                            showSizeFilterDropDownMenu = true
                        },
                        enabled = true,
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
                                        text = { Text(SizeFilter.All.label) },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.All)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(SizeFilter.TenMB.label) },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.TenMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(SizeFilter.HundredMB.label) },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.HundredMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(SizeFilter.OverHundredMB.label) },
                                        onClick = {
                                            onUpdateSizeFilter(SizeFilter.OverHundredMB)
                                            showSizeFilterDropDownMenu = false
                                        },
                                    )
                                }
                            }
                        },
                        label = { Text(text = mainState.sizeFilter.label) },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.extensionFilter !is ExtensionFilter.All,
                        onClick = {
                            showExtensionFilterDropDownMenu = true
                        },
                        enabled = true,
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
                                        text = { Text(ExtensionFilter.All.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.All)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Docs.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Docs)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Slides.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Slides)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Sheets.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Sheets)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Video.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Video)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Audio.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Audio)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Picture.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Picture)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Compressed.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Compressed)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(ExtensionFilter.Pdf.label) },
                                        onClick = {
                                            onUpdateExtensionFilter(ExtensionFilter.Pdf)
                                            showExtensionFilterDropDownMenu = false
                                        },
                                    )
                                }
                            }
                        },
                        label = { Text(text = mainState.extensionFilter.label) },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize(),
                        selected = mainState.timeFilter !is TimeFilter.All,
                        onClick = {
                            showTimeFilterDropDownMenu = true
                        },
                        enabled = true,
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
                                        text = { Text(TimeFilter.All.label) },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.All)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(TimeFilter.WithinThreeDays.label) },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThreeDays)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(TimeFilter.WithinThisWeek.label) },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThisWeek)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(TimeFilter.WithinThisMonth.label) },
                                        onClick = {
                                            onUpdateTimeFilter(TimeFilter.WithinThisMonth)
                                            showTimeFilterDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(TimeFilter.MoreThanAMonth.label) },
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
                                        text = { Text("自定义范围") },
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
                        label = { Text(text = mainState.timeFilter.label) },
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.4f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            item {
                if (mainState.filterData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "无搜索结果", style = MaterialTheme.typography.bodyMedium)
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
                    .fillMaxHeight(), verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(2.dp))
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
                            append(FileUtil.getSizeString(file.size))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.Top),
                text = file.timestamp.toTimeUntilNow(),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

