@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.AvatarUtil.getCircledBitmap
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.saket.swipe.rememberSwipeableActionsState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    layoutType: MessageLayoutType,
    windowSize: WindowSizeClass,
    ) {

    val viewModel = hiltViewModel<MessagePageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val snackBarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()
    val sharedState by mainSharedViewModel.uiState.collectAsState()
    var snackBarJob: Job? by remember {
        mutableStateOf(null)
    }
    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    is MessagePageEffect.ShowSnackBar -> {
                        snackBarJob?.cancel()
                        snackBarJob = launch {
                            delay(4000)
                            snackBarHostState.currentSnackbarData?.dismiss()
                        }

                        snackBarHostState.showSnackbar(it.message, it.label).also { result ->
                            when (result) {
                                SnackbarResult.ActionPerformed -> {
                                    launch(Dispatchers.IO) {
                                        it.callback?.invoke()
                                    }
                                }

                                SnackbarResult.Dismissed -> {}
                                else -> {}
                            }
                        }
                    }
                }
            }
    }
    val chatLazyPagingData = state.chatPagingDataFlow.collectAsLazyPagingItems()
    val pinnedChatLazyPagingData = state.pinnedChatPagingDataFlow.collectAsLazyPagingItems()
    val searchBarPadding by animateDpAsState(
        targetValue = if (sharedState.search.isActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    val shouldHoverSearchBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }
    val searchBarShadowElevation by animateDpAsState(
        targetValue = if (shouldHoverSearchBar) 3.dp else 0.dp
    )
    val menuState by remember {
        derivedStateOf {
            sharedState.search.isActive
        }
    }
    val menuPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pathmorph_drawer_hamburger_to_arrow),
        atEnd = menuState
    )

    EnabledChatFilterDialog(
        openDialog = state.openEnabledChatFilterDialog,
        enabledList = state.enabledChatFilterList,
        onConfirm = {
            viewModel.sendEvent(MessagePageEvent.UpdateEnabledChatFilterList(it))
            viewModel.sendEvent(MessagePageEvent.OpenEnabledChatFilterDialog(false))
        },
        onDismiss = {
            viewModel.sendEvent(
                MessagePageEvent.OpenEnabledChatFilterDialog(
                    false
                )
            )
        }
    )

    EditChatTagsDialog(
        openDialog = state.editingChatTags != null,
        tags = state.editingChatTags?.tags ?: emptyList(),
        onConfirm = {
            viewModel.sendEvent(
                MessagePageEvent.UpdateChatTags(
                    state.editingChatTags!!.chatId,
                    it,
                    state.editingChatTags!!.tags
                )
            )
            viewModel.sendEvent(MessagePageEvent.UpdateEditingChatTags(null))
        },
        onDismiss = {
            viewModel.sendEvent(MessagePageEvent.UpdateEditingChatTags(null))
        })

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            if(windowSize.widthSizeClass == WindowWidthSizeClass.Expanded){
                DockedSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .statusBarsPadding()
                        .clearFocusOnKeyboardDismiss(),
                    query = sharedState.search.query,
                    onQueryChange = {
                        mainSharedViewModel.sendEvent(
                            MainSharedEvent.QueryInput(it)
                        )
                    },
                    onSearch = {
                        if (it.isNotBlank()) {
                            mainSharedViewModel.sendEvent(MainSharedEvent.SearchConfirm(it))
                        }
                    },
                    active = sharedState.search.isActive,
                    onActiveChange = { mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (sharedState.search.isActive) {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(!sharedState.openDrawer.open))
                                }
                            }
                        ) {
                            Image(
                                painter = menuPainter, contentDescription = "drawer",
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = !sharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { mainSharedViewModel.sendEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.size(30.dp),
                                    model = sharedState.datastore.localAvatarUri,
                                    contentDescription = "user_avatar",
                                ) {
                                    val state = painter.state
                                    val namedAvatarBm =
                                        AvatarUtil.createNamedAvatarBm(
                                            backgroundColor = MaterialTheme.colorScheme.primary.toArgb(),
                                            textColor = MaterialTheme.colorScheme.onPrimary.toArgb(),
                                            name = sharedState.datastore.localName
                                        ).getCircledBitmap().asImageBitmap()
                                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                        Image(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .placeholder(
                                                    visible = state is AsyncImagePainter.State.Loading,
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    highlight = PlaceholderHighlight.fade(),
                                                ),
                                            bitmap = namedAvatarBm,
                                            contentDescription = "named_avatar"
                                        )
                                    } else {
                                        SubcomposeAsyncImageContent()
                                    }
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = sharedState, onEvent = mainSharedViewModel::sendEvent)
                }
            } else {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = searchBarPadding)
                        .clearFocusOnKeyboardDismiss(),
                    query = sharedState.search.query,
                    onQueryChange = {
                        mainSharedViewModel.sendEvent(
                            MainSharedEvent.QueryInput(it)
                        )
                    },
                    onSearch = {
                        if (it.isNotBlank()) {
                            mainSharedViewModel.sendEvent(MainSharedEvent.SearchConfirm(it))
                        }
                    },
                    active = sharedState.search.isActive,
                    onActiveChange = { mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (sharedState.search.isActive) {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(!sharedState.openDrawer.open))
                                }
                            }
                        ) {
                            Image(
                                painter = menuPainter, contentDescription = "drawer",
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = !sharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { mainSharedViewModel.sendEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                SubcomposeAsyncImage(
                                    modifier = Modifier.size(30.dp),
                                    model = sharedState.datastore.localAvatarUri,
                                    contentDescription = "user_avatar",
                                ) {
                                    val state = painter.state
                                    val namedAvatarBm =
                                        AvatarUtil.createNamedAvatarBm(
                                            backgroundColor = MaterialTheme.colorScheme.primary.toArgb(),
                                            textColor = MaterialTheme.colorScheme.onPrimary.toArgb(),
                                            name = sharedState.datastore.localName
                                        ).getCircledBitmap().asImageBitmap()
                                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                        Image(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .placeholder(
                                                    visible = state is AsyncImagePainter.State.Loading,
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    highlight = PlaceholderHighlight.fade(),
                                                ),
                                            bitmap = namedAvatarBm,
                                            contentDescription = "named_avatar"
                                        )
                                    } else {
                                        SubcomposeAsyncImageContent()
                                    }
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = sharedState, onEvent = mainSharedViewModel::sendEvent)
                }
            }

        }) { it ->
        LazyColumn(
            contentPadding = it,
            state = listState,
        ) {
            item(key = "pinned") {
                AnimatedVisibility(
                    visible = pinnedChatLazyPagingData.itemCount > 0,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "置顶",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = pinnedChatLazyPagingData.itemCount > 0,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(
                            count = pinnedChatLazyPagingData.itemCount,
                            key = pinnedChatLazyPagingData.itemKey { it.chatId },
                            contentType = pinnedChatLazyPagingData.itemContentType { "pinned_chat" }
                        ) { index ->
                            val item = pinnedChatLazyPagingData[index]!!
                            var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                            ChatDropdownMenu(
                                chat = item,
                                isMenuVisible = isMenuVisible,
                                onEvent = viewModel::sendEvent,
                                onDismiss = { isMenuVisible = false })
                            PinnedChatItems(
                                modifier = Modifier.animateItemPlacement(),
                                chat = item,
                                onClick = {},
                                onLongClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isMenuVisible = true
                                },
                            )
                        }
                    }
                }
            }
            item(key = "main") {
                AnimatedVisibility(
                    visible = pinnedChatLazyPagingData.itemCount > 0,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.main),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        MyFilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = false, label = {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "filter"
                                )
                            }) {
                            viewModel.sendEvent(MessagePageEvent.OpenEnabledChatFilterDialog(true))
                        }
                    }
                    item {
                        if (state.selectedChatFilterLists.contains(ChatFilter.Normal)) {
                            MyFilterChip(
                                modifier = Modifier.padding(end = 8.dp),
                                selected = false,
                                label = { Text(text = stringResource(id = R.string.get_chat_filter_normal)) }) {
                            }
                        }
                    }
                    items(items = state.enabledChatFilterList) {
                        MyFilterChip(selected = it in state.selectedChatFilterLists,
                            modifier = Modifier.padding(end = 8.dp),
                            label = { Text(text = it.label ?: stringResource(id = it.labelResId)) }) {
                            viewModel.sendEvent(
                                MessagePageEvent.AddOrRemoveSelectedChatFilter(
                                    it
                                )
                            )
                        }
                    }
                }
            }
