package com.ojhdtapp.parabox.ui.navigation.suite

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.menu.MenuNavigationType
import com.ojhdtapp.parabox.ui.menu.MenuPageEvent
import com.ojhdtapp.parabox.ui.navigation.MenuTab

internal val ClassicBottomBarHeight = 80.dp

internal fun MenuTab.labelResId(): Int = when (this) {
    MenuTab.Message -> R.string.conversation
    MenuTab.File -> R.string.work
    MenuTab.Contact -> R.string.contact_person
}

internal fun MenuTab.icon(selected: Boolean) = when (this) {
    MenuTab.Message -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
    MenuTab.File -> if (selected) Icons.Filled.Work else Icons.Outlined.WorkOutline
    MenuTab.Contact -> if (selected) Icons.Filled.Contacts else Icons.Outlined.Contacts
}

@Composable
internal fun ClassicMenuNavigationBar(
    currentTab: MenuTab,
    unreadCount: Int,
    enabled: Boolean,
    onSelectTab: (MenuTab) -> Unit,
    onEvent: (MenuPageEvent) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = NavigationBarDefaults.windowInsets.union(
            WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        ),
    ) {
        MenuTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val title = stringResource(tab.labelResId())
            NavigationBarItem(
                selected = selected,
                enabled = enabled,
                onClick = {
                    onEvent(MenuPageEvent.OnBarItemClicked(selected))
                    onSelectTab(tab)
                },
                icon = {
                    BadgedBox(badge = {
                        if (tab == MenuTab.Message && unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) { Text(unreadCount.toString()) }
                        }
                    }) {
                        Icon(tab.icon(selected), contentDescription = title)
                    }
                },
                label = { Text(title, style = MaterialTheme.typography.labelLarge) },
                alwaysShowLabel = false,
            )
        }
    }
}

@Composable
fun MenuNavigationDrawerContent(
    modifier: Modifier = Modifier,
    currentTab: MenuTab,
    mainSharedState: MainSharedState,
    navigationType: MenuNavigationType,
    onSelectTab: (MenuTab) -> Unit,
    onOpenSettings: () -> Unit,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    Spacer(modifier = Modifier.statusBarsPadding())
    IconButton(
        modifier = Modifier.padding(12.dp),
        onClick = { onEvent(MenuPageEvent.OnMenuClick) },
    ) {
        Icon(imageVector = Icons.AutoMirrored.Outlined.MenuOpen, contentDescription = "menu_open")
    }
    if (navigationType == MenuNavigationType.BOTTOM_NAVIGATION) {
        ExtendedFloatingActionButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            onClick = { onEvent(MenuPageEvent.OnFABClicked) },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.new_contact),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    } else {
        ExtendedFloatingActionButton(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = {
                Text(text = stringResource(id = R.string.new_contact))
            },
            icon = {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
            },
            onClick = { onEvent(MenuPageEvent.OnFABClicked) },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
    NavigationDrawerItem(
        modifier = Modifier
            .padding(12.dp, 0.dp, 12.dp, 6.dp)
            .height(48.dp),
        selected = currentTab == MenuTab.Message,
        onClick = {
            onSelectTab(MenuTab.Message)
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))
        },
        icon = {
            Icon(
                imageVector = if (currentTab == MenuTab.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                contentDescription = stringResource(id = R.string.conversation),
            )
        },
        label = { Text(text = stringResource(id = R.string.conversation)) },
        badge = {
            if (mainSharedState.datastore.messageBadgeNum > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = "${mainSharedState.datastore.messageBadgeNum}",
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        },
    )
    NavigationDrawerItem(
        modifier = Modifier
            .padding(12.dp, 0.dp, 12.dp, 6.dp)
            .height(48.dp),
        selected = currentTab == MenuTab.File,
        onClick = {
            onSelectTab(MenuTab.File)
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))
        },
        icon = {
            Icon(
                imageVector = if (currentTab == MenuTab.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                contentDescription = stringResource(id = R.string.work),
            )
        },
        label = { Text(text = stringResource(id = R.string.work)) },
    )
    NavigationDrawerItem(
        modifier = Modifier
            .padding(12.dp, 0.dp, 12.dp, 6.dp)
            .height(48.dp),
        selected = currentTab == MenuTab.Contact,
        onClick = {
            onSelectTab(MenuTab.Contact)
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))
        },
        icon = {
            Icon(
                imageVector = if (currentTab == MenuTab.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                contentDescription = stringResource(id = R.string.contact_person),
            )
        },
        label = { Text(text = stringResource(id = R.string.contact_person)) },
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
                contentDescription = stringResource(id = R.string.settings),
            )
        },
        label = { Text(text = stringResource(id = R.string.settings)) },
        selected = false,
        onClick = {
            onEvent(MenuPageEvent.OnDrawerItemClicked(false))
            onOpenSettings()
        },
    )
}

@Composable
fun MenuNavigationRail(
    modifier: Modifier = Modifier,
    currentTab: MenuTab,
    mainSharedState: MainSharedState,
    onSelectTab: (MenuTab) -> Unit,
    onEvent: (event: MenuPageEvent) -> Unit,
) {
    NavigationRail(
        modifier = modifier,
        windowInsets = NavigationRailDefaults.windowInsets.union(
            WindowInsets.displayCutout.only(WindowInsetsSides.Start),
        ),
        header = {
            IconButton(
                modifier = Modifier.statusBarsPadding(),
                onClick = { onEvent(MenuPageEvent.OnMenuClick) },
            ) {
                Icon(imageVector = Icons.Outlined.Menu, contentDescription = "menu")
            }
            FloatingActionButton(
                onClick = { onEvent(MenuPageEvent.OnFABClicked) },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                ),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "add")
            }
        },
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        NavigationRailItem(
            selected = currentTab == MenuTab.Message,
            onClick = { onSelectTab(MenuTab.Message) },
            icon = {
                BadgedBox(badge = {
                    if (mainSharedState.datastore.messageBadgeNum > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(text = "${mainSharedState.datastore.messageBadgeNum.takeIf { it < 99 } ?: "99+"}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (currentTab == MenuTab.Message) Icons.AutoMirrored.Default.Chat else Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = stringResource(id = R.string.conversation),
                    )
                }
            },
            label = null,
        )
        NavigationRailItem(
            selected = currentTab == MenuTab.File,
            onClick = { onSelectTab(MenuTab.File) },
            icon = {
                Icon(
                    imageVector = if (currentTab == MenuTab.File) Icons.Default.Work else Icons.Outlined.WorkOutline,
                    contentDescription = stringResource(id = R.string.work),
                )
            },
            label = null,
        )
        NavigationRailItem(
            selected = currentTab == MenuTab.Contact,
            onClick = { onSelectTab(MenuTab.Contact) },
            icon = {
                Icon(
                    imageVector = if (currentTab == MenuTab.Contact) Icons.Default.Contacts else Icons.Outlined.Contacts,
                    contentDescription = stringResource(id = R.string.contact_person),
                )
            },
            label = null,
        )
    }
}
