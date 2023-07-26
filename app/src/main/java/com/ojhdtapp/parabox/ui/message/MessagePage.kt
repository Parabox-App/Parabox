@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material3.fade
import com.google.accompanist.placeholder.placeholder
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.AvatarUtil.getCircledBitmap
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*
import kotlinx.coroutines.flow.collectLatest
import me.saket.cascade.CascadeDropdownMenu
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.rememberSwipeableActionsState
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    layoutType: MessageLayoutType
) {
    val viewModel = hiltViewModel<MessagePageViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val state by viewModel.uiState.collectAsState()
    val sharedState by mainSharedViewModel.uiState.collectAsState()
    LaunchedEffect(key1 = sharedState, block = {
        Log.d("parabox", "active: ${sharedState.search.isActive}")
    })
    LaunchedEffect(Unit) {
        viewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    else -> {}
                }
            }
    }
    val chatLazyPagingData = state.chatPagingDataFlow.collectAsLazyPagingItems()
    val searchBarPadding by animateDpAsState(
        targetValue = if (sharedState.search.isActive) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    val shouldHoverSearchBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }
    val searchBarShadowElevation by animateDpAsState(
        targetValue = if (shouldHoverSearchBar) 3.dp else 0.dp
    )
    val menuState by remember {
        derivedStateOf {
            sharedState.search.isActive
        }
    }
    val menuPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pathmorph_drawer_hamburger_to_arrow),
        atEnd = menuState
    )

    Scaffold(modifier = modifier,
        topBar = {
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = searchBarPadding),
                query = sharedState.search.query,
                onQueryChange = {
                    mainSharedViewModel.sendEvent(
                        MainSharedEvent.QueryInput(it)
                    )
                },
                onSearch = {
                    mainSharedViewModel.sendEvent(MainSharedEvent.SearchConfirm(it))
                },
                active = sharedState.search.isActive,
                onActiveChange = { mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(it)) },
                placeholder = { Text(text = "搜索 Parabox") },
                leadingIcon = {
                    IconButton(
                        onClick = {
                            if (sharedState.search.isActive) {
                                mainSharedViewModel.sendEvent(MainSharedEvent.TriggerSearchBar(false))
                            } else {
                                mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(!sharedState.openDrawer.open))
                            }
                        }
                    ) {
                        Image(
                            painter = menuPainter, contentDescription = "drawer",
                            contentScale = ContentScale.FillBounds
                        )
                    }
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = !sharedState.search.isActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = { mainSharedViewModel.sendEvent(MainSharedEvent.SearchAvatarClicked) },
                        ) {
                            SubcomposeAsyncImage(
                                modifier = Modifier.size(30.dp),
                                model = sharedState.datastore.localAvatarUri,
                                contentDescription = "user_avatar",
                            ) {
                                val state = painter.state
                                val namedAvatarBm =
                                    AvatarUtil.createNamedAvatarBm(
                                        backgroundColor = MaterialTheme.colorScheme.primary.toArgb(),
                                        textColor = MaterialTheme.colorScheme.onPrimary.toArgb(),
                                        name = sharedState.datastore.localName
                                    ).getCircledBitmap().asImageBitmap()
                                if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .placeholder(
                                                visible = state is AsyncImagePainter.State.Loading,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                highlight = PlaceholderHighlight.fade(),
                                            ),
                                        bitmap = namedAvatarBm,
                                        contentDescription = "named_avatar"
                                    )
                                } else {
                                    SubcomposeAsyncImageContent()
                                }
                            }
                        }
                    }
                },
                shadowElevation = searchBarShadowElevation,
            ) {
                SearchContent(state = sharedState, onEvent = mainSharedViewModel::sendEvent)
            }
        }) { it ->
        LazyColumn(
            contentPadding = it,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
                        if (it is GetChatFilter.Tag) {
                            MyFilterChip(selected = it in state.selectedGetChatFilterList,
                                modifier = Modifier.padding(end = 8.dp),
                                label = { Text(text = it.tag) }) {
                                viewModel.sendEvent(
                                    MessagePageEvent.AddOrRemoveSelectedGetChatFilter(
                                        it
                                    )
                                )
                            }
                        } else {
                            MyFilterChip(selected = it in state.selectedGetChatFilterList,
                                modifier = Modifier.padding(end = 8.dp),
                                label = { Text(text = stringResource(id = it.labelResId)) }) {
                                viewModel.sendEvent(
                                    MessagePageEvent.AddOrRemoveSelectedGetChatFilter(
                                        it
                                    )
                                )
                            }
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
                val threshold = 84.dp
                val swipeableActionsState = rememberSwipeableActionsState()
                val reachThreshold by remember{
                    derivedStateOf {
                        abs(swipeableActionsState.offset.value) > with(density) { threshold.toPx() }
                    }
                }
                LaunchedEffect(reachThreshold){
                    if(reachThreshold){
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                val scale by animateFloatAsState(
                    if (reachThreshold) 1f else 0.75f
                )
                val archive = SwipeAction(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Archive, contentDescription = "archive",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .scale(scale),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    background = MaterialTheme.colorScheme.primary,
                    onSwipe = {}
                )
                val done = SwipeAction(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Done, contentDescription = "archive",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .scale(scale),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    background = MaterialTheme.colorScheme.primary,
                    onSwipe = {}
                )
                val isFirst = index == 0
                val isLast = index == chatLazyPagingData.itemCount - 1
                val topRadius by animateDpAsState(
                    targetValue = if (isFirst && swipeableActionsState.offset.value == 0f) 24.dp else 3.dp
                )
                val bottomRadius by animateDpAsState(
                    targetValue = if (isLast && swipeableActionsState.offset.value == 0f) 24.dp else 3.dp
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = topRadius,
                                topEnd = topRadius,
                                bottomEnd = bottomRadius,
                                bottomStart = bottomRadius
                            )
                        )
                        .animateItemPlacement()
                ) {
                    var isMenuVisible by rememberSaveable { mutableStateOf(false) }
                    CascadeDropdownMenu(
                        expanded = isMenuVisible,
                        onDismissRequest = { isMenuVisible = false },
                        modifier = Modifier.clip(MaterialTheme.shapes.medium),
                        properties = PopupProperties(
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        ),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Horizon") },
                            children = {
                                DropdownMenuItem(
                                    text = { Text("Zero Dawn") },
                                    onClick = { }
                                )
                                DropdownMenuItem(
                                    text = { Text("Forbidden West") },
                                    onClick = { }
                                )
                            }
                        )
                    }
                    SwipeableActionsBox(
                        state = swipeableActionsState,
                        startActions = if (state.datastore.enableSwipeToDismiss) listOf(archive) else emptyList(),
                        endActions = if (state.datastore.enableSwipeToDismiss) listOf(done) else emptyList(),
                        swipeThreshold = threshold,
                        backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.secondary
                    ) {
                        if (chatLazyPagingData[index] == null) {
                            EmptyChatItem()
                        } else {
                            val contact by viewModel.getLatestMessageSenderWithCache(
                                chatLazyPagingData[index]!!.message?.senderId
                            ).collectAsState(initial = Resource.Loading())
                            ChatItem(
                                chatWithLatestMessage = chatLazyPagingData[index]!!,
                                contact = contact,
                                onLongClick = {
                                    isMenuVisible = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}