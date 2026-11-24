package com.ojhdtapp.parabox.ui.message.chat

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.DismissibleBottomSheet
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.PredictiveBackDefaults
import com.ojhdtapp.parabox.ui.common.imeVisibleAsState
import com.ojhdtapp.parabox.ui.common.rememberDismissibleBottomSheetState
import com.ojhdtapp.parabox.ui.message.MessagePageEffect
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.parabox.ui.message.chat.top_bar.NormalChatTopBar
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.VerticalDragType
import com.origeek.imageViewer.previewer.rememberPreviewerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun rememberChatImagePreviewerState(
    state: MessagePageState,
): ImagePreviewerState {
    return rememberPreviewerState(
        verticalDragType = VerticalDragType.UpAndDown,
        pageCount = {
            state.chatDetail.imagePreviewerState.imageSnapshotList.size
        },
        getKey = {
            val imageItem = state.chatDetail.imagePreviewerState.imageSnapshotList.getOrNull(it)
            val elementId = imageItem?.elementId ?: 0L
            if (state.chatDetail.infoAreaState.expanded) {
                "info_area_$elementId"
            } else {
                elementId
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    previewerState: ImagePreviewerState,
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel>,
    onNavigateBack: () -> Unit,
    onToggleInfoArea: () -> Unit,
    onEvent: (MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    if (layoutType == LayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Crossfade(
                targetState = state.chatDetail.chat == null,
                label = "chat_empty_normal_crossfade",
            ) { isEmpty ->
                if (isEmpty) {
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
                        layoutType = layoutType,
                        previewerState = previewerState,
                        messageLazyPagingItems = messageLazyPagingItems,
                        renderPreviewerOverlay = false,
                        handleInfoAreaInline = false,
                        onNavigateBack = onNavigateBack,
                        onToggleInfoArea = onToggleInfoArea,
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
            layoutType = layoutType,
            previewerState = previewerState,
            messageLazyPagingItems = messageLazyPagingItems,
            renderPreviewerOverlay = false,
            handleInfoAreaInline = false,
            onNavigateBack = onNavigateBack,
            onToggleInfoArea = onToggleInfoArea,
            onEvent = onEvent,
            onMainSharedEvent = onMainSharedEvent,
        )
    }
}

/**
 * Extra pane for Chat Info rendered by Navigation 3 [androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.extraPane].
 */
@Composable
fun ChatInfoExtraPane(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    layoutType: LayoutType,
    previewerState: ImagePreviewerState,
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel>,
    onClose: () -> Unit,
    onEvent: (MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val containerModifier = if (layoutType == LayoutType.SPLIT) {
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    } else {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    }

    Box(modifier = containerModifier) {
        if (state.chatDetail.chat != null) {
            InfoArea(
                modifier = Modifier.fillMaxSize(),
                contactPagingDataFlow = viewModel.contactPagingDataFlow,
                infoAreaState = state.chatDetail.infoAreaState,
                previewerState = previewerState,
                imageSnapshotList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                onImageClick = { _, elementId ->
                    coroutineScope.launch {
                        refreshImageSnapshotList(
                            elementId = elementId,
                            oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                            messageLazyPagingItems = messageLazyPagingItems,
                            onEvent = onEvent,
                        )
                    }
                },
                onLazyGridEndReached = {
                    coroutineScope.launch {
                        val oldList = state.chatDetail.imagePreviewerState.imageSnapshotList
                        oldList.lastOrNull()?.indexInPaging?.let {
                            if (it < messageLazyPagingItems.itemCount) {
                                messageLazyPagingItems[it]
                            } else if (messageLazyPagingItems.itemCount > 0) {
                                messageLazyPagingItems[messageLazyPagingItems.itemCount - 1]
                                Log.d("parabox", "$it/${messageLazyPagingItems.itemCount} index out of bound")
                            }
                        }
                        delay(200)
                        refreshImageSnapshotList(
                            elementId = null,
                            oldList = oldList,
                            messageLazyPagingItems = messageLazyPagingItems,
                            onEvent = onEvent,
                        )
                    }
                },
                onShowContactDetail = {
                    onMainSharedEvent(MainSharedEvent.ShowContactDetailDialog(it))
                },
                onClose = onClose,
                onEvent = onEvent,
            )
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    previewerState: ImagePreviewerState = rememberChatImagePreviewerState(state),
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel> = viewModel.messagePagingDataFlow.collectAsLazyPagingItems(),
    renderPreviewerOverlay: Boolean = true,
    handleInfoAreaInline: Boolean = true,
    onNavigateBack: () -> Unit = {},
    onToggleInfoArea: (() -> Unit)? = null,
    onEvent: (e: MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemUiController = LocalSystemUiController.current
    val sheetState = rememberDismissibleBottomSheetState(initialValue = DrawerValue.Closed)
    val lazyListState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 2
        }
    }

    LaunchedEffect(messageLazyPagingItems.itemCount) {
        if (lazyListState.firstVisibleItemIndex == 1 && lazyListState.firstVisibleItemScrollOffset < 100) {
            delay(50)
            lazyListState.animateScrollToItem(0)
        }
    }

    if (renderPreviewerOverlay) {
        ChatImagePreviewerEffectHost(
            viewModel = viewModel,
            state = state,
            layoutType = layoutType,
            previewerState = previewerState,
            messageLazyPagingItems = messageLazyPagingItems,
            onEvent = onEvent,
        )
    }

    // Sync inline bottom toolbar state with ViewModel state
    LaunchedEffect(sheetState.isOpen) {
        if (state.chatDetail.editAreaState.expanded != sheetState.isOpen) {
            onEvent(MessagePageEvent.OpenEditArea(sheetState.isOpen))
        }
    }
    LaunchedEffect(state.chatDetail.editAreaState.expanded) {
        if (state.chatDetail.editAreaState.expanded && !sheetState.isOpen) {
            sheetState.open()
        } else if (!state.chatDetail.editAreaState.expanded && sheetState.isOpen) {
            sheetState.close()
        }
    }

    // Close edit toolbar whenever IME opens
    val imeVisible by imeVisibleAsState()
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            onEvent(MessagePageEvent.OpenEditArea(false))
        }
    }

    // Single priority-ordered transient back handler (never shadows route navigation or sheets/previewers)
    val transientBackAction: (() -> Unit)? = when {
        state.chatDetail.selectedMessageList.isNotEmpty() -> {
            { onEvent(MessagePageEvent.ClearSelectedMessage) }
        }
        state.chatDetail.editAreaState.mode != EditAreaMode.NORMAL -> {
            { onEvent(MessagePageEvent.UpdateEditAreaMode(EditAreaMode.NORMAL)) }
        }
        state.chatDetail.editAreaState.chosenQuoteReply != null -> {
            { onEvent(MessagePageEvent.ChooseQuoteReply(null)) }
        }
        else -> null
    }
    BackHandler(enabled = transientBackAction != null) {
        transientBackAction?.invoke()
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                            onNavigateBack()
                        }
                    },
                    onToggleInfoArea = {
                        if (onToggleInfoArea != null) {
                            onToggleInfoArea()
                        } else {
                            onEvent(MessagePageEvent.OpenInfoArea(!state.chatDetail.infoAreaState.expanded))
                        }
                    },
                    onEvent = onEvent,
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) { paddingValues ->
            val bottomPadding by remember(imeVisible) {
                derivedStateOf {
                    if (imeVisible) {
                        0.dp
                    } else {
                        paddingValues.calculateBottomPadding() * (1f - sheetState.openFraction)
                    }
                }
            }
            val sheetHeight by remember(state.chatDetail.editAreaState.mode) {
                derivedStateOf {
                    if (state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER) 320.dp else 160.dp
                }
            }
            DismissibleBottomSheet(
                sheetContent = {
                    Crossfade(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                        targetState = state.chatDetail.editAreaState.mode == EditAreaMode.LOCATION_PICKER,
                        label = "location toolbar",
                    ) { isLocationPicker ->
                        if (isLocationPicker) {
                            LocationPicker(
                                modifier = Modifier.height(320.dp),
                                state = state.chatDetail.editAreaState,
                                onEvent = onEvent,
                            )
                        } else {
                            Toolbar(
                                modifier = Modifier.height(160.dp),
                                state = state.chatDetail.editAreaState,
                                onEvent = onEvent,
                            )
                        }
                    }
                },
                gesturesEnabled = state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording && !WindowInsets.isImeVisible,
                predictiveBackEnabled = transientBackAction == null && !previewerState.canClose,
                sheetHeight = sheetHeight,
                sheetState = sheetState,
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
                                    key = messageLazyPagingItems.itemKey { it.id },
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
                                                    item.sender,
                                                ) ?: false),
                                                isLast = !((after as? ChatPageUiModel.MessageWithSender)?.sender?.platformEqual(
                                                    item.sender,
                                                ) ?: false),
                                                shouldDisplayTime = mainSharedState.datastore.displayTimeOnEachMsg,
                                                onImageClick = { elementId ->
                                                    coroutineScope.launch {
                                                        refreshImageSnapshotList(
                                                            elementId = elementId,
                                                            oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                                                            messageLazyPagingItems = messageLazyPagingItems,
                                                            onEvent = onEvent,
                                                        )
                                                    }
                                                },
                                                onAvatarClick = {
                                                    onMainSharedEvent(MainSharedEvent.LoadContactDetailDialog(item.sender.contactId))
                                                },
                                                onAvatarLongClick = {},
                                                onEvent = onEvent,
                                            )
                                        }

                                        is ChatPageUiModel.Divider -> {
                                            TimeDivider(timestamp = item.timestamp)
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            visible = fabExtended,
                            enter = slideInHorizontally { it * 2 },
                            exit = slideOutHorizontally { it * 2 },
                        ) {
                            FloatingActionButton(
                                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                                onClick = {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                },
                            ) {
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
                        onEvent = onEvent,
                    )
                }
            }
        }

        // Inline fallback for BubbleActivity (standalone single-page host without ListDetailSceneStrategy)
        if (handleInfoAreaInline) {
            val inlineBackProgress = remember { Animatable(0f) }
            PredictiveBackHandler(enabled = state.chatDetail.infoAreaState.expanded && !previewerState.canClose) { backEvents ->
                try {
                    backEvents.collect { event ->
                        inlineBackProgress.snapTo(PredictiveBackDefaults.Easing.transform(event.progress))
                    }
                    onEvent(MessagePageEvent.OpenInfoArea(false))
                    inlineBackProgress.snapTo(0f)
                } catch (e: CancellationException) {
                    inlineBackProgress.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
            AnimatedVisibility(
                visible = state.chatDetail.infoAreaState.expanded,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
            ) {
                val p = inlineBackProgress.value
                ChatInfoExtraPane(
                    modifier = Modifier.graphicsLayer {
                        val scale = lerp(1f, 0.92f, p)
                        scaleX = scale
                        scaleY = scale
                        translationX = size.width * 0.15f * p
                    },
                    viewModel = viewModel,
                    state = state,
                    layoutType = layoutType,
                    previewerState = previewerState,
                    messageLazyPagingItems = messageLazyPagingItems,
                    onClose = { onEvent(MessagePageEvent.OpenInfoArea(false)) },
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent,
                )
            }
        }

        if (renderPreviewerOverlay) {
            MyImagePreviewer(
                previewerState = previewerState,
                state = state.chatDetail,
                layoutType = layoutType,
                onEvent = onEvent,
            )
        }
    }
}

