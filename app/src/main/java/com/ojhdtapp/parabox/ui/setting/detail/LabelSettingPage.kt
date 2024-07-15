package com.ojhdtapp.parabox.ui.setting.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pushNew
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.drag_drop.DraggableItem
import com.ojhdtapp.parabox.ui.common.drag_drop.rememberDragDropState
import com.ojhdtapp.parabox.ui.message.NewChatFilterDialog
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LabelSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler(enabled = layoutType != LayoutType.SPLIT) {
        scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
    }
    if (layoutType == LayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = "标签",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Content(
                    modifier = Modifier.weight(1f),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    navigation = navigation,
                    stackState = stackState,
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent,
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "标签",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest) }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                state = state,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                navigation = navigation,
                stackState = stackState,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalDecomposeApi::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val context = LocalContext.current
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dragDropState = rememberDragDropState(
        listState,
        1,
        mainSharedState.datastore.enabledChatFilterList.size
    ) { fromIndex, toIndex ->
        onMainSharedEvent(MainSharedEvent.OnChatFilterListReordered(fromIndex, toIndex))
    }
    var openCustomTagFilterDialog by remember {
        mutableStateOf(false)
    }
    val suggestChatLabelList by remember(mainSharedState.datastore.enabledChatFilterList) {
        derivedStateOf {
            ChatFilter.allFilterList.filter { it !in mainSharedState.datastore.enabledChatFilterList }
        }
    }
    NewChatFilterDialog(
        openDialog = openCustomTagFilterDialog,
        onConfirm = {
            onMainSharedEvent(MainSharedEvent.OnChatFilterAdded(ChatFilter.Tag(it.trim())))
            openCustomTagFilterDialog = false
        },
        onDismiss = { openCustomTagFilterDialog = false }
    )
    LazyColumn(
        modifier = modifier
            .pointerInput(dragDropState) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, offset ->
                        change.consume()
                        dragDropState.onDrag(offset = offset)

                        if (overscrollJob?.isActive == true)
                            return@detectDragGesturesAfterLongPress

                        dragDropState
                            .checkForOverScroll()
                            .takeIf { it != 0f }
                            ?.let {
                                overscrollJob =
                                    scope.launch {
                                        dragDropState.state.animateScrollBy(
                                            it, tween(easing = FastOutLinearInEasing)
                                        )
                                    }
                            }
                            ?: run { overscrollJob?.cancel() }
                    },
                    onDragStart = { offset -> dragDropState.onDragStart(offset) },
                    onDragEnd = {
                        dragDropState.onDragInterrupted()
                        overscrollJob?.cancel()
                    },
                    onDragCancel = {
                        dragDropState.onDragInterrupted()
                        overscrollJob?.cancel()
                    }
                )
            },
        state = listState
    ) {
        item {
            SettingHeader(
                text = "已添加",
            )
        }
        itemsIndexed(items = mainSharedState.datastore.enabledChatFilterList) { index: Int, item: ChatFilter ->
            DraggableItem(
                dragDropState = dragDropState,
                index = index + 1
            ) { isDragging ->
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
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                onMainSharedEvent(MainSharedEvent.OnChatFilterRemoved(item))
                                isMenuVisible = false
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete label")
                            }
                        )
                    }
                    SettingItem(
                        title = item.label ?: stringResource(id = item.labelResId),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = "drag",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                isMenuVisible = true
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "more",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        selected = isDragging,
                        layoutType = layoutType,
                        clickableOnly = true
                    ) {
                        if (item is ChatFilter.Tag) {
                            onEvent(SettingPageEvent.UpdateSelectedTagLabel(item))
                            navigation.pushNew(DefaultSettingComponent.SettingConfig.LabelDetailSetting)
                        }
                    }
                }
            }
        }
        item {
            SettingItem(
                title = "新增标签", selected = false, layoutType = layoutType,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.NewLabel,
                        contentDescription = "add label",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            ) {
                openCustomTagFilterDialog = true
            }
        }
        if (suggestChatLabelList.isNotEmpty()) {
            item {
                SettingHeader(
                    text = "建议添加",
                )
            }
        }
        items(suggestChatLabelList) {
            SettingItem(
                title = stringResource(id = it.labelResId),
                subTitle = stringResource(id = it.descriptionResId),
                selected = false,
                layoutType = layoutType
            ) {
                onMainSharedEvent(MainSharedEvent.OnChatFilterAdded(it))
            }
        }
    }
}
