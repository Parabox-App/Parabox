package com.ojhdtapp.parabox.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.NavGraphs
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.appCurrentDestinationAsState
import kotlinx.coroutines.launch

enum class MenuNavigationDestination(
//    val direction: DirectionDestinationSpec,
    val graph: com.ojhdtapp.parabox.NavGraph,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val labelResId: Int,
) {
    Message(NavGraphs.message, Icons.Outlined.Chat, Icons.Default.Chat, R.string.conversation),
    File(NavGraphs.file, Icons.Outlined.WorkOutline, Icons.Default.Work, R.string.work),
    Contact(NavGraphs.contact, Icons.Outlined.Contacts, Icons.Default.Contacts, R.string.contact_person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuPageUiState: MenuPageUiState,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
//    val currentDestination: Destination =
//        navController.appCurrentDestinationAsState().value ?: NavGraphs.root.startAppDestination
    Column {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            MenuNavigationDestination.values().forEach { destination ->
                val isCurrentDestOnBackStack =
                    navController.appCurrentDestinationAsState().value in destination.graph.destinations
//                val isCurrentDestOnBackStack = navController.isRouteOnBackStack(destination.graph)
                NavigationBarItem(
                    selected = isCurrentDestOnBackStack,
                    onClick = {
                        if (isCurrentDestOnBackStack) onEvent(MenuPageEvent.onBarItemClicked)
                        else {
                            navController.navigate(destination.graph.route) {
                                popUpTo(NavGraphs.menu.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (destination.graph == NavGraphs.message && menuPageUiState.messageBadgeNum != 0)
                                Badge { Text(text = "${menuPageUiState.messageBadgeNum}") }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationDrawerContent(
    navController: NavController,
    messageBadge: Int = 0,
    onEvent: (event: MenuPageEvent) -> Unit,
) =
    ModalDrawerSheet(
        modifier = Modifier
            .width(304.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        val coroutineScope = rememberCoroutineScope()
        Spacer(modifier = Modifier.statusBarsPadding())
        IconButton(
            modifier = Modifier.padding(12.dp),
            onClick = {
                onEvent(MenuPageEvent.OnDrawerClose)
            }
        ) {
            Icon(imageVector = Icons.Outlined.MenuOpen, contentDescription = "menu_open")
        }
        ExtendedFloatingActionButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            onClick = { onEvent(MenuPageEvent.OnFABClicked) },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.new_contact),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        }
        Spacer(modifier = Modifier.height(32.dp))
        MenuNavigationDestination.values().forEach { destination ->
            val isCurrentDestOnBackStack =
                navController.appCurrentDestinationAsState().value in destination.graph.destinations
            NavigationDrawerItem(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp)
                    .height(48.dp),
                icon = {
                    Icon(
                        imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                        contentDescription = stringResource(id = destination.labelResId)
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = destination.labelResId)
                    )
                },
                badge = {
                    if (destination.graph == NavGraphs.message && messageBadge != 0)
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { Text(text = "$messageBadge", modifier = Modifier.padding(4.dp)) }
                },
                selected = isCurrentDestOnBackStack,
                onClick = {
                    if (isCurrentDestOnBackStack) onEvent(MenuPageEvent.OnDrawerItemClicked(true))
                    else {
                        coroutineScope.launch {
                            navController.navigate(destination.graph.route) {
                                popUpTo(NavGraphs.menu.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            onEvent(MenuPageEvent.OnDrawerItemClicked(false))
                        }
                    }
                })
        }
//                NavigationDrawerItem(
//                    modifier = Modifier
//                        .height(48.dp)
//                        .padding(horizontal = 12.dp),
//                    icon = {
//                        Icon(
//                            imageVector = Icons.Outlined.Extension,
//                            contentDescription = "plugin"
//                        )
//                    },
//                    label = {
//                            Text(text = stringResource(R.string.extension),)
//                    },
//                    selected = false,
//                    onClick = {
//                        coroutineScope.launch {
//                            drawerState.close()
//                            mainNavController.navigate(ExtensionPageDestination)
//                        }
//                    })

    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationRail(
    modifier: Modifier = Modifier,
    navController: NavController,
    messageBadge: Int = 0,
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
        Spacer(modifier = Modifier.height(32.dp))
        MenuNavigationDestination.values().forEach { destination ->
            val isCurrentDestOnBackStack =
                navController.appCurrentDestinationAsState().value in destination.graph.destinations
            NavigationRailItem(
                selected = isCurrentDestOnBackStack,
                onClick = {
                    if (isCurrentDestOnBackStack) onSelfItemClick()
                    else {
//                        navController.navigate(destination.graph) {
//                            popUpTo(NavGraphs.menu) {
//                                saveState = true
//                            }
//                            launchSingleTop = true
//                            restoreState = true
//                        }
                    }
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (destination.graph == NavGraphs.message && messageBadge != 0)
                                Badge { Text(text = "${if (messageBadge > 99) "99+" else messageBadge}") }
                        }) {
                        Icon(
                            imageVector = if (isCurrentDestOnBackStack) destination.iconSelected else destination.icon,
                            contentDescription = stringResource(id = destination.labelResId)
                        )
                    }
                },
                label = null,
            )
        }
    }
}