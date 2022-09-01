package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.itemsBeforeAndAfterReverseIndexed
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class)
@RootNavGraph(start = false)
@Destination
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit,
    isInSplitScreen: Boolean = false
) {

//    val viewModel: MessagePageViewModel = hiltViewModel()
    val messageState by mainSharedViewModel.messageStateFlow.collectAsState()
    val context = LocalContext.current
    Crossfade(targetState = messageState.state) {
        when (it) {
            MessageState.NULL -> {
                NullChatPage(modifier = modifier)
            }
//            MessageState.ERROR -> {
//                ErrorChatPage(modifier = modifier, errMessage = messageState.message ?: "请重试") {}
//            }
            MessageState.LOADING, MessageState.SUCCESS -> {
                NormalChatPage(
                    modifier = modifier,
                    navigator = navigator,
                    messageState = messageState,
                    mainSharedViewModel = mainSharedViewModel,
                    sizeClass = sizeClass,
                    isInSplitScreen = isInSplitScreen,
                    onRecallMessage = {
                        onEvent(
                            ActivityEvent.RecallMessage(
                                messageId = it
                            )
                        )
                    },
                    onStopSplitting = {
                        mainNavController.navigate(ChatPageDestination())
                    },
                    onBackClick = {
                        if (!isInSplitScreen || sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                            mainNavController.navigateUp()
                        }
                        mainSharedViewModel.clearMessage()
                    },
                    onSend = {
                        val selectedPluginConnection = messageState.selectedPluginConnection
                            ?: messageState.pluginConnectionList.firstOrNull()
                        if (selectedPluginConnection == null) {
                            Toast.makeText(context, "未选择有效的发送出口", Toast.LENGTH_SHORT).show()
                        } else {
                            onEvent(
                                ActivityEvent.SendMessage(
                                    contents = it,
                                    pluginConnection = selectedPluginConnection.toSenderPluginConnection()
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    messageState: MessageState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    isInSplitScreen: Boolean = false,
    onRecallMessage: (messageId: Long) -> Unit,
    onStopSplitting: () -> Unit = {},
    onBackClick: () -> Unit,
    onSend: (contents: List<com.ojhdtapp.messagedto.message_content.MessageContent>) -> Unit
) {
    val pluginPackageNameList =
        mainSharedViewModel.pluginListStateFlow.collectAsState().value.map { it.packageName }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Top AppBar
    var menuExpanded by remember {
        mutableStateOf(false)
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
//    val scrollFraction = scrollBehavior.state.overlappedFraction
//    val topAppBarColor by TopAppBarDefaults.smallTopAppBarColors().containerColor(scrollFraction)
//    val colorTransitionFraction = scrollBehavior.state.overlappedFraction
//    val fraction = if (colorTransitionFraction > 0.01f) 1f else 0f
//    val appBarContainerColor by animateColorAsState(
//        targetValue = lerp(
//            MaterialTheme.colorScheme.surface,
//            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
//            FastOutLinearInEasing.transform(fraction)
//        ),
//        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
//    )
    val appBarContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    // Bottom Sheet
    val navigationBarHeight = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    var changedTextFieldHeight by remember {
        mutableStateOf(0)
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
    )

    val peakHeight by remember {
        derivedStateOf {
            density.run {
                navigationBarHeight + 88.dp + changedTextFieldHeight.toDp()
            }
        }
    }
//        navigationBarHeight + 88.dp + with(LocalDensity.current) {
//            changedTextFieldHeight.toDp()
//        }

    // List Scroll && To Latest FAB
    val scrollState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 2
        }
    }
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    BottomSheetScaffold(
//        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        scaffoldState = scaffoldState,
        topBar = {
            Crossfade(targetState = mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
                if (it) {
                    SmallTopAppBar(
                        modifier = Modifier
                            .background(color = appBarContainerColor)
                            .statusBarsPadding(),
                        title = {
                            AnimatedContent(targetState = mainSharedViewModel.selectedMessageStateList.size.toString(),
                                transitionSpec = {
                                    // Compare the incoming number with the previous number.
                                    if (targetState > initialState) {
                                        // If the target number is larger, it slides up and fades in
                                        // while the initial (smaller) number slides up and fades out.
                                        slideInVertically { height -> height } + fadeIn() with
                                                slideOutVertically { height -> -height } + fadeOut()
                                    } else {
                                        // If the target number is smaller, it slides down and fades in
                                        // while the initial number slides down and fades out.
                                        slideInVertically { height -> -height } + fadeIn() with
                                                slideOutVertically { height -> height } + fadeOut()
                                    }.using(
                                        // Disable clipping since the faded slide-in/out should
                                        // be displayed out of bounds.
                                        SizeTransform(clip = false)
                                    )
                                }) { num ->
                                Text(text = num, style = MaterialTheme.typography.titleLarge)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { mainSharedViewModel.clearSelectedMessageStateList() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "close"
                                )
                            }
                        },
                        actions = {
                            AnimatedVisibility(
                                visible = mainSharedViewModel.selectedMessageStateList.size == 1,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {

                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Reply,
                                        contentDescription = "reply"
                                    )

                                }
                            }
                            AnimatedVisibility(
                                visible = mainSharedViewModel.selectedMessageStateList.size == 1,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    Toast.makeText(context, "内容已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                    clipboardManager.setText(AnnotatedString(mainSharedViewModel.selectedMessageStateList.first().contents.getContentString()))
                                    mainSharedViewModel.clearSelectedMessageStateList()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "copy"
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart)
                            ) {
                                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "more"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.width(192.dp)
                                ) {
                                    if (mainSharedViewModel.selectedMessageStateList.first().sentByMe) {
                                        DropdownMenuItem(
                                            text = { Text(text = "尝试撤回") },
                                            onClick = {
                                                onRecallMessage(mainSharedViewModel.selectedMessageStateList.first().messageId)
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Undo,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                    DropdownMenuItem(
                                        text = { Text(text = "回复") },
                                        onClick = {
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Reply,
                                                contentDescription = null
                                            )
                                        })
                                    DropdownMenuItem(
                                        text = { Text(text = "从聊天记录移除") },
                                        onClick = {
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = null
                                            )
                                        })
                                }
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = appBarContainerColor
                        )
//                        scrollBehavior = scrollBehavior
                    )
                } else {
                    SmallTopAppBar(
                        modifier = Modifier
                            .background(color = appBarContainerColor)
                            .statusBarsPadding(),
                        title = { Text(text = messageState.contact?.profile?.name ?: "会话") },
                        navigationIcon = {
                            IconButton(onClick = { onBackClick() }) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = "back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "search"
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart)
                            ) {
                                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "more"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.width(192.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = "会话信息") },
                                        onClick = {
                                            menuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Info,
                                                contentDescription = null
                                            )
                                        })
                                    if (isInSplitScreen) {
                                        DropdownMenuItem(
                                            text = { Text(text = "解除分屏") },
                                            onClick = {
                                                menuExpanded = false
                                                onStopSplitting()
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.VerticalSplit,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                    Box(
                                        modifier = Modifier
                                            .wrapContentSize(Alignment.BottomCenter)
                                    ) {
                                        var pluginConnectionMenuExpanded by remember(
                                            menuExpanded
                                        ) {
                                            mutableStateOf(false)
                                        }
                                        DropdownMenuItem(
                                            text = { Text(text = "消息发送出口") },
                                            onClick = {
                                                pluginConnectionMenuExpanded =
                                                    !pluginConnectionMenuExpanded
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Contacts,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowRight,
                                                    contentDescription = null
                                                )
                                            })
                                        DropdownMenu(
                                            expanded = pluginConnectionMenuExpanded,
                                            onDismissRequest = {
                                                pluginConnectionMenuExpanded = false
                                            }) {
                                            messageState.pluginConnectionList.forEach {
                                                val connectionName by remember {
                                                    mutableStateOf(
                                                        PluginService.queryPluginConnectionName(
                                                            it.connectionType
                                                        )
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(text = "$connectionName - ${it.id}") },
                                                    onClick = {
                                                        mainSharedViewModel.updateSelectedPluginConnection(
                                                            it
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = if (it.objectId == messageState.selectedPluginConnection?.objectId) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                                            contentDescription = "radio"
                                                        )
                                                    })
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = appBarContainerColor
                        )
//                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabExtended,
                enter = slideInHorizontally { it * 2 }  // slide in from the right
                , exit = slideOutHorizontally { it * 2 } // slide out to the right
            ) {
                FloatingActionButton(onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollToItem(0)
                    }
                }, modifier = Modifier.offset(y = (-42).dp)) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "to_latest"
                    )
                }
            }
            Spacer(modifier = Modifier.size(1.dp))
        },
        sheetContent = {
            EditArea(
                isBottomSheetExpand = scaffoldState.bottomSheetState.isExpanded,
                packageNameList = pluginPackageNameList,
                onBottomSheetExpand = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                },
                onBottomSheetCollapse = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.collapse()
                    }
                }, onSend = onSend,
                onTextFieldHeightChange = { px ->
                    changedTextFieldHeight = px
                })
        },
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetPeekHeight = peakHeight,
        sheetElevation = 3.dp
    ) {
        val pagingDataFlow = remember(messageState) {
            mainSharedViewModel.receiveMessagePagingDataFlow(messageState.pluginConnectionList.map { it.objectId })
        }
        val lazyPagingItems =
            pagingDataFlow.collectAsLazyPagingItems()
        if (messageState.state == MessageState.LOADING || lazyPagingItems.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
//            val timedList =
//                remember(lazyPagingItems.itemSnapshotList) {
//                    lazyPagingItems.itemSnapshotList.items.toTimedMessages()
//                }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                state = scrollState,
                contentPadding = it,
                reverseLayout = true
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                itemsBeforeAndAfterReverseIndexed(
                    items = lazyPagingItems,
                    key = { it.messageId }) { value, beforeValue, afterValue, index ->
                    if (value != null) {
                        val shouldShowTimeDivider = remember(value, afterValue) {
                            beforeValue == null || abs(value.timestamp - beforeValue.timestamp) > 120000 || (index + 1) % 40 == 0
                        }
                        val willShowTimeDivider = remember(value, afterValue) {
                            afterValue == null || abs(value.timestamp - afterValue.timestamp) > 120000
                        }
                        val isFirst = remember(value, afterValue) {
                            shouldShowTimeDivider || beforeValue == null || value.sentByMe != beforeValue.sentByMe || value.profile.name != beforeValue.profile.name
                        }
                        val isLast = remember(value, afterValue) {
                            willShowTimeDivider || afterValue == null || value.sentByMe != afterValue.sentByMe || value.profile.name != afterValue.profile.name
                        }
                        MessageBlock(
                            message = value,
                            selectedMessageStateList = mainSharedViewModel.selectedMessageStateList,
                            shouldShowTimeDivider = shouldShowTimeDivider,
                            isFirst = isFirst,
                            isLast = isLast,
                            userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
                            avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
                            onMessageClick = {
                                if (mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
                                    mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
                                        value
                                    )
                                }
                            },
                            onMessageLongClick = {
                                mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
                                    value
                                )
                            },
                            onQuoteReplyClick = { messageId ->
                                coroutineScope.launch {
                                    val idList =
                                        lazyPagingItems.itemSnapshotList.items.map { it.messageId }
                                    idList.lastIndexOf(messageId).also {
                                        if (it != -1) {
                                            scrollState.animateScrollToItem(it)
                                        } else {
                                            Toast.makeText(context, "无法定位消息", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                                }
                            }
                        )
                    }

                }
//                timedList.forEach { (timestamp, chatBlockList) ->
//                    items(
//                        items = chatBlockList,
//                        key = { "${it.profile.name}:${timestamp}:${it.messages.first().timestamp}" }) { chatBlock ->
//                        com.ojhdtapp.parabox.ui.message.ChatBlock(
//                            modifier = Modifier.fillMaxWidth(),
//                            mainSharedViewModel = mainSharedViewModel,
//                            data = chatBlock,
//                            sentByMe = chatBlock.messages.first().sentByMe,
//                            userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
//                            avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
//                        )
//                    }
//                    item(key = "$timestamp") {
//                        TimeDivider(timestamp = timestamp)
//                    }
//                }
                if (lazyPagingItems.loadState.append == LoadState.Loading) {
                    item("loadingIndicator") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(36.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}
//@Composable
//fun ErrorChatPage(modifier: Modifier = Modifier, errMessage: String, onRetry: () -> Unit) {
//    Column(
//        modifier = modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text(text = errMessage, style = MaterialTheme.typography.bodyLarge)
//        OutlinedButton(onClick = onRetry) {
//            Text(text = "重试")
//        }
//    }
//}

@Composable
fun NullChatPage(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "选择会话",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MessageBlock(
    modifier: Modifier = Modifier,
    message: Message,
    selectedMessageStateList: SnapshotStateList<Message>,
    shouldShowTimeDivider: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    userName: String,
    avatarUri: String?,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onQuoteReplyClick: (messageId: Long) -> Unit
) {
    Column() {
        if (shouldShowTimeDivider) {
            TimeDivider(timestamp = message.timestamp)
        } else if (isFirst) {
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.sentByMe) Arrangement.End else Arrangement.Start
        ) {
            if (message.sentByMe) {
                Spacer(modifier = Modifier.width(64.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (isFirst) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    SingleMessage(
                        message = message,
                        isFirst = isFirst,
                        isLast = isLast,
                        isSelected = selectedMessageStateList.contains(message),
                        onClick = onMessageClick,
                        onLongClick = onMessageLongClick,
                        onQuoteReplyClick = onQuoteReplyClick
                    )
                    if (!isLast) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                MessageAvatar(
                    shouldDisplay = isFirst,
                    avatar = null,
                    avatarUri = avatarUri
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Spacer(modifier = Modifier.width(16.dp))
                MessageAvatar(
                    shouldDisplay = isFirst,
                    avatar = message.profile.avatar,
                    avatarUri = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    if (isFirst) {
                        Text(
                            text = message.profile.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    SingleMessage(
                        message = message,
                        isFirst = isFirst,
                        isLast = isLast,
                        isSelected = selectedMessageStateList.contains(message),
                        onClick = onMessageClick,
                        onLongClick = onMessageLongClick,
                        onQuoteReplyClick = onQuoteReplyClick
                    )
                    if (!isLast) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                Spacer(modifier = Modifier.width(64.dp))
            }
        }
    }
}

@Composable
fun MessageAvatar(
    modifier: Modifier = Modifier,
    shouldDisplay: Boolean,
    avatar: String?,
    avatarUri: String?
) =
    Box(modifier = Modifier.size(42.dp)) {
        if (shouldDisplay) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUri?.let { Uri.parse(it) } ?: avatar ?: R.drawable.avatar)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                    .build(),
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }

@Composable
fun TimeDivider(modifier: Modifier = Modifier, timestamp: Long) {
    Row(
        modifier = modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = timestamp.toDescriptiveTime(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Divider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

//@Composable
//fun ChatBlock(
//    modifier: Modifier = Modifier,
//    mainSharedViewModel: MainSharedViewModel,
//    data: ChatBlock,
//    sentByMe: Boolean,
//    userName: String,
//    avatarUri: String?
//) {
//    Row(
//        modifier = modifier
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//    ) {
//        if (sentByMe) {
//            Spacer(modifier = Modifier.width(48.dp))
//            ChatBlockMessages(
//                modifier = Modifier.weight(1f),
//                mainSharedViewModel = mainSharedViewModel,
//                data = data,
//                sentByMe = sentByMe,
//                userName = userName
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            ChatBlockAvatar(avatar = avatarUri)
//        } else {
//            ChatBlockAvatar(avatar = data.profile.avatar)
//            Spacer(modifier = Modifier.width(8.dp))
//            ChatBlockMessages(
//                modifier = Modifier.weight(1f),
//                mainSharedViewModel = mainSharedViewModel,
//                data = data,
//                sentByMe = sentByMe,
//                userName = userName
//            )
//            Spacer(modifier = Modifier.width(48.dp))
//        }
//    }
//}
//
//@Composable
//fun ChatBlockAvatar(
//    modifier: Modifier = Modifier,
//    avatar: String? = null,
//    avatarUri: String? = null
//) {
//    AsyncImage(
//        model = ImageRequest.Builder(LocalContext.current)
//            .data(avatarUri?.let { Uri.parse(it) } ?: avatar ?: R.drawable.avatar)
//            .crossfade(true)
//            .diskCachePolicy(CachePolicy.ENABLED)
//            .build(),
//        contentDescription = "avatar",
//        contentScale = ContentScale.Crop,
//        modifier = Modifier
//            .size(42.dp)
//            .clip(CircleShape)
//    )
//}
//
//@Composable
//fun ChatBlockMessages(
//    modifier: Modifier = Modifier,
//    mainSharedViewModel: MainSharedViewModel,
//    data: ChatBlock,
//    sentByMe: Boolean,
//    userName: String,
//) {
//    Column(
//        modifier = modifier,
//        horizontalAlignment = if (sentByMe) Alignment.End else Alignment.Start
//    ) {
//        Text(
//            text = if (sentByMe) userName else data.profile.name,
//            style = MaterialTheme.typography.labelMedium,
//            color = MaterialTheme.colorScheme.primary
//        )
//        data.messages.forEachIndexed { index, message ->
//            Spacer(modifier = Modifier.height(2.dp))
//            SingleMessage(
//                message = message,
//                isFirst = index == 0,
//                isLast = index == data.messages.lastIndex,
//                isSelected = mainSharedViewModel.selectedMessageStateList.contains(message),
//                onClick = {
//                    if (mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
//                        mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(message)
//                    }
//                },
//                onLongClick = {
//                    mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
//                        message
//                    )
//                })
//        }
//    }
//}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleMessage(
    modifier: Modifier = Modifier,
    message: Message,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuoteReplyClick: (messageId: Long) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val topStartRadius by animateDpAsState(targetValue = if (message.sentByMe || isFirst) 24.dp else 0.dp)
    val topEndRadius by animateDpAsState(targetValue = if (!message.sentByMe || isFirst) 24.dp else 0.dp)
    val bottomStartRadius by animateDpAsState(targetValue = if (message.sentByMe || isLast) 24.dp else 0.dp)
    val bottomEndRadius by animateDpAsState(targetValue = if (!message.sentByMe || isLast) 24.dp else 0.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (message.sentByMe) {
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        } else {
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        }
    )
    val textColor by animateColorAsState(
        targetValue = if (message.sentByMe) {
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
        } else {
            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        }
    )
    Row(verticalAlignment = Alignment.Bottom) {
        if (message.sentByMe && !message.verified) {
            if (abs(System.currentTimeMillis() - message.timestamp) > 6000) {
                Icon(
                    modifier = Modifier.padding(bottom = 11.dp, end = 4.dp),
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "error",
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 14.dp, end = 4.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        Column(
            modifier = modifier
                .clip(
                    RoundedCornerShape(
                        topStart = topStartRadius,
                        topEnd = topEndRadius,
                        bottomStart = bottomStartRadius,
                        bottomEnd = bottomEndRadius
                    )
                )
                .background(
                    backgroundColor
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .animateContentSize()
        ) {
            message.contents.forEachIndexed { index, messageContent ->
                messageContent.toLayout(
                    textColor,
                    context,
                    density,
                    index,
                    topStartRadius,
                    topEndRadius,
                    message,
                    bottomEndRadius,
                    bottomStartRadius,
                    onQuoteReplyClick = onQuoteReplyClick
                )
            }
        }
    }
}

@Composable
private fun MessageContent.toLayout(
    textColor: Color,
    context: Context,
    density: Density,
    index: Int,
    topStartRadius: Dp,
    topEndRadius: Dp,
    message: Message,
    bottomEndRadius: Dp,
    bottomStartRadius: Dp,
    onQuoteReplyClick: (messageId: Long) -> Unit = {},
) {
    when (this) {
        is At, AtAll -> Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            text = getContentString(),
            color = MaterialTheme.colorScheme.primary
        )
        is PlainText -> Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            text = text,
            color = textColor
        )
        is Image -> {
            var imageWidth by remember {
                mutableStateOf(80.dp)
            }
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
            LaunchedEffect(key1 = this@toLayout) {
                imageWidth = if (uriString != null) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        context.contentResolver.openInputStream(Uri.parse(uriString))
                            .use {
                                BitmapFactory.decodeStream(
                                    it,
                                    null,
                                    options
                                )
                            }
                        with(density) {
                            options.outWidth.toDp().coerceIn(80.dp, 320.dp)
                        }
                    } catch (e: FileNotFoundException) {
                        with(density) {
                            width
                                .toDp()
                                .coerceIn(80.dp, 320.dp)
                        }
                    }
                } else {
                    with(density) {
                        width
                            .toDp()
                            .coerceIn(80.dp, 320.dp)
                    }
                }
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uriString?.also { Uri.parse(it) }
                        ?: url)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .width(imageWidth)
//                    .defaultMinSize(minHeight = 80.dp)
//                        .height(with(LocalDensity.current) {
//                            min(height.toDp(), 600.dp)
//                        })
                    .padding(horizontal = 3.dp, vertical = 3.dp)
                    .clip(
                        RoundedCornerShape(
                            if (index == 0) (topStartRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp,
                            if (index == 0) (topEndRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp,
                            if (index == message.contents.lastIndex) (bottomEndRadius - 3.dp).coerceAtLeast(
                                0.dp
                            ) else 0.dp,
                            if (index == message.contents.lastIndex) (bottomStartRadius - 3.dp).coerceAtLeast(
                                0.dp
                            ) else 0.dp
                        )
                    )
            )
        }
        is QuoteReply -> {
            Surface(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .clickable {
                            if (quoteMessageId != null) {
                                onQuoteReplyClick(quoteMessageId)
                            }
                        }
                        .padding(
                            horizontal = 12.dp,
                            vertical = 12.dp
                        )
                ) {
                    Row() {
                        quoteMessageSenderName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            quoteMessageTimestamp?.let {
                                Text(
                                    modifier = Modifier.width(IntrinsicSize.Max),
                                    text = it.toDescriptiveTime(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                    quoteMessageContent?.forEachIndexed { index, messageContent ->
                        messageContent.toLayout(
                            textColor,
                            context,
                            density,
                            index,
                            0.dp,
                            0.dp,
                            message,
                            0.dp,
                            0.dp
                        )
                    }
                    if (quoteMessageContent == null) {
                        Text(
                            text = "无法定位消息",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        is File -> {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (extension) {
                                "apk" -> Icons.Outlined.Android
                                "txt" -> Icons.Outlined.Description
                                "mp3", "wav", "flac", "ape", "wma" -> Icons.Outlined.AudioFile
                                "mp4" -> Icons.Outlined.VideoFile
                                "zip", "rar", "7z", "gz", "tar.gz" -> Icons.Outlined.FolderZip
                                else -> Icons.Outlined.FilePresent
                            }, contentDescription = "type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column() {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = FileUtil.getSizeString(size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

        }
    }
}

