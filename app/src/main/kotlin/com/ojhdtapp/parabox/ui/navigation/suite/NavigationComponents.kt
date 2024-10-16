package com.ojhdtapp.parabox.ui.navigation.suite

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.menu.MenuPageEvent
import com.ojhdtapp.parabox.ui.navigation.DefaultMenuComponent
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import kotlinx.coroutines.launch

@Composable
fun MenuNavigationBar(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>,
    mainSharedState: MainSharedState,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        NavigationBarItem(selected = stackState.active.instance is MenuComponent.MenuChild.Message, onClick = {
            onEvent(MenuPageEvent.OnBarItemClicked(stackState.active.instance is MenuComponent.MenuChild.Message))
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.Message) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.Message)
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
                    imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
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

        NavigationBarItem(selected = stackState.active.instance is MenuComponent.MenuChild.File, onClick = {
            onEvent(MenuPageEvent.OnBarItemClicked(stackState.active.instance is MenuComponent.MenuChild.File))
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.File) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.File)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
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
        NavigationBarItem(selected = stackState.active.instance is MenuComponent.MenuChild.Contact, onClick = {
            onEvent(MenuPageEvent.OnBarItemClicked(stackState.active.instance is MenuComponent.MenuChild.Contact))
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.Contact) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.Contact)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalDecomposeApi::class)
@Composable
fun MenuNavigationDrawerContent(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    rootNavigation: StackNavigation<DefaultRootComponent.RootConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>,
    mainSharedState: MainSharedState,
    navigationType: MenuNavigationType,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Spacer(modifier = Modifier.statusBarsPadding())
    IconButton(
        modifier = Modifier.padding(12.dp),
        onClick = {
            onEvent(MenuPageEvent.OnMenuClick)
        }
    ) {
        Icon(imageVector = Icons.AutoMirrored.Outlined.MenuOpen, contentDescription = "menu_open")
    }
    if (navigationType == MenuNavigationType.BOTTOM_NAVIGATION) {
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
    } else {
        ExtendedFloatingActionButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = {
                Text(
                    text = stringResource(id = R.string.new_contact),
                )
            },
            icon = {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
            },
            onClick = {
                onEvent(MenuPageEvent.OnFABClicked)
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
    NavigationDrawerItem(
        modifier = Modifier
            .padding(12.dp, 0.dp, 12.dp, 6.dp)
            .height(48.dp),
        selected = stackState.active.instance is MenuComponent.MenuChild.Message, onClick = {
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.Message) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.Message)
            }
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))

        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
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
        selected = stackState.active.instance is MenuComponent.MenuChild.File,
        onClick = {
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.File) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.File)
            }
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))

        },
        icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
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
        selected = stackState.active.instance is MenuComponent.MenuChild.Contact,
        onClick = {
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.Contact) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.Contact)
            }
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))

        },
        icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
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
                rootNavigation.pushNew(DefaultRootComponent.RootConfig.Setting)
            }
        })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuNavigationRail(
    modifier: Modifier = Modifier,
    navigation: StackNavigation<DefaultMenuComponent.MenuConfig>,
    stackState: ChildStack<*, MenuComponent.MenuChild>,
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
        NavigationRailItem(selected = stackState.active.instance is MenuComponent.MenuChild.Message, onClick = {
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.Message) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.Message)
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
                    imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = stringResource(id = R.string.conversation)
                )
            }
        },
            label = null
        )

        NavigationRailItem(selected = stackState.active.instance is MenuComponent.MenuChild.File, onClick = {
            navigation.bringToFront(DefaultMenuComponent.MenuConfig.File) {
                navigation.replaceAll(DefaultMenuComponent.MenuConfig.File)
            }
        }, icon = {
            Icon(
                imageVector = if (stackState.active.instance is MenuComponent.MenuChild.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                contentDescription = stringResource(id = R.string.work)
            )
        },
            label = null
        )

        NavigationRailItem(
            selected = stackState.active.instance is MenuComponent.MenuChild.Contact,
            onClick = {
                navigation.bringToFront(DefaultMenuComponent.MenuConfig.Contact) {
                    navigation.replaceAll(DefaultMenuComponent.MenuConfig.Contact)
                }
            }, icon = {
                Icon(
                    imageVector = if (stackState.active.instance is MenuComponent.MenuChild.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                    contentDescription = stringResource(id = R.string.contact_person)
                )
            },
            label = null
        )
    }
}

