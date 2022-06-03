package com.ojhdtapp.parabox.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Settings
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
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class NavigationDestination(
    val direction: DirectionDestinationSpec,
    val icon: ImageVector,
    val label: String,
) {
    Message(MessagePageDestination, Icons.Outlined.Chat, "会话"),
    File(FilePageDestination, Icons.Outlined.FileDownload, "文件"),
    Setting(SettingPageDestination, Icons.Outlined.Settings, "设置")
}

@Composable
fun NavigationBar(
    navController: NavController
) {
    val currentDestination: Destination =
        navController.appCurrentDestinationAsState().value ?: NavGraphs.root.startAppDestination
    NavigationBar() {
        NavigationDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = currentDestination == destination.direction,
                onClick = {
                    navController.navigate(destination.direction) {
                        launchSingleTop = true
                    }
                },
            icon = { Icon(imageVector = destination.icon, contentDescription = destination.label)},
            label = { Text(text = destination.label)},
            alwaysShowLabel = false)
        }
    }
}