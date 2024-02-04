package com.ojhdtapp.parabox.ui.menu

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.suite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ojhdtapp.parabox.NavGraphs
import com.ojhdtapp.parabox.appCurrentDestinationAsState
import com.ojhdtapp.parabox.destinations.MessageAndChatPageWrapperUIDestination
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.ChatPickerDialog
import com.ojhdtapp.parabox.ui.common.ContactPickerDialog
import com.ojhdtapp.parabox.ui.common.DateRangePickerDialog
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.common.MyDismissibleNavigationDrawer
import com.ojhdtapp.parabox.ui.common.MyDrawerState
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawer
import com.ojhdtapp.parabox.ui.common.rememberMyDrawerState
import com.ojhdtapp.parabox.ui.message.MessageAndChatPageWrapperUI
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.NavHostEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.material3.adaptive.navigation.suite.NavigationSuiteScaffoldLayout as NavigationSuiteScaffoldLayout

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveNavigationSuiteApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Destination
@RootNavGraph(start = true)
@Composable
fun MenuPage(
    navigator: DestinationsNavigator,
    navController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
) {
    val navigationType: MenuNavigationType
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = MenuNavigationType.BOTTOM_NAVIGATION
        }

        WindowWidthSizeClass.Medium -> {
            navigationType = MenuNavigationType.NAVIGATION_RAIL
        }

        WindowWidthSizeClass.Expanded -> {
            navigationType = if (devicePosture is DevicePosture.BookPosture) {
                MenuNavigationType.NAVIGATION_RAIL
            } else {
                MenuNavigationType.PERMANENT_NAVIGATION_DRAWER
            }
        }

        else -> {
            navigationType = MenuNavigationType.BOTTOM_NAVIGATION
        }
    }
    MenuNavigationWrapperUI(
        navController = navController,
        navigator = navigator,
        mainSharedViewModel = mainSharedViewModel,
        navigationType = navigationType,
        windowSize = windowSize,
        devicePosture = devicePosture,
    )
    // ** NavigationSuiteScaffold impl **

