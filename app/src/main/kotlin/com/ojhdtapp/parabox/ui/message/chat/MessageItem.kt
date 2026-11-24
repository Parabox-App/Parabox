package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.toFormattedTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.contains
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.common.ParaboxAlertDialog
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.AudioLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.FileLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.ImageLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.LocationLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.MessageContentContainer
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.PlainTextLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.QuoteReplyLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.UnsupportedLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.VideoLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAnnotatedText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAtAll
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.message.simplifyText
import com.origeek.imageViewer.previewer.ImagePreviewerState
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.rememberSwipeableActionsState
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    state: MessagePageState.ChatDetail,
    messageWithSender: ChatPageUiModel.MessageWithSender,
    previewerState: ImagePreviewerState,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    isHighlighted: Boolean = false,
    shouldDisplayTime: Boolean = false,
    onImageClick: (elementId: Long) -> Unit,
    onQuoteReplyClick: (quoteMessageUid: String) -> Unit = {},
    resolveQuoteFallback: ((quoteMessageUid: String) -> Pair<String?, String>?)? = null,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onEvent: (e: MessagePageEvent) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isSelected by remember(state.selectedMessageList.size) {
        derivedStateOf {
            state.selectedMessageList.contains(messageWithSender.message.messageId)
        }
    }
    val isSentByMe = messageWithSender.message.sentByMe
    val sendStatus = state.sendingStates[messageWithSender.message.messageId]
    val isVerified = messageWithSender.message.verified || sendStatus is MessagePageState.MessageSendStatus.Sent
    val showSendingSpinner = isSentByMe && sendStatus is MessagePageState.MessageSendStatus.Sending
    val failedReason: String? = if (isSentByMe && !isVerified && !showSendingSpinner) {
        (sendStatus as? MessagePageState.MessageSendStatus.Failed)?.errorMessage ?: "消息未送达，请重试"
    } else {
        null
    }
    var showFailedDialog by rememberSaveable { mutableStateOf(false) }

    if (showFailedDialog && failedReason != null) {
        ParaboxAlertDialog(
            onDismissRequest = { showFailedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "send failed",
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = "消息发送失败") },
            text = { Text(text = failedReason) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFailedDialog = false
                        onEvent(MessagePageEvent.RetrySendMessage(messageWithSender.message))
                    }
                ) {
                    Text(text = "重试")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFailedDialog = false
                        onEvent(MessagePageEvent.DeleteFailedMessage(messageWithSender.message.messageId))
                    }
                ) {
                    Text(text = "删除", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    val topStartRadius by animateDpAsState(targetValue = if (isFirst) 24.dp else 3.dp)
    val bottomStartRadius by animateDpAsState(targetValue = if (isLast) 24.dp else 3.dp)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isSentByMe -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.onTertiaryContainer
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            isSentByMe -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    )
    val swipeableActionsState = rememberSwipeableActionsState()
    val reachThreshold by remember {
        derivedStateOf {
            abs(swipeableActionsState.offset.value) > with(density) { 48.dp.toPx() }
        }
    }
    LaunchedEffect(reachThreshold) {
        if (reachThreshold) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val scale by animateFloatAsState(
        if (reachThreshold) 1f else 0.75f
    )
    SwipeableActionsBox(
        modifier = modifier.fillMaxWidth(),
        state = swipeableActionsState,
        startActions = listOf(SwipeAction(
            icon = {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.Outlined.Checklist,
                            contentDescription = "select or not",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            background = Color.Transparent,
            onSwipe = {
                onEvent(MessagePageEvent.AddOrRemoveSelectedMessage(messageWithSender.message))
            }
        )),
        endActions = listOf(SwipeAction(
            icon = {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = "reply",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            background = Color.Transparent,
            onSwipe = {
                onEvent(MessagePageEvent.ChooseQuoteReply(messageWithSender))
            }
        )),
        swipeThreshold = 48.dp,
        backgroundUntilSwipeThreshold = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isSentByMe) {
                if (isFirst) {
                    CommonAvatar(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = onAvatarClick,
                                onLongClick = onAvatarLongClick
                            ),
                        model = CommonAvatarModel(
                            model = messageWithSender.sender.avatar.getModel(),
                            name = messageWithSender.sender.name
                        )
                    )
                } else {
                    Spacer(modifier = Modifier.width(42.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
            ) {
                var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                DisableSelection {
                    MessageDropdownMenu(
                        message = messageWithSender.message,
                        isVerified = isVerified,
                        isMenuVisible = isMenuVisible,
                        onEvent = onEvent,
                        onReply = {
                            onEvent(MessagePageEvent.ChooseQuoteReply(messageWithSender))
                        },
                        onDismiss = { isMenuVisible = false })
                }
                if (isFirst && (!isSentByMe || shouldDisplayTime)) {
                    DisableSelection {
                        Row(verticalAlignment = Alignment.Bottom) {
                            if (!isSentByMe) {
                                Text(
                                    text = messageWithSender.sender.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (shouldDisplayTime) {
                                if (!isSentByMe) Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = messageWithSender.message.timestamp.toFormattedTime(context),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = if (isSentByMe) Arrangement.End else Arrangement.Start,
                ) {
                    if (isSentByMe) {
                        when {
                            showSendingSpinner -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp, bottom = 10.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            failedReason != null -> {
                                IconButton(
                                    modifier = Modifier
                                        .padding(end = 2.dp, bottom = 2.dp)
                                        .size(32.dp),
                                    onClick = { showFailedDialog = true },
                                ) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "send error",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxWidth = constraints.maxWidth - with(density) {
                                        90.dp.roundToPx()
                                    }.coerceAtLeast(0),
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                        shape = if (isSentByMe) {
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = topStartRadius,
                                bottomStart = 24.dp,
                                bottomEnd = bottomStartRadius
                            )
                        } else {
                            RoundedCornerShape(
                                topStart = topStartRadius,
                                topEnd = 24.dp,
                                bottomStart = bottomStartRadius,
                                bottomEnd = 24.dp
                            )
                        },
                        border = BorderStroke(3.dp, backgroundColor),
                        color = backgroundColor,
                        onClick = {
                            if (state.selectedMessageList.isEmpty()) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isMenuVisible = true
                            } else {
                                onEvent(MessagePageEvent.AddOrRemoveSelectedMessage(messageWithSender.message))
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            LaunchedEffect(Unit) {
                                messageWithSender.message.contents.filterIsInstance<ParaboxAt>().forEach {
                                    if (!state.atCache.containsKey("${messageWithSender.sender.pkg}${it.target.uid}")) {
                                        onEvent(
                                            MessagePageEvent.QueryAtTargetWithCache(
                                                messageWithSender.sender.pkg,
                                                it.target.uid
                                            )
                                        )
                                    }
                                }
                            }
                            val simplifyContent = messageWithSender.message.contents.simplifyText()
                            MessageContentContainer(shouldBreak = simplifyContent.filterNotNull().map {
                                it is ParaboxImage || it is ParaboxQuoteReply || it is ParaboxForward || it is ParaboxAudio || it is ParaboxFile || it is ParaboxVideo || it is ParaboxLocation
                            }) {
                                simplifyContent.forEachIndexed { index, paraboxMessageElement ->
                                    paraboxMessageElement?.toLayout(
                                        textColor = textColor,
                                        elementId = messageWithSender.message.contentsId[index],
                                        pkg = messageWithSender.message.pkg,
                                        atCache = state.atCache,
                                        previewerState = previewerState,
                                        onImageClick = onImageClick,
                                        onQuoteReplyClick = onQuoteReplyClick,
                                        resolveQuoteFallback = resolveQuoteFallback,
                                    )
                                }
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {

                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedVisibility(
                    visible = isLast,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }


}

fun MessageItemSelf(
    modifier: Modifier = Modifier,
    message: Message,
    onEvent: (e: MessagePageEvent) -> Unit,
) {

}

@Composable
private fun ParaboxMessageElement.toLayout(
    textColor: Color,
    elementId: Long,
    pkg: String,
    previewerState: ImagePreviewerState,
    atCache: Map<String, Resource<Contact>>,
    onImageClick: (elementId: Long) -> Unit,
    onQuoteReplyClick: (quoteMessageUid: String) -> Unit,
    resolveQuoteFallback: ((quoteMessageUid: String) -> Pair<String?, String>?)?,
) {
    when (this) {
        is ParaboxPlainText -> PlainTextLayout(text = buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) {
                append(text)
            }
        })

        // Annotated
        is ParaboxAt, ParaboxAtAll -> PlainTextLayout(text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(contentToString())
            }
        })

        is ParaboxAnnotatedText -> {
            val primary = MaterialTheme.colorScheme.primary
            val text =
                buildAnnotatedString {
                    forEachIndexed { index, paraboxText ->
                        when (paraboxText) {
                            is ParaboxAt -> {
                                val targetName = paraboxText.target.basicInfo.name?.let { "@$it" }
                                    ?: atCache["${pkg}${paraboxText.target.uid}"].takeIf { it is Resource.Success }?.data?.name?.let { "@$it" }
                                    ?: paraboxText.contentToString()
                                withStyle(SpanStyle(color = primary)) {
                                    append(targetName)
                                }
                            }

                            is ParaboxAtAll -> {
                                withStyle(SpanStyle(color = primary)) {
                                    append(paraboxText.contentToString())
                                }
                            }

                            else -> {
                                withStyle(SpanStyle(color = textColor)) {
                                    append(paraboxText.contentToString())
                                }
                            }
                        }
                    }
                }
            PlainTextLayout(text = text)
        }


        is ParaboxImage -> ImageLayout(
            model = resourceInfo.getModel(),
            elementId = elementId,
            previewerState = previewerState,
            onClick = onImageClick
        )

        is ParaboxAudio -> AudioLayout(
            resourceInfo = resourceInfo,
            length = length,
        )
        is ParaboxVideo -> VideoLayout(
            fileName = fileName,
            length = length,
            textColor = textColor,
        )
        is ParaboxFile -> FileLayout(
            fileName = name,
            fileSize = size,
            textColor = textColor,
        )
        is ParaboxLocation -> LocationLayout(
            name = name,
            latitude = latitude,
            longitude = longitude,
            textColor = textColor,
        )
        is ParaboxQuoteReply -> {
            val rawSenderName = sender?.basicInfo?.name?.takeIf { it.isNotBlank() }
            val rawSummary = messages.joinToString(" ") { it.contentToString() }.trim()
            val quoteId = id?.takeIf { it.isNotBlank() }
            val fallback = if ((rawSenderName == null || rawSummary.isEmpty()) && quoteId != null) {
                resolveQuoteFallback?.invoke(quoteId)
            } else {
                null
            }
            QuoteReplyLayout(
                senderName = rawSenderName ?: fallback?.first,
                summary = rawSummary.ifEmpty { fallback?.second.orEmpty() },
                textColor = textColor,
                onClick = quoteId?.let { targetUid -> { onQuoteReplyClick(targetUid) } },
            )
        }
        is ParaboxForward -> {}
        is ParaboxUnsupported -> UnsupportedLayout()
    }
}
