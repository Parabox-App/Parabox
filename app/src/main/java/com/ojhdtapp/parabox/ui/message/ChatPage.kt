package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.mlkit.nl.entityextraction.*
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.message_content.*
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.ui.MainSharedUiEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.bubble.BubbleActivity
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.RoundedCornerDropdownMenu
import com.origeek.imageViewer.ImagePreviewer
import com.origeek.imageViewer.rememberPreviewerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    onRecallMessage = { type, id ->
                        onEvent(
                            ActivityEvent.RecallMessage(
                                type = type,
                                messageId = id
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
                        mainSharedViewModel.clearQuoteMessage()
                        mainSharedViewModel.clearAt()
                        val selectedPluginConnection = messageState.selectedPluginConnection
                            ?: messageState.pluginConnectionList.firstOrNull()
                        if (selectedPluginConnection == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.no_plugin_connection_selected),
                                Toast.LENGTH_SHORT
                            ).show()
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
                    onShowInBubble = {
                        if (messageState.contact != null && messageState.selectedPluginConnection != null) {
                            onEvent(
                                ActivityEvent.ShowInBubble(
                                    contact = messageState.contact!!.copy(
                                        profile = Profile(
                                            messageState.contact?.profile?.name ?: "会话",
                                            null,
                                            null,
                                            null
                                        )
                                    ),
                                    message = Message(
                                        emptyList(),
                                        Profile("null", null, null, null),
                                        System.currentTimeMillis(),
                                        Long.MIN_VALUE,
                                        true,
                                        true,
                                        null
                                    ),
                                    channelId = messageState.selectedPluginConnection!!.connectionType.toString()
                                )
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class, ExperimentalLayoutApi::class, ExperimentalCoilApi::class
)
@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    messageState: MessageState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    isInSplitScreen: Boolean = false,
    onRecallMessage: (type: Int, messageId: Long) -> Unit,
    onStopSplitting: () -> Unit = {},
    onBackClick: () -> Unit,
    onSend: (contents: List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onStartAudioPlaying: (uri: Uri?, url: String?) -> Unit,
    onPauseAudioPlaying: () -> Unit,
    onResumeAudioPlaying: () -> Unit,
    onSetAudioProgressByFraction: (progressFraction: Float) -> Unit,
    onVibrate: () -> Unit,
    onShowInBubble: () -> Unit,
) {
    // Util
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    // Paging
    val pagingDataFlow = remember(messageState) {
        mainSharedViewModel.receiveMessagePagingDataFlow(messageState.pluginConnectionList.map { it.objectId })
    }
    val lazyPagingItems =
        pagingDataFlow.collectAsLazyPagingItems()

    val pluginPackageNameList =
        mainSharedViewModel.pluginListStateFlow.collectAsState().value.map { it.packageName }

    // Top AppBar
    var menuExpanded by remember {
        mutableStateOf(false)
    }
//    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
    val paddingValues = WindowInsets.systemBars.asPaddingValues()
    var changedTextFieldHeight by remember {
        mutableStateOf(0)
    }
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
    )
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
    LaunchedEffect(true) {
        mainSharedViewModel.uiEventFlow.collect {
            if (it is MainSharedUiEvent.NavigateToChatMessage) {
                lazyPagingItems.itemSnapshotList.items.map { it.messageId }
                    .lastIndexOf(it.message.messageId).let { index ->
                        if (index != -1) {
                            scrollState.animateScrollToItem(index)
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
    val userName by mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME)
    val avatarUri by mainSharedViewModel.userAvatarFlow.collectAsState(initial = null)

    // Smart Reply
    var smartReplyList by remember {
        mutableStateOf<List<String>>(emptyList())
    }
    LaunchedEffect(pagingDataFlow.collectAsLazyPagingItems().itemCount) {
        messageState.contact?.contactId?.let {
            smartReplyList = (context as MainActivity).getSmartReplyList(it).map {
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
                Text(
                    text = stringResource(R.string.delete_message)
                )
            },
            text = {
                Text(
                    stringResource(
                        id = R.string.delete_message_text,
                        mainSharedViewModel.selectedMessageStateList.size
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteMessageConfirmDialog = false
                        mainSharedViewModel.deleteMessage(mainSharedViewModel.selectedMessageStateList.map { it.messageId })
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
                            mainSharedViewModel.deleteMessage(listOf(it))
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
    // Search
    var searchText by remember {
        mutableStateOf("")
    }
    var enableSearch by remember {
        mutableStateOf(false)
    }
    val searchResult by remember {
        derivedStateOf {
            if (searchText.isBlank()) emptyList()
            else
//            lazyPagingItems.itemSnapshotList.items.filter {
//                it.contents.getContentString().contains(searchText)
//            }
                lazyPagingItems.itemSnapshotList.items.mapIndexed { index, message ->
                    index to message
                }.filter {
                    it.second.contents.getContentString().contains(searchText)
                }
        }
    }
    var searchExpanded by remember {
        mutableStateOf(false)
    }
    // Image Preview
    val imageViewerState = rememberPreviewerState()
    LaunchedEffect(messageState) {
        imageViewerState.hide()
    }
    val imageList =
        produceState(
            initialValue = emptyList<Pair<Long, ImageBitmap>>(),
            key1 = lazyPagingItems.itemSnapshotList
        ) {
            if (!imageViewerState.show) {
                value =
                    lazyPagingItems.itemSnapshotList.items.fold(mutableListOf<Pair<Long, ImageBitmap>>()) { acc, message ->
                        val imageMessageList = message.contents.filterIsInstance<Image>()
                        val lastIndex = imageMessageList.lastIndex
                        imageMessageList.reversed().forEachIndexed { index, t ->
                            if (t.uriString != null) {
                                Log.d("parabox", "${t.uriString}")
                                FileUtil.getBitmapFromUri(context, Uri.parse(t.uriString))?.let {
                                    acc.add(
                                        "${message.messageId}${
                                            (lastIndex - index).coerceIn(
                                                0,
                                                lastIndex
                                            )
                                        }".toLong() to it.asImageBitmap()
                                    )
                                }
                            } else if (t.url != null) {
                                val loader = ImageLoader(context)
                                val request = ImageRequest.Builder(context)
                                    .data(t.url)
                                    .allowHardware(false)
                                    .build()
                                val bitmap = try {
                                    val result = (loader.execute(request) as SuccessResult).drawable
                                    (result as BitmapDrawable).bitmap.asImageBitmap()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    ImageBitmap(1, 1)
                                }
                                acc.add(
                                    "${message.messageId}${
                                        (lastIndex - index).coerceIn(
                                            0,
                                            lastIndex
                                        )
                                    }".toLong() to bitmap
                                )
                            }
                        }
                        acc
                    }.reversed()
            }
        }
    var showImagePreviewerToolbar by remember {
        mutableStateOf(true)
    }
    var imagePreviewerMenuExpanded by remember {
        mutableStateOf(false)
    }
    ImagePreviewer(modifier = Modifier.zIndex(9f),
        count = imageList.value.size,
        state = imageViewerState,
        imageLoader = { index ->
            if (index < imageList.value.size) {
                imageList.value[index].second
            } else {
                ImageBitmap(1, 1)
            }
        },
        foreground = { total, current ->
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
                        IconButton(onClick = { imageViewerState.hide() }) {
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
                                                imageList.value.getOrNull(current)?.first
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
                                                imageList.value.getOrNull(current)?.first
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
        },
        onTap = {
            showImagePreviewerToolbar = !showImagePreviewerToolbar
        }
    )
    val useDarkIcons = isSystemInDarkTheme()
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(imageViewerState.show) {
        if (imageViewerState.show) {
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
            Crossfade(
                targetState = when {
                    enableSearch -> 1
                    mainSharedViewModel.selectedMessageStateList.isNotEmpty() -> 2
                    else -> 3
                }
            ) {
                when (it) {
                    1 -> {
                        TopAppBar(
                            title = {
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.TopStart)
                                ) {
                                    androidx.compose.material3.TextField(
                                        value = searchText,
                                        onValueChange = {
                                            searchText = it
                                            searchExpanded = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        placeholder = {
                                            Text(text = stringResource(R.string.search_in_conversation))
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Search
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                focusManager.clearFocus()
                                                searchExpanded = true
                                            }
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                focusManager.clearFocus()
                                                searchExpanded = true
                                            }) {
                                                Icon(
                                                    Icons.Outlined.Search,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        colors = TextFieldDefaults.textFieldColors(
                                            containerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent
                                        )
                                    )
                                    RoundedCornerDropdownMenu(
                                        expanded = searchExpanded && searchResult.isNotEmpty(),
                                        onDismissRequest = { searchExpanded = false },
                                    ) {
                                        searchResult.forEach {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = it.second.contents.getContentString(),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                onClick = {
                                                    coroutineScope.launch {
                                                        scrollState.animateScrollToItem(it.first)
                                                    }
                                                },
                                                leadingIcon = {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(
                                                                it.second.profile.avatar
                                                                    ?: it.second.profile.avatarUri
                                                            )
                                                            .crossfade(true)
                                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                            .build(),
                                                        contentDescription = "avatar",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(color = appBarContainerColor)
                                .statusBarsPadding(),
                            navigationIcon = {
                                IconButton(onClick = {
                                    enableSearch = false
                                    searchExpanded = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "close"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = appBarContainerColor,
                            )
//                        scrollBehavior = scrollBehavior
                        )
                    }

                    2 -> {
                        TopAppBar(
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
                            modifier = Modifier
                                .background(color = appBarContainerColor)
                                .statusBarsPadding(),
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
                                    visible = mainSharedViewModel.selectedMessageStateList.isNotEmpty(),
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    IconButton(onClick = {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.save_to_clipboard),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        clipboardManager.setText(AnnotatedString(
                                            buildString {
                                                mainSharedViewModel.selectedMessageStateList.forEachIndexed { index, s ->
                                                    if (index != mainSharedViewModel.selectedMessageStateList.lastIndex) {
                                                        append(s.contents.getContentString())
                                                        append(" ")
                                                    }
                                                }
                                            }
                                        ))
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
                                    RoundedCornerDropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.width(192.dp)
                                    ) {
                                        if (mainSharedViewModel.selectedMessageStateList.size == 1 && mainSharedViewModel.selectedMessageStateList.firstOrNull()?.sentByMe == true) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(R.string.try_to_recall)) },
                                                onClick = {
                                                    if (mainSharedViewModel.selectedMessageStateList.size == 1) {
                                                        val message =
                                                            mainSharedViewModel.selectedMessageStateList.first()
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
                                        if (mainSharedViewModel.selectedMessageStateList.size == 1) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(R.string.reply)) },
                                                onClick = {
                                                    if (mainSharedViewModel.selectedMessageStateList.size == 1)
                                                        mainSharedViewModel.setQuoteMessage(
                                                            mainSharedViewModel.selectedMessageStateList.firstOrNull(),
                                                            userName
                                                        )
                                                    mainSharedViewModel.clearSelectedMessageStateList()
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
                    }

                    3 -> {
                        TopAppBar(
                            title = {
                                Text(
                                    text = messageState.contact?.profile?.name
                                        ?: stringResource(R.string.conversation),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier
                                .background(color = appBarContainerColor)
                                .statusBarsPadding(),
                            navigationIcon = {
                                IconButton(onClick = { onBackClick() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    enableSearch = true
                                    searchText = ""
                                }) {
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
                                    RoundedCornerDropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.width(192.dp)
                                    ) {
                                        if (isInSplitScreen) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(R.string.stop_splitting)) },
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
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            DropdownMenuItem(
                                                text = { Text(text = stringResource(R.string.display_in_bubble)) },
                                                onClick = {
                                                    menuExpanded = false
                                                    onShowInBubble()
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.BubbleChart,
                                                        contentDescription = null
                                                    )
                                                })
                                        }
                                    }
                                }
                            }, colors = TopAppBarDefaults.smallTopAppBarColors(
                                containerColor = appBarContainerColor
                            )
//                        scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mainSharedViewModel.recordAmplitudeStateList.isNotEmpty() || mainSharedViewModel.quoteMessageState.value != null || mainSharedViewModel.atState.value != null || fabExtended,
                enter = slideInHorizontally { it * 2 }  // slide in from the right
                , exit = slideOutHorizontally { it * 2 } // slide out to the right
            ) {
                ExtendedFloatingActionButton(
                    expanded = mainSharedViewModel.recordAmplitudeStateList.isNotEmpty() || mainSharedViewModel.quoteMessageState.value != null || mainSharedViewModel.atState.value != null && quoteExtended,
                    onClick = {
                        if (mainSharedViewModel.recordAmplitudeStateList.isNotEmpty()) {
                            if (mainSharedViewModel.audioRecorderState.value.let {
                                    it !is AudioRecorderState.Ready
                                }) {

                            } else {
                                if (mainSharedViewModel.isAudioPlaying.value) {
                                    onPauseAudioPlaying()
                                } else {
                                    onResumeAudioPlaying()
                                }
                            }
                        } else if (mainSharedViewModel.quoteMessageState.value != null) {
                            coroutineScope.launch {
                                val idList =
                                    lazyPagingItems.itemSnapshotList.items.map { it.messageId }
                                idList.lastIndexOf(mainSharedViewModel.quoteMessageState.value?.messageId)
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
                                mainSharedViewModel.audioRecorderState.value.let {
                                    it !is AudioRecorderState.Ready
                                } -> 1

                                mainSharedViewModel.recordAmplitudeStateList.isNotEmpty() && mainSharedViewModel.isAudioPlaying.value -> 2
                                mainSharedViewModel.recordAmplitudeStateList.isNotEmpty() -> 3
                                mainSharedViewModel.atState.value != null -> 4
                                mainSharedViewModel.quoteMessageState.value != null -> 5
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
                            if (mainSharedViewModel.recordAmplitudeStateList.isNotEmpty()) {
                                AmplitudeIndicator(
                                    modifier = Modifier.size(
                                        width = 60.dp,
                                        height = 24.dp
                                    ),
                                    amplitudeList = mainSharedViewModel.recordAmplitudeStateList,
                                    progressFraction = mainSharedViewModel.audioPlayerProgressFraction.value,
                                    onPause = onPauseAudioPlaying,
                                    onResumeAtFraction = {
                                        onSetAudioProgressByFraction(it)
                                        onResumeAudioPlaying()
                                    }
                                )
                            } else {
                                if (mainSharedViewModel.quoteMessageState.value == null) {
                                    mainSharedViewModel.atState.value?.let {
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
                                            IconButton(onClick = { mainSharedViewModel.clearAt() }) {
                                                Icon(
                                                    modifier = Modifier.size(18.dp),
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "cancel"
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    mainSharedViewModel.quoteMessageState.value?.let {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.widthIn(0.dp, 208.dp)) {
                                                Text(
//                                                    text = "${if(mainSharedViewModel.atState.value!=null)"@" else ""}${it.profile.name}",
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
                                            IconButton(onClick = { mainSharedViewModel.clearQuoteMessage() }) {
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
                isBottomSheetExpand = scaffoldState.bottomSheetState.isExpanded,
                packageNameList = pluginPackageNameList,
                quoteMessageSelected = mainSharedViewModel.quoteMessageState.value,
                at = mainSharedViewModel.atState.value,
                audioState = audioState,
                audioRecorderState = mainSharedViewModel.audioRecorderState.value,
                memeUpdateFlag = memeUpdateFlag,
                functionalAreaOpacity = opacityState,
                onMemeUpdate = { memeUpdateFlag++ },
                onAudioStateChanged = { audioState = it },
                onAudioRecorderStateChanged = { mainSharedViewModel.setAudioRecorderState(it) },
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
                    mainSharedViewModel.clearRecordAmplitudeStateList()
                },
                onStartRecording = onStartRecording,
                onStopRecording = onStopRecording,
                onClearRecording = {
                    mainSharedViewModel.clearRecordAmplitudeStateList()
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
        BackHandler(enabled = mainSharedViewModel.quoteMessageState.value != null) {
            mainSharedViewModel.clearQuoteMessage()
        }
        BackHandler(enabled = mainSharedViewModel.selectedMessageStateList.size != 0) {
            mainSharedViewModel.clearSelectedMessageStateList()
        }
        BackHandler(enabled = enableSearch) {
            enableSearch = false
            searchText = ""
            searchExpanded = false
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
                                            com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText(
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
                                    mainSharedViewModel.clearRecordAmplitudeStateList()
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
                            selectedMessageStateList = mainSharedViewModel.selectedMessageStateList,
                            shouldShowTimeDivider = shouldShowTimeDivider,
                            clickingMessage = clickingMessage,
                            isFirst = isFirst,
                            isLast = isLast,
                            userName = userName,
                            avatarUri = avatarUri,
                            isTranslationEnabled = mainSharedViewModel.translationFlow.collectAsState(
                                initial = true
                            ).value,
                            onClickingDismiss = { clickingMessage = null },
                            onClickingEvent = {
                                when (it) {
                                    is SingleMessageEvent.FailRetry -> {
                                        mainSharedViewModel.deleteMessage(listOf(value.messageId))
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
                                        mainSharedViewModel.setQuoteMessage(
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
                                                context.getString(
                                                    R.string.save_to_local_text,
                                                    images.size
                                                ),
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
                                if (mainSharedViewModel.selectedMessageStateList.isNotEmpty()) {
                                    mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
                                        value
                                    )
                                } else {
                                    if (value.contents.any { it is Image }) {
                                        val index =
                                            imageList.value.indexOfLast { it.first == "${value.messageId}0".toLong() }
                                        if (index != -1) {
                                            imageViewerState.show(index)
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
                                    mainSharedViewModel.addOrRemoveItemOfSelectedMessageStateList(
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
                                    mainSharedViewModel.setAtState(
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
//                timedList.forEach { (timestamp, chatBlockList) ->
//                    items(
//                        items = chatBlockList,
//                        key = { "${it.profile.name}:${timestamp}:${it.messages.first().timestamp}" }) { chatBlock ->
//                        com.ojhdtapp.parabox.ui.message.ChatBlock(
//                            modifier = Modifier.fillMaxWidth(),
//                            mainSharedViewModel = mainSharedViewModel,
//                            data = chatBlock,
//                            sentByMe = chatBlock.messages.first().sentByMe,
//                            userName = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
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
                text = stringResource(R.string.select_conversation),
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
    clickingMessage: Message?,
    isFirst: Boolean,
    isLast: Boolean,
    userName: String,
    avatarUri: String?,
    fromBubble: Boolean = false,
    isTranslationEnabled: Boolean,
    onClickingDismiss: () -> Unit,
    onClickingEvent: (event: SingleMessageEvent) -> Unit,
    onMessageClick: () -> Unit,
    onMessageLongClick: () -> Unit,
    onQuoteReplyClick: (messageId: Long) -> Unit,
    onAudioClick: (uri: Uri?, url: String?) -> Unit,
    onAvatarLongClick: () -> Unit,
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
//                Spacer(modifier = Modifier.width(64.dp))
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
                        clickingMessage = clickingMessage,
                        fromBubble = fromBubble,
                        isTranslationEnabled = isTranslationEnabled,
                        onClickingDismiss = onClickingDismiss,
                        onClickingEvent = onClickingEvent,
                        onClick = onMessageClick,
                        onLongClick = onMessageLongClick,
                        onQuoteReplyClick = onQuoteReplyClick,
                        onAudioClick = onAudioClick,
                    )
                    if (!isLast) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                MessageAvatar(
                    shouldDisplay = isFirst,
                    avatar = null,
                    avatarUri = avatarUri,
                    name = userName,
                    onClick = {},
                    onLongClick = onAvatarLongClick,
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Spacer(modifier = Modifier.width(16.dp))
                MessageAvatar(
                    shouldDisplay = isFirst,
                    avatar = message.profile.avatar,
                    avatarUri = message.profile.avatarUri,
                    name = message.profile.name,
                    onClick = {},
                    onLongClick = onAvatarLongClick,
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
                        clickingMessage = clickingMessage,
                        fromBubble = fromBubble,
                        isTranslationEnabled = isTranslationEnabled,
                        onClickingDismiss = onClickingDismiss,
                        onClickingEvent = onClickingEvent,
                        onClick = onMessageClick,
                        onLongClick = onMessageLongClick,
                        onQuoteReplyClick = onQuoteReplyClick,
                        onAudioClick = onAudioClick,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageAvatar(
    modifier: Modifier = Modifier,
    shouldDisplay: Boolean,
    avatar: String?,
    avatarUri: String?,
    name: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) =
    Box(modifier = Modifier.size(42.dp)) {
        if (shouldDisplay) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        AvatarUtil.getAvatar(
                            uri = avatarUri?.let { Uri.parse(it) },
                            url = avatar,
                            name = name,
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    )
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                    .build(),
                contentDescription = "avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .combinedClickable(
                        enabled = true,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
            )
        }
    }

@Composable
fun TimeDivider(modifier: Modifier = Modifier, timestamp: Long) {
    val context = LocalContext.current
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
            text = timestamp.toDescriptiveTime(context),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SingleMessage(
    modifier: Modifier = Modifier,
    message: Message,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    clickingMessage: Message?,
    fromBubble: Boolean = false,
    isTranslationEnabled: Boolean,
    onClickingDismiss: () -> Unit,
    onClickingEvent: (event: SingleMessageEvent) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuoteReplyClick: (messageId: Long) -> Unit = {},
    onAudioClick: (uri: Uri?, url: String?) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
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
    val reverseTextColor by animateColorAsState(
        targetValue = if (!message.sentByMe) {
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
        } else {
            if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        }
    )
    val primaryTextColor =
        if (message.sentByMe) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
    var entities by remember {
        mutableStateOf<List<Pair<Entity, String>>>(
            emptyList()
        )
    }
    var shouldTranslate by remember {
        mutableStateOf(false)
    }
    var translatedText by remember {
        mutableStateOf<String?>(null)
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val messageMaxWidth = screenWidth - 130.dp
    LaunchedEffect(true) {
        val tempList = mutableListOf<Pair<Entity, String>>()
        if (fromBubble) {
            (context as BubbleActivity).getEntityAnnotationList(message.contents.getContentString())
                .forEach { entityAnnotation ->
                    entityAnnotation.entities.forEach {
                        tempList.add(it to entityAnnotation.annotatedText)
                    }
                }
        } else {
            (context as MainActivity).getEntityAnnotationList(message.contents.getContentString())
                .forEach { entityAnnotation ->
                    entityAnnotation.entities.forEach {
                        tempList.add(it to entityAnnotation.annotatedText)
                    }
                }
        }
        entities = tempList
        val languageCode =
            if (fromBubble) {
                (context as BubbleActivity).getLanguageCode(message.contents.getContentString())
            } else {
                (context as MainActivity).getLanguageCode(message.contents.getContentString())
            }
        shouldTranslate = !(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
            ?.let { LanguageUtil.languageTagMapper(it) }
            ?.contentEquals(languageCode) ?: true)
    }
    SelectionContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(align = if (message.sentByMe) Alignment.TopEnd else Alignment.TopStart)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (message.sentByMe) {
                    if (!message.verified) {
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (abs(System.currentTimeMillis() - message.timestamp) > 5000) {
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
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }
                MessageContentContainer(
                    modifier = modifier
                        .widthIn(0.dp, messageMaxWidth)
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
                        .combinedClickable(
                            enabled = !isSelected,
                            onClick = onClick,
                            onLongClick = onLongClick
                        )
//                        .clickable(enabled = isSelected, onClick = onClick)
                        .animateContentSize(),
                    shouldBreak = message.contents.map {
                        it is Image || it is QuoteReply || it is Audio || it is File
                    }.plus(arrayOf(true, true))
                ) {
                    message.contents.forEachIndexed { index, messageContent ->
                        messageContent.toLayout(
                            textColor,
                            reverseTextColor,
                            primaryTextColor,
                            context,
                            density,
                            index,
                            topStartRadius,
                            topEndRadius,
                            message,
                            bottomEndRadius,
                            bottomStartRadius,
                            isSelected,
                            onQuoteReplyClick = onQuoteReplyClick,
                            onAudioClick = onAudioClick,
                        )
                    }
                    AnimatedVisibility(visible = translatedText != null) {
                        Surface(
                            modifier = Modifier.padding(8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = translatedText!!,
                                modifier = Modifier.padding(12.dp),
                                color = textColor
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = entities) {
                            when (it.first.type) {
                                Entity.TYPE_ADDRESS -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = {
                                            BrowserUtil.launchMap(context, it.second)
                                        },
                                        label = {
                                            Text(
                                                text = it.second.ellipsis(maxLength = 10),
                                                color = textColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Place,
                                                contentDescription = "address",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_DATE_TIME -> {
                                    val timestamp = it.first.asDateTimeEntity()!!.timestampMillis
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = {
                                            if (fromBubble) {
                                                (context as MainActivity).startActivity(
                                                    Intent(
                                                        Intent.ACTION_INSERT
                                                    ).apply {
                                                        data = CalendarContract.Events.CONTENT_URI
                                                        putExtra(
                                                            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                                            timestamp
                                                        )
                                                        putExtra(
                                                            CalendarContract.EXTRA_EVENT_END_TIME,
                                                            timestamp
                                                        )
                                                    })
                                            } else {
                                                (context as MainActivity).startActivity(
                                                    Intent(
                                                        Intent.ACTION_INSERT
                                                    ).apply {
                                                        data = CalendarContract.Events.CONTENT_URI
                                                        putExtra(
                                                            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                                            timestamp
                                                        )
                                                        putExtra(
                                                            CalendarContract.EXTRA_EVENT_END_TIME,
                                                            timestamp
                                                        )
                                                    })
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = timestamp.toDescriptiveDateAndTime(),
                                                color = textColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Event,
                                                contentDescription = "date",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_EMAIL -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = {
                                            BrowserUtil.composeEmail(
                                                context,
                                                arrayOf(it.second),
                                                null
                                            )
                                        },
                                        label = { Text(text = it.second, color = textColor) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Email,
                                                contentDescription = "email",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_FLIGHT_NUMBER -> {
                                    val flightStr = it.first.asFlightNumberEntity()!!
                                        .let { "${it.airlineCode} ${it.flightNumber}" }
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = { clipboardManager.setText(AnnotatedString(text = flightStr)) },
                                        label = {
                                            Text(text = flightStr, color = textColor)
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FlightTakeoff,
                                                contentDescription = "flight",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_IBAN -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = { clipboardManager.setText(AnnotatedString(text = it.second)) },
                                        label = { Text(text = it.second, color = textColor) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountBalance,
                                                contentDescription = "bank",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_ISBN -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = { clipboardManager.setText(AnnotatedString(text = it.second)) },
                                        label = { Text(text = it.second, color = textColor) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.LocalLibrary,
                                                contentDescription = "isbn",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_PAYMENT_CARD -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = { clipboardManager.setText(AnnotatedString(text = it.second)) },
                                        label = { Text(text = it.second, color = textColor) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.CreditCard,
                                                contentDescription = "card",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_PHONE -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = {
                                            if (fromBubble) {
                                                (context as BubbleActivity).startActivity(
                                                    Intent(
                                                        Intent.ACTION_DIAL
                                                    ).apply {
                                                        data = Uri.parse("tel:${it.second}")
                                                    })
                                            } else {
                                                (context as MainActivity).startActivity(
                                                    Intent(
                                                        Intent.ACTION_DIAL
                                                    ).apply {
                                                        data = Uri.parse("tel:${it.second}")
                                                    })
                                            }
                                        },
                                        label = { Text(text = it.second, color = textColor) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Call,
                                                contentDescription = "call",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                Entity.TYPE_URL -> {
                                    AssistChip(
                                        shape = CircleShape,
                                        onClick = { BrowserUtil.launchURL(context, it.second) },
                                        label = {
                                            Text(
                                                text = it.second.ellipsis(20),
                                                color = textColor
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Link,
                                                contentDescription = "url",
                                                tint = primaryTextColor,
                                            )
                                        }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
                if (!message.sentByMe) {
                    if (isTranslationEnabled) {
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.animation.AnimatedVisibility(visible = shouldTranslate && translatedText.isNullOrEmpty()) {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        val text = if (fromBubble) {
                                            (context as BubbleActivity).getTranslation(message.contents.getContentString())
                                        } else {
                                            (context as MainActivity).getTranslation(message.contents.getContentString())
                                        }
                                        if (text == null) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.translation_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            translatedText = text
                                        }

                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Translate,
                                        contentDescription = "translate",
                                        tint = textColor
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }

            }
            MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = CircleShape)) {
                DropdownMenu(
                    offset = DpOffset(0.dp, 4.dp),
                    expanded = clickingMessage == message,
                    onDismissRequest = onClickingDismiss
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                        if (message.sentByMe && !message.verified) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.FailRetry)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "retry"
                                )
                            }
                        }
                        if (message.verified) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.Reply)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Reply,
                                    contentDescription = "reply"
                                )
                            }
                        }
                        if (message.sentByMe && message.verified) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.Recall)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Undo,
                                    contentDescription = "recall"
                                )
                            }
                        }
                        if (message.contents.any { it is Image }) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.Favorite)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = "favorite"
                                )
                            }
                        }
                        if (message.contents.any { it !is Image }) {
                            IconButton(onClick = {
                                onLongClick()
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "select"
                                )
                            }
                        }
                        if (message.contents.any { it is PlainText }) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.Copy)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "copy"
                                )
                            }
                        }
                        if (message.contents.any { it is Image }) {
                            IconButton(onClick = {
                                onClickingEvent(SingleMessageEvent.Download)
                                onClickingDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "download"
                                )
                            }
                        }
                        IconButton(onClick = {
                            // Temp ClickingMessage
                            onClickingEvent(SingleMessageEvent.Delete)
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "delete"
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageContent.toLayout(
    textColor: Color,
    reverseTextColor: Color,
    primaryTextColor: Color,
    context: Context,
    density: Density,
    index: Int,
    topStartRadius: Dp,
    topEndRadius: Dp,
    message: Message,
    bottomEndRadius: Dp,
    bottomStartRadius: Dp,
    isSelected: Boolean,
    onQuoteReplyClick: (messageId: Long) -> Unit = {},
    onAudioClick: (uri: Uri?, url: String?) -> Unit,
) {
    when (this) {
        is At, AtAll -> Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            text = getContentString(),
            color = primaryTextColor,
        )

        is PlainText ->
            if (isSelected) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    text = text,
                    color = textColor
                )
            } else {
                DisableSelection {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        text = text,
                        color = textColor
                    )
                }
            }
//                SelectionContainer {
//                    if(isSelected){
//                        Text(
//                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
//                            text = text,
//                            color = textColor
//                        )
//                    } else {
//                        DisableSelection {
//                            Text(
//                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
//                                text = text,
//                                color = textColor
//                            )
//                        }
//                    }
//                }
        is Image -> {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
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
                    .widthIn(128.dp, 320.dp)
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
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
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
                                    text = it.toDescriptiveTime(context),
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
                            if (message.sentByMe) reverseTextColor else textColor,
                            if (message.sentByMe) textColor else reverseTextColor,
                            primaryTextColor,
                            context,
                            density,
                            index,
                            0.dp,
                            0.dp,
                            message,
                            0.dp,
                            0.dp,
                            isSelected,
                            onAudioClick = onAudioClick
                        )
                    }
                    if (quoteMessageContent == null) {
                        Text(
                            text = stringResource(R.string.cannot_locate_msg),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        is Audio -> {
            Row(
                modifier = Modifier.padding(start = 1.dp, end = 12.dp, top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    onClick = {
                        onAudioClick(uriString?.let { Uri.parse(it) }, url)
                    }
                ) {
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = "record",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = length.toMSString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
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
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (extension) {
                                "apk" -> Icons.Outlined.Android
                                "bmp", "jpeg", "jpg", "png", "tif", "gif", "pcx", "tga", "exif", "fpx", "svg", "psd", "cdr", "pcd", "dxf", "ufo", "eps", "ai", "raw", "webp", "avif", "apng", "tiff" -> Icons.Outlined.Image
                                "txt", "log", "md", "json", "xml" -> Icons.Outlined.Description
                                "cd", "wav", "aiff", "mp3", "wma", "ogg", "mpc", "flac", "ape", "3gp" -> Icons.Outlined.AudioFile
                                "avi", "wmv", "mp4", "mpeg", "mpg", "mov", "flv", "rmvb", "rm", "asf" -> Icons.Outlined.VideoFile
                                "zip", "rar", "7z", "bz2", "tar", "jar", "gz", "deb" -> Icons.Outlined.FolderZip
                                else -> Icons.Outlined.FilePresent
                            }, contentDescription = "type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column() {
                    Text(
                        text = name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryTextColor
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

