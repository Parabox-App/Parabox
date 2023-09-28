package com.ojhdtapp.parabox.ui.message.chat.top_bar

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationGraphicsApi::class)
@Composable
fun NormalChatTopBar(
    modifier: Modifier = Modifier,
    chatDetail: MessagePageState.ChatDetail,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val messageSelected by remember {
        derivedStateOf {
            chatDetail.selectedMessageList.isNotEmpty()
        }
    }

    val navigationIconPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(
            id = R.drawable.avd_pathmorph_drawer_arrow_to_cross
        ), atEnd = messageSelected
    )

    TopAppBar(modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = chatDetail.chat?.name ?: "")
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                if (messageSelected) {
                    onEvent(MessagePageEvent.ClearSelectedMessage)
                } else {
                    onEvent(MessagePageEvent.LoadMessage(null))
                }
            }) {
                Image(
                    modifier = Modifier.size(24.dp),
                    painter = navigationIconPainter, contentDescription = "navigation_icon",
                    contentScale = ContentScale.FillBounds
                )
            }
        },
        actions = {})
}