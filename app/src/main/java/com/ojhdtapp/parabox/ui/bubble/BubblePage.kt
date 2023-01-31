package com.ojhdtapp.parabox.ui.bubble

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.FileUtil.replacedIfUnavailable
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.ui.message.AmplitudeIndicator
import com.ojhdtapp.parabox.ui.message.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.EditArea
import com.ojhdtapp.parabox.ui.message.MessageBlock
import com.ojhdtapp.parabox.ui.message.MessageState
import com.ojhdtapp.parabox.ui.message.SingleMessageEvent
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.RoundedCornerDropdownMenu
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.rememberPreviewerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun BubblePage(
    modifier: Modifier = Modifier,
    viewModel: BubblePageViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val context = LocalContext.current
    val messageState by viewModel.messageStateFlow.collectAsState()
    BubbleChatPage(
        messageState = messageState,
        viewModel = viewModel,
        sizeClass = sizeClass,
        onRecallMessage = { type, id ->
            onEvent(
                ActivityEvent.RecallMessage(
                    type = type,
                    messageId = id
                )
            )
        },
        onLaunchApp = {
            onEvent(ActivityEvent.LaunchApp)
        },
        onSend = {
            viewModel.clearQuoteMessage()
            viewModel.clearAt()
            val selectedPluginConnection = messageState.selectedPluginConnection
                ?: messageState.pluginConnectionList.firstOrNull()
            if (selectedPluginConnection == null) {
                Toast.makeText(context, context.getString(R.string.no_plugin_connection_selected), Toast.LENGTH_SHORT).show()
            } else {
                onEvent(
                    ActivityEvent.SendMessage(
                        contents = it,
                        pluginConnection = selectedPluginConnection.toSenderPluginConnection(),
                        sendType = selectedPluginConnection.connectionType
                    )
                )
            }
        },
        onStartRecording = {
            onEvent(ActivityEvent.StartRecording)
        },
        onStopRecording = {
            onEvent(ActivityEvent.StopRecording)
        },
        onStartAudioPlaying = { uri, url ->
            onEvent(ActivityEvent.StartAudioPlaying(uri, url))
        },
        onPauseAudioPlaying = {
            onEvent(ActivityEvent.PauseAudioPlaying)
        },
        onResumeAudioPlaying = {
            onEvent(ActivityEvent.ResumeAudioPlaying)
        },
        onSetAudioProgressByFraction = {
            onEvent(ActivityEvent.SetAudioProgress(it))
        },
        onVibrate = {
            onEvent(ActivityEvent.Vibrate)
        },
    )
}

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class, ExperimentalCoilApi::class
)
@Composable
fun BubbleChatPage(
    modifier: Modifier = Modifier,
    messageState: MessageState,
    viewModel: BubblePageViewModel,
    sizeClass: WindowSizeClass,
    onRecallMessage: (type: Int, messageId: Long) -> Unit,
    onLaunchApp: () -> Unit,
    onSend: (contents: List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartAudioPlaying: (uri: Uri?, url: String?) -> Unit,
    onPauseAudioPlaying: () -> Unit,
    onResumeAudioPlaying: () -> Unit,
    onSetAudioProgressByFraction: (progressFraction: Float) -> Unit,
    onVibrate: () -> Unit,
) {
    // Util
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    // Paging
    val pagingDataFlow = remember(messageState) {
        viewModel.receiveMessagePagingDataFlow(messageState.pluginConnectionList.map { it.objectId })
    }
    val lazyPagingItems =
        pagingDataFlow.collectAsLazyPagingItems()

    val pluginPackageNameList =
        viewModel.pluginListStateFlow.collectAsState().value.map { it.packageName }

    // Top AppBar
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val appBarContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

    // Bottom Sheet
    val paddingValues = WindowInsets.systemBars.asPaddingValues()
    var changedTextFieldHeight by remember {
        mutableStateOf(0)
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
    )
    val allowBubbleHome = viewModel.allowBubbleHomeFlow.collectAsState(initial = false).value
    LaunchedEffect(key1 = scaffoldState.bottomSheetState.targetValue) {
        if (scaffoldState.bottomSheetState.progress.fraction != 1f && scaffoldState.bottomSheetState.progress.fraction != 0f)
            onVibrate()
    }
    val opacityState by remember {
        derivedStateOf {
            if (scaffoldState.bottomSheetState.direction == 1f) {
                1 - scaffoldState.bottomSheetState.progress.fraction.coerceIn(0f, 1f)
            } else if (scaffoldState.bottomSheetState.direction == -1f) {
                scaffoldState.bottomSheetState.progress.fraction.coerceIn(0f, 1f)
            } else {
                if (scaffoldState.bottomSheetState.currentValue == BottomSheetValue.Expanded) {
                    1f
                } else {
                    0f
                }
            }
        }
    }

    val peakHeight by remember {
        derivedStateOf {
            density.run {
                paddingValues.calculateBottomPadding() + 88.dp + changedTextFieldHeight.toDp()
            }
        }
    }

    // List Scroll && To Latest FAB
    val scrollState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 2
        }
    }

    // Quote
    val quoteExtended by remember {
        derivedStateOf {
            !scrollState.isScrollInProgress
        }
    }

    // Clicking
    var clickingMessage by remember {
        mutableStateOf<Message?>(null)
    }

    // User Profile
    val userName by viewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME)
    val avatarUri by viewModel.userAvatarFlow.collectAsState(initial = null)

    // Smart Reply
    var smartReplyList by remember {
        mutableStateOf<List<String>>(emptyList())
    }
    LaunchedEffect(lazyPagingItems.itemSnapshotList.items.lastOrNull()?.messageId) {
        messageState.contact?.contactId?.let {
            smartReplyList = (context as BubbleActivity).getSmartReplyList(it).map {
                it.text
            }
        }

    }

    // Delete Confirm Dialog
    var showDeleteMessageConfirmDialog by remember { mutableStateOf(false) }
    if (showDeleteMessageConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteMessageConfirmDialog = false
            },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = {
                Text(text = stringResource(R.string.delete_message))
            },
            text = {
                Text(
                    stringResource(id = R.string.delete_message_text, viewModel.selectedMessageStateList.size)
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteMessageConfirmDialog = false
                        viewModel.deleteMessage(viewModel.selectedMessageStateList.map { it.messageId })
                        lazyPagingItems.refresh()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteMessageConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    var showDeleteClickingMessageConfirmDialog by remember { mutableStateOf(false) }
    if (showDeleteClickingMessageConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteClickingMessageConfirmDialog = false
                clickingMessage = null

            },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = {
                Text(text = stringResource(id = R.string.delete_message))
            },
            text = {
                Text(
                    stringResource(R.string.delete_single_message_text)
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteClickingMessageConfirmDialog = false
                        clickingMessage?.messageId?.let {
                            viewModel.deleteMessage(listOf(it))
                            lazyPagingItems.refresh()
                            clickingMessage = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteClickingMessageConfirmDialog = false
                        clickingMessage = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    // Audio State Hoisting for Gesture Control
    var audioState by remember {
        mutableStateOf(false)
    }
    // Meme Related
    var memeUpdateFlag by remember {
        mutableStateOf(0)
    }
    // Image Preview
    val imageList =
        lazyPagingItems.itemSnapshotList.items.fold(mutableListOf<Pair<Long, Image>>()) { acc, message ->
            val imageMessageList = message.contents.filterIsInstance<Image>()
            val lastIndex = imageMessageList.lastIndex
            imageMessageList.reversed().forEachIndexed { index, t ->
                if (t.uriString != null || t.url != null) {
                    val imageId = "${message.messageId}${(lastIndex - index).coerceIn(0, lastIndex)}".toLong()
                    acc.add(
                        imageId to t
                    )
                }
            }
            acc
        }.reversed()
    val imageViewerState = rememberPreviewerState(enableVerticalDrag = true){
        imageList[it].first
    }
    LaunchedEffect(messageState) {
        imageViewerState.close()
    }
    var showImagePreviewerToolbar by remember {
        mutableStateOf(true)
    }
    var imagePreviewerMenuExpanded by remember {
        mutableStateOf(false)
    }
    val useDarkIcons = isSystemInDarkTheme()
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(imageViewerState.visible){
        if (imageViewerState.visible) {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = false
            )
        } else {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = !useDarkIcons
            )
        }
    }
    BottomSheetScaffold(
//        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        scaffoldState = scaffoldState,
        topBar = {
            Crossfade(targetState = viewModel.selectedMessageStateList.isNotEmpty()) {
                if (it) {
                    TopAppBar(
                        title = {
                            AnimatedContent(targetState = viewModel.selectedMessageStateList.size.toString(),
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
                        modifier = Modifier
                            .background(color = appBarContainerColor)
                            .statusBarsPadding(),
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelectedMessageStateList() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "close"
                                )
                            }
                        },
                        actions = {
                            AnimatedVisibility(
                                visible = viewModel.selectedMessageStateList.size == 1,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.save_to_clipboard),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    clipboardManager.setText(AnnotatedString(viewModel.selectedMessageStateList.first().contents.getContentString()))
                                    viewModel.clearSelectedMessageStateList()
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
                                RoundedCornerDropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.width(192.dp)
                                ) {
                                    if (viewModel.selectedMessageStateList.size == 1 && viewModel.selectedMessageStateList.firstOrNull()?.sentByMe == true) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.try_to_recall)) },
                                            onClick = {
                                                if (viewModel.selectedMessageStateList.size == 1) {
                                                    val message =
                                                        viewModel.selectedMessageStateList.first()
                                                    onRecallMessage(
                                                        message.sendType!!,
                                                        message.messageId
                                                    )
                                                }
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Undo,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                    if (viewModel.selectedMessageStateList.size == 1) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(R.string.reply)) },
                                            onClick = {
                                                if (viewModel.selectedMessageStateList.size == 1)
                                                    viewModel.setQuoteMessage(
                                                        viewModel.selectedMessageStateList.firstOrNull(),
                                                        userName
                                                    )
                                                viewModel.clearSelectedMessageStateList()
                                                menuExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Reply,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.remove_from_history)) },
                                        onClick = {
                                            menuExpanded = false
                                            showDeleteMessageConfirmDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = null
                                            )
                                        })
                                }
                            }
                        }, colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = appBarContainerColor
                        )
