package com.ojhdtapp.parabox.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.ojhdtapp.parabox.ui.destinations.Destination
import com.ojhdtapp.parabox.ui.destinations.FilePageDestination
import com.ojhdtapp.parabox.ui.destinations.MessagePageDestination
import com.ojhdtapp.parabox.ui.destinations.SettingPageDestination
import com.ojhdtapp.parabox.ui.startAppDestination
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ojhdtapp.parabox.ui.NavGraph
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
    val label: String,
) {
    Message(NavGraphs.message, Icons.Outlined.Chat, Icons.Default.Chat, "会话"),
    File(NavGraphs.file, Icons.Outlined.WorkOutline, Icons.Default.Work, "工作"),
    Setting(NavGraphs.setting, Icons.Outlined.Settings, Icons.Default.Settings, "设置")
}

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
                                contentDescription = destination.label
                            )
                        }
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
        Surface(
            modifier = Modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    navController: NavController,
    messageBadge: Int = 0,
    settingBadge: Boolean = false,
    onSelfItemClick: () -> Unit,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            Spacer(modifier = Modifier.statusBarsPadding())
            NavigationDestination.values().forEach { destination ->
                val isCurrentDestOnBackStack =
                    navController.appCurrentDestinationAsState().value in destination.graph.destinations
                NavigationDrawerItem(
                    icon = {
                        BadgedBox(badge = {
                            if (destination.graph == NavGraphs.message && messageBadge != 0)
                                Badge { Text(text = "$messageBadge") }
                            else if (destination.graph == NavGraphs.setting && settingBadge)
                                Badge()
                        }) {
                            Icon(
                                imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                                contentDescription = destination.label
                            )
                        }
                    },
                    label = { Text(text = destination.label) },
                    selected = isCurrentDestOnBackStack,
                    onClick = {
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
                    })
            }
        }) {
        content()
    }
}

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
        modifier = modifier.statusBarsPadding(),
        header = {
            IconButton(onClick = onMenuClick) {
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
                    BadgedBox(badge = {
                        if (destination.graph == NavGraphs.message && messageBadge != 0)
                            Badge { Text(text = "$messageBadge") }
                        else if (destination.graph == NavGraphs.setting && settingBadge)
                            Badge()
                    }) {
                        Icon(
                            imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                            contentDescription = destination.label
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                })
        }
    }
}