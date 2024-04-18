package com.ojhdtapp.parabox.ui.message.chat

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.ImagePreviewerState
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyImagePreviewer(
    modifier: Modifier = Modifier,
    state: MessagePageState.ChatDetail,
    layoutType: MessageLayoutType,
    previewerState: ImagePreviewerState,
    onEvent: (e: MessagePageEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val systemUiController = LocalSystemUiController.current
    ImagePreviewer(modifier = modifier.fillMaxSize(),
        state = previewerState,
        imageLoader = { index ->
            if (index < state.imagePreviewerState.imageSnapshotList.size) {
                val imageItem = state.imagePreviewerState.imageSnapshotList[index]
                if (index == state.imagePreviewerState.imageSnapshotList.lastIndex) {
                }
                rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(imageItem.image.resourceInfo.getModel())
                        .size(Size.ORIGINAL)
                        .build(),
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
                    state.imagePreviewerState.showToolbar,
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
                                    previewerState.closeTransform()
                                    systemUiController.reset()
//                                    onEvent(MessagePageEvent.UpdateImagePreviewerSnapshotList(emptyList(), -1))
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                                    onEvent(MessagePageEvent.ExpandImagePreviewerMenu(!state.imagePreviewerState.expandMenu))
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = "more"
                                    )
                                }
                                CascadeDropdownMenu(
                                    expanded = state.imagePreviewerState.expandMenu,
                                    onDismissRequest = {
                                        onEvent(MessagePageEvent.ExpandImagePreviewerMenu(false))
                                    },
                                    properties = PopupProperties(
                                        dismissOnBackPress = true,
                                        dismissOnClickOutside = true,
                                        focusable = true
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(R.string.add_meme))
                                        },
                                        onClick = {
                                            onEvent(MessagePageEvent.ExpandImagePreviewerMenu(false))
                                            try {
                                                val image =
                                                    state.imagePreviewerState.imageSnapshotList.getOrNull(current)?.image
                                                        ?: throw NoSuchElementException("id lost")
                                                // TODO: downloader
                                                when (image.resourceInfo) {
                                                    is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> {
                                                        onEvent(
                                                            MessagePageEvent.AddMeme(
                                                                (image.resourceInfo as ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo).uri,
                                                                {},
                                                                {})
                                                        )
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
                                                    state.imagePreviewerState.imageSnapshotList.getOrNull(current)?.image
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
                onEvent(MessagePageEvent.ExpandImagePreviewerToolbar(!state.imagePreviewerState.showToolbar))
            }
        },
        onCloseWithDrag = {
            systemUiController.reset()
        },
        reverseLayout = !state.infoAreaState.expanded
    )
}