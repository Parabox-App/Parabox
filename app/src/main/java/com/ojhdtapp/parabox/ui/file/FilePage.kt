package com.ojhdtapp.parabox.ui.file

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.accompanist.flowlayout.FlowRow
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.message.AreaState
import com.ojhdtapp.parabox.ui.message.ContactReadFilterState
import com.ojhdtapp.parabox.ui.setting.EditUserNameDialog
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.FileNavGraph
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ojhdtapp.parabox.ui.util.UserProfileDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    var searchBarState by remember {
        mutableStateOf(SearchAppBar.NONE)
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = true) {
        viewModel.onSearch("", true)
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is FilePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar((it.message))
                }
            }
        }
    }
    UserProfileDialog(
        openDialog = mainSharedViewModel.showUserProfileDialogState.value,
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
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
        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
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
                activateState = searchBarState,
                avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
                onActivateStateChanged = {
                    searchBarState = it
                    when (it) {
                        SearchAppBar.SEARCH -> viewModel.setArea(FilePageState.SEARCH_AREA)
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
                onDropdownMenuItemEvent = {}
            )
        },
        bottomBar = {

        }) {
        AnimatedContent(
            modifier = Modifier.padding(it), targetState = mainState.area,
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
                    onSetRecentFilter = { type, value -> viewModel.setRecentFilter(type, value) }
                )
                FilePageState.SEARCH_AREA -> SearchArea()
                else -> {}
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainArea(
    modifier: Modifier = Modifier,
    mainState: FilePageState,
    onSetRecentFilter: (type: Int, value: Boolean) -> Unit
) {
    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize(),
        columns = GridCells.Adaptive(352.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).animateItemPlacement()
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
                    mainAxisSpacing = 8.dp
                ) {
                    FilterChip(
                        modifier = Modifier
                            .animateContentSize()
                            .animateItemPlacement(),
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
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
                        border = FilterChipDefaults.filterChipBorder(borderColor = MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                if (mainState.data.isEmpty()) {
                    val context = LocalContext.current
                    val imageLoader = ImageLoader.Builder(context)
                        .components {
                            add(SvgDecoder.Factory())
                        }
                        .build()
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.empty_2)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(256.dp)
                            .padding(vertical = 16.dp)
                    )
                    Text(
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally),
                        text = "暂无可显示的文件",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).animateItemPlacement()
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "已连接服务",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SearchArea(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
    ) {

    }
}


