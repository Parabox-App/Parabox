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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.toTimeUntilNow
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.common.CommonAvatar

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(
    modifier: Modifier = Modifier,
    chatWithLatestMessage: ChatWithLatestMessage,
    contact: Resource<Contact>,
    icon: @Composable() (() -> Unit)? = null,
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
            MaterialTheme.colorScheme.surfaceContainer
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

        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                        if (icon != null) {
                            icon()
                        } else {
                            CommonAvatar(
                                model = chatWithLatestMessage.chat.avatar.getModel(),
                                name = chatWithLatestMessage.chat.name,
                            )
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
                    text = chatWithLatestMessage.chat.name
                        ?: context.getString(R.string.contact_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.placeholder(
                        visible = contact !is Resource.Success,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                    text = buildAnnotatedString {
                        chatWithLatestMessage.message?.also { message ->
                            message.contentString
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            if (chatWithLatestMessage.message?.sentByMe == true) {
                                append(username)
                            } else {
                                append(contact.data?.name)
                            }
                            append(": ")
                        }
                        withStyle(style = SpanStyle(color = textColor)) {
                            append(chatWithLatestMessage.message?.contentString)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Top),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = (chatWithLatestMessage.message?.timestamp)?.toTimeUntilNow(
                        context
                    ) ?: "",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (chatWithLatestMessage.chat.unreadMessageNum > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(text = "${chatWithLatestMessage.chat.unreadMessageNum}") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PinnedChatItems(
    modifier: Modifier = Modifier,
    chat: Chat,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val backgroundColor = Color.Transparent
    val textColor = MaterialTheme.colorScheme.onSurface
    val avatarBackgroundColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(
            interactionSource = remember {
                MutableInteractionSource()
            },
            indication = LocalIndication.current,
            enabled = true,
            onLongClick = onLongClick,
            onClick = onClick
        ),
        color = backgroundColor,
        tonalElevation = 3.dp
    ) {
        Box() {
            Column(
                modifier = Modifier
                    .width(width = 72.dp)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                        .background(avatarBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    icon?.invoke() ?: CommonAvatar(model = chat.avatar.getModel(), name = chat.name)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chat.unreadMessageNum > 0) {
                Badge(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text(text = "${chat.unreadMessageNum}") }
            }
        }
    }
}

@Composable
fun EmptyChatItem(
    modifier: Modifier = Modifier,
    icon: @Composable() (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
//            .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .placeholder(
                        true, MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon?.invoke()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f), verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.placeholder(
                        visible = true,
                        MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                    text = context.getString(R.string.contact_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    modifier = Modifier.placeholder(
                        visible = true,
                        MaterialTheme.colorScheme.secondaryContainer,
                        highlight = PlaceholderHighlight.fade(),
                    ),
                    text = context.getString(R.string.contact_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}