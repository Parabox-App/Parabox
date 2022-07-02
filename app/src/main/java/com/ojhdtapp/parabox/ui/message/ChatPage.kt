package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    contact: Contact,
) {
    val viewModel: MessagePageViewModel = hiltViewModel()
    val messageState = viewModel.messageStateFlow.collectAsState().value
    Crossfade(targetState = messageState.state) {
        when (it) {
            MessageState.NULL -> {
                NullChatPage()
            }
            MessageState.ERROR -> {
                ErrorChatPage(errMessage = messageState.message ?: "请重试") {}
            }
            MessageState.LOADING or MessageState.SUCCESS -> {
                NormalChatPage(messageState = messageState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NormalChatPage(modifier: Modifier = Modifier, messageState: MessageState) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarScrollState())
    BottomSheetScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = { Text(text = messageState.profile?.name ?: "会话") },
                navigationIcon = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.ArrowBack, contentDescription = "back")
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "search")

                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        sheetContent = {
            EditArea(){}
        }) {
        LazyColumn(modifier = Modifier.padding(it)) {

        }
    }
}

@Composable
fun ErrorChatPage(modifier: Modifier = Modifier, errMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = errMessage, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

@Composable
fun NullChatPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "选择会话", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TimeDivider(modifier: Modifier = Modifier, timestamp: Long) {
    Row(
        modifier = modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text(text = timestamp.toDescriptiveTime())
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun EditArea(modifier: Modifier = Modifier, onSend: (text: String) -> Unit) {
    var inputText by remember {
        mutableStateOf("")
    }
    var shouldToolbarShrink by remember {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Crossfade(targetState = shouldToolbarShrink) {
                if (it) {
                    Row() {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircleOutline,
                                contentDescription = "more"
                            )
                        }
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = "emoji"
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { shouldToolbarShrink = false }) {
                        Icon(
                            imageVector = Icons.Outlined.NavigateNext,
                            contentDescription = "expand"
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .animateContentSize()
            ) {
                BasicTextField(value = inputText, onValueChange = {
                    if(it.length > 6) shouldToolbarShrink = true
                    inputText = it },
                    enabled = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "输入内容",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    })
            }
            AnimatedVisibility(visible = !inputText.isNullOrEmpty()) {
//                OutlinedButton(onClick = {onSend(inputText)}) {
//                    Text(text = "发送")
//                }
                FloatingActionButton(onClick = { /*TODO*/ }) {
                    Icon(imageVector = Icons.Outlined.Send, contentDescription = "send")
                }
            }
        }
    }
}