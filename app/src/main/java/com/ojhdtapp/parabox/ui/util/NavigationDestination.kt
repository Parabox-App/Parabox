package com.ojhdtapp.parabox.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.appCurrentDestinationAsState
import com.ojhdtapp.parabox.ui.startAppDestination
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.NavGraph
import com.ojhdtapp.parabox.ui.destinations.*
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.utils.isRouteOnBackStack
import kotlinx.coroutines.launch

enum class NavigationDestination(
//    val direction: DirectionDestinationSpec,
    val graph: NavGraph,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val labelResId: Int,
) {
    Message(NavGraphs.message, Icons.Outlined.Chat, Icons.Default.Chat, R.string.conversation),
    File(NavGraphs.file, Icons.Outlined.WorkOutline, Icons.Default.Work, R.string.work),
    Setting(NavGraphs.setting, Icons.Outlined.Settings, Icons.Default.Settings, R.string.settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    messageBadge: Int = 0,
    settingBadge: Boolean = false,
    onSelfItemClick: () -> Unit,
) {
//    val currentDestination: Destination =
//        navController.appCurrentDestinationAsState().value ?: NavGraphs.root.startAppDestination
    Column() {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationDestination.values().forEach { destination ->
                val isCurrentDestOnBackStack =
                    navController.appCurrentDestinationAsState().value in destination.graph.destinations
//                val isCurrentDestOnBackStack = navController.isRouteOnBackStack(destination.graph)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
//                        if (currentDestination == destination.direction) {
//                            onSelfItemClick()
//                        } else {
//                            navController.navigate(destination.direction) {
//                                launchSingleTop = true
//                            }
//                        }
                        if (isCurrentDestOnBackStack) onSelfItemClick()
                        else {
                            navController.navigate(destination.graph) {
                                popUpTo(NavGraphs.menu) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (destination.graph == NavGraphs.message && messageBadge != 0)
                                Badge { Text(text = "$messageBadge") }
                            else if (destination.graph == NavGraphs.setting && settingBadge)
                                Badge()
                        }) {
                            Icon(
                                imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                                contentDescription = stringResource(id = destination.labelResId)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = destination.labelResId),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
//        Surface(
//            modifier = Modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp
//        ) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(
//                        WindowInsets.systemBars
//                            .asPaddingValues()
//                            .calculateBottomPadding()
//                    )
//            )
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    navController: NavController,
    mainNavController: NavController,
    messageBadge: Int = 0,
    settingBadge: Boolean = false,
    onSelfItemClick: () -> Unit,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    sizeClass: WindowSizeClass,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(304.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.statusBarsPadding())
                IconButton(modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 24.dp), onClick = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }) {
                    Icon(imageVector = Icons.Outlined.MenuOpen, contentDescription = "menu_open")
                }
                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                    NavigationDestination.values().forEach { destination ->
                        val isCurrentDestOnBackStack =
                            navController.appCurrentDestinationAsState().value in destination.graph.destinations
                        NavigationDrawerItem(
                            modifier = Modifier
                                .height(48.dp)
                                .padding(horizontal = 12.dp),
                            icon = {
                                Icon(
                                    imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                                    contentDescription = stringResource(id = destination.labelResId)
                                )
//                                BadgedBox(badge = {
//                                    if (destination.graph == NavGraphs.message && messageBadge != 0)
//                                        Badge { Text(text = "$messageBadge") }
//                                    else if (destination.graph == NavGraphs.setting && settingBadge)
//                                        Badge()
//                                }) {
//                                    Icon(
//                                        imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
//                                        contentDescription = destination.label
//                                    )
//                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = destination.labelResId)
                                )
                            },
                            badge = {
                                if (destination.graph == NavGraphs.message && messageBadge != 0)
//                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
//                                        Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
//                                            Text(text = "$messageBadge 条新消息", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
//                                        }
//                                    }
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ) { Text(text = "$messageBadge", modifier = Modifier.padding(4.dp)) }
                                else if (destination.graph == NavGraphs.setting && settingBadge)
                                    Badge()
                            },
                            selected = isCurrentDestOnBackStack,
                            onClick = {
                                if (isCurrentDestOnBackStack) onSelfItemClick()
                                else {
                                    coroutineScope.launch {
                                        navController.navigate(destination.graph) {
                                            popUpTo(NavGraphs.menu) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        drawerState.close()
                                    }
                                }
                            })
                    }
                    Divider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                }
                Box(modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
                    Text(text = stringResource(R.string.connection), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                NavigationDrawerItem(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Extension,
                            contentDescription = "plugin"
                        )
                    },
                    label = {
                            Text(text = stringResource(R.string.extension),)
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            mainNavController.navigate(ExtensionPageDestination)
                        }
                    })
                NavigationDrawerItem(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Cloud,
                            contentDescription = "cloud"
                        )
                    },
                    label = {
                            Text(text = stringResource(R.string.connect_cloud_service),)
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            mainNavController.navigate(CloudPageDestination)
                        }
                    })
                NavigationDrawerItem(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = "fcm"
                        )
                    },
                    label = {
                            Text(text = stringResource(id = R.string.fcm))
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            mainNavController.navigate(FCMPageDestination)
                        }
                    })
                Divider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
                NavigationDrawerItem(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "help and support"
                        )
                    },
                    label = {
                        Text(text = stringResource(id = R.string.support))
                    },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            mainNavController.navigate(SupportPageDestination)
                        }
                    })

            }
        }) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRail(
    modifier: Modifier = Modifier,
    navController: NavController,
    messageBadge: Int = 0,
    settingBadge: Boolean = false,
    onSelfItemClick: () -> Unit,
    onMenuClick: () -> Unit,
    onFABClick: () -> Unit
) {
    NavigationRail(
        modifier = modifier,
        containerColor = NavigationRailDefaults.ContainerColor,
        header = {
            IconButton(modifier = Modifier.statusBarsPadding(), onClick = onMenuClick) {
                Icon(imageVector = Icons.Outlined.Menu, contentDescription = "menu")
            }
            FloatingActionButton(
//                modifier = Modifier.padding(top = 16.dp),
                onClick = onFABClick,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
            }
        }
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        NavigationDestination.values().forEach { destination ->
            val isCurrentDestOnBackStack =
                navController.appCurrentDestinationAsState().value in destination.graph.destinations
            NavigationRailItem(
                selected = isCurrentDestOnBackStack, onClick = {
                    if (isCurrentDestOnBackStack) onSelfItemClick()
                    else {
                        navController.navigate(destination.graph) {
                            popUpTo(NavGraphs.menu) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                        if (destination.graph == NavGraphs.message && messageBadge != 0)
                            Badge { Text(text = "${if(messageBadge > 99) "99+" else messageBadge}") }
                        else if (destination.graph == NavGraphs.setting && settingBadge)
                            Badge()
                    }) {
                        Icon(
                            imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                            contentDescription = stringResource(id = destination.labelResId)
                        )
                    }
                },
//                label = {
//                    Text(
//                        text = destination.label,
//                        style = MaterialTheme.typography.labelLarge
//                    )
//                },
            label = null,
            )
        }
    }
}