package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DismissibleBottomSheet
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawerReverse
import com.ojhdtapp.parabox.ui.common.rememberMyDrawerState
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    state: MessagePageState,
    mainNavController: NavController,
    mainSharedState: MainSharedState,
    layoutType: MessageLayoutType,
    windowSize: WindowSizeClass,
    onEvent: (MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
    ) {
    Crossfade(targetState = state.chatDetail.chat == null, label = "chat_empty_normal_crossfade") {
        if (it) {
            EmptyChatPage(
                modifier = modifier,
            )
        } else {
            NormalChatPage(
                modifier = modifier,
                state = state,
                sharedState = mainSharedState,
                mainNavController = mainNavController,
                layoutType = layoutType,
                windowSize = windowSize,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent,
            )
        }

    }
}

@Composable
fun NormalChatPage(
    modifier: Modifier = Modifier,
    state: MessagePageState,
    sharedState: MainSharedState,
    mainNavController: NavController,
    layoutType: MessageLayoutType,
    windowSize: WindowSizeClass,
    onEvent: (e: MessagePageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val drawerState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val lazyListState = rememberLazyListState()
    val fabExtended by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 2
        }
    }
    LaunchedEffect(state.chatDetail.editAreaState.expanded) {
        if (state.chatDetail.editAreaState.expanded) {
            sheetState.open()
        } else {
            sheetState.close()
        }
    }
    MyModalNavigationDrawerReverse(
        drawerContent = {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .background(Color.Green)
            )
        },
        gesturesEnabled = state.chatDetail.chat != null
                && state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording,
        drawerState = drawerState,
        drawerWidth = 360.dp,
    ) {
        DismissibleBottomSheet(
            sheetContent = {
                Toolbar(modifier = Modifier.height(160.dp), state = state.chatDetail.editAreaState, onEvent = onEvent)
            },
            gesturesEnabled = state.chatDetail.editAreaState.audioRecorderState !is AudioRecorderState.Recording,
            sheetHeight = 160.dp, sheetState = sheetState
        ) {
            Scaffold(
                topBar = {},
            ) { paddingValues ->
                val bottomPadding = animateDpAsState(
                    targetValue = if (sheetState.isOpen) 0.dp else paddingValues.calculateBottomPadding(),
                    label = "edit_area_bottom_padding"
                )
                Column() {
                    val messageLazyPagingItems = state.messagePagingDataFlow.collectAsLazyPagingItems()
                    MyImagePreviewer(
                        messageLazyPagingItems = messageLazyPagingItems,
                        state = state.chatDetail.imagePreviewerState,
                        onEvent = onEvent
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {

                    }
                    EditArea(
                        modifier = Modifier
                            .padding(bottom = bottomPadding.value)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                        state = state.chatDetail.editAreaState,
                        onEvent = onEvent
                    )
                }
            }
        }

    }
}