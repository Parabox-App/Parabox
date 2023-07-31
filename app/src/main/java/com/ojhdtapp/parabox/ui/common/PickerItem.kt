package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.core.util.AvatarUtil

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PickerItem(
    modifier: Modifier = Modifier,
    avatarModel: Any?,
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = avatarModel,
                    contentDescription = "chat_avatar",
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val state = painter.state
                    val namedAvatarBm =
                        AvatarUtil.createNamedAvatarBm(
                            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
                            name = title
                        ).asImageBitmap()
                    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .placeholder(
                                    visible = state is AsyncImagePainter.State.Loading,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    highlight = PlaceholderHighlight.fade(),
                                ),
                            bitmap = namedAvatarBm,
                            contentDescription = "named_avatar"
                        )
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}