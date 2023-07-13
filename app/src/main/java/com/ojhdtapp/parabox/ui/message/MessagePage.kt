@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*
import kotlinx.coroutines.flow.collectLatest


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
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    else -> {}
                }
            }
    }
    val chatLazyPagingData = state.chatPagingDataFlow.collectAsLazyPagingItems()
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
                Text(text = "text")
            }
            item {
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item {
                        MyFilterChip(
                            modifier = Modifier.padding(end = 8.dp),
                            selected = false, label = {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "filter"
                                )
                            }) {
                            viewModel.sendEvent(MessagePageEvent.OpenEnabledChatFilterDialog(true))
                        }
                    }
                    item {
                        if (state.selectedGetChatFilterList.contains(GetChatFilter.Normal)) {
                            MyFilterChip(
                                modifier = Modifier.padding(end = 8.dp),
                                selected = false,
                                label = { Text(text = stringResource(id = R.string.get_chat_filter_normal)) }) {
                            }
                        }
                    }
                    items(items = state.enabledGetChatFilterList) {
                        MyFilterChip(selected = it in state.selectedGetChatFilterList,
                            modifier = Modifier.padding(end = 8.dp),
                            label = { Text(text = stringResource(id = it.labelResId)) }) {
                            viewModel.sendEvent(MessagePageEvent.AddOrRemoveSelectedGetChatFilter(it))
                        }
                    }
                }
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
                    enabled = state.datastore.enableSwipeToDismiss,
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