package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.core.util.AvatarUtil

@Composable
fun CommonAvatar(
    modifier: Modifier = Modifier,
    model: Any?,
    name: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) =
    SubcomposeAsyncImage(
        model = model,
        contentDescription = "chat_avatar",
        modifier = modifier.fillMaxSize(),
    ) {
        val state = painter.state
        val namedAvatarBm =
            AvatarUtil.createNamedAvatarBm(
                backgroundColor = backgroundColor.toArgb(),
                textColor = textColor.toArgb(),
                name = name ?: "name"
            ).asImageBitmap()
        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .placeholder(
                        visible = state is AsyncImagePainter.State.Loading,
                        color = backgroundColor,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                bitmap = namedAvatarBm,
                contentDescription = "named_avatar"
            )
        } else {
            SubcomposeAsyncImageContent()
        }
    }