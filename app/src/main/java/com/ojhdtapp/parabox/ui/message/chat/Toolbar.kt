package com.ojhdtapp.parabox.ui.message.chat

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.buildFileName
import com.ojhdtapp.parabox.core.util.launchLocationSetting
import com.ojhdtapp.parabox.core.util.launchSetting
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    state: MessagePageState.EditAreaState,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fileUtil = remember {
        FileUtil(context)
    }
    val chooseImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    onEvent(MessagePageEvent.ChooseImageUri(it))
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    val addMemeLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    onEvent(MessagePageEvent.AddMeme(it, {
                        Log.d("PhotoPicker", "${it.name} saved")
                    }, {
                        Log.d("PhotoPicker", "Save failed")
                    }))
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    var tempCameraUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            if (it) {
                onEvent(MessagePageEvent.ChooseImageUri(tempCameraUri!!))
                tempCameraUri = null
            }
        }
    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
            if (it != null) {
                val size = context.contentResolver.openFileDescriptor(it, "r")?.use {
                    it.statSize
                } ?: 0
                val name = fileUtil.getFileName(it) ?: ""
                onEvent(MessagePageEvent.SendFileMessage(fileUri = it, size = size, name = name))
            }
        }
    val locationPermissionState =
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    if (state.showLocationPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = {
                onEvent(MessagePageEvent.ShowLocationPermissionDeniedDialog(false))
            },
            icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            title = {
                Text(text = stringResource(R.string.request_permission))
            },
            text = {
                Text(
                    stringResource(id = R.string.location_permission_text)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(MessagePageEvent.ShowLocationPermissionDeniedDialog(false))
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text(stringResource(R.string.try_request_permission))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEvent(MessagePageEvent.ShowLocationPermissionDeniedDialog(false))
                        context.launchLocationSetting()
                    }
                ) {
                    Text(stringResource(R.string.redirect_to_setting))
                }
            }
        )
    }
    AnimatedContent(
        modifier = modifier,
        targetState = state.toolbarState,
        transitionSpec = {
            if (targetState == ToolbarState.Emoji) {
                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)) togetherWith slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start
                )
            } else {
                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) togetherWith slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End
                ))
            }
        }, label = "toolbar"
    ) {
        when (it) {
            ToolbarState.Emoji -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
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
                            .weight(1f), targetState = memeState, label = "meme_favorite"
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
                                if (state.memeList.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillParentMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_meme),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            TextButton(onClick = {
                                                addMemeLauncher.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            }) {
                                                Text(text = stringResource(R.string.add_meme_by_hand))
                                            }
                                        }
                                    }
                                } else {
                                    items(items = state.memeList) {
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
                                                                onEvent(MessagePageEvent.SendMemeMessage(it))
                                                            }
                                                        }, onLongClick = {
                                                            showMemeDeleteBtn = !showMemeDeleteBtn
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
                                                            onEvent(MessagePageEvent.RemoveMeme(it, {}, {}))
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
                                        if (state.memeList.isNotEmpty())
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
                                                        addMemeLauncher.launch(
                                                            PickVisualMediaRequest(
                                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                                            )
                                                        )
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
                                ) { emoji ->
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
                                                    onEvent(
                                                        MessagePageEvent.UpdateEditAreaInput(
                                                            state.input.copy(
                                                                text = buildString {
                                                                    append(state.input.getTextBeforeSelection(500))
                                                                    append(emoji)
                                                                    append(state.input.getTextAfterSelection(500))
                                                                },
                                                                selection = TextRange(state.input.selection.end + emoji.length)
                                                            )
                                                        )
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ToolbarState.Tools -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 4.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        onClick = {
                            chooseImageLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
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
                                text = stringResource(R.string.gallery),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        onClick = {
                            val fileName =
                                buildFileName(FileUtil.EXTERNAL_FILES_DIR_CAMERA, FileUtil.DEFAULT_IMAGE_EXTENSION)
                            val path =
                                fileUtil.createPathOnExternalFilesDir(FileUtil.EXTERNAL_FILES_DIR_CAMERA, fileName)
                            fileUtil.getUriForFile(path)?.let {
                                cameraLauncher.launch(it)
                            } ?: kotlin.run {
                                Log.d("parabox", "tempCameraUri is null")
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
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
                                text = stringResource(R.string.take_photos),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
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
                                text = stringResource(R.string.file),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(24.dp),
                        onClick = {
                            if (locationPermissionState.allPermissionsGranted) {
                                onEvent(MessagePageEvent.UpdateEditAreaMode(EditAreaMode.LOCATION_PICKER))
                            } else {
                                onEvent(MessagePageEvent.ShowLocationPermissionDeniedDialog(true))
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                modifier = Modifier.padding(bottom = 8.dp),
                                imageVector = Icons.Outlined.Place,
                                contentDescription = "location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.location),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                }
            }
        }
    }
}