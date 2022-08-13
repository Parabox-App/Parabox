package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.domain.model.toTimedMessages
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
) {

//    val viewModel: MessagePageViewModel = hiltViewModel()
    val messageState by mainSharedViewModel.messageStateFlow.collectAsState()
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
                    onBackClick = {
                        if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                            mainSharedViewModel.clearMessage()
                        } else {
                            mainNavController.navigateUp()
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
    onBackClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    // Top AppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollFraction = scrollBehavior.state.overlappedFraction
    val topAppBarColor by TopAppBarDefaults.smallTopAppBarColors().containerColor(scrollFraction)
    // Bottom Sheet
    val navigationBarHeight = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    var changedTextFieldHeight by remember {
        mutableStateOf(0)
    }
    val peakHeight =
        navigationBarHeight + 88.dp + with(LocalDensity.current) { changedTextFieldHeight.toDp() }
    //temp
//    val peakHeight = navigationBarHeight + 88.dp
    // List Scroll && To Latest FAB
    val scrollState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 2
        }
    }
    BottomSheetScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Crossfade(targetState = mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
                if (it) {
                    SmallTopAppBar(
                        modifier = Modifier
                            .background(color = topAppBarColor)
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
                                IconButton(onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Reply,
                                        contentDescription = "reply"
                                    )

                                }
                            }
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "more"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                } else {
                    SmallTopAppBar(
                        modifier = Modifier
                            .background(color = topAppBarColor)
                            .statusBarsPadding(),
                        title = { Text(text = messageState.contact?.profile?.name ?: "会话") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
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
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "more"
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
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
            EditArea(onTextFieldHeightChange = { px ->
                changedTextFieldHeight = px
            }, onSend = {})
        },
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetPeekHeight = peakHeight,
        sheetElevation = 3.dp
    ) {
        val pagingDataFlow = remember(messageState) {
            mainSharedViewModel.receiveMessagePagingDataFlow(messageState.pluginConnectionObjectIdList)
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
            val timedList =
                remember(lazyPagingItems.itemCount) {
                    Log.d("parabox", "${lazyPagingItems.itemCount}")
                    lazyPagingItems.itemSnapshotList.items.toTimedMessages()
                }

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
                timedList.forEach { (timestamp, chatBlockList) ->
                    items(
                        items = chatBlockList,
                        key = { "${it.profile.name}:${timestamp}:${it.messages.first().timestamp}" }) { chatBlock ->
                        com.ojhdtapp.parabox.ui.message.ChatBlock(
                            modifier = Modifier.fillMaxWidth(),
                            mainSharedViewModel = mainSharedViewModel,
                            data = chatBlock,
                            sentByMe = false
                        )
                    }
                    item(key = "$timestamp") {
                        TimeDivider(timestamp = timestamp)
                    }
                }
                if (lazyPagingItems.loadState.append == LoadState.Loading) {
                    item("loadingIndicator") {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
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

@Composable
fun ChatBlock(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    data: ChatBlock,
    sentByMe: Boolean
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
//        horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start
    ) {
        if (sentByMe) {
            Spacer(modifier = Modifier.width(48.dp))
            ChatBlockMessages(
                modifier = Modifier.weight(1f),
                mainSharedViewModel = mainSharedViewModel,
                data = data,
                sentByMe = sentByMe
            )
            Spacer(modifier = Modifier.width(8.dp))
            ChatBlockAvatar(avatar = data.profile.avatar)
        } else {
            ChatBlockAvatar(avatar = data.profile.avatar)
            Spacer(modifier = Modifier.width(8.dp))
            ChatBlockMessages(
                modifier = Modifier.weight(1f),
                mainSharedViewModel = mainSharedViewModel,
                data = data,
                sentByMe = sentByMe
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun ChatBlockAvatar(modifier: Modifier = Modifier, avatar: String? = null) {
    if (avatar == null) {
        Surface(
            modifier = modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {}
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatar)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                .build(),
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
        )
    }


}

@Composable
fun ChatBlockMessages(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    data: ChatBlock,
    sentByMe: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (sentByMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = data.profile.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        data.messages.forEachIndexed { index, message ->
            Spacer(modifier = Modifier.height(2.dp))
            SingleMessage(
                contents = message.contents,
                sentByMe = sentByMe,
                isFirst = index == 0,
                isLast = index == data.messages.lastIndex,
                isSelected = mainSharedViewModel.selectedMessageStateList.contains(message),
                onClick = {
                    if (mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
                        mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(message)
                    }
                },
                onLongClick = {
                    mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
                        message
                    )
                })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleMessage(
    modifier: Modifier = Modifier,
    contents: List<MessageContent>,
    sentByMe: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val topStartRadius by animateDpAsState(targetValue = if (sentByMe || isFirst) 24.dp else 0.dp)
    val topEndRadius by animateDpAsState(targetValue = if (!sentByMe || isFirst) 24.dp else 0.dp)
    val bottomStartRadius by animateDpAsState(targetValue = if (sentByMe || isLast) 24.dp else 0.dp)
    val bottomEndRadius by animateDpAsState(targetValue = if (!sentByMe || isLast) 24.dp else 0.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (sentByMe) {
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        } else {
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        }
    )
    val textColor by animateColorAsState(
        targetValue = if (sentByMe) {
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
        } else {
            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        }
    )
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
    ) {
        contents.forEachIndexed { index, messageContent ->
            when (messageContent) {
                is At -> Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    text = messageContent.getContentString(),
                    color = MaterialTheme.colorScheme.primary
                )
                is PlainText -> Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    text = messageContent.text,
                    color = textColor
                )
                is Image -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(messageContent.url)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
//                        .scale(Scale.FIT)
                        .build(),
                    contentDescription = "image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .width(with(LocalDensity.current) {
                            messageContent.width.toDp().coerceIn(80.dp, 320.dp)
                        })
//                        .height(with(LocalDensity.current) {
//                            min(messageContent.height.toDp(), 600.dp)
//                        })
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                        .clip(
                            RoundedCornerShape(
                                if (index == 0) (topStartRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp,
                                if (index == 0) (topEndRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp,
                                if (index == contents.lastIndex) (bottomEndRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp,
                                if (index == contents.lastIndex) (bottomStartRadius - 3.dp).coerceAtLeast(0.dp) else 0.dp
                            )
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditArea(
    modifier: Modifier = Modifier,
    onSend: (text: String) -> Unit,
    onTextFieldHeightChange: (height: Int) -> Unit
) {
    var inputText by remember {
        mutableStateOf("")
    }
    var shouldToolbarShrink by remember {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier
//            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val relocation = remember { BringIntoViewRequester() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .bringIntoViewRequester(relocation)
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {

                Crossfade(
                    modifier = Modifier.padding(vertical = 4.dp),
                    targetState = shouldToolbarShrink
                ) {
                    if (it) {
                        IconButton(onClick = { shouldToolbarShrink = false }) {
                            Icon(
                                imageVector = Icons.Outlined.NavigateNext,
                                contentDescription = "expand"
                            )
                        }
                    } else {
                        Row() {
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "more"
                                )
                            }
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "emoji"
                                )
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                        .padding(bottom = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = {}
                ) {
                    val originalBoxHeight = with(LocalDensity.current) {
                        24.dp.toPx().toInt()
                    }
                    //temp
//                    val originalBoxHeight = 56
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(12.dp)
                            .onSizeChanged { onTextFieldHeightChange(it.height - originalBoxHeight) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val scope = rememberCoroutineScope()
                        BasicTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusEvent {
                                    if (it.isFocused) scope.launch { delay(200); relocation.bringIntoView() }
                                }
                                .clearFocusOnKeyboardDismiss(),
                            value = inputText,
                            onValueChange = {
                                if (it.length > 6) shouldToolbarShrink = true
                                else if (it.isEmpty()) shouldToolbarShrink = false
                                inputText = it
                            },
                            enabled = true,
                            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "输入内容",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            },
                            cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                AnimatedVisibility(visible = inputText.isNotEmpty(),
//                    enter = slideInHorizontally { width -> width },
//                    exit = slideOutHorizontally { width -> width }
                    enter = expandHorizontally() { width -> 0 },
                    exit = shrinkHorizontally() { width -> 0 }
                ) {
                    FloatingActionButton(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.padding(start = 16.dp),
                        elevation = elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = "send")
                    }
                }
            }
        }
    }
}