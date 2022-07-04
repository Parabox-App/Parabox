package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.ui.util.MessageNavGraph
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@MessageNavGraph(start = false)
@Destination
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    viewModel: MessagePageViewModel
) {
//    val viewModel: MessagePageViewModel = hiltViewModel()
    val messageState by viewModel.messageStateFlow.collectAsState()
    Crossfade(targetState = messageState.state) {
        when (it) {
            MessageState.NULL -> {
                NullChatPage()
            }
            MessageState.ERROR -> {
                ErrorChatPage(errMessage = messageState.message ?: "请重试") {}
            }
            MessageState.LOADING, MessageState.SUCCESS -> {
                NormalChatPage(navigator = navigator, messageState = messageState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    messageState: MessageState
) {
    // Top AppBar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())
    val scrollFraction = scrollBehavior.scrollFraction
    val topAppBarColor = TopAppBarDefaults.smallTopAppBarColors().containerColor(scrollFraction)
    // Bottom Sheet
    val navigationBarHeight = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    var changedTextFieldHeight by remember {
        mutableStateOf(0)
    }
//    val peakHeight =
//        navigationBarHeight + 88.dp + with(LocalDensity.current) { changedTextFieldHeight.toDp() }

    //temp
    val peakHeight = navigationBarHeight + 88.dp
    BottomSheetScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                modifier = Modifier
                    .background(color = topAppBarColor.value)
                    .statusBarsPadding(),
                title = { Text(text = messageState.profile?.name ?: "会话") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "back")
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "search")

                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
                    }
                },
                scrollBehavior = scrollBehavior
            )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = it
        ) {
            messageState.data!!.forEach { (timestamp, chatBlockList) ->
                item {
                    TimeDivider(timestamp = timestamp)
                }
                items(items = chatBlockList) { chatBlock ->
                    ChatBlock(data = chatBlock, sentByMe = true)
                }
            }
        }
    }
}

@Composable
fun ErrorChatPage(modifier: Modifier = Modifier, errMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = errMessage, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

@Composable
fun NullChatPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "选择会话", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun TimeDivider(modifier: Modifier = Modifier, timestamp: Long) {
    Row(
        modifier = modifier.height(32.dp),
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
fun ChatBlock(modifier: Modifier = Modifier, data: ChatBlock, sentByMe: Boolean) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = if (sentByMe) Arrangement.End else Arrangement.Start
    ) {
        if (sentByMe) {
            ChatBlockMessages(data = data, sentByMe = sentByMe)
            Spacer(modifier = Modifier.width(8.dp))
            ChatBlockAvatar()
        } else {
            ChatBlockAvatar()
            Spacer(modifier = Modifier.width(8.dp))
            ChatBlockMessages(data = data, sentByMe = sentByMe)
        }
    }
}

@Composable
fun ChatBlockAvatar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {

    }
}

@Composable
fun ChatBlockMessages(data: ChatBlock, sentByMe: Boolean) {
    Column(
        horizontalAlignment = if(sentByMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = data.profile.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        data.content.forEachIndexed { index, messageContents ->
            Spacer(modifier = Modifier.height(2.dp))
            SingleMessage(
                contents = messageContents,
                sentByMe = sentByMe,
                isFirst = index == 0,
                isLast = index == data.content.size - 1,
                onClick = {},
                onLongClick = {})
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val topStartRadius by animateDpAsState(targetValue = if (sentByMe || isFirst) 24.dp else 0.dp)
    val topEndRadius by animateDpAsState(targetValue = if (!sentByMe || isFirst) 24.dp else 0.dp)
    val bottomStartRadius by animateDpAsState(targetValue = if (sentByMe || isLast) 24.dp else 0.dp)
    val bottomEndRadius by animateDpAsState(targetValue = if (!sentByMe || isLast) 24.dp else 0.dp)
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
                if (sentByMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        contents.forEach {
            when (it) {
                is At -> Text(
                    text = it.getContentString(),
                    color = if (sentByMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                is PlainText -> Text(
                    text = it.getContentString() + "$isFirst" + "$isLast",
                    color = if (sentByMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                is Image -> Text(
                    text = it.getContentString(),
                    color = if (sentByMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
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
//                    val originalBoxHeight = with(LocalDensity.current) {
//                        24.dp.toPx().toInt()
//                    }
                    //temp
                    val originalBoxHeight = 56
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
                AnimatedVisibility(visible = !inputText.isNullOrEmpty(),
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