@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.core.util.toAvatarBitmap
import com.ojhdtapp.parabox.core.util.toTimeUntilNow
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import com.ojhdtapp.parabox.ui.util.MessageNavGraph
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import com.valentinilk.shimmer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class
)
@Destination
@MessageNavGraph(start = true)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState
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
    val contactState by viewModel.contactStateFlow.collectAsState()
    val archivedContact by viewModel.archivedContactStateFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collectLatest { it ->
            when (it) {
                is MessagePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar(it.message, it.label)
                }
                is MessagePageUiEvent.UpdateMessageBadge -> {
                    mainSharedViewModel.setMessageBadge(it.value)
                }
            }
        }
    }
    Row() {
        // Left
        GroupActionDialog(
            showDialog = viewModel.showGroupActionDialogState.value,
            state = viewModel.groupInfoState.value,
            sizeClass = sizeClass,
            onDismiss = {
                viewModel.setShowGroupActionDialogState(false)
            }, onConfirm = viewModel::groupContact
        )
        EditActionDialog(
            showDialog = viewModel.showEditActionDialogState.value,
            contact = contactState.data.findLast { it.contactId == viewModel.selectedContactStateList.firstOrNull()?.contactId },
            sizeClass = sizeClass,
            onDismiss = { viewModel.setShowEditActionDialogState(false) },
            onConfirm = {},
            onEvent = {
                when (it) {
                    is EditActionDialogEvent.ProfileAndTagUpdate -> {
                        viewModel.setContactProfileAndTag(it.contactId, it.profile, it.tags)
                        viewModel.addContactTag(it.tags)
                    }
                    is EditActionDialogEvent.EnableNotificationStateUpdate -> {
                        viewModel.setContactNotification(it.contactId, it.value)
                    }
                    is EditActionDialogEvent.PinnedStateUpdate -> {
                        viewModel.setContactPinned(it.contactId, it.value)
                    }
                    is EditActionDialogEvent.ArchivedStateUpdate -> {
                        viewModel.setContactArchived(it.contactId, it.value)
                    }
                }
            }
        )
        TagEditAlertDialog(
            showDialog = viewModel.showTagEditAlertDialogState.value,
            contact = contactState.data.findLast { it.contactId == viewModel.selectedContactStateList.firstOrNull()?.contactId },
            sizeClass = sizeClass,
            onDismiss = { viewModel.setShowTagEditAlertDialogState(false) },
            onConfirm = { id: Long, tags: List<String> ->
                viewModel.setContactTag(id, tags)
                viewModel.addContactTag(tags)
                viewModel.setShowTagEditAlertDialogState(false)
                viewModel.clearSelectedContactStateList()
            }
        )
        Scaffold(
            modifier = modifier
                .weight(1f)
                .shadow(8.dp)
                .zIndex(1f),
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
            topBar = {
                SearchAppBar(
                    text = viewModel.searchText.value,
                    onTextChange = viewModel::setSearchText,
                    placeholder = "搜索会话",
                    activateState = viewModel.searchBarActivateState.value,
                    onActivateStateChanged = {
                        viewModel.setSearchBarActivateState(it)
                        viewModel.clearSelectedContactStateList()
                        when (it) {
                            SearchAppBar.SEARCH -> {
                                viewModel.setAreaState(AreaState.SearchArea)
                            }
                            SearchAppBar.NONE, SearchAppBar.SELECT, SearchAppBar.ARCHIVE_SELECT -> {
                                viewModel.setAreaState(AreaState.MessageArea)
                            }
                            else -> {}
                        }
                    },
                    selection = viewModel.selectedContactStateList,
                    onGroupAction = {
                        viewModel.getGroupInfoPack()
                        viewModel.setShowGroupActionDialogState(true)
                    },
                    onEditAction = {
                        viewModel.setShowEditActionDialogState(true)
                    },
                    onNewTagAction = {
                        viewModel.setShowTagEditAlertDialogState(true)
                    },
                    onHideAction = {
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = "会话已暂时隐藏",
                                actionLabel = "取消",
                                duration = SnackbarDuration.Short
                            )
                                .also { result ->
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.cancelContactHidden()
                                        }
                                        SnackbarResult.Dismissed -> {}
                                        else -> {}
                                    }
                                }
                        }
                        viewModel.setContactHidden(
                            viewModel.selectedContactStateList.toList().map { it.contactId })
                    },
                    onPinAction = {
                        viewModel.setContactPinned(
                            viewModel.selectedContactStateList.toList().map { it.contactId }, it
                        )
                    },
                    onArchiveAction = {
                        viewModel.setContactArchived(
                            viewModel.selectedContactStateList.toList().map { it.contactId }, it
                        )
                    },
                    onMarkAsReadAction = {
                        if (it) {
                            viewModel.clearContactUnreadNum(
                                viewModel.selectedContactStateList.toList().map { it.contactId })
                        } else {
                            viewModel.restoreContactUnreadNum(
                                viewModel.selectedContactStateList.toList().map { it.contactId })
                        }
                    },
                    onExpandAction = {},
                    sizeClass = sizeClass,
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    }
                )
            },
            bottomBar = {

            },
            floatingActionButton = {
                if (sizeClass.widthSizeClass != WindowWidthSizeClass.Medium) {
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
                }
            },
        ) { paddingValues ->
            AnimatedContent(targetState = viewModel.areaState.value,
                transitionSpec = {
                    when (initialState) {
                        AreaState.MessageArea -> scaleIn(tween(200), 1.1f) + fadeIn(tween(200)) with
                                scaleOut(tween(200), 0.9f) + fadeOut(tween(200))
                        AreaState.SearchArea, AreaState.ArchiveArea -> scaleIn(
                            tween(200),
                            0.9f
                        ) + fadeIn(tween(200)) with
                                scaleOut(tween(200), 1.1f) + fadeOut(tween(200))
                        else -> fadeIn() with fadeOut()
                    }
                }) {
                when (it) {
                    AreaState.MessageArea -> MessageArea(
                        Modifier,
                        listState,
                        paddingValues,
                        viewModel,
                        contactState,
                        archivedContact,
                        coroutineScope,
                        snackBarHostState,
                        mainSharedViewModel,
                        sizeClass,
                        shimmerInstance,
                        mainNavController
                    )
                    AreaState.SearchArea -> SearchArea(
                        Modifier,
                        paddingValues,
                        viewModel,
                        coroutineScope,
                        snackBarHostState,
                        mainSharedViewModel,
                        sizeClass,
                        shimmerInstance,
                        mainNavController
                    )
                    AreaState.ArchiveArea -> ArchiveArea(
                        Modifier,
                        paddingValues,
                        viewModel,
                        archivedContact,
                        coroutineScope,
                        snackBarHostState,
                        mainSharedViewModel,
                        sizeClass,
                        shimmerInstance,
                        mainNavController
                    )
                    else -> {}
                }
            }

        }
        // Right
        if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
            ChatPage(
                modifier = Modifier.width(560.dp),
                navigator = navigator,
                mainNavController = mainNavController,
                mainSharedViewModel = mainSharedViewModel,
                sizeClass = sizeClass
            )
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

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class
)
@Composable
fun RowScope.MessageArea(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    paddingValues: PaddingValues,
    viewModel: MessagePageViewModel,
    contactState: ContactState,
    archivedContact: List<Contact>,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    shimmerInstance: Shimmer,
    mainNavController: NavController
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = paddingValues
    ) {
        item(key = "tag") {
            val hashTagList = remember {
                mutableStateListOf<String>()
            }
            var hashTagText by remember {
                mutableStateOf("")
            }
            var hashTagError by remember {
                mutableStateOf<String>("")
            }
            var hashTagShouldShowError by remember {
                mutableStateOf(false)
            }
            var onConfirmDelete by remember {
                mutableStateOf(false)
            }
            val isEditing = viewModel.tagEditing.value
            val hashTagLazyListState = rememberLazyListState()
            val hashTagFocusRequester = remember { FocusRequester() }
            val hashTagInteraction = remember { MutableInteractionSource() }
            val rowInteraction = remember { MutableInteractionSource() }
            LaunchedEffect(key1 = true, block = {
                viewModel.contactTagStateFlow.collectLatest {
                    it.map { it.value }.let {
                        hashTagList.retainAll(it)
                        it.forEach {
                            if (!hashTagList.contains(it))
                                hashTagList.add(it)
                        }
                    }
                }
            })
            HashTagEditor(
                textFieldValue = hashTagText,
                enabled = isEditing,
                onValueChanged = {
                    val values = FormUtil.splitPerSpaceOrNewLine(it)

                    if (values.size >= 2) {
                        onConfirmDelete = false
                        if (!FormUtil.checkTagMinimumCharacter(values[0])) {
                            hashTagError = "标签应至少包含两个字符"
                            hashTagShouldShowError = true
                        } else if (!FormUtil.checkTagMaximumCharacter(values[0])) {
                            hashTagError = "标签长度不应超过50"
                            hashTagShouldShowError = true
                        } else if (hashTagList.contains(values[0])) {
                            hashTagError = "该标签已存在"
                            hashTagShouldShowError = true
                        } else {
                            hashTagShouldShowError = false
                        }

                        if (!hashTagShouldShowError) {
                            viewModel.addContactTag(values[0])
                            hashTagText = ""
                        }
                    } else {
                        hashTagText = it
                    }
                },
                placeHolderWhenEnabled = "自定义标签筛选",
                lazyListState = hashTagLazyListState,
                focusRequester = hashTagFocusRequester,
                textFieldInteraction = hashTagInteraction,
                rowInteraction = rowInteraction,
                errorMessage = hashTagError,
                shouldShowError = hashTagShouldShowError,
                listOfChips = hashTagList,
                selectedListOfChips = viewModel.selectedContactTagStateList,
                innerModifier = Modifier.onKeyEvent {
                    if (it.key.keyCode == Key.Backspace.keyCode && hashTagText.isBlank()) {
                        if (onConfirmDelete) {
                            viewModel.contactTagStateFlow.value.lastOrNull()?.let {
                                viewModel.deleteContactTag(it.value)
                            }
                            onConfirmDelete = false
                        } else {
                            onConfirmDelete = true
                        }
                    }
                    false
                },
                onChipClick = { chipIndex ->
                    if (viewModel.contactTagStateFlow.value.isNotEmpty()) {
                        hashTagList.getOrNull(chipIndex)?.let {
                            viewModel.addOrRemoveItemOfSelectedContactTagStateList(it)
                        }
                    }
                },
                onChipClickWhenEnabled = { chipIndex ->
                    if (viewModel.contactTagStateFlow.value.isNotEmpty()) {
                        hashTagList.getOrNull(chipIndex)?.let {
                            viewModel.deleteContactTag(it)
                        }
                    }
                },
                padding = HashTagEditor.PADDING_SMALL,
                onConfirmDelete = onConfirmDelete
            ) {
                var showDropDownMenu by remember {
                    mutableStateOf(false)
                }
                FilterChip(
                    modifier = Modifier
                        .animateContentSize()
                        .padding(end = 8.dp),
                    selected = viewModel.typeFilter.value !is ContactTypeFilterState.All,
                    onClick = { showDropDownMenu = !showDropDownMenu },
                    enabled = !isEditing,
                    selectedIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )

                    },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = "expand",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                            androidx.compose.material3.DropdownMenu(
                                expanded = showDropDownMenu,
                                onDismissRequest = { showDropDownMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部") },
                                    onClick = {
                                        viewModel.setTypeFilter(
                                            ContactTypeFilterState.All()
                                        )
                                        showDropDownMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("已编组") },
                                    onClick = {
                                        viewModel.setTypeFilter(
                                            ContactTypeFilterState.Grouped()
                                        )
                                        showDropDownMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("未编组") },
                                    onClick = {
                                        viewModel.setTypeFilter(
                                            ContactTypeFilterState.Ungrouped()
                                        )
                                        showDropDownMenu = false
                                    },
                                )
                            }
                        }
                    },
                    label = { Text(text = viewModel.typeFilter.value.label) }
                )
                FilterChip(modifier = Modifier
                    .animateContentSize()
                    .padding(end = 8.dp),
                    selected = viewModel.readFilter.value is ContactReadFilterState.Unread,
                    onClick = {
                        viewModel.setReadFilter(
                            if (viewModel.readFilter.value is ContactReadFilterState.Unread) ContactReadFilterState.All() else ContactReadFilterState.Unread()
                        )
                    },
                    enabled = !isEditing,
                    selectedIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )

                    },
                    label = { Text(text = "未读") })
                FilterChip(modifier = Modifier
                    .animateContentSize(), selected = false, onClick = {
                    viewModel.setTagEditing(!isEditing)
                    hashTagText = ""
                    hashTagError = ""
                    hashTagShouldShowError = false
                    onConfirmDelete = false
                    viewModel.clearSelectedContactTagStateList()
                },
                    label = {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    })
            }