//    val mainSharedState by mainSharedViewModel.uiState.collectAsState()
//    val navSuiteType =
//        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
//    val menuNavController = rememberNavController()
//    val menuNavHostEngine = rememberAnimatedNavHostEngine(
//        navHostContentAlignment = Alignment.TopCenter,
//        rootDefaultAnimations = RootNavGraphDefaultAnimations(
//            enterTransition = { fadeIn(tween(300)) + slideInVertically { 80 } },
//            exitTransition = { fadeOut() },
//        ),
//        defaultAnimationsForNestedNavGraph = emptyMap()
//    )
//    val appCurrentDestinationState by menuNavController.appCurrentDestinationAsState()
//    NavigationSuiteScaffold(
//        navigationSuiteItems = {
//            MenuNavigationDestination.values().forEach { destination ->
//                val selected = appCurrentDestinationState in destination.graph.destinations
//                item(
//                    selected = selected,
//                    onClick = {
//                        menuNavController.navigate(destination.graph.route) {
//                            popUpTo(NavGraphs.menu.route) {
//                                saveState = true
//                            }
//                            launchSingleTop = true
//                            restoreState = true
//                        }
//                    },
//                    label = {
//                        Text(
//                            text = stringResource(id = destination.labelResId),
//                            style = MaterialTheme.typography.labelLarge
//                        )
//                    },
//                    icon = {
//                            Icon(
//                                imageVector = if (selected) destination.iconSelected else destination.icon,
//                                contentDescription = stringResource(id = destination.labelResId)
//                            )
//                    },
//                    badge = {
//                        if (destination.graph == NavGraphs.message && mainSharedState.datastore.messageBadgeNum != 0)
//                            Badge { Text(text = "${mainSharedState.datastore.messageBadgeNum}") }
//                    },
//                    alwaysShowLabel = false
//                )
//            }
//        },
//        layoutType = navSuiteType,
//    ) {
//        val listState = rememberLazyListState()
//        DestinationsNavHost(
//            modifier = Modifier
//                .fillMaxSize(),
//            navGraph = NavGraphs.menu,
//            engine = menuNavHostEngine,
//            navController = menuNavController,
//            dependenciesContainerBuilder = {
//                dependency(mainSharedViewModel)
//                dependency(listState)
//                dependency(windowSize)
//                dependency(devicePosture)
//            }
//        ) {
//
//        }
//    }
}

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun MenuNavigationWrapperUI(
    navController: NavController,
    navigator: DestinationsNavigator,
    mainSharedViewModel: MainSharedViewModel,
    navigationType: MenuNavigationType,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
) {
    // Destination
    val menuNavController = rememberNavController()
    val menuNavHostEngine = rememberAnimatedNavHostEngine(
        navHostContentAlignment = Alignment.TopCenter,
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = { fadeIn(tween(300)) + slideInVertically { 80 } },
            exitTransition = { fadeOut() },
        ),
        defaultAnimationsForNestedNavGraph = emptyMap()
//        mapOf(
//                    NavGraphs.message to NestedNavGraphDefaultAnimations(
//                        enterTransition = { scaleIn(tween(200), 0.9f) + fadeIn(tween(200)) },
//                        exitTransition = { scaleOut(tween(200), 1.1f) + fadeOut(tween(200)) },
//                        popEnterTransition = {scaleIn(tween(200), 1.1f) + fadeIn(tween(200))},
//                        popExitTransition = {scaleOut(tween(200), 0.9f) + fadeOut(tween(200))}
//                    )
//        )
    )
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackBarHostState = remember { SnackbarHostState() }
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
                coroutineScope.launch {
                    if (!listState.canScrollForward) {
                        listState.animateScrollToItem(0)
                    } else {
                        listState.animateScrollBy(1000f)
                    }
                }
            }
        }
    }

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
        MyDismissibleNavigationDrawer(drawerContent = {
            MenuNavigationDrawerContent(
                navController = menuNavController,
                messageBadge = mainSharedState.datastore.messageBadgeNum,
                onEvent = menuEventHandler
            )
        }, drawerState = drawerState, drawerWidth = 304.dp) {
            MenuAppContent(
                navController = navController,
                menuNavController = menuNavController,
                menuNavHostEngine = menuNavHostEngine,
                navigationType = navigationType,
                listState = listState,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                mainSharedState = mainSharedState,
                windowSize = windowSize,
                devicePosture = devicePosture,
                onEvent = menuEventHandler,
            )
        }
    } else {
        MyModalNavigationDrawer(
            drawerContent = {
                MenuNavigationDrawerContent(
                    navController = menuNavController,
                    messageBadge = mainSharedState.datastore.messageBadgeNum,
                    onEvent = menuEventHandler,
                )
            },
            drawerState = drawerState,
            gesturesEnabled = !mainSharedState.search.isActive,
            drawerWidth = 304.dp
        ) {
            MenuAppContent(
                navController = navController,
                menuNavController = menuNavController,
                menuNavHostEngine = menuNavHostEngine,
                navigationType = navigationType,
                listState = listState,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                mainSharedState = mainSharedState,
                windowSize = windowSize,
                devicePosture = devicePosture,
                onEvent = menuEventHandler
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MenuAppContent(
    navController: NavController,
    menuNavController: NavHostController,
    menuNavHostEngine: NavHostEngine,
    navigationType: MenuNavigationType,
    listState: LazyListState,
    drawerState: MyDrawerState,
    bottomSheetState: SheetState,
    mainSharedViewModel: MainSharedViewModel,
    mainSharedState: MainSharedState,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
    onEvent: (event: MenuPageEvent) -> Unit,
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

    Row(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = navigationType == MenuNavigationType.NAVIGATION_RAIL,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
        ) {
            MenuNavigationRail(
                navController = menuNavController,
                onEvent = onEvent
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.inverseOnSurface),
            contentAlignment = Alignment.BottomCenter
        ) {
            DestinationsNavHost(
                modifier = Modifier
                    .fillMaxSize(),
                navGraph = NavGraphs.menu,
                engine = menuNavHostEngine,
                navController = menuNavController,
                dependenciesContainerBuilder = {
//                        dependency(NavGraphs.message) {
//                            val parentEntry = remember(navBackStackEntry) {
//                                menuNavController.getBackStackEntry(NavGraphs.message.route)
//                            }
//                            hiltViewModel<MessagePageViewModel>(parentEntry)
//                        }
                    dependency(mainSharedViewModel)
                    dependency(listState)
                    dependency(drawerState)
                    dependency(bottomSheetState)
                    dependency(windowSize)
                    dependency(devicePosture)
                }
            ) {
//                composable(MessageAndChatPageWrapperUIDestination){
//                    MessageAndChatPageWrapperUI(
//                        mainNavController = navController,
//                        mainSharedState = mainSharedState,
//                        listState = listState,
//                        navigationType = navigationType,
//                        windowSize = windowSize,
//                        devicePosture = devicePosture,
//                        onMainSharedEvent = mainSharedViewModel::sendEvent
//                    )
//                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = navigationType == MenuNavigationType.BOTTOM_NAVIGATION && mainSharedState.showNavigationBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                MenuNavigationBar(
                    mainSharedState = mainSharedState,
                    navController = menuNavController,
                    onEvent = onEvent,
                )
            }
        }
    }
}