/**
 * Shared effect host for opening and paging images inside [MyImagePreviewer],
 * lifted above both [ChatPage] (detailPane) and [ChatInfoExtraPane] (extraPane).
 */
@Composable
fun ChatImagePreviewerEffectHost(
    viewModel: MessagePageViewModel,
    state: MessagePageState,
    layoutType: LayoutType,
    previewerState: ImagePreviewerState,
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel>,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemUiController = LocalSystemUiController.current

    LaunchedEffect(previewerState.currentPage) {
        if (previewerState.canClose) {
            Log.d(
                "parabox",
                "scroll gettingIndex=${previewerState.currentPage}/${state.chatDetail.imagePreviewerState.imageSnapshotList.lastIndex}",
            )
            if (previewerState.currentPage == state.chatDetail.imagePreviewerState.imageSnapshotList.lastIndex &&
                state.chatDetail.imagePreviewerState.imageSnapshotList.isNotEmpty()
            ) {
                val imagePreviewerItem = state.chatDetail.imagePreviewerState.imageSnapshotList.last()
                if (imagePreviewerItem.indexInPaging < messageLazyPagingItems.itemCount) {
                    messageLazyPagingItems[imagePreviewerItem.indexInPaging]
                } else if (messageLazyPagingItems.itemCount > 0) {
                    messageLazyPagingItems[messageLazyPagingItems.itemCount - 1]
                    Log.d(
                        "parabox",
                        "${imagePreviewerItem.indexInPaging}/${messageLazyPagingItems.itemCount} index out of bound",
                    )
                }
                delay(200)
                refreshImageSnapshotList(
                    elementId = imagePreviewerItem.elementId,
                    oldList = state.chatDetail.imagePreviewerState.imageSnapshotList,
                    messageLazyPagingItems = messageLazyPagingItems,
                    onEvent = onEvent,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collectLatest {
            when (it) {
                is MessagePageEffect.ImagePreviewerOpenTransform -> {
                    if (it.index > -1) {
                        if (previewerState.canOpen) {
                            previewerState.openTransform(it.index)
                            if (layoutType == LayoutType.NORMAL) {
                                systemUiController.setStatusBarColor(false)
                            }
                        } else {
                            previewerState.scrollToPage(it.index, 0f)
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

suspend fun refreshImageSnapshotList(
    elementId: Long?,
    oldList: List<MessagePageState.ImagePreviewerState.ImagePreviewerItem>,
    messageLazyPagingItems: LazyPagingItems<ChatPageUiModel>,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val newList = oldList.toMutableList().apply {
        messageLazyPagingItems.itemSnapshotList.forEachIndexed { index, chatPageUiModel ->
            if (chatPageUiModel is ChatPageUiModel.MessageWithSender) {
                val tempList = mutableListOf<MessagePageState.ImagePreviewerState.ImagePreviewerItem>()
                chatPageUiModel.message.contents.forEachIndexed { mIndex, paraboxMessageElement ->
                    if (paraboxMessageElement is ParaboxImage) {
                        val mElementId = chatPageUiModel.message.contentsId[mIndex]
                        if (this.find { it.elementId == mElementId } == null) {
                            Log.d(
                                "parabox",
                                "image add;elementId = $mElementId;indexOfPaging=$index;itemCount=${messageLazyPagingItems.itemCount}",
                            )
                            tempList.add(
                                0,
                                MessagePageState.ImagePreviewerState.ImagePreviewerItem(
                                    image = paraboxMessageElement,
                                    elementId = mElementId,
                                    indexInPaging = index,
                                ),
                            )
                        }
                    }
                }
                addAll(tempList)
            }
        }
    }.sortedBy { it.indexInPaging }
    val targetElementIndex = newList.indexOfFirst { it.elementId == elementId }
    Log.d("parabox", "image clicked;index: $targetElementIndex")
    onEvent(
        MessagePageEvent.UpdateImagePreviewerSnapshotList(
            newList,
            targetElementIndex,
        ),
    )
}
