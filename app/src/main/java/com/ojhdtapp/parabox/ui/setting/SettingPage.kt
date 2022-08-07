package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.menu.MenuSharedViewModel
import com.ojhdtapp.parabox.ui.util.SettingNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@SettingNavGraph(start = true)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState
) {
    val coroutineScope = rememberCoroutineScope()
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarState()
    )
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colorTransitionFraction = scrollBehavior.state.collapsedFraction ?: 0f
            val appBarContainerColor by TopAppBarDefaults.largeTopAppBarColors()
                .containerColor(colorTransitionFraction)
            LargeTopAppBar(
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                title = { Text("设置") },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(imageVector = Icons.Outlined.Menu, contentDescription = "menu")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "more"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            LazyVerticalGrid(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                columns = GridCells.Fixed(if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 2 else 1)
            ) {
                item {
                    ThemeBlock(
                        modifier = Modifier.fillMaxWidth(),
                        userName = "User",
                        version = "1.0",
                        onBlockClick = {},
                        onUserNameClick = {},
                        onVersionClick = {},
                        padding = if (sizeClass.widthSizeClass != WindowWidthSizeClass.Compact) 32.dp else 16.dp,
                    )
                }
                val list = (0..75).map { it.toString() }
                items(count = list.size) {
                    Text(
                        text = list[it],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun ThemeBlock(
    modifier: Modifier = Modifier,
    userName: String,
    version: String,
    onBlockClick: () -> Unit,
    onUserNameClick: () -> Unit,
    onVersionClick: () -> Unit,
    padding: Dp = 16.dp,
) =
    Row(
        modifier = modifier
            .aspectRatio(2f)
            .padding(horizontal = padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var shapeState by remember {
            mutableStateOf(true)
        }
        val roundedCornerA by animateDpAsState(targetValue = if (shapeState) 72.dp else 24.dp)
        val roundedCornerB by animateDpAsState(targetValue = if (shapeState) 24.dp else 72.dp)

        Surface(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(
                topStart = roundedCornerA,
                topEnd = roundedCornerB,
                bottomStart = roundedCornerB,
                bottomEnd = roundedCornerA,
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        shapeState = !shapeState
                        onBlockClick()
                    },
                contentAlignment = Alignment.Center
            ) {

            }
        }
        Spacer(modifier = Modifier.width(padding))
        Column(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .clickable { onUserNameClick() }
                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "如何称呼您",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(padding))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .clickable { onVersionClick() }
                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "版本",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
