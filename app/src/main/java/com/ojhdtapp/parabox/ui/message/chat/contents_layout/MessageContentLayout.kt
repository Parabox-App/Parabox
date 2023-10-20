package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import android.graphics.BitmapFactory
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.TransformImageView
import kotlinx.coroutines.launch

@Composable
fun PlainTextLayout(
    modifier: Modifier = Modifier,
    text: AnnotatedString
) {
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = text
    )
}

@Composable
fun ImageLayout(
    modifier: Modifier = Modifier,
    model: Any?,
    elementId: Long,
    previewerState: ImagePreviewerState,
    onClick: (elementId: Long) -> Unit,
) {
    var boxWidth by remember {
        mutableStateOf(128.dp)
    }
    var boxHeight by remember {
        mutableStateOf(0.dp)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(model)
            .build(),
        error = painterResource(id = R.drawable.image_lost),
        fallback = painterResource(id = R.drawable.image_lost),
        imageLoader = imageLoader,
        contentScale = ContentScale.FillWidth,
        onSuccess = {
            coroutineScope.launch {
                Log.d("parabox", "image size: ${it.result.request.sizeResolver.size()}")
                val bitmap = it.result.drawable.toBitmap()
                val originalWidthDp = with(density) {
                    bitmap.width.toDp()
                }
                val originalHeightDp = with(density) {
                    bitmap.height.toDp()
                }
                if (originalWidthDp != 0.dp) {
                    boxWidth = originalWidthDp.coerceIn(128.dp, 320.dp)
                    boxHeight = boxWidth / originalWidthDp * originalHeightDp
                }
            }
        },
        onError = {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.image_lost)
            boxWidth = with(density) {
                bitmap.width.toDp()
            }
            boxHeight = with(density) {
                bitmap.height.toDp()
            }
        }
    )
    Box(
        modifier = Modifier.size(128.dp)
    ) {
        TransformImageView(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures {
                    onClick(elementId)
                }
            },
            key = elementId,
            painter = painter,
            previewerState = previewerState,
        )
    }
}

@Composable
fun AudioLayout(
    modifier: Modifier = Modifier,
) {

}

@Composable
fun FileLayout(
    modifier: Modifier = Modifier
) {

}

@Composable
fun LocationLayout(
    modifier: Modifier = Modifier
) {

}