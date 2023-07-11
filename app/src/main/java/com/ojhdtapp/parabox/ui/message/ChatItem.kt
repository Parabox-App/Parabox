package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.AsyncImagePainter.State.Empty.painter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(
    modifier: Modifier = Modifier,
    chatWithLatestMessage: ChatWithLatestMessage,
    icon: @Composable() (() -> Unit)? = null,
    isFirst: Boolean,
    isLast: Boolean,
    isLoading: Boolean = true,
    isSelected: Boolean = false,
    isEditing: Boolean = false,
    isExpanded: Boolean = false,
    username: String = "",
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val backgroundColor by
    animateColorAsState(
        targetValue = if (isEditing && isExpanded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    )
    val textColor by animateColorAsState(
        targetValue = if (isEditing && isExpanded) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
    val avatarBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    )
    val topRadius by animateDpAsState(
        targetValue = if (isFirst) 24.dp else 3.dp
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (isLast) 24.dp else 3.dp
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = LocalIndication.current,
                enabled = true,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomEnd = bottomRadius,
            bottomStart = bottomRadius
        ),
        color = backgroundColor,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
//            .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .placeholder(true, MaterialTheme.colorScheme.secondaryContainer)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(48.dp)
                        .background(avatarBackgroundColor)
                        .clickable {
                            onAvatarClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = isSelected) {
                        if (it) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = "selected",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            icon?.invoke() ?: SubcomposeAsyncImage(
                                model = chatWithLatestMessage.chat?.avatar?.getModel(),
                                contentDescription = "chat_avatar",
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                val state = painter.state
                                val namedAvatarBm =
                                    AvatarUtil.createNamedAvatarBm(
                                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer.toArgb(),
                                        textColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb(),
                                        name = chatWithLatestMessage.chat?.name ?: "name"
                                    ).asImageBitmap()
                                if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .placeholder(
                                                visible = state is AsyncImagePainter.State.Loading,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                        bitmap = namedAvatarBm,
                                        contentDescription = "named_avatar"
                                    )
                                } else {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f), verticalArrangement = Arrangement.Center
            ) {
                    Text(
                        modifier = Modifier.placeholder(visible = isLoading, MaterialTheme.colorScheme.onSecondaryContainer),
                        text = chatWithLatestMessage.chat.name ?: context.getString(R.string.contact_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        maxLines = 1
                    )
                Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = buildAnnotatedString {
//                            chatWithLatestMessage.message?.also { message ->
//                                message.
//                            }
//                            if (subTitle.isNullOrEmpty()) {
//                                if (contact?.profile?.name != contact?.latestMessage?.sender && contact?.latestMessage?.sender != null) {
//                                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
//                                        append(if (contact.latestMessage.sentByMe) username else contact.latestMessage.sender)
//                                        append(": ")
//                                    }
//                                }
//                                withStyle(style = SpanStyle(color = if (noBackground) MaterialTheme.colorScheme.onSurface else textColor)) {
//                                    append(subTitle ?: contact?.latestMessage?.content ?: "")
//                                }
//                            } else {
//                                withStyle(style = SpanStyle(color = if (noBackground) MaterialTheme.colorScheme.onSurface else textColor)) {
//                                    append(subTitle)
//                                }
//                            }
//                        },
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = textColor,
//                        maxLines = 1
//                    )
            }
            if (!isLoading) {
//                Column(
//                    modifier = Modifier.align(Alignment.Top),
//                    horizontalAlignment = Alignment.End,
//                    verticalArrangement = Arrangement.Top
//                ) {
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = (timestamp ?: contact?.latestMessage?.timestamp)?.toTimeUntilNow(
//                            context
//                        )
//                            ?: "",
//                        style = MaterialTheme.typography.labelMedium
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    val unreadMessagesNum =
//                        unreadMessagesNum ?: contact?.latestMessage?.unreadMessagesNum ?: 0
//                    if (unreadMessagesNum != 0) {
//                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(text = "$unreadMessagesNum") }
//                    }
//                }
            }
        }
    }
}