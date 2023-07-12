@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    drawerState: DrawerState,
    bottomSheetState: SheetState,
    layoutType: MessageLayoutType
) {
    val viewModel = hiltViewModel<MessagePageViewModel>()
    val chatLazyPagingData = viewModel.getChatPagingDataFlow().collectAsLazyPagingItems()
    Scaffold(modifier = modifier,
        topBar = {
//        SearchBar(query = , onQueryChange = , onSearch = , active = , onActiveChange = ) {
//
//    }
        }) { it ->
        LazyColumn(
            contentPadding = it,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                androidx.compose.material3.Text(text = "text")
            }
//            if (chatLazyPagingData.loadState.refresh == LoadState.Loading) {
//                items(12) {
//                    EmptyChatItem(
//                        modifier = Modifier
//                            .padding(start = 16.dp, end = 16.dp),
//                        isFirst = it == 0,
//                        isLast = it == 11
//                    )
//                }
//            }
            items(
                count = chatLazyPagingData.itemCount,
                key = chatLazyPagingData.itemKey { it.chat.chatId },
                contentType = chatLazyPagingData.itemContentType { "chat" }
            ) { index ->
                SwipeToDismissContact(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .animateItemPlacement(),
                    enabled = viewModel.enableSwipeToDismissFlow.collectAsState(initial = false).value,
                    startToEndIcon = Icons.Outlined.Archive,
                    endToStartIcon = Icons.Outlined.MarkChatRead,
                    onDismissedToEnd = { true },
                    onDismissedToStart = { true },
                    onVibrate = { }) {
                    if (chatLazyPagingData[index] == null) {
                        EmptyChatItem(
                            isFirst = index == 0,
                            isLast = index == chatLazyPagingData.itemCount - 1
                        )
                    } else {
                        val contact by viewModel.getLatestMessageSenderWithCache(
                            chatLazyPagingData[index]!!.message?.senderId
                        ).collectAsState(initial = Resource.Loading())
                        ChatItem(
                            chatWithLatestMessage = chatLazyPagingData[index]!!,
                            contact = contact,
                            isFirst = index == 0,
                            isLast = index == chatLazyPagingData.itemCount - 1
                        )
                    }
                }
            }
        }
    }
}