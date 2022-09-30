package com.ojhdtapp.parabox.ui.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.BottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.*
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.toMessageContentList
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun EditArea(
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    isBottomSheetExpand: Boolean,
    packageNameList: List<String>,
    quoteMessageSelected: Message?,
    at: At?,
    audioState: Boolean,
    audioRecorderState: AudioRecorderState,
    memeUpdateFlag: Int = 0,
    functionalAreaOpacity: Float,
    onMemeUpdate: () -> Unit,
    onAudioStateChanged: (value: Boolean) -> Unit,
    onAudioRecorderStateChanged: (value: AudioRecorderState) -> Unit,
    onBottomSheetExpand: () -> Unit,
    onBottomSheetCollapse: () -> Unit,
    onSend: (contents: List<MessageContent>) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecording: () -> Unit,
    onTextFieldHeightChange: (height: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Meme
    val memeList = remember {
        mutableStateListOf<File>()
    }
    LaunchedEffect(key1 = memeUpdateFlag) {
        memeList.clear()
        context.getExternalFilesDir("meme")?.listFiles()
            ?.filter {
                it.path.endsWith(".jpg") || it.path.endsWith(".jpeg") || it.path.endsWith(".png") || it.path.endsWith(
                    ".gif"
                )
            }
            ?.reversed()
            ?.also {
                memeList.addAll(it)
            }
    }

    // Image Send
    val gallerySelected = remember {
        mutableStateListOf<Uri>()
    }
    val cameraSelected = remember {
        mutableStateListOf<Uri>()
    }
    var emojiState by remember {
        mutableStateOf(false)
    }
//    var inputText by remember {
//        mutableStateOf("")
//    }
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = ""
            )
        )
    }
    var shouldToolbarShrink by remember {
        mutableStateOf(false)
    }
    val audioPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.RECORD_AUDIO)

    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let {
                    var i = 0
                    while (i < it.clipData!!.itemCount) {
                        it.clipData!!.getItemAt(i).also {
                            if (!gallerySelected.contains(it.uri)) {
                                gallerySelected.add(it.uri)
                            }
                            Log.d("parabox", it.uri.toString())
                        }
                        i++
                    }
                }
            }
        }
    val imagePickerSLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) {
            it.forEach {
                if (!gallerySelected.contains(it)) {
                    gallerySelected.add(it)
                }
                Log.d("parabox", it.toString())
            }
        }
    val memePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val path = context.getExternalFilesDir("meme")!!
                it.data?.let {
                    var i = 0
                    while (i < it.clipData!!.itemCount) {
                        it.clipData!!.getItemAt(i).also {
                            FileUtil.copyFileToPath(
                                context, path,
                                "Image_${
                                    System.currentTimeMillis()
                                        .toDateAndTimeString()
                                }.jpg",
                                it.uri
                            )
                        }
                        i++
                    }
                }
                onMemeUpdate()
            }
        }
    val memePickerSLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) {
            val path = context.getExternalFilesDir("meme")!!
            it.forEach {
                FileUtil.copyFileToPath(
                    context, path,
                    "Image_${
                        System.currentTimeMillis()
                            .toDateAndTimeString()
                    }.jpg",
                    it
                )
            }
            onMemeUpdate()
        }
    val targetCameraShotUri = remember(cameraSelected.size) {
//        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val path = context.getExternalFilesDir("chat")!!
        val outPutFile = File(path.also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }, "${System.currentTimeMillis().toDateAndTimeString()}.jpg")
        FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider", outPutFile
        )
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            if (it) {
                cameraSelected.add(targetCameraShotUri)
                Log.d("parabox", targetCameraShotUri.toString())
            }
        }
    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            Log.d("parabox", it.toString())
        }

    val cameraAccessible by remember {
        mutableStateOf<Boolean>(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
    }

    val permissionDeniedDialog = remember { mutableStateOf(false) }
    if (permissionDeniedDialog.value) {
        AlertDialog(
            onDismissRequest = {
                permissionDeniedDialog.value = false
            },
            icon = { Icon(Icons.Outlined.KeyboardVoice, contentDescription = null) },
            title = {
                Text(text = "权限申请")
            },
            text = {
                Text(
                    "要发送语音消息，您需要授权本应用使用设备麦克风。\n您亦可前往设置页面手动授权。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionDeniedDialog.value = false
                        audioPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("尝试授权")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        permissionDeniedDialog.value = false
                    }
                ) {
                    Text("转到设置")
                }
            }
        )
    }
    Surface(
        modifier = modifier
//            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface),
        tonalElevation = 3.dp
    ) {
        Column() {
            val relocation = remember { BringIntoViewRequester() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .bringIntoViewRequester(relocation)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                var isEditing by remember {
                    mutableStateOf(false)
                }
                val keyboardController = LocalSoftwareKeyboardController.current
                Crossfade(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .animateContentSize(),
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
                            IconButton(
                                enabled = audioRecorderState.let{
                                    it !is AudioRecorderState.Recording && it !is AudioRecorderState.Confirmed
                                },
                                onClick = {
                                keyboardController?.hide()
                                if (!isBottomSheetExpand) {
                                    onBottomSheetExpand()
                                    emojiState = false
                                } else {
                                    if (emojiState) {
                                        emojiState = false
                                    } else {
                                        onBottomSheetCollapse()
                                    }
                                }
                                onAudioStateChanged(false)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "more"
                                )
                            }
                            IconButton(
                                enabled = audioRecorderState.let{
                                    it !is AudioRecorderState.Recording && it !is AudioRecorderState.Confirmed
                                },
                                onClick = {
                                keyboardController?.hide()
                                if (!isBottomSheetExpand) {
                                    onBottomSheetExpand()
                                    emojiState = true
                                } else {
                                    if (!emojiState) {
                                        emojiState = true
                                    } else {
                                        onBottomSheetCollapse()
                                    }
                                }
                                onAudioStateChanged(false)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "emoji"
                                )
                            }
                        }
                    }
                }
//                var audioRecorderState by remember {
//                    mutableStateOf<AudioRecorderState>(AudioRecorderState.Ready)
//                }
                LaunchedEffect(key1 = audioState) {
                    if (!audioState) {
                        if(audioRecorderState is AudioRecorderState.Done){
                            onClearRecording()
                        }
                        onAudioRecorderStateChanged(AudioRecorderState.Ready)
                    }
                }
                AnimatedVisibility(visible = audioRecorderState is AudioRecorderState.Done) {
                    Surface(
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            onAudioRecorderStateChanged(AudioRecorderState.Ready)
                            onClearRecording()
                        }
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "clear",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.zIndex(2f),
                        visible = audioState,
                        enter = expandHorizontally { 0 },
                        exit = shrinkHorizontally { 0 }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            Row(
                                modifier = Modifier.indication(
                                    interactionSource,
                                    LocalIndication.current
                                ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .pointerInteropFilter {
                                            val press =
                                                PressInteraction.Press(Offset(it.x, it.y))
                                            when (it.action) {
                                                MotionEvent.ACTION_DOWN -> {
                                                    onAudioRecorderStateChanged(AudioRecorderState.Recording)
                                                    interactionSource.tryEmit(press)
                                                    onStartRecording()
                                                }
                                                MotionEvent.ACTION_MOVE -> {
                                                    onAudioRecorderStateChanged(
                                                        if (it.y < -150) AudioRecorderState.Confirmed else AudioRecorderState.Recording
                                                    )
                                                }
                                                MotionEvent.ACTION_UP -> {
                                                    interactionSource.tryEmit(
                                                        PressInteraction.Release(
                                                            press
                                                        )
                                                    )
                                                    onStopRecording()
                                                    if (audioRecorderState is AudioRecorderState.Confirmed) {
                                                        onClearRecording()
                                                        sendAudio(context, packageNameList) {
                                                            onSend(it)
                                                            onAudioRecorderStateChanged(
                                                                AudioRecorderState.Ready
                                                            )
                                                        }
                                                    } else {
                                                        onAudioRecorderStateChanged(
                                                            AudioRecorderState.Done
                                                        )
                                                    }
                                                }
                                                else -> false
                                            }
                                            true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = audioRecorderState.text,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                AnimatedVisibility(
                                    visible = audioRecorderState is AudioRecorderState.Ready,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally(),
                                ) {
                                    IconButton(onClick = {
                                        onAudioStateChanged(false)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Keyboard,
                                            contentDescription = "keyboard",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            onBottomSheetCollapse()
                            isEditing = true
                            coroutineScope.launch {
                                delay(200)
                                relocation.bringIntoView()
                            }
                        }
                    ) {
                        val originalBoxHeight = with(density) {
                            24.dp.toPx().toInt()
                        }
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .weight(1f)
                                    .padding(12.dp)
                                    .onSizeChanged {
                                        onTextFieldHeightChange(it.height - originalBoxHeight)
                                        coroutineScope.launch {
                                            delay(150)
                                            relocation.bringIntoView()
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val focusRequester = remember { FocusRequester() }
                                LaunchedEffect(isEditing) {
                                    if (isEditing) focusRequester.requestFocus()
                                }
                                BasicTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
//                                        .onFocusEvent {
//                                            if (it.isFocused) coroutineScope.launch {
//                                                delay(200)
//                                                relocation.bringIntoView()
//                                            }
//                                        }
                                        .focusRequester(focusRequester)
                                        .clearFocusOnKeyboardDismiss() {
                                            isEditing = false
                                        },
                                    value = textFieldValueState,
                                    onValueChange = {
                                        if (it.text.length > 6) shouldToolbarShrink = true
                                        else if (it.text.isEmpty()) shouldToolbarShrink = false
                                        textFieldValueState = it
                                    },
                                    enabled = isEditing && !audioState,
                                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (textFieldValueState.text.isEmpty()) {
                                            Text(
                                                text = "输入内容",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                            )
                                        }
                                        innerTextField()
                                    },
                                    cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
                                )
                            }
                            AnimatedVisibility(
                                visible = textFieldValueState.text.isEmpty() && gallerySelected.isEmpty() && cameraSelected.isEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    if (audioPermissionState.status.isGranted) {
                                        onBottomSheetCollapse()
                                        onAudioStateChanged(true)
                                        isEditing = false
                                    } else {
                                        permissionDeniedDialog.value = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardVoice,
                                        contentDescription = "voice"
                                    )
                                }
                            }
                        }
                    }

                }
                AnimatedVisibility(visible = ((textFieldValueState.text.isNotEmpty() || gallerySelected.isNotEmpty() || cameraSelected.isNotEmpty() || quoteMessageSelected != null || at != null) && !audioState) || (audioRecorderState is AudioRecorderState.Done && audioState),
//                    enter = slideInHorizontally { width -> width },
//                    exit = slideOutHorizontally { width -> width }
                    enter = expandHorizontally() { width -> 0 },
                    exit = shrinkHorizontally() { width -> 0 }
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (audioState) {
                                sendAudio(context, packageNameList) {
                                    onSend(it)
                                    onAudioRecorderStateChanged(AudioRecorderState.Ready)
                                }
                            } else {
                                val content = mutableListOf<MessageContent>()
                                if (quoteMessageSelected != null) {
                                    content.add(
                                        QuoteReply(
                                            quoteMessageSenderName = quoteMessageSelected.profile.name,
                                            quoteMessageTimestamp = quoteMessageSelected.timestamp,
                                            quoteMessageId = quoteMessageSelected.messageId,
                                            quoteMessageContent = quoteMessageSelected.contents.toMessageContentList()
                                        )
                                    )
                                }
                                if (at != null) {
                                    content.add(at)
                                }
                                if (textFieldValueState.text.isNotEmpty()) {
                                    content.add(PlainText(textFieldValueState.text))
                                }
                                gallerySelected.forEach {
                                    FileUtil.getUriByCopyingFileToPath(
                                        context,
                                        //                                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Parabox/Chat"),
                                        context.getExternalFilesDir("chat")!!,
                                        "${System.currentTimeMillis().toDateAndTimeString()}.jpg",
                                        it
                                    )?.also {
                                        packageNameList.forEach { packageName ->
                                            context.grantUriPermission(
                                                packageName,
                                                it,
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        }
                                        val intent = Intent().apply {
                                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            setDataAndType(it, "image/*")
                                        }
                                        content.add(Image(uri = it))
                                    }
                                }
                                cameraSelected.forEach {
                                    packageNameList.forEach { packageName ->
                                        context.grantUriPermission(
                                            packageName,
                                            it,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                    val intent = Intent().apply {
                                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        setDataAndType(it, context.contentResolver.getType(it))
                                    }
                                    content.add(Image(uri = it))
                                }
                                Log.d("parabox", content.toString())
                                if (content.size > 0) {
                                    onSend(content)
                                    onBottomSheetCollapse()
                                }
                                textFieldValueState = TextFieldValue(
                                    text = ""
                                )
                                gallerySelected.clear()
                                cameraSelected.clear()
                            }
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = "send")
                    }
                }
            }
            AnimatedContent(
                modifier = Modifier.alpha(functionalAreaOpacity),
                targetState = emojiState,
                transitionSpec = {
                    if (targetState) {
                        (slideIntoContainer(AnimatedContentScope.SlideDirection.Start) with slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Start
                        ))
                    } else {
                        (slideIntoContainer(AnimatedContentScope.SlideDirection.End) with slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.End
                        ))
                    }
                }) {
                if (it) {
                    Row(
                        modifier = Modifier
                            .height(height)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 4.dp)
                    ) {
                        var memeState by remember {
                            mutableStateOf(true)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(0.5f)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp, bottom = 2.dp),
                                color = if (memeState) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            memeState = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = "favorite",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp, top = 2.dp),
                                color = if (memeState) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            memeState = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Face,
                                        contentDescription = "face",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        AnimatedContent(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f), targetState = memeState
                        ) {
                            if (it) {
                                var showMemeDeleteBtn by remember {
                                    mutableStateOf(false)
                                }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 16.dp),
                                    contentPadding = PaddingValues(end = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (memeList.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier.fillParentMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "暂无自定义表情",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                TextButton(onClick = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val maxNumPhotosAndVideos = 10
                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
                                                            .apply {
                                                                type = "image/*"
                                                                putExtra(
                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                    maxNumPhotosAndVideos
                                                                )
                                                            }
                                                            .also {
                                                                memePickerLauncher.launch(it)
                                                            }
                                                    } else {
                                                        memePickerSLauncher.launch("image/*")
                                                    }
                                                }) {
                                                    Text(text = "手动添加")
                                                }
                                            }
                                        }
                                    } else {
                                        items(items = memeList, key = { it.name }) {
                                            Surface(
                                                modifier = Modifier.animateItemPlacement(),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                tonalElevation = 3.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(3.dp)
                                                ) {
                                                    val imageLoader =
                                                        ImageLoader.Builder(context)
                                                            .components {
                                                                if (Build.VERSION.SDK_INT >= 28) {
                                                                    add(ImageDecoderDecoder.Factory())
                                                                } else {
                                                                    add(GifDecoder.Factory())
                                                                }
                                                            }
                                                            .build()
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(
                                                            LocalContext.current
                                                        )
                                                            .data(it)
                                                            .crossfade(true)
                                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                            .build(),
                                                        imageLoader = imageLoader,
                                                        contentDescription = "meme",
                                                        contentScale = ContentScale.FillHeight,
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .widthIn(0.dp, 144.dp)
                                                            .clip(
                                                                RoundedCornerShape(13.dp)
                                                            )
                                                            .combinedClickable(onClick = {
                                                                if (showMemeDeleteBtn) {
                                                                    showMemeDeleteBtn = false
                                                                } else {
                                                                    FileUtil
                                                                        .getUriOfFile(
                                                                            context,
                                                                            it
                                                                        )
                                                                        ?.let {
                                                                            packageNameList.forEach { packageName ->
                                                                                context.grantUriPermission(
                                                                                    packageName,
                                                                                    it,
                                                                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                                )
                                                                            }
                                                                            onSend(
                                                                                listOf(
                                                                                    Image(
                                                                                        uri = it
                                                                                    )
                                                                                )
                                                                            )
                                                                        }
                                                                }
                                                            }, onLongClick = {
                                                                showMemeDeleteBtn =
                                                                    !showMemeDeleteBtn
                                                            }),
                                                    )
                                                    androidx.compose.animation.AnimatedVisibility(
                                                        modifier = Modifier.align(Alignment.TopEnd),
                                                        visible = showMemeDeleteBtn,
                                                        enter = scaleIn(),
                                                        exit = scaleOut()
                                                    ) {
                                                        FilledIconButton(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .padding(4.dp),
                                                            onClick = {
                                                                it.delete()
                                                                onMemeUpdate()
                                                            },
                                                        ) {
                                                            Icon(
                                                                modifier = Modifier.size(14.dp),
                                                                imageVector = Icons.Outlined.Clear,
                                                                contentDescription = "delete"
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        item {
                                            if (memeList.size > 0)
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(96.dp)
                                                        .animateItemPlacement(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    SmallFloatingActionButton(
                                                        onClick = {
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                                val maxNumPhotosAndVideos = 10
                                                                Intent(MediaStore.ACTION_PICK_IMAGES)
                                                                    .apply {
                                                                        type = "image/*"
                                                                        putExtra(
                                                                            MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                            maxNumPhotosAndVideos
                                                                        )
                                                                    }
                                                                    .also {
                                                                        memePickerLauncher.launch(
                                                                            it
                                                                        )
                                                                    }
                                                            } else {
                                                                memePickerSLauncher.launch("image/*")
                                                            }
                                                        },
                                                        shape = CircleShape,
                                                        elevation = FloatingActionButtonDefaults.elevation(
                                                            defaultElevation = 0.dp,
                                                            pressedElevation = 0.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Add,
                                                            contentDescription = "add"
                                                        )
                                                    }
//                                                Text(text = "添加", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                        }
                                    }
                                }
                            } else {
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(
                                        items = listOf(
                                            "\uD83E\uDD24",
                                            "\uD83E\uDD13",
                                            "\uD83D\uDE05",
                                            "\uD83D\uDE02",
                                            "\uD83E\uDD23",
                                            "\uD83D\uDE2D",
                                            "\uD83E\uDD70",
                                            "\uD83D\uDE0B",
                                            "\uD83E\uDD17",
                                            "\uD83D\uDE13",
                                            "\uD83E\uDD7A",
                                            "\uD83E\uDD14",
                                            "\uD83D\uDE09",
                                            "\uD83D\uDE19",
                                            "\uD83D\uDE0D",
                                            "\uD83D\uDE1D",
                                            "\uD83E\uDEE3",
                                            "\uD83D\uDE31",
                                            "\uD83D\uDE21",
                                            "\uD83D\uDE0E",
                                            "\uD83D\uDC2E",
                                            "\uD83D\uDC34",
                                            "\uD83D\uDC2D",
                                            "\uD83D\uDC30",
                                            "\uD83D\uDC38",
                                            "\uD83D\uDC22",
                                            "\uD83D\uDC12",
                                            "\uD83D\uDC37",
                                            "\uD83D\uDCA7",
                                            "☔"
                                        )
                                    ) {
                                        Surface(
                                            modifier = Modifier.width(48.dp),
                                            color = Color.Transparent,
                                            tonalElevation = 0.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        val value =
                                                            textFieldValueState.let { textFieldValue ->
                                                                textFieldValue
                                                                    .getTextBeforeSelection(
                                                                        500
                                                                    )
                                                                    .toString()
                                                                    .plus(it)
                                                                    .plus(
                                                                        textFieldValue.getTextAfterSelection(
                                                                            500
                                                                        )
                                                                    )
                                                                    .toString()
                                                            }
                                                        textFieldValueState =
                                                            textFieldValueState.copy(
                                                                text = value,
                                                                selection = TextRange(
                                                                    textFieldValueState.selection.end + it.length
                                                                )
                                                            )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = gallerySelected.isNotEmpty() || cameraSelected.isNotEmpty()) {
                        if (it) {
                            Row(
                                modifier = Modifier
                                    .height(height)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 4.dp)
                            ) {
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(0.5f)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(
                                                start = if (cameraAccessible) 0.dp else 2.dp,
                                                bottom = if (cameraAccessible) 0.dp else 2.dp
                                            ),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val maxNumPhotosAndVideos = 10
                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
                                                            .apply {
                                                                type = "image/*"
                                                                putExtra(
                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                    maxNumPhotosAndVideos
                                                                )
                                                            }
                                                            .also {
                                                                imagePickerLauncher.launch(it)
                                                            }
                                                    } else {
                                                        imagePickerSLauncher.launch("image/*")
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BadgedBox(badge = {
                                                gallerySelected.size.let {
                                                    if (it > 0) {
                                                        Badge {
                                                            Text(
                                                                text = "$it",
                                                                modifier = Modifier.semantics {
                                                                    contentDescription =
                                                                        "$it selected gallery images"
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                                    contentDescription = "gallery add",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (cameraAccessible) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 2.dp, top = 2.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        cameraLauncher.launch(
                                                            targetCameraShotUri
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BadgedBox(badge = {
                                                    cameraSelected.size.let {
                                                        if (it > 0) {
                                                            Badge {
                                                                Text(
                                                                    text = "$it",
                                                                    modifier = Modifier.semantics {
                                                                        contentDescription =
                                                                            "$it selected camera images"
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AddAPhoto,
                                                        contentDescription = "camera add",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(items = gallerySelected, key = { it.toString() }) {
                                        Surface(
                                            modifier = Modifier.animateItemPlacement(),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .clickable { }
                                            ) {
                                                val imageLoader = ImageLoader.Builder(context)
                                                    .components {
                                                        if (Build.VERSION.SDK_INT >= 28) {
                                                            add(ImageDecoderDecoder.Factory())
                                                        } else {
                                                            add(GifDecoder.Factory())
                                                        }
                                                    }
                                                    .build()
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(it)
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                        .build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = "gallery",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .width(144.dp)
                                                        .clip(
                                                            RoundedCornerShape(13.dp)
                                                        )
                                                )
                                                FilledIconButton(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .padding(4.dp)
                                                        .align(Alignment.TopEnd),
                                                    onClick = { gallerySelected.remove(it) },
//                                                    colors = IconButtonDefaults.iconButtonColors(
//                                                        contentColor = MaterialTheme.colorScheme.primary
//                                                    )
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(14.dp),
                                                        imageVector = Icons.Outlined.Clear,
                                                        contentDescription = "delete"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(items = cameraSelected, key = { it.toString() }) {
                                        Surface(
                                            modifier = Modifier.animateItemPlacement(),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .clickable { }) {
                                                val imageLoader = ImageLoader.Builder(context)
                                                    .components {
                                                        if (Build.VERSION.SDK_INT >= 28) {
                                                            add(ImageDecoderDecoder.Factory())
                                                        } else {
                                                            add(GifDecoder.Factory())
                                                        }
                                                    }
                                                    .build()
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(it)
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                        .build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = "camera",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .width(144.dp)
                                                        .clip(
                                                            RoundedCornerShape(13.dp)
                                                        )
                                                )
                                                FilledIconButton(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .padding(4.dp)
                                                        .align(Alignment.TopEnd),
                                                    onClick = { cameraSelected.remove(it) },
//                                                    colors = IconButtonDefaults.iconButtonColors(
//                                                        contentColor = MaterialTheme.colorScheme.primary
//                                                    )
                                                ) {
                                                    Icon(
                                                        modifier = Modifier.size(14.dp),
                                                        imageVector = Icons.Outlined.Clear,
                                                        contentDescription = "delete"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .height(height)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                if (isBottomSheetExpand) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val maxNumPhotosAndVideos = 10
                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
                                                            .apply {
                                                                type = "image/*"
                                                                putExtra(
                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                    maxNumPhotosAndVideos
                                                                )
                                                            }
                                                            .also {
                                                                imagePickerLauncher.launch(it)
                                                            }
                                                    } else {
                                                        imagePickerSLauncher.launch("image/*")
                                                    }
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = "image",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "相册",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                if (cameraAccessible) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    if (isBottomSheetExpand) {
                                                        cameraLauncher.launch(targetCameraShotUri)
                                                    }
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                imageVector = Icons.Outlined.PhotoCamera,
                                                contentDescription = "camera",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "拍摄",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                if (isBottomSheetExpand) {
                                                    filePickerLauncher.launch("*/*")
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = "file",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "文件",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Videocam,
                                            contentDescription = "video",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "视频",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun sendAudio(
    context: Context,
    packageNameList: List<String>,
    onSend: (contents: List<MessageContent>) -> Unit
) {
    val originalPath = File(context.externalCacheDir!!.absoluteFile, "audio_record.mp3")
    val audioFile = File(context.getExternalFilesDir("chat")!!, "audio_record.mp3")
    val targetFileName = "${System.currentTimeMillis().toDateAndTimeString()}.mp3"
    FileUtil.getUriByCopyingFileToPath(
        context,
        context.getExternalFilesDir("chat")!!,
        targetFileName,
        originalPath,
    )?.let {
        packageNameList.forEach { packageName ->
            context.grantUriPermission(
                packageName,
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val duration = MediaMetadataRetriever().apply {
            setDataSource("${context.getExternalFilesDir("chat")!!.absoluteFile}/$targetFileName")
        }.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val size = audioFile.length()
        onSend(
            listOf(
                Audio(
                    length = duration,
                    fileName = targetFileName,
                    fileSize = size,
                    uri = it
                )
            )
        )
    }
}