//                        scrollBehavior = scrollBehavior
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = messageState.contact?.profile?.name ?: stringResource(R.string.conversation),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier
                            .background(color = appBarContainerColor)
                            .statusBarsPadding(),
                        actions = {
                            if(allowBubbleHome){
                                IconButton(onClick = { onLaunchApp() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Home,
                                        contentDescription = "launch app"
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
                                RoundedCornerDropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.width(192.dp)
                                ) {
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
                                            text = { Text(text = stringResource(R.string.select_plugin_connection)) },
                                            onClick = {
                                                pluginConnectionMenuExpanded =
                                                    !pluginConnectionMenuExpanded
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ManageAccounts,
                                                    contentDescription = null
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowRight,
                                                    contentDescription = null
                                                )
                                            })
                                        RoundedCornerDropdownMenu(
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
                                                        viewModel.updateSelectedPluginConnection(
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
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(R.string.refresh)) },
                                        onClick = {
                                            menuExpanded = false
                                            lazyPagingItems.refresh()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Refresh,
                                                contentDescription = null
                                            )
                                        })
                                }
                            }
                        }, colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = appBarContainerColor
                        )
//                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.recordAmplitudeStateList.isNotEmpty() || viewModel.quoteMessageState.value != null || viewModel.atState.value != null || fabExtended,
                enter = slideInHorizontally { it * 2 }  // slide in from the right
                , exit = slideOutHorizontally { it * 2 } // slide out to the right
            ) {
                ExtendedFloatingActionButton(
                    expanded = viewModel.recordAmplitudeStateList.isNotEmpty() || viewModel.quoteMessageState.value != null || viewModel.atState.value != null && quoteExtended,
                    onClick = {
                        if (viewModel.recordAmplitudeStateList.isNotEmpty()) {
                            if (viewModel.audioRecorderState.value.let {
                                    it !is AudioRecorderState.Ready
                                }) {

                            } else {
                                if (viewModel.isAudioPlaying.value) {
                                    onPauseAudioPlaying()
                                } else {
                                    onResumeAudioPlaying()
                                }
                            }
                        } else if (viewModel.quoteMessageState.value != null) {
                            coroutineScope.launch {
                                val idList =
                                    lazyPagingItems.itemSnapshotList.items.map { it.messageId }
                                idList.lastIndexOf(viewModel.quoteMessageState.value?.messageId)
                                    .also {
                                        if (it != -1) {
                                            scrollState.animateScrollToItem(it)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.cannot_locate_msg),
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                            }
                        } else {
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(0)
                            }
                        }
                    }, modifier = Modifier.offset(y = (-42).dp),
                    icon = {
                        Crossfade(
                            targetState = when {
                                viewModel.audioRecorderState.value.let {
                                    it !is AudioRecorderState.Ready
                                } -> 1

                                viewModel.recordAmplitudeStateList.isNotEmpty() && viewModel.isAudioPlaying.value -> 2
                                viewModel.recordAmplitudeStateList.isNotEmpty() -> 3
                                viewModel.atState.value != null -> 4
                                viewModel.quoteMessageState.value != null -> 5
                                else -> 6
                            }
                        ) {
                            when (it) {
                                1 -> Icon(
                                    imageVector = Icons.Outlined.SettingsVoice,
                                    contentDescription = "voice"
                                )

                                2 -> Icon(
                                    imageVector = Icons.Outlined.Pause,
                                    contentDescription = "pause"
                                )

                                3 -> Icon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "resume"
                                )

                                4 -> Icon(
                                    imageVector = Icons.Outlined.AlternateEmail,
                                    contentDescription = "at"
                                )

                                5 -> Icon(
                                    imageVector = Icons.Outlined.Reply,
                                    contentDescription = "reply"
                                )

                                else -> Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = "to_latest"
                                )
                            }
                        }
                    },
                    text = {
                        Box(modifier = Modifier.animateContentSize()) {
                            if (viewModel.recordAmplitudeStateList.isNotEmpty()) {
                                AmplitudeIndicator(
                                    modifier = Modifier.size(
                                        width = 60.dp,
                                        height = 24.dp
                                    ),
                                    amplitudeList = viewModel.recordAmplitudeStateList,
                                    progressFraction = viewModel.audioPlayerProgressFraction.value,
                                    onPause = onPauseAudioPlaying,
                                    onResumeAtFraction = {
                                        onSetAudioProgressByFraction(it)
                                        onResumeAudioPlaying()
                                    }
                                )
                            } else {
                                if (viewModel.quoteMessageState.value == null) {
                                    viewModel.atState.value?.let {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier.widthIn(0.dp, 208.dp),
                                                text = it.name,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = { viewModel.clearAt() }) {
                                                Icon(
                                                    modifier = Modifier.size(18.dp),
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "cancel"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.quoteMessageState.value?.let {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.widthIn(0.dp, 208.dp)) {
                                                Text(
//                                                    text = "${if(viewModel.atState.value!=null)"@" else ""}${it.profile.name}",
                                                    text = it.profile.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = it.contents.getContentString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = { viewModel.clearQuoteMessage() }) {
                                                Icon(
                                                    modifier = Modifier.size(18.dp),
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "cancel"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    })
            }
            Spacer(modifier = Modifier.size(1.dp))
        },
        sheetContent = {
            EditArea(
                height = 128.dp,
                isBottomSheetExpand = scaffoldState.bottomSheetState.isExpanded,
                packageNameList = pluginPackageNameList,
                quoteMessageSelected = viewModel.quoteMessageState.value,
                at = viewModel.atState.value,
                audioState = audioState,
                audioRecorderState = viewModel.audioRecorderState.value,
                memeUpdateFlag = memeUpdateFlag,
                functionalAreaOpacity = opacityState,
                onMemeUpdate = { memeUpdateFlag++ },
                onAudioStateChanged = { audioState = it },
                onAudioRecorderStateChanged = { viewModel.setAudioRecorderState(it) },
                onBottomSheetExpand = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.expand()
                    }
                },
                onBottomSheetCollapse = {
                    coroutineScope.launch {
                        scaffoldState.bottomSheetState.collapse()
                    }
                }, onSend = {
                    onSend(it)
                    // return bottom after message sent
                    coroutineScope.launch {
                        scrollState.animateScrollToItem(0)
                        delay(5000)
                        lazyPagingItems.refresh()
                    }
                    viewModel.clearRecordAmplitudeStateList()
                },
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onClearRecording = {
                    viewModel.clearRecordAmplitudeStateList()
                },
                onTextFieldHeightChange = { px ->
                    changedTextFieldHeight = px
                })
        },
        sheetGesturesEnabled = !audioState,
        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
        sheetPeekHeight = peakHeight,
        sheetElevation = 3.dp
    ) {
        // Back Handlers (The last execute first)
        BackHandler(enabled = viewModel.quoteMessageState.value != null) {
            viewModel.clearQuoteMessage()
        }
        BackHandler(enabled = viewModel.selectedMessageStateList.size != 0) {
            viewModel.clearSelectedMessageStateList()
        }
        BackHandler(enabled = scaffoldState.bottomSheetState.isExpanded) {
            coroutineScope.launch {
                scaffoldState.bottomSheetState.collapse()
            }
        }
        if (messageState.state == MessageState.LOADING) {
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
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusManager.clearFocus()
                            if (scaffoldState.bottomSheetState.isExpanded) {
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.collapse()
                                }
                            }
                        }
                    },
                state = scrollState,
                contentPadding = it,
                reverseLayout = true,
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    AnimatedVisibility(
                        visible = smartReplyList.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = smartReplyList) {
                                androidx.compose.material3.OutlinedButton(onClick = {
                                    onSend(
                                        listOf(
                                            PlainText(
                                                text = it
                                            )
                                        )
                                    )
                                    smartReplyList = emptyList()
                                    // return bottom after message sent
                                    coroutineScope.launch {
                                        scrollState.animateScrollToItem(0)
                                        delay(5000)
                                        lazyPagingItems.refresh()
                                    }
                                    viewModel.clearRecordAmplitudeStateList()
                                }) {
                                    Text(text = it)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                }
                itemsBeforeAndAfterReverseIndexed(
                    items = lazyPagingItems,
                    key = { it.messageId }) { value, beforeValue, afterValue, index ->
                    if (value != null) {
                        val shouldShowTimeDivider = remember(value, beforeValue, afterValue) {
                            beforeValue == null || abs(value.timestamp - beforeValue.timestamp) > 120000 || (index + 1) % 40 == 0
                        }
                        val willShowTimeDivider = remember(value, beforeValue, afterValue) {
                            afterValue == null || abs(value.timestamp - afterValue.timestamp) > 120000
                        }
                        val isFirst = remember(value, beforeValue, afterValue) {
                            shouldShowTimeDivider || beforeValue == null || value.sentByMe != beforeValue.sentByMe || value.profile.name != beforeValue.profile.name
                        }
                        val isLast = remember(value, beforeValue, afterValue) {
                            willShowTimeDivider || afterValue == null || value.sentByMe != afterValue.sentByMe || value.profile.name != afterValue.profile.name
                        }
                        MessageBlock(
                            message = value,
                            selectedMessageStateList = viewModel.selectedMessageStateList,
                            shouldShowTimeDivider = shouldShowTimeDivider,
                            clickingMessage = clickingMessage,
                            isFirst = isFirst,
                            isLast = isLast,
                            userName = userName,
                            avatarUri = avatarUri,
                            fromBubble = true,
                            isTranslationEnabled = viewModel.translationFlow.collectAsState(initial = true).value,
                            imageViewerState = imageViewerState,
                            onClickingDismiss = { clickingMessage = null },
                            onClickingEvent = {
                                when (it) {
                                    is SingleMessageEvent.FailRetry -> {
                                        viewModel.deleteMessage(listOf(value.messageId))
                                        lazyPagingItems.refresh()
                                        onSend(value.contents.toMessageContentList())
                                        // return bottom after message sent
                                        coroutineScope.launch {
                                            scrollState.animateScrollToItem(0)
                                            delay(5000)
                                            lazyPagingItems.refresh()
                                        }
                                    }

                                    is SingleMessageEvent.Recall -> {
                                        onRecallMessage(value.sendType!!, value.messageId)
                                    }

                                    is SingleMessageEvent.Favorite -> {
                                        val path = context.getExternalFilesDir("meme")!!
                                        val images = value.contents.filter { it is Image }
                                        images.forEach {
                                            (it as Image).uriString?.let { uriString ->
                                                val uri = Uri.parse(uriString)
                                                FileUtil.copyFileToPath(
                                                    context, path,
                                                    it.fileName,
                                                    uri
                                                )
                                            } ?: it.url?.let { url ->
                                                context.imageLoader.diskCache?.get(url)
                                                    ?.use { snapshot ->
                                                        val imageFile = snapshot.data.toFile()
                                                        FileUtil.copyFileToPath(
                                                            context,
                                                            path,
                                                            it.fileName,
                                                            imageFile
                                                        )
                                                    }
                                            }
                                        }
                                        memeUpdateFlag++
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.add_meme_text, images.size),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    is SingleMessageEvent.Copy -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.save_to_clipboard),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        clipboardManager.setText(AnnotatedString(value.contents.getContentString()))
                                    }

                                    is SingleMessageEvent.Reply -> {
                                        viewModel.setQuoteMessage(
                                            value,
                                            userName
                                        )
                                    }

                                    is SingleMessageEvent.Download -> {
                                        try {
                                            val images = value.contents.filter { it is Image }
                                            if (value.sentByMe) {
                                                images.forEach {
                                                    (it as Image).uriString?.let { uriString ->
                                                        FileUtil.saveImageToExternalStorage(
                                                            context,
                                                            Uri.parse(uriString)
                                                        )
                                                    }
                                                }
                                            } else {
                                                images.forEach {
                                                    (it as Image).url?.let { url ->
                                                        context.imageLoader.diskCache?.get(url)
                                                            ?.use { snapshot ->
                                                                val imageFile =
                                                                    snapshot.data.toFile()
                                                                FileUtil.saveImageToExternalStorage(
                                                                    context,
                                                                    imageFile
                                                                )
                                                            }
                                                    }
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.save_to_local_text, images.size),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.save_to_local_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    is SingleMessageEvent.Delete -> {
                                        showDeleteClickingMessageConfirmDialog = true
                                    }
                                }
                            },
                            onMessageClick = {
                                if (viewModel.selectedMessageStateList.isNotEmpty()) {
                                    viewModel.addOrRemoveItemOfSelectedMessageStateList(
                                        value
                                    )
                                } else {
                                    if (value.contents.any { it is Image }) {
                                        val index =
                                            imageList.indexOfLast { it.first == "${value.messageId}0".toLong() }
                                        if (index != -1) {
                                            coroutineScope.launch {
                                                imageViewerState.openTransform(index)
                                            }
                                        }
                                    } else {
                                        clickingMessage = value
                                    }
                                }
                            },
                            onMessageLongClick = {
                                focusManager.clearFocus()
                                if (value.contents.any { it is Image }) {
                                    clickingMessage = value
                                } else {
                                    viewModel.addOrRemoveItemOfSelectedMessageStateList(
                                        value
                                    )
                                }
                            },
                            onQuoteReplyClick = { messageId ->
                                coroutineScope.launch {
                                    val idList =
                                        lazyPagingItems.itemSnapshotList.items.map { it.messageId }
                                    idList.lastIndexOf(messageId).also {
                                        if (it != -1) {
                                            scrollState.animateScrollToItem(it)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.cannot_locate_msg),
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }
                                }
                            },
                            onAudioClick = { uri, url ->
                                onStartAudioPlaying(uri, url)
                            },
                            onAvatarLongClick = {
                                value.profile.id?.let { id ->
                                    viewModel.setAtState(
                                        com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.At(
                                            target = id,
                                            name = value.profile.name,
                                        )
                                    )
                                    onVibrate()
                                }
                            }
                        )
                    }

                }
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
    ImagePreviewer(modifier = Modifier.fillMaxSize(),
        count = imageList.size,
        state = imageViewerState,
        imageLoader = { index ->
            if (index < imageList.size) {
                val t = imageList[index].second
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()
                rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(t.uriString?.let { Uri.parse(it).replacedIfUnavailable(context)}
                            ?: t.url)
                        .size(Size.ORIGINAL)
                        .build(),
                    imageLoader = imageLoader,
                    error = painterResource(id = R.drawable.image_lost),
                    fallback = painterResource(id = R.drawable.image_lost),
                )
            } else {
                painterResource(R.drawable.image_lost)
            }
        },
        previewerLayer = {
            foreground = { current ->
                AnimatedVisibility(
                    showImagePreviewerToolbar,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    androidx.compose.material3.TopAppBar(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f))
                            .statusBarsPadding(),
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    imageViewerState.closeTransform()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = "back"
                                )
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize(Alignment.TopStart)
                            ) {
                                IconButton(onClick = {
                                    imagePreviewerMenuExpanded = !imagePreviewerMenuExpanded
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "more"
                                    )
                                }
                                RoundedCornerDropdownMenu(
                                    expanded = imagePreviewerMenuExpanded,
                                    onDismissRequest = { imagePreviewerMenuExpanded = false },
                                    modifier = Modifier.width(192.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.add_meme))
                                        },
                                        onClick = {
                                            imagePreviewerMenuExpanded = false
                                            try {
                                                val imageId =
                                                    imageList.getOrNull(current)?.first
                                                        ?: throw NoSuchElementException("id lost")
                                                val imageIndex = imageId.toString().last().digitToInt()
                                                val messageId = imageId.toString().let {
                                                    it.substring(0, it.length - 1).toLong()
                                                }
                                                val message =
                                                    lazyPagingItems.itemSnapshotList.items.findLast { it.messageId == messageId }
                                                val image =
                                                    message?.contents?.filterIsInstance<Image>()
                                                        ?.getOrNull(imageIndex)
                                                        ?: throw NoSuchElementException("image lost")
                                                val path = context.getExternalFilesDir("meme")!!
                                                image.uriString?.let { uriString ->
                                                    val uri = Uri.parse(uriString)
                                                    FileUtil.copyFileToPath(
                                                        context, path,
                                                        image.fileName,
                                                        uri
                                                    )
                                                } ?: image.url?.let { url ->
                                                    context.imageLoader.diskCache?.get(url)
                                                        ?.use { snapshot ->
                                                            val imageFile = snapshot.data.toFile()
                                                            FileUtil.copyFileToPath(
                                                                context,
                                                                path,
                                                                image.fileName,
                                                                imageFile
                                                            )
                                                        }
                                                }
                                                memeUpdateFlag++
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.add_meme_text, 1),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: NoSuchElementException) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.cannot_locate_img),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FavoriteBorder,
                                                contentDescription = "favorite"
                                            )
                                        })
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.save_to_local))
                                        },
                                        onClick = {
                                            imagePreviewerMenuExpanded = false
                                            try {
                                                val imageId =
                                                    imageList.getOrNull(current)?.first
                                                        ?: throw NoSuchElementException("id lost")
                                                val imageIndex = imageId.toString().last().digitToInt()
                                                val messageId = imageId.toString().let {
                                                    it.substring(0, it.length - 1).toLong()
                                                }
                                                val message =
                                                    lazyPagingItems.itemSnapshotList.items.findLast { it.messageId == messageId }
                                                val image =
                                                    message?.contents?.filterIsInstance<Image>()
                                                        ?.getOrNull(imageIndex)
                                                        ?: throw NoSuchElementException("image lost")
                                                image.uriString?.let { uriString ->
                                                    FileUtil.saveImageToExternalStorage(
                                                        context,
                                                        Uri.parse(uriString)
                                                    )
                                                } ?: image.url?.let { url ->
                                                    context.imageLoader.diskCache?.get(url)
                                                        ?.use { snapshot ->
                                                            val imageFile =
                                                                snapshot.data.toFile()
                                                            FileUtil.saveImageToExternalStorage(
                                                                context,
                                                                imageFile
                                                            )
                                                        }
                                                } ?: throw NoSuchElementException("image lost")
                                                memeUpdateFlag++
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.save_to_local_text, 1),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.cannot_locate_img),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = "download"
                                            )
                                        })
                                }
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = Color(red = 230, green = 225, blue = 229),
                            titleContentColor = Color(red = 230, green = 225, blue = 229),
                            actionIconContentColor = Color(red = 230, green = 225, blue = 229),
                        )
                    )
                }
            }
        },
        detectGesture = {
            onTap = {
                showImagePreviewerToolbar = !showImagePreviewerToolbar
            }
        }
    )
}