@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ojhdtapp.parabox.core.util.toAvatarBitmap
import com.ojhdtapp.parabox.core.util.toTimeUntilNow
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.valentinilk.shimmer.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
) {
    val viewModel: MessagePageViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val shimmerInstance = rememberShimmer(shimmerBounds = ShimmerBounds.View)
    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is MessagePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(it.message)
                }
            }
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::setSearchText
            )
        },
        bottomBar = {
            com.ojhdtapp.parabox.ui.util.NavigationBar(
                navController = navController
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "发起会话") },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "new contact"
                    )
                },
                expanded = expandedFab,
                onClick = { /*TODO*/ })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            state = listState,
            contentPadding = paddingValues
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "未编组",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (viewModel.ungroupedContactState.value.isLoading) {
                itemsIndexed(listOf(null, null, null, null)) { index, _ ->
                    ContactItem(
                        contact = null,
                        isFirst = index == 0,
                        isLast = index == 3,
                        isLoading = true
                    ) {}
                    if (index < 3)
                        Spacer(modifier = Modifier.height(3.dp))
                }
            } else {
                val ungroupedContactList = viewModel.ungroupedContactState.value.data
                itemsIndexed(items = ungroupedContactList) { index, item ->
                    var loading by remember {
                        mutableStateOf(false)
                    }
                    ContactItem(
                        contact = item,
                        isFirst = index == 0,
                        isLast = index == ungroupedContactList.lastIndex,
                        isLoading = loading,
                        shimmer = shimmerInstance
                    ) {
                        loading = !loading
                    }
                    if (index < ungroupedContactList.lastIndex)
                        Spacer(modifier = Modifier.height(3.dp))
                }
            }
            item {
                Button(onClick = { viewModel.testFun() }) {
                    Text(text = "Do it")
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "其他",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
//        Column(
//            modifier = modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Button(onClick = {
//                onConnectBtnClicked()
//            }, enabled = viewModel.pluginInstalledState.value && !viewModel.sendAvailableState.value) {
//                Text(text = "Connect")
//            }
//            Button(
//                onClick = { onSendBtnClicked() },
//                enabled = viewModel.sendAvailableState.value
//            ) {
//                Text(text = "Send")
//            }
//            Text(text = viewModel.message.value)
//        }
    }
}

@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    contact: Contact?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isTop: Boolean = false,
    isLoading: Boolean = true,
    shimmer: Shimmer? = null,
    onClick: () -> Unit
) {
    val topRadius = animateDpAsState(targetValue = if (isFirst) 28.dp else 0.dp)
    val bottomRadius = animateDpAsState(targetValue = if (isLast) 28.dp else 0.dp)
    val background =
        animateColorAsState(targetValue = if (isTop) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = topRadius.value,
                    topEnd = topRadius.value,
                    bottomEnd = bottomRadius.value,
                    bottomStart = bottomRadius.value
                )
            )
            .background(background.value)
            .clickable { onClick() }
            .padding(16.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .shimmer(shimmer)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        } else {
            if (contact?.profile?.avatar == null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            } else {
                Image(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    bitmap = contact.profile.avatar.toAvatarBitmap(),
                    contentDescription = "avatar"
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(), verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(2.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(48.dp, 22.dp)
                        .shimmer(shimmer)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            } else {
                Text(
                    text = contact?.profile?.name ?: "会话名称",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(112.dp, 18.dp)
                        .shimmer(shimmer)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            } else {
                Text(
                    text = contact?.latestMessage?.content ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
            }
        }
        if (!isLoading) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact?.latestMessage?.timestamp?.toTimeUntilNow() ?: "",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                val unreadMessagesNum = contact?.latestMessage?.unreadMessagesNum ?: 0
                if (unreadMessagesNum != 0) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .defaultMinSize(minWidth = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$unreadMessagesNum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}