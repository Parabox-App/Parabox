package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
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
    val viewModel = hiltViewModel<SettingPageViewModel>()
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
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.TopStart)
                    ) {
                        IconButton(onClick = {
                            expanded = !expanded
                        }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "开放源代码许可",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    expanded = false
                                })
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            // Plugin List State
            val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()

            Row() {
                EditUserNameDialog(
                    openDialog = viewModel.editUserNameDialogState.value,
                    userName = viewModel.userNameFlow.collectAsState(initial = "User").value,
                    onConfirm = {
                        viewModel.setEditUserNameDialogState(false)
                        viewModel.setUserName(it)
                    },
                    onDismiss = { viewModel.setEditUserNameDialogState(false) }
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = innerPadding,
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    item {
                        ThemeBlock(
                            modifier = Modifier.fillMaxWidth(),
                            userName = viewModel.userNameFlow.collectAsState(initial = "User").value,
                            version = "1.0",
                            onBlockClick = {},
                            onUserNameClick = {
                                viewModel.setEditUserNameDialogState(true)
                            },
                            onVersionClick = {},
                            padding = if (sizeClass.widthSizeClass == WindowWidthSizeClass.Medium) 32.dp else 16.dp,
                        )
                    }
                    item(key = "extension_status") {
                        AnimatedVisibility(
                            visible = pluginList.isNotEmpty(),
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            PreferencesCategory(text = "扩展状态")
                        }
                    }
                    items(
                        items = pluginList,
                        key = { it.packageName }) {
                        NormalPreference(
                            title = it.name,
                            subtitle = it.packageName,
                            leadingIcon = {
                                AsyncImage(model = it.icon, contentDescription = "icon", modifier = Modifier.size(24.dp).clip(
                                    CircleShape))
                            },
                            trailingIcon = {
                                when (it.runningStatus) {
                                    AppModel.RUNNING_STATUS_DISABLED -> Icon(
                                        imageVector = Icons.Outlined.Block,
                                        contentDescription = "disabled"
                                    )
                                    AppModel.RUNNING_STATUS_ERROR -> Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = "error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    AppModel.RUNNING_STATUS_RUNNING -> Icon(
                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                        contentDescription = "running",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {}
                        )
                    }
                    item(key = "function") {
                        PreferencesCategory(text = "行为")
                    }
                    item(key = "extension") {
                        NormalPreference(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Extension,
                                    contentDescription = "plugin"
                                )
                            },
                            title = "扩展",
                            subtitle = "管理已安装的扩展"
                        ) {}
                    }
                    item(key = "cloud") {
                        NormalPreference(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Cloud,
                                    contentDescription = "cloud"
                                )
                            },
                            title = "连接云端服务",
                            subtitle = "添加或修改云端服务连接"
                        ) {}
                    }
                    item(key = "backup") {
                        NormalPreference(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = "backup and restore"
                                )
                            },
                            title = "备份与还原",
                            subtitle = "数据导出及恢复，存储空间管理"
                        ) {}
                    }
                    item(key = "palette") {
                        NormalPreference(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "theme"
                                )
                            },
                            title = "用户界面",
                            subtitle = "主题和语言"
                        ) {}
                    }
                    item(key = "experimental") {
                        NormalPreference(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Science,
                                    contentDescription = "experimental"
                                )
                            },
                            title = "高级选项",
                            subtitle = "实验性特性与功能"
                        ) {}
                    }
                }
                if (sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                    LazyColumn(
                        modifier = Modifier.width(560.dp),
                    ) {

                    }
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
