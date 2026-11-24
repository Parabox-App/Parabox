package com.ojhdtapp.parabox.ui.navigation.suite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.window.core.layout.WindowSizeClass
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.menu.MenuPageEvent
import com.ojhdtapp.parabox.ui.navigation.MenuTab
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Unified adaptive navigation shell using Material 3 Drawers (with native built-in predictive back),
 * NavigationRail, and a floating bottom bar driven by [currentWindowAdaptiveInfo] and Navigation 3 state.
 *
 * Note: Intentionally registers NO manual BackHandler so Material 3's ModalDrawerSheet /
 * DismissibleDrawerSheet predictive back animations run uninterrupted.
 */
@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    currentTab: MenuTab,
    isAtTopLevel: Boolean,
    onSelectTab: (MenuTab) -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val navigationType = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
            if (adaptiveInfo.windowPosture.isTabletop) {
                MenuNavigationType.NAVIGATION_RAIL
            } else {
                MenuNavigationType.PERMANENT_NAVIGATION_DRAWER
            }
        }
        windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
            MenuNavigationType.NAVIGATION_RAIL
        }
        else -> MenuNavigationType.BOTTOM_NAVIGATION
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val mainSharedState by mainSharedViewModel.uiState.collectAsState()

    val showBottomBar = navigationType == MenuNavigationType.BOTTOM_NAVIGATION &&
        isAtTopLevel &&
        !mainSharedState.search.isActive

    var bottomBarCollapsed by rememberSaveable { mutableStateOf(false) }
    val collapseThreshold = with(LocalDensity.current) { 24.dp.toPx() }
    val bottomBarScrollConnection = if (showBottomBar && mainSharedState.datastore.useFloatingNavigation) {
        remember(currentTab, collapseThreshold) {
            val scroll = BottomBarScroll()
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput && abs(consumed.y) > abs(consumed.x)) {
                        scroll.onScroll(consumed.y, collapseThreshold, bottomBarCollapsed)?.let {
                            bottomBarCollapsed = it
                        }
                    }
                    return Offset.Zero
                }
            }
        }
    } else {
        null
    }

    // One-way sync: when DrawerState settles (e.g. via swipe or native predictive back), reflect to shared state.
    LaunchedEffect(drawerState.currentValue) {
        if (mainSharedState.openDrawer.open != drawerState.isOpen) {
            mainSharedViewModel.sendEvent(MainSharedEvent.OpenDrawer(drawerState.isOpen, snap = true))
        }
    }

    // Respond to programmatic drawer open/close events (e.g. hamburger button in SearchBar).
    LaunchedEffect(mainSharedState.openDrawer) {
        val targetOpen = mainSharedState.openDrawer.open
        if (targetOpen != drawerState.isOpen) {
            if (mainSharedState.openDrawer.snap) {
                drawerState.snapTo(if (targetOpen) DrawerValue.Open else DrawerValue.Closed)
            } else {
                if (targetOpen) drawerState.open() else drawerState.close()
            }
        }
    }

    val menuEventHandler: (event: MenuPageEvent) -> Unit = { event ->
        when (event) {
            is MenuPageEvent.OnFABClicked -> Unit
            is MenuPageEvent.OnDrawerItemClicked -> {
                if (!event.selfClicked) {
                    coroutineScope.launch { drawerState.close() }
                }
            }
            is MenuPageEvent.OnMenuClick -> {
                coroutineScope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
            }
            is MenuPageEvent.OnBarItemClicked -> {
                if (event.selfClicked) {
                    mainSharedViewModel.sendEvent(MainSharedEvent.PageListScrollBy)
                }
            }
        }
    }

    if (navigationType == MenuNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        DismissibleNavigationDrawer(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            drawerState = drawerState,
            drawerContent = {
                DismissibleDrawerSheet(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    drawerState = drawerState,
                    windowInsets = DrawerDefaults.windowInsets.union(
                        WindowInsets.displayCutout.only(WindowInsetsSides.Start),
                    ),
                ) {
                    MenuNavigationDrawerContent(
                        currentTab = currentTab,
                        mainSharedState = mainSharedState,
                        navigationType = navigationType,
                        onSelectTab = onSelectTab,
                        onOpenSettings = {
                            coroutineScope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                        onEvent = menuEventHandler,
                    )
                }
            },
        ) {
            MenuAppContent(
                currentTab = currentTab,
                navigationType = navigationType,
                showBottomBar = showBottomBar,
                bottomBarCollapsed = bottomBarCollapsed,
                bottomBarScrollConnection = bottomBarScrollConnection,
                drawerState = drawerState,
                mainSharedState = mainSharedState,
                onSelectTab = onSelectTab,
                onEvent = menuEventHandler,
                content = content,
            )
        }
    } else {
        ModalNavigationDrawer(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            drawerState = drawerState,
            gesturesEnabled = !mainSharedState.search.isActive && isAtTopLevel,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(304.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    drawerState = drawerState,
                    windowInsets = DrawerDefaults.windowInsets.union(
                        WindowInsets.displayCutout.only(WindowInsetsSides.Start),
                    ),
                ) {
                    MenuNavigationDrawerContent(
                        currentTab = currentTab,
                        mainSharedState = mainSharedState,
                        navigationType = navigationType,
                        onSelectTab = onSelectTab,
                        onOpenSettings = {
                            coroutineScope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                        onEvent = menuEventHandler,
                    )
                }
            },
        ) {
            MenuAppContent(
                currentTab = currentTab,
                navigationType = navigationType,
                showBottomBar = showBottomBar,
                bottomBarCollapsed = bottomBarCollapsed,
                bottomBarScrollConnection = bottomBarScrollConnection,
                drawerState = drawerState,
                mainSharedState = mainSharedState,
                onSelectTab = onSelectTab,
                onEvent = menuEventHandler,
                content = content,
            )
        }
    }
}

