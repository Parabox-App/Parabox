package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.core.util.ImageUtil

@Composable
fun CommonAvatar(
    modifier: Modifier = Modifier,
    model: CommonAvatarModel,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
){
    val context = LocalContext.current
    val namePainter = remember(model.name, backgroundColor, textColor) {
        BitmapPainter(ImageUtil.createNamedAvatarBm(
            backgroundColor = backgroundColor.toArgb(),
            textColor = textColor.toArgb(),
            name = model.name ?: "name"
        ).asImageBitmap())
    }
    val request = remember(context, model.model) {
        ImageRequest.Builder(context)
            .data(model.model)
            .crossfade(true)
            .build()
    }
    AsyncImage(
        modifier = modifier,
        model = request,
        contentDescription = "avatar",
        contentScale = ContentScale.Crop,
        placeholder = namePainter,
        error = namePainter,
        fallback = namePainter,
    )
}
//    SubcomposeAsyncImage(
//        model = model,
//        contentDescription = "chat_avatar",
//        modifier = modifier.fillMaxSize(),
//    ) {
//        val state = painter.state
//        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
//            val namedAvatarBm =
//                AvatarUtil.createNamedAvatarBm(
//                    backgroundColor = backgroundColor.toArgb(),
//                    textColor = textColor.toArgb(),
//                    name = name ?: "name"
//                ).asImageBitmap()
//            Image(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .placeholder(
//                        visible = state is AsyncImagePainter.State.Loading,
//                        color = backgroundColor,
//                        highlight = PlaceholderHighlight.fade(),
//                    ),
//                bitmap = namedAvatarBm,
//                contentDescription = "named_avatar"
//            )
//        } else {
//            SubcomposeAsyncImageContent()
//        }
//    }

@Stable
data class CommonAvatarModel(
    val model: Any?,
    val name: String?
)