package com.ojhdtapp.parabox.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ojhdtapp.parabox.NavGraphs
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.spec.NavHostEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
    ExperimentalMaterial3Api::class,
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
        mainSharedViewModel = mainSharedViewModel,
        navigationType = navigationType,
        windowSize = windowSize,
        devicePosture = devicePosture,
    )
}

@OptIn(
    ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun MenuNavigationWrapperUI(
    navController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    navigationType: MenuNavigationType,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
) {
    // Destination
    val menuNavController = rememberAnimatedNavController()
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
    // List
    val listState = rememberLazyListState()
    // Drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            }

            is MenuPageEvent.OnMenuClick -> {
                coroutineScope.launch {
                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                }
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

    // ui Event
    LaunchedEffect(Unit) {
        mainSharedViewModel.uiEffect.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED).collectLatest {
            when (it) {
                is MainSharedEffect.ShowSnackBar -> {

                }
                else -> {}
            }
        }
    }
    LaunchedEffect(Unit) {
        if (navigationType == MenuNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            drawerState.snapTo(DrawerValue.Open)
        }
    }
    BackHandler(drawerState.currentValue == DrawerValue.Open) {
        coroutineScope.launch {
            drawerState.close()
        }
    }
    BackHandler(bottomSheetState.currentValue == SheetValue.Expanded) {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }
    if (navigationType == MenuNavigationType.PERMANENT_NAVIGATION_DRAWER) {
        DismissibleNavigationDrawer(drawerContent = {
            MenuNavigationDrawerContent(
                navController = menuNavController,
                messageBadge = mainSharedState.datastore.messageBadgeNum,
                onEvent = menuEventHandler
            )
        }, drawerState = drawerState) {
            MenuAppContent(
                navController = navController,
                menuNavController = menuNavController,
                menuNavHostEngine = menuNavHostEngine,
                mainSharedState = mainSharedState,
                navigationType = navigationType,
                listState = listState,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                windowSize = windowSize,
                devicePosture = devicePosture,
                onEvent = menuEventHandler,
            )
        }
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                MenuNavigationDrawerContent(
                    navController = menuNavController,
                    messageBadge = mainSharedState.datastore.messageBadgeNum,
                    onEvent = menuEventHandler,
                )
            },
            drawerState = drawerState
        ) {
            MenuAppContent(
                navController = navController,
                menuNavController = menuNavController,
                menuNavHostEngine = menuNavHostEngine,
                mainSharedState = mainSharedState,
                navigationType = navigationType,
                listState = listState,
                drawerState = drawerState,
                bottomSheetState = bottomSheetState,
                mainSharedViewModel = mainSharedViewModel,
                windowSize = windowSize,
                devicePosture = devicePosture,
                onEvent = menuEventHandler
            )
        }
    }
//    Column {
//        Row(modifier = Modifier.fillMaxSize()) {
//            if (sizeClass.widthSizeClass == WindowWidthSizeClass.Medium) {
//                MenuNavigationRail(
//                    modifier = Modifier.zIndex(1f),
//                    navController = menuNavController,
//                    messageBadge = messageBadgeNum,
//                    onSelfItemClick = {
//                        coroutineScope.launch {
//                            if (!listState.canScrollForward) {
//                                listState.animateScrollToItem(0)
//                            } else {
//                                listState.animateScrollBy(1000f)
//                            }
//                        }
//                    },
//                    onMenuClick = {
//                        coroutineScope.launch {
//                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
//                        }
//                    },
//                    onFABClick = {
//                        coroutineScope.launch {
//                            bottomSheetState.show()
//                        }
//                    })
//            }
//            DestinationsNavHost(
//                navGraph = NavGraphs.menu,
//                engine = menuNavHostEngine,
//                navController = menuNavController,
//                dependenciesContainerBuilder = {
//                    dependency(mainSharedViewModel)
//                    dependency(drawerState)
//                    dependency(sizeClass)
//                }
//            ) {
//                composable(MessagePageDestination) {
//                    MessagePage(
//                        navigator = destinationsNavigator,
//                        mainNavController = navController,
//                        mainSharedViewModel = mainSharedViewModel,
//                        sizeClass = sizeClass,
//                        listState = listState,
//                        drawerState = drawerState,
//                        bottomSheetState = bottomSheetState,
//                    )
//                }
//                composable(FilePageDestination) {
//                    FilePage(
//                    )
//                }
//            }
//        }
//        if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact
//        ) {
//            MenuNavigationBar(
//                navController = menuNavController,
//                messageBadge = messageBadgeNum,
//                onSelfItemClick = {
//                    coroutineScope.launch {
//                        if (!listState.canScrollForward) {
//                            listState.animateScrollToItem(0)
//                        } else {
//                            listState.animateScrollBy(1000f)
//                        }
//                    }
//                },
//            )
//        }
//    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MenuAppContent(
    navController: NavController,
    menuNavController: NavHostController,
    menuNavHostEngine: NavHostEngine,
    mainSharedState: MainSharedState,
    navigationType: MenuNavigationType,
    listState: LazyListState,
    drawerState: DrawerState,
    bottomSheetState: SheetState,
    mainSharedViewModel: MainSharedViewModel,
    windowSize: WindowSizeClass,
    devicePosture: DevicePosture,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

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
        Column(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.inverseOnSurface),
            verticalArrangement = Arrangement.Bottom
        ) {
            DestinationsNavHost(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
            }

            AnimatedVisibility(
                visible = navigationType == MenuNavigationType.BOTTOM_NAVIGATION,
                enter = slideInVertically(),
                exit = slideOutVertically()
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