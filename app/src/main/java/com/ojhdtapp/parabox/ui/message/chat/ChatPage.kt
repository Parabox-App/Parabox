package com.ojhdtapp.parabox.ui.message.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DismissibleBottomSheet
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawerReverse
import com.ojhdtapp.parabox.ui.common.rememberMyDrawerState
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import com.ojhdtapp.parabox.ui.message.MessagePageEffect
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.parabox.ui.message.chat.top_bar.NormalChatTopBar
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    mainNavController: NavController,
    mainSharedState: MainSharedState,
    layoutType: MessageLayoutType,
    onEvent: (MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    Crossfade(
        targetState = layoutType == MessageLayoutType.SPLIT && !state.chatDetail.shouldDisplay,
        label = "chat_empty_normal_crossfade"
    ) {
        if (it) {
            EmptyChatPage(
                modifier = modifier,
                layoutType = layoutType,
            )
        } else {
            NormalChatPage(
                modifier = modifier,
                viewModel = viewModel,
                state = state,
                sharedState = mainSharedState,
                mainNavController = mainNavController,
                layoutType = layoutType,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent,
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    sharedState: MainSharedState,
    mainNavController: NavController,
    layoutType: MessageLayoutType,
    onEvent: (e: MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val drawerState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val lazyListState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 2
        }
    }
    val messageLazyPagingItems = viewModel.messagePagingDataFlow.collectAsLazyPagingItems()
    LaunchedEffect(messageLazyPagingItems.itemCount) {
        if (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset < 50) {
            lazyListState.animateScrollToItem(0)
        }
    }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = {
            state.chatDetail.imagePreviewerState.imageSnapshotList.size
        },
        getKey = {
            state.chatDetail.imagePreviewerState.imageSnapshotList.getOrNull(it)?.first ?: 0L
        }
    )
    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collectLatest {
            when (it) {
                is MessagePageEffect.ImagePreviewerOpenTransform -> {
                    if (previewerState.canOpen && it.index > -1) {
                        previewerState.openTransform(it.index)
                    }
                }

                else -> {}
            }
        }
    }
//    LaunchedEffect(key1 = state.chatDetail.imagePreviewerState.targetElementIndex, block = {
//        if (previewerState.canOpen && state.chatDetail.imagePreviewerState.targetElementIndex > -1) {
//            previewerState.openTransform(state.chatDetail.imagePreviewerState.targetElementIndex)
//        }
//    })
    LaunchedEffect(state.chatDetail.editAreaState.expanded) {
        if (state.chatDetail.editAreaState.expanded) {
            sheetState.open()
        } else {
            sheetState.close()
        }
    }
    // close edit area each time ime is visible
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(WindowInsets.isImeVisible) {
        if (imeVisible) {
            onEvent(MessagePageEvent.OpenEditArea(false))
        }
    }


    BackHandler(sheetState.isOpen) {
        coroutineScope.launch {
            sheetState.close()
        }
    }
    BackHandler(state.chatDetail.editAreaState.mode != EditAreaMode.NORMAL) {
        onEvent(MessagePageEvent.UpdateEditAreaMode(EditAreaMode.NORMAL))
    }
    BackHandler(drawerState.isOpen) {
        coroutineScope.launch {
            drawerState.close()
        }
    }
    BackHandler(state.chatDetail.selectedMessageList.isNotEmpty()) {
        onEvent(MessagePageEvent.ClearSelectedMessage)
    }

    MyModalNavigationDrawerReverse(
        drawerContent = {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .background(Color.Green)
            )
        },
        gesturesEnabled = state.chatDetail.shouldDisplay
                && state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording,
        drawerState = drawerState,
        drawerWidth = 360.dp,
    ) {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                NormalChatTopBar(chatDetail = state.chatDetail, scrollBehavior = scrollBehavior, onEvent = onEvent)
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { paddingValues ->
            val bottomPadding = animateDpAsState(
                targetValue = if (sheetState.isOpen || WindowInsets.isImeVisible) 0.dp else paddingValues.calculateBottomPadding(),
                label = "edit_area_bottom_padding"
            )
            val sheetHeight by remember(state.chatDetail.editAreaState) {
                derivedStateOf { if (state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER) 320.dp else 160.dp }
            }
            DismissibleBottomSheet(
                modifier = Modifier.imePadding(),
                sheetContent = {
                    Crossfade(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                        targetState = state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER,
                        label = "location toolbar"
                    ) {
                        if (it) {
                            LocationPicker(
                                modifier = Modifier.height(320.dp),
                                state = state.chatDetail.editAreaState,
                                onEvent = onEvent
                            )
                        } else {
                            Toolbar(
                                modifier = Modifier.height(160.dp),
                                state = state.chatDetail.editAreaState,
                                onEvent = onEvent
                            )
                        }
                    }
                },
                gesturesEnabled = state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording && !WindowInsets.isImeVisible,
                sheetHeight = sheetHeight, sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    Box(modifier = Modifier.weight(1f)) {
                        SelectionContainer(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = lazyListState,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                reverseLayout = true,
                            ) {
                                items(
                                    count = messageLazyPagingItems.itemCount,
                                    key = messageLazyPagingItems.itemKey { it.id }
                                ) {
                                    val item = messageLazyPagingItems[it]
                                    when (item) {
                                        is ChatPageUiModel.MessageWithSender -> {
                                            val before = messageLazyPagingItems.peek(it + 1)
                                            val after = if (it > 0) messageLazyPagingItems.peek(it - 1) else null
                                            MessageItem(
                                                state = state.chatDetail,
                                                messageWithSender = item,
                                                previewerState = previewerState,
                                                isFirst = !((before as? ChatPageUiModel.MessageWithSender)?.sender?.platformEqual(
                                                    item.sender
                                                ) ?: false),
                                                isLast = !((after as? ChatPageUiModel.MessageWithSender)?.sender?.platformEqual(
                                                    item.sender
                                                ) ?: false),
                                                onImageClick = { elementId ->
                                                    coroutineScope.launch {
                                                        var index = -1
                                                        val imageSnapshotList =
                                                            messageLazyPagingItems.itemSnapshotList.items.filterIsInstance<ChatPageUiModel.MessageWithSender>()
                                                                .map { it.message }
                                                                .fold(
                                                                    initial = mutableListOf<Pair<Long, ParaboxImage>>(),
                                                                    operation = { acc, message ->
                                                                        message.contents.forEachIndexed { mIndex, paraboxMessageElement ->
                                                                            if (paraboxMessageElement is ParaboxImage) {
                                                                                val mElementId =
                                                                                    message.contentsId[mIndex]
                                                                                if (elementId == mElementId) {
                                                                                    index = acc.size
                                                                                }
                                                                                acc.add(mElementId to paraboxMessageElement)
                                                                            }
                                                                        }
                                                                        acc
                                                                    }).reversed()
                                                        Log.d(
                                                            "parabox",
                                                            "image clicked;index: ${imageSnapshotList.size - index - 1}"
                                                        )
                                                        if (index > -1) {
                                                            onEvent(
                                                                MessagePageEvent.UpdateImagePreviewerSnapshotList(
                                                                    imageSnapshotList,
                                                                    imageSnapshotList.size - index - 1
                                                                )
                                                            )
                                                        }
                                                    }
                                                },
                                                onEvent = onEvent
                                            )
                                        }

                                        is ChatPageUiModel.Divider -> {
                                            TimeDivider(
                                                timestamp = item.timestamp
                                            )
                                        }

                                        else -> {

                                        }
                                    }
                                }
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            visible = fabExtended,
                            enter = slideInHorizontally { it * 2 },  // slide in from the right
                            exit = slideOutHorizontally { it * 2 } // slide out to the right
                        ) {
                            FloatingActionButton(
                                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                                onClick = {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                }) {
                                Icon(imageVector = Icons.Outlined.ArrowDownward, contentDescription = "to_latest")
                            }
                        }
                    }

                    EditArea(
                        modifier = Modifier
                            .padding(bottom = bottomPadding.value)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                        state = state.chatDetail.editAreaState,
                        onEvent = onEvent
                    )
                }
            }
        }
    }
    MyImagePreviewer(
        previewerState = previewerState,
        state = state.chatDetail.imagePreviewerState,
        onEvent = onEvent
    )
}