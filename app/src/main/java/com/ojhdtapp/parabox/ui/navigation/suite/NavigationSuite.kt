package com.ojhdtapp.parabox.ui.navigation.suite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.ChatPickerDialog
import com.ojhdtapp.parabox.ui.common.ContactPickerDialog
import com.ojhdtapp.parabox.ui.common.DateRangePickerDialog
import com.ojhdtapp.parabox.ui.common.MyDismissibleNavigationDrawer
import com.ojhdtapp.parabox.ui.common.MyDrawerState
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawer
import com.ojhdtapp.parabox.ui.common.rememberMyDrawerState
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.menu.MenuPageEvent
import com.ojhdtapp.parabox.ui.navigation.DefaultMenuComponent
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    rootNavigation: StackNavigation<DefaultRootComponent.RootConfig>,
    rootStackState: ChildStack<*, RootComponent.RootChild>,
    menuNavigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    menuStackState: ChildStack<*, MenuComponent.MenuChild>,
    content: @Composable () -> Unit
) {
    val mainSharedViewModel = hiltViewModel<MainSharedViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val navigationType: MenuNavigationType
    when (currentWindowAdaptiveInfo().windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = MenuNavigationType.BOTTOM_NAVIGATION
        }

        WindowWidthSizeClass.Medium -> {
            navigationType = MenuNavigationType.NAVIGATION_RAIL
        }

        WindowWidthSizeClass.Expanded -> {
            navigationType = if (currentWindowAdaptiveInfo().windowPosture.isTabletop) {
                MenuNavigationType.NAVIGATION_RAIL
            } else {
                MenuNavigationType.PERMANENT_NAVIGATION_DRAWER
            }
        }

        else -> {
            navigationType = MenuNavigationType.BOTTOM_NAVIGATION
        }
    }

    // List
    val listState = rememberLazyListState()
    // Drawer
    val drawerState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val bottomSheetState = rememberModalBottomSheetState()

    // Main State
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()

    val menuEventHandler: (event: MenuPageEvent) -> Unit = {
        when (it) {
            is MenuPageEvent.OnFABClicked -> {

            }

            is MenuPageEvent.OnDrawerItemClicked -> {
                if (it.selfClicked) {

                } else {
                    mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(false))
                }
            }

            is MenuPageEvent.OnMenuClick -> {
                mainSharedViewModel.sendEvent(
                    MainSharedEvent.OpenDrawer(!mainSharedState.openDrawer.open)
                )
            }

            is MenuPageEvent.OnBarItemClicked -> {
                mainSharedViewModel.sendEvent(
                    MainSharedEvent.PageListScrollBy
                )
            }
        }
    }

    // sync drawer state
    LaunchedEffect(drawerState.isAnimationRunning) {
        if (!drawerState.isAnimationRunning) {
            mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(drawerState.isOpen, true))
        }
    }

    // ui Event
    LaunchedEffect(mainSharedState.openDrawer) {
        coroutineScope.launch {
            if (mainSharedState.openDrawer.snap) {
                drawerState.snapTo(if (mainSharedState.openDrawer.open) DrawerValue.Open else DrawerValue.Closed)
            } else {
                if (mainSharedState.openDrawer.open) drawerState.open() else drawerState.close()
            }
        }
    }
    LaunchedEffect(mainSharedState.openBottomSheet) {
        coroutineScope.launch {
            if (mainSharedState.openBottomSheet.snap) {

            } else {
                if (mainSharedState.openBottomSheet.open) bottomSheetState.expand() else bottomSheetState.hide()
            }
        }
    }
    LaunchedEffect(Unit) {
        mainSharedViewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collectLatest {
                when (it) {
                    else -> {}
                }
            }
    }
    LaunchedEffect(Unit) {
        if (navigationType == MenuNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(open = false, snap = false))
        }
        if (navigationType == MenuNavigationType.NAVIGATION_RAIL) {
            mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(open = false, snap = false))
        }
        if (navigationType == MenuNavigationType.BOTTOM_NAVIGATION) {
            mainSharedViewModel.sendEvent(MainSharedEvent.ShowNavigationBar(true))
        }
    }
    BackHandler(mainSharedState.openDrawer.open) {
        mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(false))
    }
    BackHandler(mainSharedState.openBottomSheet.open) {
        mainSharedViewModel.sendEvent(MainSharedEvent.OpenBottomSheet(false))
    }
    if (navigationType == MenuNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        MyDismissibleNavigationDrawer(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            drawerContent = {
                MenuNavigationDrawerContent(
                    navigation = menuNavigation,
                    stackState = menuStackState,
                    rootNavigation = rootNavigation,
                    mainSharedState = mainSharedState,
                    onEvent = menuEventHandler
                )
            }, drawerState = drawerState, drawerWidth = 304.dp) {
            MenuAppContent(
                navigation = menuNavigation,
                stackState = menuStackState,
                navigationType = navigationType,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                mainSharedState = mainSharedState,
                onEvent = menuEventHandler,
                content = content
            )
        }
    } else {
        MyModalNavigationDrawer(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            drawerContent = {
                MenuNavigationDrawerContent(
                    navigation = menuNavigation,
                    stackState = menuStackState,
                    rootNavigation = rootNavigation,
                    mainSharedState = mainSharedState,
                    onEvent = menuEventHandler
                )
            },
            drawerState = drawerState,
            gesturesEnabled = !mainSharedState.search.isActive,
            drawerWidth = 304.dp
        ) {
            MenuAppContent(
                navigation = menuNavigation,
                stackState = menuStackState,
                navigationType = navigationType,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                mainSharedState = mainSharedState,
                onEvent = menuEventHandler,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuAppContent(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>,
    navigationType: MenuNavigationType,
    drawerState: MyDrawerState,
    bottomSheetState: SheetState,
    mainSharedViewModel: MainSharedViewModel,
    mainSharedState: MainSharedState,
    onEvent: (event: MenuPageEvent) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    ChatPickerDialog(
        openDialog = mainSharedState.chatPicker.showDialog,
        data = mainSharedState.chatPicker.result,
        query = mainSharedState.chatPicker.query,
        onQueryChange = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatQueryInput(it))
        },
        loadState = mainSharedState.chatPicker.loadState,
        onConfirm = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(it))
        },
        onDismiss = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(null))
        }
    )
    ContactPickerDialog(openDialog = mainSharedState.contactPicker.showDialog,
        data = mainSharedState.contactPicker.result,
        query = mainSharedState.contactPicker.query,
        onQueryChange = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactQueryInput(it))
        },
        loadState = mainSharedState.contactPicker.loadState,
        onConfirm = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(it))
        },
        onDismiss = {
            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(null))
        }
    )
    DateRangePickerDialog(openDialog = mainSharedState.openDateRangePicker, onConfirm = { start, end ->
        mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(start to end))
    }, onDismiss = {
        mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(null))
    })

    Row(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            modifier = Modifier.zIndex(1f),
            visible = navigationType == MenuNavigationType.NAVIGATION_RAIL,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            MenuNavigationRail(
                modifier = Modifier.zIndex(-1f),
                navigation = navigation,
                stackState = stackState,
                mainSharedState = mainSharedState,
                onEvent = onEvent
            )
        }
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            content()
            androidx.compose.animation.AnimatedVisibility(
                visible = navigationType == MenuNavigationType.BOTTOM_NAVIGATION && mainSharedState.showNavigationBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) { MenuNavigationBar(
                navigation = navigation,
                stackState = stackState,
                    mainSharedState = mainSharedState,
                    onEvent = onEvent,
                )
            }
        }
    }
}