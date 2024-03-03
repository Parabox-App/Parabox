package com.ojhdtapp.parabox.ui.navigation.suite

import com.ojhdtapp.parabox.ui.menu.MenuPageEvent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkOutline
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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.replaceAll
import com.ojhdtapp.parabox.NavGraph
import com.ojhdtapp.parabox.NavGraphs
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.appCurrentDestinationAsState
import com.ojhdtapp.parabox.destinations.SettingPageWrapperUiDestination
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.launch

@Composable
fun MenuNavigationBar(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultRootComponent.Config>,
    stackState: ChildStack<*, RootComponent.Child>,
    mainSharedState: MainSharedState,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavigationBarItem(selected = stackState.active.instance is RootComponent.Child.Message, onClick = {
            navigation.bringToFront(DefaultRootComponent.Config.Message) {
                navigation.replaceAll(DefaultRootComponent.Config.Message)
            }
        }, icon = {
            BadgedBox(badge = {
                if (mainSharedState.datastore.messageBadgeNum > 0)
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(text = "${mainSharedState.datastore.messageBadgeNum}") }
            }) {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = stringResource(id = R.string.conversation)
                )
            }
        },
            label = {
                Text(
                    text = stringResource(id = R.string.conversation),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            alwaysShowLabel = false
        )

        NavigationBarItem(selected = stackState.active.instance is RootComponent.Child.File, onClick = {
            navigation.bringToFront(DefaultRootComponent.Config.File) {
                navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.File)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is RootComponent.Child.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                contentDescription = stringResource(id = R.string.work)
            )
        },
            label = {
                Text(
                    text = stringResource(id = R.string.work),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(selected = stackState.active.instance is RootComponent.Child.Contact, onClick = {
            navigation.bringToFront(DefaultRootComponent.Config.Contact) {
                navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.Contact)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is RootComponent.Child.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                contentDescription = stringResource(id = R.string.contact_person)
            )
        },
            label = {
                Text(
                    text = stringResource(id = R.string.contact_person),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            alwaysShowLabel = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationDrawerContent(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultRootComponent.Config>,
    stackState: ChildStack<*, RootComponent.Child>,
    mainSharedState: MainSharedState,
    onEvent: (event: MenuPageEvent) -> Unit,
) =
    ModalDrawerSheet(
        modifier = modifier
            .width(304.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        val coroutineScope = rememberCoroutineScope()
        Spacer(modifier = Modifier.statusBarsPadding())
        IconButton(
            modifier = Modifier.padding(12.dp),
            onClick = {
                onEvent(MenuPageEvent.OnMenuClick)
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
        NavigationDrawerItem(
            modifier = Modifier
                .padding(12.dp, 0.dp, 12.dp, 6.dp)
                .height(48.dp),
            selected = stackState.active.instance is RootComponent.Child.Message, onClick = {
                navigation.bringToFront(DefaultRootComponent.Config.Message) {
                    navigation.replaceAll(DefaultRootComponent.Config.Message)
                }
                onEvent(MenuPageEvent.OnDrawerItemClicked(false))

            }, icon = {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = stringResource(id = R.string.conversation)
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.conversation),
                )
            },
            badge = {
                if (mainSharedState.datastore.messageBadgeNum > 0)
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = "${mainSharedState.datastore.messageBadgeNum}",
                            modifier = Modifier.padding(4.dp)
                        )
                    }
            }
        )
        NavigationDrawerItem(
            modifier = Modifier
                .padding(12.dp, 0.dp, 12.dp, 6.dp)
                .height(48.dp),
            selected = stackState.active.instance is RootComponent.Child.File,
            onClick = {
                navigation.bringToFront(DefaultRootComponent.Config.File) {
                    navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.File)
                }
                onEvent(MenuPageEvent.OnDrawerItemClicked(false))

            },
            icon = {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                    contentDescription = stringResource(id = R.string.work)
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.work),
                )
            },
        )

        NavigationDrawerItem(
            modifier = Modifier
                .padding(12.dp, 0.dp, 12.dp, 6.dp)
                .height(48.dp),
            selected = stackState.active.instance is RootComponent.Child.Contact,
            onClick = {
                navigation.bringToFront(DefaultRootComponent.Config.Contact) {
                    navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.Contact)
                }
                onEvent(MenuPageEvent.OnDrawerItemClicked(false))

            },
            icon = {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                    contentDescription = stringResource(id = R.string.contact_person)
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.contact_person),
                )
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        NavigationDrawerItem(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp)
                .height(48.dp),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(id = R.string.settings)
                )
            },
            label = {
                Text(
                    text = stringResource(id = R.string.settings)
                )
            },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    onEvent(MenuPageEvent.OnDrawerItemClicked(false))
                    // TODO: navigate to settings
                }
            })
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationRail(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultRootComponent.Config>,
    stackState: ChildStack<*, RootComponent.Child>,
    mainSharedState: MainSharedState,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        header = {
            IconButton(
                modifier = Modifier.statusBarsPadding(),
                onClick = { onEvent(MenuPageEvent.OnMenuClick) }) {
                Icon(imageVector = Icons.Outlined.Menu, contentDescription = "menu")
            }
            FloatingActionButton(
                onClick = { onEvent(MenuPageEvent.OnFABClicked) },
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
        NavigationRailItem(selected = stackState.active.instance is RootComponent.Child.Message, onClick = {
            navigation.bringToFront(DefaultRootComponent.Config.Message) {
                navigation.replaceAll(DefaultRootComponent.Config.Message)
            }
        }, icon = {
            BadgedBox(badge = {
                if (mainSharedState.datastore.messageBadgeNum > 0)
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) { Text(text = "${mainSharedState.datastore.messageBadgeNum.takeIf { it < 99 } ?: "99+"}") }
            }) {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = stringResource(id = R.string.conversation)
                )
            }
        },
            label = null
        )

        NavigationRailItem(selected = stackState.active.instance is RootComponent.Child.File, onClick = {
            navigation.bringToFront(DefaultRootComponent.Config.File) {
                navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.File)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is RootComponent.Child.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                contentDescription = stringResource(id = R.string.work)
            )
        },
            label = null
        )

        NavigationRailItem(
            selected = stackState.active.instance is RootComponent.Child.Contact,
            onClick = {
                navigation.bringToFront(DefaultRootComponent.Config.Contact) {
                    navigation.replaceAll(DefaultRootComponent.Config.Message, DefaultRootComponent.Config.Contact)
                }
            }, icon = {
                Icon(
                    imageVector = if (stackState.active.instance is RootComponent.Child.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                    contentDescription = stringResource(id = R.string.contact_person)
                )
            },
            label = null
        )
    }
}

