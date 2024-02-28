package com.ojhdtapp.parabox.ui.contact

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.CommonAvatar
import com.ojhdtapp.parabox.ui.common.CommonAvatarModel
import com.ojhdtapp.parabox.ui.common.SearchContent
import com.ojhdtapp.parabox.ui.common.clearFocusOnKeyboardDismiss
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationGraphicsApi::class)
@Composable
fun ContactPage(
    modifier: Modifier = Modifier,
    viewModel: ContactPageViewModel,
    mainNavController: NavController,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val snackBarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()
    val contactPagingData = viewModel.contactPagingDataFlow.collectAsLazyPagingItems()
    val menuPainter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(id = R.drawable.avd_pathmorph_drawer_hamburger_to_arrow),
        atEnd = mainSharedState.search.isActive
    )
    val listState = rememberLazyListState()
    val searchBarPadding by animateDpAsState(
        targetValue = if (mainSharedState.search.isActive || layoutType == ContactLayoutType.SPLIT) 0.dp else 16.dp,
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
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            if (layoutType == ContactLayoutType.SPLIT) {
                DockedSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .clearFocusOnKeyboardDismiss(),
                    query = mainSharedState.search.query,
                    onQueryChange = {
                        onMainSharedEvent(
                            MainSharedEvent.QueryInput(it)
                        )
                    },
                    onSearch = {
                        if (it.isNotBlank()) {
                            onMainSharedEvent(MainSharedEvent.SearchConfirm(it))
                        }
                    },
                    active = mainSharedState.search.isActive,
                    onActiveChange = { onMainSharedEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (mainSharedState.search.isActive) {
                                    onMainSharedEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    onMainSharedEvent(MainSharedEvent.OpenDrawer(!mainSharedState.openDrawer.open))
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
                            visible = !mainSharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { onMainSharedEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonAvatar(
                                        model = CommonAvatarModel(
                                            model = mainSharedState.datastore.localAvatarUri,
                                            name = mainSharedState.datastore.localName
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        textColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = mainSharedState, onEvent = onMainSharedEvent)
                }
            } else {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = searchBarPadding)
                        .clearFocusOnKeyboardDismiss(),
                    query = mainSharedState.search.query,
                    onQueryChange = {
                        onMainSharedEvent(
                            MainSharedEvent.QueryInput(it)
                        )
                    },
                    onSearch = {
                        if (it.isNotBlank()) {
                            onMainSharedEvent(MainSharedEvent.SearchConfirm(it))
                        }
                    },
                    active = mainSharedState.search.isActive,
                    onActiveChange = { onMainSharedEvent(MainSharedEvent.TriggerSearchBar(it)) },
                    placeholder = { Text(text = "搜索 Parabox") },
                    leadingIcon = {
                        IconButton(
                            onClick = {
                                if (mainSharedState.search.isActive) {
                                    onMainSharedEvent(MainSharedEvent.TriggerSearchBar(false))
                                } else {
                                    onMainSharedEvent(MainSharedEvent.OpenDrawer(!mainSharedState.openDrawer.open))
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
                            visible = !mainSharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { onMainSharedEvent(MainSharedEvent.SearchAvatarClicked) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CommonAvatar(
                                        model = CommonAvatarModel(
                                            model = mainSharedState.datastore.localAvatarUri,
                                            name = mainSharedState.datastore.localName
                                        ),
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        textColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    },
                    shadowElevation = searchBarShadowElevation,
                    colors = SearchBarDefaults.colors(dividerColor = Color.Transparent)
                ) {
                    SearchContent(state = mainSharedState, onEvent = onMainSharedEvent)
                }
            }
        }
    ) {
        Box {
            LazyColumn(
                contentPadding = it,
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = contactPagingData.itemCount,
                    key = contactPagingData.itemKey { it.contact.contactId }) { index ->
                    val item = contactPagingData[index]
                    if (item == null) {
                        EmptyContactItem()
                    } else {
                        ContactItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            name = item.contact.name,
                            lastName = (index - 1).takeIf { it >= 0 }
                                ?.let { contactPagingData.peek(it) }?.contact?.name,
                            avatarModel = item.contact.avatar.getModel(),
                            extName = item.extensionInfo.alias,
                            onClick = {})
                    }
                }
                item {
                    if (layoutType == ContactLayoutType.NORMAL) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            InternalLazyColumnScrollbar(
                listState = listState,
                modifier = Modifier.padding(top = 144.dp, bottom = 112.dp),
                selectionMode = ScrollbarSelectionMode.Full,
                thumbColor = MaterialTheme.colorScheme.secondary,
                thumbSelectedColor = MaterialTheme.colorScheme.primary,
                indicatorContent = { index: Int, isThumbSelected: Boolean ->
                    AnimatedVisibility(visible = isThumbSelected, enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp, bottom = 48.dp)
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary,
                                    RoundedCornerShape(40.dp, 40.dp, 8.dp, 40.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormUtil.getFirstLetter(contactPagingData.peek(index)?.contact?.name),
                                color = MaterialTheme.colorScheme.onSecondary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            )
        }

    }
}