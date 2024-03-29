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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
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
import me.saket.cascade.CascadeDropdownMenu
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationGraphicsApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun ContactPage(
    modifier: Modifier = Modifier,
    viewModel: ContactPageViewModel,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Nothing>,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val snackBarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsState()
    val contactPagingData = viewModel.contactPagingDataFlow.collectAsLazyPagingItems()
    val friendPagingData = viewModel.friendPagingDataFlow.collectAsLazyPagingItems()
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
                        .padding(horizontal = 2.dp)
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
                            Icon(painter = menuPainter, contentDescription = "drawer")
                        }
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = !mainSharedState.search.isActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    var isMenuVisible by remember {
                                        mutableStateOf(false)
                                    }
                                    CascadeDropdownMenu(
                                        expanded = isMenuVisible,
                                        onDismissRequest = { isMenuVisible = false },
                                        offset = DpOffset(16.dp, 0.dp),
                                        properties = PopupProperties(
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true,
                                            focusable = true
                                        ),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        DropdownMenuHeader {
                                            Text(text = "筛选")
                                        }
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Text(text = "仅显示好友")
                                            },
                                            onClick = {
                                                viewModel.sendEvent(ContactPageEvent.ToggleFriendOnly)
                                                isMenuVisible = false
                                            },
                                            trailingIcon = {
                                                Checkbox(checked = state.friendOnly, onCheckedChange = {
                                                    viewModel.sendEvent(ContactPageEvent.ToggleFriendOnly)
                                                    isMenuVisible = false
                                                })
                                            })
                                    }
                                    IconButton(onClick = { isMenuVisible = true }) {
                                        Icon(imageVector = Icons.Outlined.Tune, contentDescription = "more")
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    var isMenuVisible by remember {
                                        mutableStateOf(false)
                                    }
                                    CascadeDropdownMenu(
                                        expanded = isMenuVisible,
                                        onDismissRequest = { isMenuVisible = false },
                                        offset = DpOffset(16.dp, 0.dp),
                                        properties = PopupProperties(
                                            dismissOnBackPress = true,
                                            dismissOnClickOutside = true,
                                            focusable = true
                                        ),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        DropdownMenuHeader {
                                            Text(text = "筛选")
                                        }
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = {
                                                Text(text = "仅显示好友")
                                            },
                                            onClick = {
                                                viewModel.sendEvent(ContactPageEvent.ToggleFriendOnly)
                                                isMenuVisible = false
                                            },
                                            trailingIcon = {
                                                Checkbox(checked = state.friendOnly, onCheckedChange = {
                                                    viewModel.sendEvent(ContactPageEvent.ToggleFriendOnly)
                                                    isMenuVisible = false
                                                })
                                            })
                                    }
                                    IconButton(onClick = { isMenuVisible = true }) {
                                        Icon(imageVector = Icons.Outlined.Tune, contentDescription = "more")
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
//                for (index in 0 until contactPagingData.itemCount) {
//                    val peekItem = contactPagingData.peek(index)
//                    val nameFirstLetter = peekItem?.contact?.name?.let { FormUtil.getFirstLetter(it) }
//                    val lastNameFirstLetter = (index - 1).takeIf { it >= 0 }
//                        ?.let { contactPagingData.peek(it) }?.contact?.name?.let { FormUtil.getFirstLetter(it) }
//                    if (lastNameFirstLetter == null || lastNameFirstLetter != nameFirstLetter) {
//                        stickyHeader {
//                            ContactStickyHeader(character = nameFirstLetter ?: "#")
//                        }
//                    }
//                    item(key = peekItem?.contact?.contactId ?: index) {
//                        val item = contactPagingData[index]
//                        if (item == null) {
//                            EmptyContactItem()
//                        } else {
//
//                            ContactItem(
//                                modifier = Modifier.padding(horizontal = if (layoutType == ContactLayoutType.SPLIT) 0.dp else 16.dp),
//                                name = item.contact.name,
//                                character = nameFirstLetter,
//                                avatarModel = item.contact.avatar.getModel(),
//                                extName = item.extensionInfo.alias,
//                                onClick = {})
//                        }
//                    }
//                }
                if (state.friendOnly) {
                    items(count = friendPagingData.itemCount,
                        key = friendPagingData.itemKey { it.contact.contactId }) { index ->
                        val item = friendPagingData[index]
                        if (item == null) {
                            EmptyContactItem()
                        } else {
                            ContactItem(
                                modifier = Modifier.padding(horizontal = if (layoutType == ContactLayoutType.SPLIT) 0.dp else 16.dp),
                                name = item.contact.name,
                                lastName = (index - 1).takeIf { it >= 0 }
                                    ?.let { friendPagingData.peek(it) }?.contact?.name,
                                avatarModel = item.contact.avatar.getModel(),
                                extName = item.extensionInfo.alias,
                                onClick = {
                                    viewModel.sendEvent(ContactPageEvent.LoadContactDetail(item))
                                    scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    onMainSharedEvent(MainSharedEvent.ShowNavigationBar(false))
                                })
                        }
                    }
                } else {
                    items(count = contactPagingData.itemCount,
                        key = contactPagingData.itemKey { it.contact.contactId }) { index ->
                        val item = contactPagingData[index]
                        if (item == null) {
                            EmptyContactItem()
                        } else {
                            ContactItem(
                                modifier = Modifier.padding(horizontal = if (layoutType == ContactLayoutType.SPLIT) 0.dp else 16.dp),
                                name = item.contact.name,
                                lastName = (index - 1).takeIf { it >= 0 }
                                    ?.let { contactPagingData.peek(it) }?.contact?.name,
                                avatarModel = item.contact.avatar.getModel(),
                                extName = item.extensionInfo.alias,
                                onClick = {
                                    viewModel.sendEvent(ContactPageEvent.LoadContactDetail(item))
                                    scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    onMainSharedEvent(MainSharedEvent.ShowNavigationBar(false))
                                })
                        }
                    }
                }
                item {
                    if (layoutType == ContactLayoutType.NORMAL) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            val paddingValues = if (layoutType == ContactLayoutType.SPLIT) {
                it
            } else {
                PaddingValues(
                    top = it.calculateTopPadding(),
                    bottom = it.calculateBottomPadding() + 80.dp
                )
            }
            InternalLazyColumnScrollbar(
                listState = listState,
//                modifier = Modifier.padding(top = 144.dp, bottom = 112.dp),
                modifier = Modifier.padding(paddingValues),
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