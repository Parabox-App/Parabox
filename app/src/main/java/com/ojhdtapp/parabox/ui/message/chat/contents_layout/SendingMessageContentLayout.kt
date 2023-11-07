package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImageSendingLayout(
    modifier: Modifier = Modifier,
    model: Any?,
    previewerState: ImagePreviewerState,
    previewIndex: Int,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.size(80.dp)) {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .size(Size.ORIGINAL)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                error = painterResource(id = R.drawable.image_lost),
                fallback = painterResource(id = R.drawable.image_lost),
                imageLoader = imageLoader,
                contentScale = ContentScale.Fit
            )
            TransformImageView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            coroutineScope.launch {
                                previewerState.openTransform(previewIndex)
                                onClick()
                            }
                        }
                    },
                key = previewIndex,
                painter = painter,
                previewerState = previewerState,
            )
            FilledIconButton(
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                onClick = onCancel,
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

@Composable
fun QuoteReplySendingLayout(
    modifier: Modifier = Modifier,
    model: ChatPageUiModel.MessageWithSender,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(modifier = Modifier.padding(horizontal = 12.dp), imageVector = Icons.AutoMirrored.Outlined.Reply, contentDescription = "quote reply", tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        append("回复")
                        append(" ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append(model.sender.name)
                        }
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.message.contentString,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(imageVector = Icons.Outlined.Clear, contentDescription = "cancel")
            }
        }
    }
}