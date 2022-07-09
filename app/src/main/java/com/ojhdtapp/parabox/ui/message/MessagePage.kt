@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ojhdtapp.parabox.core.util.toAvatarBitmap
import com.ojhdtapp.parabox.core.util.toTimeUntilNow
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.MainScreenSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.MessageNavGraph
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.valentinilk.shimmer.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class
)
@Destination
@MessageNavGraph(start = true)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    viewModel: MessagePageViewModel,
    sharedViewModel: MainScreenSharedViewModel,
) {
//    val viewModel: MessagePageViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    val shimmerInstance = rememberShimmer(shimmerBounds = ShimmerBounds.View)
    val expandedFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }
    var searchBarActivateState by remember {
        mutableStateOf(SearchAppBar.NONE)
    }
    val contactState by viewModel.contactStateFlow.collectAsState()
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is MessagePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(it.message)
                }
                is MessagePageUiEvent.UpdateMessageBadge -> {
                    sharedViewModel.setMessageBadge(it.value)
                }
            }
        }
    }
    GroupActionDialog(showDialog = viewModel.showGroupActionDialogState.value,
        state = viewModel.groupInfoState.value, onDismiss = {
        viewModel.setShowGroupActionDialogState(false)
    }, onConfirm = viewModel::groupContact)
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::setSearchText,
                placeholder = "搜索会话",
                activateState = searchBarActivateState,
                onActivateStateChanged = {
                    searchBarActivateState = it
                    viewModel.clearSelectedContactIdStateList()
                },
                selectedNum = "${viewModel.selectedContactIdStateList.size}",
                isGroupActionAvailable = viewModel.selectedContactIdStateList.size > 1,
                onGroupAction = {
                    viewModel.getGroupInfoPack()
                    viewModel.setShowGroupActionDialogState(true)
                }
            )
        },
        bottomBar = {
            com.ojhdtapp.parabox.ui.util.NavigationBar(
                navController = navController,
                messageBadge = sharedViewModel.messageBadge.value,
                onSelfItemClick = {},
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
                onClick = { })
        },
    ) { paddingValues ->
        LazyColumn(
//            modifier = Modifier.padding(horizontal = 16.dp),
            state = listState,
            contentPadding = paddingValues
        ) {
            item(key = "ungrouped") {
                Box(
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                        .animateItemPlacement()
                ) {
                    Text(
                        text = "未编组",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (contactState.isLoading) {
                itemsIndexed(
                    items = listOf(null, null, null, null),
                    key = { index, _ -> index }) { index, _ ->
                    ContactItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItemPlacement(),
                        contact = null,
                        topRadius = 28.dp,
                        bottomRadius = 28.dp,
                        isLoading = true,
                        onClick = {},
                        onLongClick = {}
                    )
                    if (index < 3)
                        Spacer(modifier = Modifier.height(3.dp))
                }
            } else {
                itemsIndexed(
                    items = contactState.data,
                    key = { _, item -> item.contactId }) { index, item ->
                    var loading by remember {
                        mutableStateOf(false)
                    }
                    val dismissState = rememberDismissState(
                        confirmStateChange = {
                            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                                viewModel.showSnackBar("Dismiss triggered")
                            }
                            true
                        }
                    )
                    val isFirst = index == 0
                    val isLast = index == contactState.data.lastIndex
                    val topRadius by animateDpAsState(targetValue = if (isFirst) 28.dp else 0.dp)
                    val bottomRadius by animateDpAsState(targetValue = if (isLast) 28.dp else 0.dp)
                    val isSelected =
                        viewModel.selectedContactIdStateList.contains(item.contactId)
                    SwipeToDismiss(
                        state = dismissState,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItemPlacement(),
                        background = {
                            val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                            val arrangement = when (direction) {
                                DismissDirection.StartToEnd -> Arrangement.Start
                                DismissDirection.EndToStart -> Arrangement.End
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = topRadius,
                                            topEnd = topRadius,
                                            bottomEnd = bottomRadius,
                                            bottomStart = bottomRadius
                                        )
                                    )
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = arrangement
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DoNotDisturb,
                                    contentDescription = "not disturb",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        directions = setOf(
                            DismissDirection.EndToStart,
                            DismissDirection.StartToEnd
                        ),
                        dismissThresholds = { dismissDirection ->
                            androidx.compose.material.FractionalThreshold(
                                0.65f
                            )
                        }
                    ) {
                        ContactItem(
                            contact = item,
                            topRadius = topRadius,
                            bottomRadius = bottomRadius,
                            isLoading = loading,
                            isSelected = isSelected,
                            shimmer = shimmerInstance,
                            onClick = {
                                if (searchBarActivateState == SearchAppBar.SELECT) {
                                    viewModel.addOrRemoveItemOfSelectedContactIdStateList(item.contactId)
                                } else {
                                    viewModel.receiveAndUpdateMessageFromContact(item)
                                    navigator.navigate(ChatPageDestination)
                                }
                            },
                            onLongClick = {
                                searchBarActivateState = SearchAppBar.SELECT
                                viewModel.addOrRemoveItemOfSelectedContactIdStateList(item.contactId)
                            }
                        )
                    }
                    if (index < contactState.data.lastIndex)
                        Spacer(modifier = Modifier.height(2.dp))
                }
            }
            item(key = "other") {
                Box(
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                        .animateItemPlacement()
                ) {
                    Text(
                        text = "其他",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            item(key = null) {
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun() }) {
                    Text(text = "btn1")
                }
            }
            item(key = null) {
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun2() }) {
                    Text(text = "btn2")
                }
            }
            item(key = null) {
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun3() }) {
                    Text(text = "btn3")
                }
            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    contact: Contact?,
    topRadius: Dp,
    bottomRadius: Dp,
//    isFirst: Boolean = false,
//    isLast: Boolean = false,
    isTop: Boolean = false,
    isLoading: Boolean = true,
    isSelected: Boolean = false,
    shimmer: Shimmer? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val background =
        animateColorAsState(targetValue = if (isTop) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = topRadius,
                    topEnd = topRadius,
                    bottomEnd = bottomRadius,
                    bottomStart = bottomRadius
                )
            )
            .background(background.value)
            .combinedClickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = LocalIndication.current,
                enabled = true,
                onLongClick = onLongClick,
                onClick = onClick
            )
//            .clickable { onClick() }
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
            Crossfade(targetState = isSelected) {
                if (it) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp, 18.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(112.dp, 10.dp)
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
                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(text = "$unreadMessagesNum") }
//                    Box(
//                        modifier = Modifier
//                            .height(16.dp)
//                            .defaultMinSize(minWidth = 16.dp)
//                            .clip(RoundedCornerShape(8.dp))
//                            .background(MaterialTheme.colorScheme.primary)
//                            .padding(horizontal = 4.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "$unreadMessagesNum",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onPrimary
//                        )
//                    }
                }
            }
        }
    }
}