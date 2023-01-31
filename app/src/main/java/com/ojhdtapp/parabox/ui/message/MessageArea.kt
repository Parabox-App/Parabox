package com.ojhdtapp.parabox.ui.message

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.navigation.navigate
import com.valentinilk.shimmer.Shimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    messageState: MessageState,
    contactState: ContactState,
    archivedContact: List<Contact>,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    shimmerInstance: Shimmer,
    mainNavController: NavController,
    onEvent: (event: ActivityEvent) -> Unit
) {
    // If you'd like to customize either the snap behavior or the layout provider
//    val snappingLayout = remember(listState) { SnapLayoutInfoProvider(listState) }
//    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    val context = LocalContext.current
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = mainSharedViewModel.isRefreshing.value),
        onRefresh = {
            onEvent(ActivityEvent.RefreshMessage)
        },
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                modifier = Modifier.offset(y = paddingValues.calculateTopPadding()),
                state = state, refreshTriggerDistance = trigger,
                scale = true,
                contentColor = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            contentPadding = paddingValues,
//            flingBehavior = flingBehavior
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
                        val values = FormUtil.splitTwoSpacesOrNewLine(it)

                        if (values.size >= 2) {
                            onConfirmDelete = false
                            if (!FormUtil.checkTagMinimumCharacter(values[0])) {
                                hashTagError = context.getString(R.string.hash_tag_error_too_short)
                                hashTagShouldShowError = true
                            } else if (!FormUtil.checkTagMaximumCharacter(values[0])) {
                                hashTagError = context.getString(R.string.hash_tag_error_too_long)
                                hashTagShouldShowError = true
                            } else if (hashTagList.contains(values[0])) {
                                hashTagError = context.getString(R.string.hash_tag_error_duplicate)
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
                    placeHolderWhenEnabled = stringResource(R.string.tag_des),
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
                    onConfirmDelete = onConfirmDelete,
                    chipContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    var showDropDownMenu by remember {
                        mutableStateOf(false)
                    }
                    MyFilterChip(
                        modifier = Modifier.padding(end = 8.dp),
                        selected = viewModel.typeFilter.value !is ContactTypeFilterState.All,
                        label = {
                            Text(text = stringResource(id = viewModel.typeFilter.value.labelResId))
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .wrapContentSize(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDropDown,
                                    contentDescription = "expand",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                                RoundedCornerDropdownMenu(
                                    expanded = showDropDownMenu,
                                    onDismissRequest = { showDropDownMenu = false },
                                    //                                modifier = Modifier.width(192.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.all)) },
                                        onClick = {
                                            viewModel.setTypeFilter(
                                                ContactTypeFilterState.All()
                                            )
                                            showDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.grouped)) },
                                        onClick = {
                                            viewModel.setTypeFilter(
                                                ContactTypeFilterState.Grouped()
                                            )
                                            showDropDownMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ungrouped)) },
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
                        enabled = !isEditing,
                        withoutLeadingIcon = true,
                    ) {
                        showDropDownMenu = !showDropDownMenu
                    }
                    MyFilterChip(
                        modifier = Modifier
                            .padding(end = 8.dp),
                        selected = viewModel.readFilter.value is ContactReadFilterState.Unread,
                        label = { Text(text = stringResource(R.string.unread)) },
                        enabled = !isEditing,
                    ) {
                        viewModel.setReadFilter(
                            if (viewModel.readFilter.value is ContactReadFilterState.Unread) ContactReadFilterState.All() else ContactReadFilterState.Unread()
                        )
                    }
                    MyFilterChip(
                        selected = isEditing,
                        label = {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        withoutLeadingIcon = true,
                    ) {
                        viewModel.setTagEditing(!isEditing)
                        hashTagText = ""
                        hashTagError = ""
                        hashTagShouldShowError = false
                        onConfirmDelete = false
                        viewModel.clearSelectedContactTagStateList()
                    }
                }
            }

            item(key = "main") {
                AnimatedVisibility(
                    visible = contactState.data.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .animateItemPlacement()
                    ) {
                        Text(
                            text = stringResource(R.string.main),
                            style = MaterialTheme.typography.labelLarge,
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
                                        message = context.getString(R.string.contact_hidden),
                                        actionLabel = context.getString(R.string.cancel),
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
                    // Smaller radius to hide lighting Corner
                    val topRadius by animateDpAsState(targetValue = if (isDragging || isFirst) 26.dp else 0.dp)
                    val bgTopRadius by animateDpAsState(targetValue = if (isFirst) 28.dp else 0.dp)
                    val bottomRadius by animateDpAsState(targetValue = if (isDragging || isLast) 26.dp else 0.dp)
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
                        onVibrate = { onEvent(ActivityEvent.Vibrate) }
                    ) {
                        ContactItem(
                            contact = item,
                            topRadius = topRadius,
                            bottomRadius = bottomRadius,
                            isTop = item.isPinned,
                            isLoading = loading,
                            isSelected = isSelected,
                            isEditing = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact && item.contactId == messageState.contact?.contactId,
                            isExpanded = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                            shimmer = shimmerInstance,
                            username = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
                            onClick = {
                                if (viewModel.searchBarActivateState.value == SearchAppBar.SELECT) {
                                    viewModel.addOrRemoveItemOfSelectedContactStateList(item)
                                } else {
                                    if (viewModel.searchBarActivateState.value != SearchAppBar.ARCHIVE_SELECT) {
                                        viewModel.clearContactUnreadNum(item.contactId)
                                        mainSharedViewModel.loadMessageFromContact(item)
                                        if (sizeClass.widthSizeClass != WindowWidthSizeClass.Expanded) {
                                            mainNavController.navigate(ChatPageDestination())
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
                AnimatedVisibility(
                    visible = !viewModel.archivedContactHidden.value && archivedContact.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .animateItemPlacement()
                    ) {
                        Text(
                            text = stringResource(R.string.other),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item(key = "archived") {
                if (!viewModel.archivedContactHidden.value && archivedContact.isNotEmpty()) {
                    val swipeableState = rememberSwipeableState(initialValue = false,
                        confirmStateChange = {
                            if (it) {
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar(
                                        message = context.getString(R.string.contact_hidden),
                                        actionLabel = context.getString(R.string.cancel),
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
                        enabled = viewModel.searchBarActivateState.value == SearchAppBar.NONE,
                        onVibrate = { onEvent(ActivityEvent.Vibrate) },
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
                            title = stringResource(R.string.archived_contact),
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
                            username = mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value,
                            onClick = {
                                if (viewModel.searchBarActivateState.value == SearchAppBar.ARCHIVE_SELECT) {
                                    viewModel.setSearchBarActivateState(SearchAppBar.NONE)
                                } else {
                                    if (viewModel.searchBarActivateState.value == SearchAppBar.NONE) {
                                        viewModel.setSearchBarActivateState(SearchAppBar.ARCHIVE)
                                        viewModel.setAreaState(AreaState.ArchiveArea)
                                    }
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
            item {
                if (contactState.data.isEmpty() && (viewModel.archivedContactHidden.value || archivedContact.isEmpty())) {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val imageLoader = ImageLoader.Builder(context)
                            .components {
                                add(SvgDecoder.Factory())
                            }
                            .build()
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.drawable.empty_dynamic else R.drawable.empty)
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .width(192.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.contact_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            //        item(key = null) {
            //            androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun() }) {
            //                Text(text = "btn1")
            //            }
            //        }
            //        item(key = null) {
            //            androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun2() }) {
            //                Text(text = "btn2")
            //            }
            //        }
            //        item(key = null) {
            //            androidx.compose.material3.OutlinedButton(onClick = { viewModel.testFun3() }) {
            //                Text(text = "btn3")
            //            }
            //        }
        }
    }
}