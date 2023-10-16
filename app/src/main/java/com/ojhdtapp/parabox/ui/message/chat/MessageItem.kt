package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.contains
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.AudioLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.FileLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.ImageLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.LocationLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.MessageContentContainer
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.PlainTextLayout
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
import com.ojhdtapp.paraboxdevelopmentkit.model.message.simplifyText

@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    state: MessagePageState.ChatDetail,
    messageWithSender: ChatPageUiModel.MessageWithSender,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    onEvent: (e: MessagePageEvent) -> Unit,
) {
    val context = LocalContext.current
    val isSelected by remember(state.selectedMessageList) {
        derivedStateOf {
            state.selectedMessageList.contains(messageWithSender.message.messageId)
        }
    }
    val topStartRadius by animateDpAsState(targetValue = if (isFirst) 24.dp else 3.dp)
    val bottomStartRadius by animateDpAsState(targetValue = if (isLast) 24.dp else 3.dp)
    val backgroundColor by animateColorAsState(
        targetValue =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    )
    val textColor by animateColorAsState(
        targetValue =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(42.dp)) {
            if (isFirst) {
                CommonAvatar(
                    model = messageWithSender.sender.avatar.getModel(),
                    name = messageWithSender.sender.name
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (isFirst) {
                Text(
                    text = messageWithSender.sender.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(
                        maxWidth = constraints.maxWidth - with(density) {
                            90.dp.roundToPx()
                        }
                    ))
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                },
                shape = RoundedCornerShape(
                    topStart = topStartRadius,
                    topEnd = 24.dp,
                    bottomStart = bottomStartRadius,
                    bottomEnd = 24.dp
                ),
                border = BorderStroke(3.dp, backgroundColor),
                color = backgroundColor
            ) {
                Column {
                    SelectionContainer {
                        val simplifyContent = messageWithSender.message.contents.simplifyText()
                        MessageContentContainer(shouldBreak = simplifyContent.map {
                            it is ParaboxImage || it is ParaboxQuoteReply || it is ParaboxForward || it is ParaboxAudio || it is ParaboxFile
                        }) {
                            simplifyContent.forEachIndexed { index, paraboxMessageElement ->
                                paraboxMessageElement.toLayout(textColor = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                    }
                }
            }
            if(isLast){
                Spacer(modifier = Modifier.height(8.dp))
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
private fun ParaboxMessageElement.toLayout(textColor: Color) {
    when (this) {
        is ParaboxPlainText -> PlainTextLayout(text = buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) {
                append(text)
            }
        })

        is ParaboxAt, ParaboxAtAll -> PlainTextLayout(text = buildAnnotatedString {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(contentToString())
            }
        })

        is ParaboxAnnotatedText -> PlainTextLayout(text = buildAnnotatedString {
            list.forEachIndexed { index, paraboxText ->
                when (paraboxText) {
                    is ParaboxAt, ParaboxAtAll -> {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(paraboxText.contentToString())
                        }
                    }

                    else -> {
                        withStyle(SpanStyle(color = textColor)) {
                            append(paraboxText.contentToString())
                        }
                    }
                }
                if (index != list.lastIndex) {
                    append(" ")
                }
            }
        })

        is ParaboxImage -> ImageLayout(model = resourceInfo.getModel())
        is ParaboxAudio -> AudioLayout()
        is ParaboxFile -> FileLayout()
        is ParaboxLocation -> LocationLayout()
        is ParaboxQuoteReply -> {}
        is ParaboxForward -> {}
    }
}
