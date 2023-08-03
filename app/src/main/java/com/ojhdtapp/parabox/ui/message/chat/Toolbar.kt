package com.ojhdtapp.parabox.ui.message.chat

import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Toolbar(
    modifier: Modifier = Modifier,
    state: MessagePageState.EditAreaState,
    onEvent: (MessagePageEvent) -> Unit,
){
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chooseImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    onEvent(MessagePageEvent.AddImageUriToChosenList(it))
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    val addMemeLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) {
                uris.forEach {
                    onEvent(MessagePageEvent.AddMemeUri(it, {
                        Log.d("PhotoPicker", "${it.name} saved")
                    }, {
                        Log.d("PhotoPicker", "Save failed")
                    }))
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    AnimatedContent(
        targetState = state.toolbarState,
        transitionSpec = {
            if (targetState == ToolbarState.Emoji) {
                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start)) with slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start
                )
            } else {
                (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) with slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End
                ))
            }
        }, label = "toolbar"
    ) {
        when(it){
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
                                if (state.memePathList.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier.fillParentMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_meme),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            TextButton(onClick = {
                                                addMemeLauncher.launch("image/*")
                                            }) {
                                                Text(text = stringResource(R.string.add_meme_by_hand))
                                            }
                                        }
                                    }
                                } else {
                                    items(items = state.memePathList) {
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
                                                                  onEvent(MessagePageEvent.RemoveMemeUri(it, {}, {}))
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
                                        if (state.memePathList.isNotEmpty())
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
                                                        addMemeLauncher.launch("image/*")
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
            }
            ToolbarState.Tools -> {

            }
        }
        if (true) {

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
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
//                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                                                        val maxNumPhotosAndVideos = 10
//                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
//                                                            .apply {
//                                                                type = "image/*"
//                                                                putExtra(
//                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
//                                                                    maxNumPhotosAndVideos
//                                                                )
//                                                            }
//                                                            .also {
//                                                                imagePickerLauncher.launch(it)
//                                                            }
//                                                    } else {
//                                                        imagePickerSLauncher.launch("image/*")
//                                                    }
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
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
//                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                                                        val maxNumPhotosAndVideos = 10
//                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
//                                                            .apply {
//                                                                type = "image/*"
//                                                                putExtra(
//                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
//                                                                    maxNumPhotosAndVideos
//                                                                )
//                                                            }
//                                                            .also {
//                                                                imagePickerLauncher.launch(it)
//                                                            }
//                                                    } else {
//                                                        imagePickerSLauncher.launch("image/*")
//                                                    }
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
                                    text = stringResource(R.string.gallery),
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
                                        text = stringResource(R.string.take_photos),
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
                                            filePickerLauncher.launch(arrayOf("*/*"))
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
                                    text = stringResource(R.string.file),
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
                    }
                }
            }
        }
    }
}