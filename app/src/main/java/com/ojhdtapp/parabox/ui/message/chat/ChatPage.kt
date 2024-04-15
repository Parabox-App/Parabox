package com.ojhdtapp.parabox.ui.message.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.DismissibleBottomSheet
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawerReverse
import com.ojhdtapp.parabox.ui.common.imeVisibleAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    mainSharedState: MainSharedState,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
    layoutType: MessageLayoutType,
    onEvent: (MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    if (layoutType == MessageLayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Crossfade(
                targetState = state.chatDetail.chat == null,
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
                        mainSharedState = mainSharedState,
                        scaffoldNavigator = scaffoldNavigator,
                        layoutType = layoutType,
                        onEvent = onEvent,
                        onMainSharedEvent = onMainSharedEvent,
                    )
                }

            }
        }
    } else {
        NormalChatPage(
            modifier = modifier,
            viewModel = viewModel,
            state = state,
            mainSharedState = mainSharedState,
            scaffoldNavigator = scaffoldNavigator,
            layoutType = layoutType,
            onEvent = onEvent,
            onMainSharedEvent = onMainSharedEvent,
        )
    }
}

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterialApi::class, ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    mainSharedState: MainSharedState,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
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
    val systemUiController = LocalSystemUiController.current
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
        if (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset < 100) {
            delay(50)
            lazyListState.animateScrollToItem(0)
        }
    }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = {
            state.chatDetail.imagePreviewerState.imageSnapshotList.size
        },
        getKey = {
            val imageItem = state.chatDetail.imagePreviewerState.imageSnapshotList.getOrNull(it)
            val elementId = imageItem?.elementId ?: 0L
            if (state.chatDetail.infoAreaState.expanded) {
                ("info_area_$elementId")
            } else {
                elementId
            }
        }
    )
    LaunchedEffect(previewerState.currentPage) {
        Log.d("parabox", "gettingIndex=${previewerState.currentPage}/${state.chatDetail.imagePreviewerState.imageSnapshotList.lastIndex}")
        if (state.chatDetail.imagePreviewerState.imageSnapshotList.isEmpty()) {
            delay(500)
            refreshImageSnapshotList(
                elementId = null,
                oldList = emptyList(),
                messageLazyPagingItems = messageLazyPagingItems,
                onEvent = onEvent
            )
        } else if (previewerState.currentPage == 0) {
//            val imagePreviewerItem = state.chatDetail.imagePreviewerState.imageSnapshotList.first()
//            messageLazyPagingItems.get(imagePreviewerItem.indexInPaging)
//            delay(100)
//            refreshImageSnapshotList(
//                elementId = imagePreviewerItem.elementId,
//                oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
//                messageLazyPagingItems = messageLazyPagingItems,
//                onEvent = onEvent
//            )
        } else if (previewerState.currentPage == state.chatDetail.imagePreviewerState.imageSnapshotList.lastIndex) {
            val imagePreviewerItem = state.chatDetail.imagePreviewerState.imageSnapshotList.last()
            messageLazyPagingItems.get(imagePreviewerItem.indexInPaging)
            delay(100)
            refreshImageSnapshotList(
                elementId = imagePreviewerItem.elementId,
                oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                messageLazyPagingItems = messageLazyPagingItems,
                onEvent = onEvent
            )
        }
    }
    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collectLatest {
            when (it) {
                is MessagePageEffect.ImagePreviewerOpenTransform -> {
                    if (it.index > -1) {
                        if (previewerState.canOpen) {
                            previewerState.openTransform(it.index)
                            if (layoutType == MessageLayoutType.NORMAL) {
                                systemUiController.setStatusBarColor(false)
                            }
                        } else {
                            previewerState.scrollToPage(it.index, 0f)
                        }
                    }
                }

                else -> {}
            }
        }
    }
    // sync latest sheet state with edit area state
    LaunchedEffect(sheetState.isOpen) {
        onEvent(MessagePageEvent.OpenEditArea(sheetState.isOpen))
    }
    LaunchedEffect(state.chatDetail.editAreaState) {
        if (state.chatDetail.editAreaState.expanded && !sheetState.isOpen) {
            sheetState.open()
        } else if (!state.chatDetail.editAreaState.expanded && sheetState.isOpen) {
            sheetState.close()
        }
    }
    // the same to drawer state
    LaunchedEffect(drawerState.isOpen) {
        onEvent(MessagePageEvent.OpenInfoArea(drawerState.isOpen))
    }
    LaunchedEffect(state.chatDetail.infoAreaState) {
        if (state.chatDetail.infoAreaState.expanded && !drawerState.isOpen) {
            drawerState.open()
        } else if (!state.chatDetail.infoAreaState.expanded && drawerState.isOpen) {
            drawerState.close()
        }
    }
    // close edit area each time ime is visible
    val imeVisible by imeVisibleAsState()
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            onEvent(MessagePageEvent.OpenEditArea(false))
        }
    }
    BackHandler(layoutType == MessageLayoutType.NORMAL) {
        scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.List)
        onMainSharedEvent(MainSharedEvent.ShowNavigationBar(true))
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
            ) {
                if (state.chatDetail.chat != null) {
                    InfoArea(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 24.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 24.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        infoAreaState = state.chatDetail.infoAreaState,
                        previewerState = previewerState,
                        imageSnapshotList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                        onImageClick = { elementId ->
                            coroutineScope.launch {
                                refreshImageSnapshotList(
                                    elementId = elementId,
                                    oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                                    messageLazyPagingItems = messageLazyPagingItems,
                                    onEvent = onEvent
                                )
                            }
                        },
                        onEvent = onEvent
                    )
                }
            }
        },
        gesturesEnabled = state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording,
        drawerState = drawerState,
        drawerWidth = 360.dp,
    ) {
        Scaffold(
            topBar = {
                NormalChatTopBar(
                    chatDetail = state.chatDetail,
                    layoutType = layoutType,
                    shouldDisplayAvatar = mainSharedState.datastore.displayAvatarOnTopAppBar,
                    onNavigateBack = {
                        if (state.chatDetail.selectedMessageList.isNotEmpty()) {
                            onEvent(MessagePageEvent.ClearSelectedMessage)
                        } else {
                            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.List)
                            onMainSharedEvent(MainSharedEvent.ShowNavigationBar(true))
                        }
                    }, onEvent = onEvent
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { paddingValues ->
            val bottomPadding by remember {
                derivedStateOf {
                    val bottomPaddingValues = paddingValues.calculateBottomPadding()
                    if (imeVisible) {
                        0.dp
                    } else {
                        when (sheetState.swipeableState.progress.to) {
                            DrawerValue.Closed -> {
                                sheetState.swipeableState.progress.fraction * bottomPaddingValues
                            }

                            DrawerValue.Open -> {
                                bottomPaddingValues * (1 - sheetState.swipeableState.progress.fraction)
                            }
                        }
                    }
                }
            }
            val sheetHeight by remember(state.chatDetail.editAreaState) {
                derivedStateOf { if (state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER) 320.dp else 160.dp }
            }
            DismissibleBottomSheet(
                sheetContent = {
                    Crossfade(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                                shouldDisplayTime = mainSharedState.datastore.displayTimeOnEachMsg,
                                                onImageClick = { elementId ->
                                                    coroutineScope.launch {
                                                        refreshImageSnapshotList(
                                                            elementId = elementId,
                                                            oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                                                            messageLazyPagingItems = messageLazyPagingItems,
                                                            onEvent = onEvent
                                                        )
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
                            .padding(bottom = bottomPadding)
                            .imePadding()
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
        layoutType = layoutType,
        onEvent = onEvent
    )
}

suspend fun refreshImageSnapshotList(
    elementId: Long?,
    oldList: List<MessagePageState.ImagePreviewerState.ImagePreviewerItem>,
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel>,
    onEvent: (MessagePageEvent) -> Unit
) {
    val newList = oldList.toMutableList().apply {
        messageLazyPagingItems.itemSnapshotList.items.forEachIndexed { index, chatPageUiModel ->
            if (chatPageUiModel is ChatPageUiModel.MessageWithSender) {
                chatPageUiModel.message.contents.forEachIndexed { mIndex, paraboxMessageElement ->
                    if (paraboxMessageElement is ParaboxImage) {

                        val mElementId =
                            chatPageUiModel.message.contentsId[mIndex]
                        if (this.find { it.elementId == mElementId } == null) {
                            Log.d(
                                "parabox",
                                "image add;elementId = ${mElementId};indexOfPaging=${index}"
                            )
                            add(
                                MessagePageState.ImagePreviewerState.ImagePreviewerItem(
                                    image = paraboxMessageElement,
                                    elementId = mElementId,
                                    indexInPaging = index
                                )
                            )
                        }
                    }
                }
            }
        }
    }.sortedBy { it.indexInPaging }
    val targetElementIndex = newList.indexOfFirst { it.elementId == elementId }
    Log.d(
        "parabox",
        "image clicked;index: ${targetElementIndex}"
    )
    onEvent(
        MessagePageEvent.UpdateImagePreviewerSnapshotList(
            newList,
            targetElementIndex
        )
    )
}