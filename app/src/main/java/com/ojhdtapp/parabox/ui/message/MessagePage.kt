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
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.saket.swipe.rememberSwipeableActionsState


@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    mainSharedViewModel: MainSharedViewModel,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
    layoutType: MessageLayoutType,
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val snackBarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
    var snackBarJob: Job? by remember {
        mutableStateOf(null)
    }
    val listState = rememberLazyListState()
    val horizontalPadding by animateDpAsState(
        targetValue = when (layoutType) {
            MessageLayoutType.NORMAL -> 16.dp
            MessageLayoutType.SPLIT -> 2.dp
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
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

                    else -> {}
                }
            }
    }
    LaunchedEffect(Unit) {
        mainSharedViewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    is MainSharedEffect.PageListScrollBy -> {
                        coroutineScope.launch {
                            if (!listState.canScrollForward) {
                                listState.animateScrollToItem(0)
                            } else {
                                listState.animateScrollBy(1000f)
                            }
                        }
                    }

                    else -> {}
                }
            }
    }
    val chatLazyPagingData = viewModel.chatPagingDataFlow.collectAsLazyPagingItems()
    val pinnedChatLazyPagingData = viewModel.pinnedChatPagingDataFlow.collectAsLazyPagingItems()
    val searchBarPadding by animateDpAsState(
        targetValue = if (mainSharedState.search.isActive || layoutType == MessageLayoutType.SPLIT) 0.dp else 16.dp,
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
    val menuPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pathmorph_drawer_hamburger_to_arrow),
        atEnd = mainSharedState.search.isActive
    )

    EnabledChatFilterDialog(
        openDialog = state.openEnabledChatFilterDialog,
        enabledList = mainSharedState.datastore.enabledChatFilterList,
        onConfirm = {
            mainSharedViewModel.sendEvent(MainSharedEvent.UpdateEnabledChatFilterList(it))
            viewModel.sendEvent(MessagePageEvent.UpdateSelectedChatFilter(
                state.selectedChatFilterLists.toMutableList().apply {
                    retainAll(it)
                    if (isEmpty()) {
                        add(ChatFilter.Normal)
                    }
                }
            ))
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
        snackbarHost = { SnackbarHost(modifier = Modifier.offset(y = 80.dp), hostState = snackBarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            if (layoutType == MessageLayoutType.SPLIT) {
                DockedSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .statusBarsPadding()
                        .clearFocusOnKeyboardDismiss(),
                    query = mainSharedState.search.query,
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
                    active = mainSharedState.search.isActive,
                    onActiveChange = { mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (mainSharedState.search.isActive) {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(!mainSharedState.openDrawer.open))
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
                            visible = !mainSharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { mainSharedViewModel.sendEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonAvatar(
                                        model = CommonAvatarModel(
                                            model = mainSharedState.datastore.localAvatarUri,
                                            name = mainSharedState.datastore.localName,
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        textColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = mainSharedState, onEvent = mainSharedViewModel::sendEvent)
                }
            } else {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = searchBarPadding)
                        .clearFocusOnKeyboardDismiss(),
                    query = mainSharedState.search.query,
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
                    active = mainSharedState.search.isActive,
                    onActiveChange = { mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (mainSharedState.search.isActive) {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(!mainSharedState.openDrawer.open))
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
                            visible = !mainSharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { mainSharedViewModel.sendEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonAvatar(
                                        model = CommonAvatarModel(
                                            model = mainSharedState.datastore.localAvatarUri,
                                            name = mainSharedState.datastore.localName,
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        textColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = mainSharedState, onEvent = mainSharedViewModel::sendEvent)
                }
            }

        }) {
        LazyColumn(
            contentPadding = it,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(
                contentType = Unit
            ) {
                Column {
                    if (pinnedChatLazyPagingData.itemCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = horizontalPadding, end = horizontalPadding, top = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = "置顶",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyRow(
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = horizontalPadding)
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
                                    onClick = {
                                        viewModel.sendEvent(MessagePageEvent.LoadMessage(item))
                                        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                        mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(false))
                                    },
                                    onLongClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isMenuVisible = true
                                    },
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = horizontalPadding, end = horizontalPadding, top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.main),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = 16.dp
                        )
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
                        items(items = mainSharedState.datastore.enabledChatFilterList) {
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
            }
            items(
                count = chatLazyPagingData.itemCount,
                key = chatLazyPagingData.itemKey { it.chat.chatId },
                contentType = chatLazyPagingData.itemContentType { "chat" }
            ) { index ->
                val topRadius = if (index == 0) 24.dp else 3.dp
                val bottomRadius = if (index == chatLazyPagingData.itemSnapshotList.size - 1) 24.dp else 3.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomEnd = bottomRadius,
                                bottomStart = bottomRadius
                            )
                        )
                ) {
                    val item = chatLazyPagingData[index]
                    if (item == null) {
                        EmptyChatItem()
                    } else {
                        var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                        ChatDropdownMenu(
                            chat = item.chat,
                            isMenuVisible = isMenuVisible,
                            onEvent = viewModel::sendEvent,
                            onDismiss = { isMenuVisible = false })
                        SwipeToDismissBox(
                            enabled = mainSharedState.datastore.enableSwipeToDismiss,
                            onVibrate = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                            startToEndIcon = if (item.chat.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            endToStartIcon = Icons.Outlined.Done,
                            onDismissedToEnd = {
                                viewModel.sendEvent(
                                    MessagePageEvent.UpdateChatArchive(
                                        item.chat.chatId,
                                        !item.chat.isArchived,
                                        item.chat.isArchived
                                    )
                                )
                            },
                            onDismissedToStart = {
                                viewModel.sendEvent(
                                    MessagePageEvent.UpdateChatHide(
                                        item.chat.chatId,
                                        true,
                                        item.chat.isHidden
                                    )
                                )
                            }) {
                            LaunchedEffect(key1 = item.message, block = {
                                if (item.message?.senderId?.let {
                                        !state.chatLatestMessageSenderCache.containsKey(it)
                                    } == true) {
                                    viewModel.sendEvent(MessagePageEvent.QueryLatestMessageSenderOfChatWithCache(item.message.senderId))
                                }
                            })
                            ChatItem(
                                chatWithLatestMessage = item,
                                contact = state.chatLatestMessageSenderCache[item.message?.senderId]
                                    ?: Resource.Loading(),
                                isEditing = state.chatDetail.chat?.chatId == item.chat.chatId,
                                isExpanded = layoutType == MessageLayoutType.SPLIT,
                                enableMarqueeEffectOnChatName = mainSharedState.datastore.enableMarqueeEffectOnChatName,
                                onClick = {
                                    viewModel.sendEvent(MessagePageEvent.LoadMessage(item.chat))
                                    scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(false))
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
            item {
                if (layoutType == MessageLayoutType.NORMAL) {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}