//            if (chatLazyPagingData.loadState.refresh == LoadState.Loading) {
//                items(12) {
//                    EmptyChatItem(
//                        modifier = Modifier
//                            .padding(start = 16.dp, end = 16.dp),
//                        isFirst = it == 0,
//                        isLast = it == 11
//                    )
//                }
//            }
            items(
                count = chatLazyPagingData.itemCount,
                key = chatLazyPagingData.itemKey { it.chat.chatId },
                contentType = chatLazyPagingData.itemContentType { "chat" }
            ) { index ->
                val swipeableActionsState = rememberSwipeableActionsState()
                val isFirst = index == 0
                val isLast = index == chatLazyPagingData.itemCount - 1
                val topRadius by animateDpAsState(
                    targetValue = if (isFirst && swipeableActionsState.offset.value == 0f) 24.dp else 3.dp, label = "top_radius"
                )
                val bottomRadius by animateDpAsState(
                    targetValue = if (isLast && swipeableActionsState.offset.value == 0f) 24.dp else 3.dp, label = "bottom_radius"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomEnd = bottomRadius,
                                bottomStart = bottomRadius
                            )
                        )
                        .animateItemPlacement()
                ) {
                    if (chatLazyPagingData[index] == null) {
                        EmptyChatItem(
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    } else {
                        val item = chatLazyPagingData[index]!!
                        var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                        ChatDropdownMenu(
                            chat = item.chat,
                            isMenuVisible = isMenuVisible,
                            onEvent = viewModel::sendEvent,
                            onDismiss = { isMenuVisible = false })
                        SwipeableActionsDismissBox(
                            enabled = state.datastore.enableSwipeToDismiss,
                            state = swipeableActionsState,
                            threshold = 72.dp,
                            onReachThreshold = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                            startToEndIcon = Icons.Outlined.Archive,
                            endToStartIcon = Icons.Outlined.Done,
                            onDismissedToEnd = {
                                viewModel.sendEvent(MessagePageEvent.UpdateChatArchive(item.chat.chatId, true, item.chat.isArchived))
                            },
                            onDismissedToStart = {
                                viewModel.sendEvent(MessagePageEvent.UpdateChatHide(item.chat.chatId, true, item.chat.isHidden))
                            }) {
                            val contact by viewModel.getLatestMessageSenderWithCache(
                                chatLazyPagingData[index]?.message?.senderId
                            ).collectAsState(initial = Resource.Loading())
                            ChatItem(
                                modifier = Modifier.padding(bottom = 2.dp),
                                chatWithLatestMessage = item,
                                contact = contact,
                                isEditing = state.currentChat?.chatId == item.chat.chatId,
                                onClick = {
                                    viewModel.sendEvent(MessagePageEvent.LoadMessage(item.chat))
                                },
                                onLongClick = {
                                    isMenuVisible = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }

                }
            }
        }
    }
}