//                    Row(
//                        modifier = Modifier
//                            .padding(vertical = 8.dp)
//                            .horizontalScroll((rememberScrollState()))
//                    ) {
//                        Spacer(modifier = Modifier.width(16.dp))
//                        if (viewModel.contactTagStateFlow.collectAsState().value.isNotEmpty()) {
//                            FilterChip(modifier = Modifier
//                                .padding(end = 8.dp)
//                                .animateContentSize(), selected = false, onClick = { /*TODO*/ },
//                                label = {
//                                    Icon(
//                                        imageVector = Icons.Outlined.Tune,
//                                        contentDescription = "",
//                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
//                                    )
//                                })
//                        }
//                        viewModel.contactTagStateFlow.collectAsState().value.forEach {
//                            FilterChip(
//                                modifier = Modifier
//                                    .padding(end = 8.dp)
//                                    .animateContentSize(),
//                                selected = viewModel.selectedContactTagStateList.contains(it),
//                                onClick = {
//                                    viewModel.addOrRemoveItemOfSelectedContactTagStateList(
//                                        it
//                                    )
//                                },
//                                label = { Text(it.value) },
//                                leadingIcon = {
//                                },
//                                selectedIcon = {
//                                    Icon(
//                                        imageVector = Icons.Outlined.Done,
//                                        contentDescription = "",
//                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
//                                    )
//                                }
//                            )
//                        }
//                    }
        }

        item(key = "main") {
            AnimatedVisibility(
                visible = contactState.data.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp, 8.dp)
                        .animateItemPlacement()
                ) {
                    Text(
                        text = "主要",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (contactState.isLoading) {
            itemsIndexed(
                items = listOf(null, null, null, null, null, null, null, null),
                key = { index, _ -> index }) { index, _ ->
                ContactItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement(),
                    contact = null,
                    topRadius = if (index == 0) 28.dp else 0.dp,
                    bottomRadius = if (index == 7) 28.dp else 0.dp,
                    isLoading = true,
                )
                if (index < 7)
                    Spacer(modifier = Modifier.height(3.dp))
            }
        } else {
            itemsIndexed(
                items = contactState.data,
                key = { _, item -> item.contactId }
            ) { index, item ->
                var loading by remember {
                    mutableStateOf(false)
                }
                val swipeableState = rememberSwipeableState(initialValue = item.isHidden,
                    confirmStateChange = {
                        if (it) {
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar(
                                    message = "会话已暂时隐藏",
                                    actionLabel = "取消",
                                    duration = SnackbarDuration.Short
                                )
                                    .also { result ->
                                        when (result) {
                                            SnackbarResult.ActionPerformed -> {
                                                viewModel.cancelContactHidden()
                                            }
                                            SnackbarResult.Dismissed -> {}
                                            else -> {}
                                        }
                                    }
                            }
                            viewModel.setContactHidden(item.contactId)
                        }
                        true
                    })
                val isFirst = index == 0
                val isLast = index == contactState.data.lastIndex
                val isDragging = swipeableState.offset.value.roundToInt() != 0
                val topRadius by animateDpAsState(targetValue = if (isDragging || isFirst) 28.dp else 0.dp)
                val bgTopRadius by animateDpAsState(targetValue = if (isFirst) 28.dp else 0.dp)
                val bottomRadius by animateDpAsState(targetValue = if (isDragging || isLast) 28.dp else 0.dp)
                val bgBottomRadius by animateDpAsState(targetValue = if (isLast) 28.dp else 0.dp)
                val isSelected =
                    viewModel.selectedContactStateList.map { it.contactId }
                        .contains(item.contactId)
                SwipeableContact(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement()
                        .animateContentSize(),
                    state = swipeableState,
                    topRadius = bgTopRadius,
                    bottomRadius = bgBottomRadius,
                    extraSpace = 16.dp,
                    enabled = viewModel.searchBarActivateState.value == SearchAppBar.NONE,
                ) {
                    ContactItem(
                        contact = item,
                        topRadius = topRadius,
                        bottomRadius = bottomRadius,
                        isTop = item.isPinned,
                        isLoading = loading,
                        isSelected = isSelected,
                        isEditing = item.contactId == mainSharedViewModel.editingContact.value,
                        isExpanded = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                        shimmer = shimmerInstance,
                        onClick = {
                            if (viewModel.searchBarActivateState.value == SearchAppBar.SELECT) {
                                viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                            } else {
                                if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                                    viewModel.clearContactUnreadNum(item.contactId)
                                    mainSharedViewModel.receiveAndUpdateMessageFromContact(
                                        contact = item,
                                        shouldSelect = true
//                                            sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                                    )
                                    if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                        mainNavController.navigate(ChatPageDestination)
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                                viewModel.setSearchBarActivateState(SearchAppBar.SELECT)
                                viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                            }
                        }
                    ) {
                        if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                            viewModel.setSearchBarActivateState(SearchAppBar.SELECT)
                            viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                        }
                    }
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
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        item(key = "archived") {
            if (!viewModel.archivedContactHidden.value && archivedContact.isNotEmpty()) {
                val swipeableState = rememberSwipeableState(initialValue = false,
                    confirmStateChange = {
                        if (it) {
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar(
                                    message = "会话已暂时隐藏",
                                    actionLabel = "取消",
                                    duration = SnackbarDuration.Short
                                )
                                    .also { result ->
                                        when (result) {
                                            SnackbarResult.ActionPerformed -> {
                                                viewModel.showArchiveContact()
                                            }
                                            SnackbarResult.Dismissed -> {}
                                            else -> {}
                                        }
                                    }
                            }
                            coroutineScope.launch {
                                delay(200)
                                viewModel.hideArchiveContact()
                            }
                        }
                        true
                    })
                SwipeableContact(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItemPlacement()
                        .animateContentSize(),
                    state = swipeableState,
                    topRadius = 28.dp, bottomRadius = 28.dp,
                    enabled = viewModel.searchBarActivateState.value == SearchAppBar.NONE
                ) {
                    ContactItem(
                        contact = null,
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = "archived",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = "已归档会话",
                        subTitle = "${archivedContact.firstOrNull()?.profile?.name}: ${archivedContact.firstOrNull()?.latestMessage?.content}",
                        timestamp = archivedContact.firstOrNull()?.latestMessage?.timestamp,
                        unreadMessagesNum = archivedContact.fold(0) { acc, contact ->
                            acc + (contact.latestMessage?.unreadMessagesNum ?: 0)
                        },
                        topRadius = 28.dp,
                        bottomRadius = 28.dp,
                        isTop = false,
                        isLoading = false,
                        isSelected = viewModel.searchBarActivateState.value == SearchAppBar.ARCHIVE_SELECT,
                        isEditing = false,
                        isExpanded = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                        shimmer = shimmerInstance,
                        onClick = {
                            if (viewModel.searchBarActivateState.value == SearchAppBar.ARCHIVE_SELECT) {
                                viewModel.setSearchBarActivateState(SearchAppBar.NONE)
                            }else{
                                viewModel.setSearchBarActivateState(SearchAppBar.ARCHIVE)
                                viewModel.setAreaState(AreaState.ArchiveArea)
                            }
//                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
//                            }
                        },
                        onLongClick = {
                            if (viewModel.searchBarActivateState.value != SearchAppBar.SELECT) {
                                viewModel.setSearchBarActivateState(SearchAppBar.ARCHIVE_SELECT)
                            }
                        }
                    ) {
                        if (viewModel.searchBarActivateState.value != SearchAppBar.SELECT) {
                            viewModel.setSearchBarActivateState(SearchAppBar.ARCHIVE_SELECT)
                        } else if (viewModel.searchBarActivateState.value == SearchAppBar.ARCHIVE_SELECT) {
                            viewModel.setSearchBarActivateState(SearchAppBar.NONE)
                        }
                    }
                }
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

@Composable
fun RowScope.SearchArea(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    viewModel: MessagePageViewModel,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    shimmerInstance: Shimmer,
    mainNavController: NavController
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {

    }
}

@Composable
fun RowScope.ArchiveArea(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    viewModel: MessagePageViewModel,
    archivedContact: List<Contact>,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    shimmerInstance: Shimmer,
    mainNavController: NavController
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = paddingValues
    ) {
        itemsIndexed(
            items = archivedContact,
            key = { _, item -> item.contactId }
        ){ index, item ->
            val isFirst = index == 0
            val isLast = index == archivedContact.lastIndex
            val topRadius by animateDpAsState(targetValue = if (isFirst) 28.dp else 0.dp)
            val bottomRadius by animateDpAsState(targetValue = if (isLast) 28.dp else 0.dp)
            val isSelected =
                viewModel.selectedContactStateList.map { it.contactId }
                    .contains(item.contactId)
            ContactItem(
                contact = item,
                topRadius = topRadius,
                bottomRadius = bottomRadius,
                isTop = false,
                isLoading = false,
                isSelected = isSelected,
                isEditing = item.contactId == mainSharedViewModel.editingContact.value,
                isExpanded = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                shimmer = shimmerInstance,
                onClick = {
                    if (viewModel.searchBarActivateState.value == SearchAppBar.SELECT) {
                        viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                    } else {
                        if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                            viewModel.clearContactUnreadNum(item.contactId)
                            mainSharedViewModel.receiveAndUpdateMessageFromContact(
                                contact = item,
                                shouldSelect = true
//                                            sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                            )
                            if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                mainNavController.navigate(ChatPageDestination)
                            }
                        }
                    }
                },
                onLongClick = {
                    if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                        viewModel.setSearchBarActivateState(SearchAppBar.SELECT)
                        viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                    }
                }
            ) {
                if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                    viewModel.setSearchBarActivateState(SearchAppBar.SELECT)
                    viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                }
            }
        if (index < archivedContact.lastIndex)
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableContact(
    modifier: Modifier = Modifier,
    state: androidx.compose.material.SwipeableState<Boolean>,
    topRadius: Dp,
    bottomRadius: Dp,
    extraSpace: Dp? = 0.dp,
    enabled: Boolean,
    content: @Composable () -> Unit
) = BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
    val extraSpaceInt = with(LocalDensity.current) {
        extraSpace?.toPx() ?: 0f
    }
    val width = constraints.maxWidth.toFloat() + extraSpaceInt
    val anchors = mapOf(0f to false, -width to true)
    val offset = state.offset.value
    val animationProcess = sqrt((-offset * 2 / width).coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .swipeable(
                state = state,
                anchors = anchors,
                thresholds = { _, _ -> androidx.compose.material.FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = enabled
            )
    ) {
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
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .offset(x = 32.dp * (1f - animationProcess))
                    .alpha(animationProcess),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "隐藏该会话",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.HideSource,
                    contentDescription = "not disturb",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Box(
            modifier = Modifier.offset(offset = { IntOffset(x = offset.roundToInt(), y = 0) }),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    modifier: Modifier = Modifier,
    contact: Contact?,
    icon: @Composable() (() -> Unit)? = null,
    title: String? = null,
    subTitle: String? = null,
    timestamp: Long? = null,
    unreadMessagesNum: Int? = null,
    topRadius: Dp,
    bottomRadius: Dp,
//    isFirst: Boolean = false,
//    isLast: Boolean = false,
    isTop: Boolean = false,
    isLoading: Boolean = true,
    isSelected: Boolean = false,
    isEditing: Boolean = false,
    isExpanded: Boolean = false,
    shimmer: Shimmer? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
) {
    val backgroundColor by
    animateColorAsState(
        targetValue = if (isEditing && isExpanded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            if (isTop) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        }
    )
    val textColor by animateColorAsState(
        targetValue = if (isEditing && isExpanded) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            if (isTop) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        }
    )
    androidx.compose.material3.Surface(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = topRadius,
                    topEnd = topRadius,
                    bottomEnd = bottomRadius,
                    bottomStart = bottomRadius
                )
            )
            .fillMaxSize()
            .combinedClickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = LocalIndication.current,
                enabled = true,
                onLongClick = onLongClick,
                onClick = onClick
            ),
        color = backgroundColor,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
//            .background(backgroundColor)
                .padding(16.dp),
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
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onAvatarClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                icon()
                            }
                        } else if (contact?.profile?.avatar == null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .clickable { onAvatarClick() }
                            )
                        } else {
                            Image(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .clickable { onAvatarClick() },
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
                        text = title ?: contact?.profile?.name ?: "会话名称",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
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
                        text = subTitle ?: contact?.latestMessage?.content ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
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
                        text = (timestamp ?: contact?.latestMessage?.timestamp)?.toTimeUntilNow()
                            ?: "",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val unreadMessagesNum =
                        unreadMessagesNum ?: contact?.latestMessage?.unreadMessagesNum ?: 0
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
}