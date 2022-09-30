package com.ojhdtapp.parabox.ui.setting

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ojhdtapp.parabox.domain.model.AppModel
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoPage(
    modifier: Modifier = Modifier,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colorTransitionFraction = scrollBehavior.state.collapsedFraction
            val appBarContainerColor by rememberUpdatedState(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    FastOutLinearInEasing.transform(colorTransitionFraction)
                )
            )
            LargeTopAppBar(
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                title = { Text("关于") },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {

                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) {
        // Plugin List State
        val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
        LazyColumn(
            contentPadding = it
        ) {
            if(sizeClass.widthSizeClass == WindowWidthSizeClass.Medium){
                item {
                    ThemeBlock(
                        modifier = Modifier.fillMaxWidth(),
                        userName = mainSharedViewModel.userNameFlow.collectAsState(initial = "User").value,
                        version = "1.0",
                        onBlockClick = {},
                        onUserNameClick = {
                            viewModel.setEditUserNameDialogState(true)
                        },
                        onVersionClick = {}
                    )
                }
            }
            item(key = "extension_status") {
                AnimatedVisibility(
                    visible = pluginList.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    PreferencesCategory(text = "扩展")
                }
            }
            items(
                items = pluginList,
                key = { it.packageName }) {
                NormalPreference(
                    title = it.name,
                    subtitle = it.packageName,
                    leadingIcon = {
                        AsyncImage(
                            model = it.icon,
                            contentDescription = "icon",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(
                                    CircleShape
                                )
                        )
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
                            AppModel.RUNNING_STATUS_CHECKING -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    onClick = {
                        it.launchIntent?.let {
                            onEvent(ActivityEvent.LaunchIntent(it.apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }))
                        }
                    }
                )
            }
        }
    }
}