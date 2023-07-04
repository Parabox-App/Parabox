@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    listState: LazyListState,
    drawerState: DrawerState,
    bottomSheetState: SheetState,
    layoutType: MessageLayoutType
) {
    val viewModel = hiltViewModel<MessagePageViewModel>()
    Scaffold(modifier = modifier,
    topBar = {
//        SearchBar(query = , onQueryChange = , onSearch = , active = , onActiveChange = ) {
//
//    }
    }) {
        LazyColumn(modifier = Modifier.padding(it),state = listState){


        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissContact(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    startToEndIcon: ImageVector? = null,
    endToStartIcon: ImageVector? = null,
    onDismissedToEnd: () -> Boolean,
    onDismissedToStart: () -> Boolean,
    onVibrate: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberDismissState(
        confirmValueChange = {
            when (it) {
                DismissValue.DismissedToEnd -> {
                    onDismissedToEnd()
                }
                DismissValue.DismissedToStart -> {
                    onDismissedToStart()
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * .25f }
    )
    LaunchedEffect(key1 = dismissState.targetValue == DismissValue.Default) {
        if (dismissState.progress != 0f && dismissState.progress != 1f)
            onVibrate()
    }
    LaunchedEffect(key1 = Unit){
        dismissState.reset()
    }
    val isDismissed = dismissState.isDismissed(DismissDirection.EndToStart)
            || dismissState.isDismissed(DismissDirection.StartToEnd)
    val isDismissedToStart = dismissState.isDismissed(DismissDirection.EndToStart)
    val isDismissedToEnd = dismissState.isDismissed(DismissDirection.StartToEnd)

    AnimatedVisibility(
        modifier = modifier.fillMaxWidth(),
        visible = !isDismissedToStart,
        exit = slideOutHorizontally { -it },
        enter = expandVertically()
    ) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = !isDismissedToEnd,
            exit = slideOutHorizontally { it }, enter = expandVertically()
        ) {
            SwipeToDismiss(
                modifier = Modifier.padding(bottom = 2.dp),
                state = dismissState,
                background = {
                    val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            DismissValue.Default -> MaterialTheme.colorScheme.secondary
                            DismissValue.DismissedToEnd -> MaterialTheme.colorScheme.primary
                            DismissValue.DismissedToStart -> MaterialTheme.colorScheme.primary
                        }
                    )
                    val textColor by animateColorAsState(
                        when (dismissState.targetValue) {
                            DismissValue.Default -> MaterialTheme.colorScheme.onSecondary
                            DismissValue.DismissedToEnd -> MaterialTheme.colorScheme.onPrimary
                            DismissValue.DismissedToStart -> MaterialTheme.colorScheme.onPrimary
                        }
                    )
                    val alignment = when (direction) {
                        DismissDirection.StartToEnd -> Alignment.CenterStart
                        DismissDirection.EndToStart -> Alignment.CenterEnd
                    }
                    val icon = when (direction) {
                        DismissDirection.StartToEnd -> startToEndIcon ?: Icons.Outlined.Done
                        DismissDirection.EndToStart -> endToStartIcon ?: Icons.Outlined.Done
                    }
                    val scale by animateFloatAsState(
                        if (dismissState.targetValue == DismissValue.Default)
                            0.75f else 1f
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            icon,
                            contentDescription = "Localized description",
                            modifier = Modifier.scale(scale),
                            tint = textColor
                        )
                    }
                },
                directions = buildSet {
                    if (!enabled) return@buildSet
                    if (startToEndIcon != null) add(DismissDirection.StartToEnd)
                    if (endToStartIcon != null) add(DismissDirection.EndToStart)
                },
                dismissContent = {
                    content()
                }
            )
        }
    }
}