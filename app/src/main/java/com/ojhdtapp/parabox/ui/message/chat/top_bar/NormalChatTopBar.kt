package com.ojhdtapp.parabox.ui.message.chat.top_bar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationGraphicsApi::class, ExperimentalAnimationApi::class)
@Composable
fun NormalChatTopBar(
    modifier: Modifier = Modifier,
    chatDetail: MessagePageState.ChatDetail,
    scrollBehavior: TopAppBarScrollBehavior,
    onEvent: (MessagePageEvent) -> Unit,
) {
    val navigationIconPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(
            id = R.drawable.avd_pathmorph_drawer_arrow_to_cross
        ), atEnd = chatDetail.selectedMessageList.isNotEmpty()
    )

    TopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = chatDetail.selectedMessageList.size,
                    transitionSpec = {
                        // Compare the incoming number with the previous number.
                        if (targetState > initialState) {
                            // If the target number is larger, it slides up and fades in
                            // while the initial (smaller) number slides up and fades out.
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            // If the target number is smaller, it slides down and fades in
                            // while the initial number slides down and fades out.
                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }.using(
                            // Disable clipping since the faded slide-in/out should
                            // be displayed out of bounds.
                            SizeTransform(clip = false)
                        )
                    }, label = ""
                ) { num ->
                    Text(text = num.takeIf { it > 0 }?.toString() ?: chatDetail.chat?.name ?: "")
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                if (chatDetail.selectedMessageList.isNotEmpty()) {
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
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "search"
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}