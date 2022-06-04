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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class NavigationDestination(
    val direction: DirectionDestinationSpec,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val label: String,
) {
    Message(MessagePageDestination, Icons.Outlined.Chat, Icons.Default.Chat, "会话"),
    File(FilePageDestination, Icons.Outlined.WorkOutline, Icons.Default.Work, "工作"),
    Setting(SettingPageDestination, Icons.Outlined.Settings, Icons.Default.Settings, "设置")
}

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    navController: NavController,
    messageBadge: Int = 0,
    settingBadge: Boolean = false,
    onSelfItemClick: () -> Unit,
) {
    val currentDestination: Destination =
        navController.appCurrentDestinationAsState().value ?: NavGraphs.root.startAppDestination
    Column() {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationDestination.values().forEach { destination ->
                NavigationBarItem(
                    selected = currentDestination == destination.direction,
                    onClick = {
                        if (currentDestination == destination.direction) {
                            onSelfItemClick()
                        } else {
                            navController.navigate(destination.direction) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (destination.direction == MessagePageDestination && messageBadge != 0)
                                Badge { Text(text = "$messageBadge") }
                            else if (destination.direction == SettingPageDestination && settingBadge)
                                Badge()
                        }) {
                            Icon(
                                imageVector = if (currentDestination == destination.direction) destination.iconSelected else destination.icon,
                                contentDescription = destination.label
                            )
                        }
                    },
                    label = { Text(text = destination.label) },
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