package com.ojhdtapp.parabox.ui.message.chat.contents_layout

import android.graphics.BitmapFactory
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
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
        mutableStateOf(128.dp)
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(model)
            .size(coil.size.Size.ORIGINAL)
            .crossfade(true)
            .memoryCacheKey("$elementId")
            .build(),
        error = painterResource(id = R.drawable.image_lost),
        fallback = painterResource(id = R.drawable.image_lost),
        contentScale = ContentScale.Fit,
        onSuccess = {
            coroutineScope.launch {
                val bitmap = it.result.drawable.toBitmap()
                val originalWidthDp = with(density) {
                    bitmap.width.toDp()
                }
                val originalHeightDp = with(density) {
                    bitmap.height.toDp()
                }
                if (originalWidthDp != 0.dp) {
                    boxWidth = originalWidthDp.coerceIn(128.dp, 320.dp)
                    boxHeight = (boxWidth / originalWidthDp * originalHeightDp).coerceAtMost(320.dp)
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
    TransformImageView(
        modifier = modifier.size(boxWidth, boxHeight).pointerInput(Unit) {
            detectTapGestures {
                onClick(elementId)
            }
        },
        key = elementId,
        painter = painter,
        previewerState = previewerState,
    )
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

@Composable
fun UnsupportedLayout(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.padding(horizontal = 9.dp, vertical = 9.dp),
        text = "不支持的消息类型",
        color = MaterialTheme.colorScheme.primary
    )
}