@Composable
fun MenuAppContent(
    modifier: Modifier = Modifier,
    currentTab: MenuTab,
    navigationType: MenuNavigationType,
    showBottomBar: Boolean,
    bottomBarCollapsed: Boolean,
    bottomBarScrollConnection: NestedScrollConnection?,
    drawerState: DrawerState,
    mainSharedState: MainSharedState,
    onSelectTab: (MenuTab) -> Unit,
    onEvent: (event: MenuPageEvent) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            modifier = Modifier.zIndex(1f),
            visible = navigationType == MenuNavigationType.NAVIGATION_RAIL,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            MenuNavigationRail(
                currentTab = currentTab,
                mainSharedState = mainSharedState,
                onSelectTab = onSelectTab,
                onEvent = onEvent,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .let {
                    if (navigationType == MenuNavigationType.NAVIGATION_RAIL) {
                        it.consumeWindowInsets(
                            WindowInsets.systemBars
                                .union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Start),
                        )
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val useFloatingNavigation = mainSharedState.datastore.useFloatingNavigation
            val bottomBarClearance = when {
                !showBottomBar -> 0.dp
                useFloatingNavigation -> floatingBottomBarClearance()
                else -> ClassicBottomBarHeight
            }
            CompositionLocalProvider(LocalBottomBarClearance provides bottomBarClearance) {
                Box(
                    modifier = bottomBarScrollConnection?.let { Modifier.nestedScroll(it) }
                        ?: Modifier,
                ) {
                    content()
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                val unreadCount = mainSharedState.datastore.messageBadgeNum
                if (useFloatingNavigation) {
                    FloatingMenuNavigationBar(
                        currentTab = currentTab,
                        unreadCount = unreadCount,
                        collapsed = bottomBarCollapsed,
                        enabled = showBottomBar,
                        onSelectTab = onSelectTab,
                        onEvent = onEvent,
                    )
                } else {
                    ClassicMenuNavigationBar(
                        currentTab = currentTab,
                        unreadCount = unreadCount,
                        enabled = showBottomBar,
                        onSelectTab = onSelectTab,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}
