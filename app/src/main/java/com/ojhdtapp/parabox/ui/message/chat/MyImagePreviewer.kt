package com.ojhdtapp.parabox.ui.message.chat

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toFile
import androidx.paging.compose.LazyPagingItems
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.ui.common.RoundedCornerCascadeDropdownMenu
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.rememberPreviewerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyImagePreviewer(
    modifier: Modifier = Modifier,
    messageLazyPagingItems: LazyPagingItems<Message>,
    state: MessagePageState.ImagePreviewerState,
    onEvent: (e: MessagePageEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val previewImageList by remember {
        derivedStateOf {
            messageLazyPagingItems.itemSnapshotList.items.fold(mutableListOf<Pair<Long, ParaboxImage>>()) { acc, message ->
                val imageMessageList = message.contents.filterIsInstance<ParaboxImage>()
                val lastIndex = imageMessageList.lastIndex
                imageMessageList.reversed().forEachIndexed { index, t ->
                    if (t.resourceInfo.getModel() != null) {
                        val imageId = "${message.messageId}${(lastIndex - index).coerceIn(0, lastIndex)}".toLong()
                        acc.add(
                            imageId to t
                        )
                    }
                }
                acc
            }.reversed()
        }
    }
    val imageViewerState = rememberPreviewerState(enableVerticalDrag = true) {
        previewImageList[it].first
    }
    val useDarkIcons = isSystemInDarkTheme()
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(imageViewerState.visible) {
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
    BackHandler(enabled = imageViewerState.visible) {
        coroutineScope.launch {
            imageViewerState.closeTransform()
        }
    }
    ImagePreviewer(modifier = Modifier.fillMaxSize(),
        count = previewImageList.size,
        state = imageViewerState,
        imageLoader = { index ->
            if (index < previewImageList.size) {
                val t = previewImageList[index].second
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
                        .data(t.resourceInfo.getModel())
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
                    state.showToolbar,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    TopAppBar(
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
                                    onEvent(MessagePageEvent.ExpandImagePreviewerMenu(!state.expandMenu))
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "more"
                                    )
                                }
                                RoundedCornerCascadeDropdownMenu(
                                    expanded = state.expandMenu,
                                    onDismissRequest = {
                                        onEvent(MessagePageEvent.ExpandImagePreviewerMenu(false))
                                    },
                                    modifier = modifier.clip(MaterialTheme.shapes.medium),
                                    properties = PopupProperties(
                                        dismissOnBackPress = true,
                                        dismissOnClickOutside = true,
                                        focusable = true
                                    ),
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.add_meme))
                                        },
                                        onClick = {
                                            onEvent(MessagePageEvent.ExpandImagePreviewerMenu(false))
                                            try {
                                                val image =
                                                    previewImageList.getOrNull(current)?.second
                                                        ?: throw NoSuchElementException("id lost")
                                                // TODO: downloader
                                                when(image.resourceInfo){
                                                    is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> {
                                                        onEvent(MessagePageEvent.AddMeme((image.resourceInfo as ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo).uri, {}, {}))
                                                    }
                                                    else -> {}
                                                }
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
                                            onEvent(MessagePageEvent.ExpandImagePreviewerMenu(false))
                                            try {
                                                val image =
                                                    previewImageList.getOrNull(current)?.second
                                                        ?: throw NoSuchElementException("id lost")
                                                onEvent(MessagePageEvent.SaveImageToLocal(image, {}, {}))
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
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = "download"
                                            )
                                        })
                                }
                            }
                        },
                        colors = topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
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
                onEvent(MessagePageEvent.ExpandImagePreviewerToolbar(!state.showToolbar))
            }
        }
    )
}