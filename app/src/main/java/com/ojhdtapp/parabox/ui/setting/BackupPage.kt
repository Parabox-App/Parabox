package com.ojhdtapp.parabox.ui.setting

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun BackupPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val cacheSize = viewModel.cacheSizeStateFlow.collectAsState()
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
                title = { Text(stringResource(R.string.backup_and_restore)) },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {
                            mainNavController.navigateUp()
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
        LazyColumn(
            contentPadding = it
        ) {
            item {
                NormalPreference(
                    title = stringResource(R.string.backup_title),
                    subtitle = stringResource(R.string.backup_subtitle),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FileOpen,
                            contentDescription = "backup"
                        )
                    },
                    onClick = {
                        onEvent(ActivityEvent.Backup)
                    }
                )
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.restore_title),
                    subtitle = stringResource(R.string.restore_subtitle),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = "restore"
                        )
                    },
                    onClick = {
                        onEvent(ActivityEvent.Restore)
                    }
                )
            }
            item {
                PreferencesCategory(text = stringResource(R.string.space_management))
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.clean_cache_title),
                    subtitle = stringResource(id = R.string.clean_cache_subtitle, cacheSize.value),
                    enabled = !viewModel.cleaningCache.value,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "clean cache",
                            tint = if (viewModel.cleaningCache.value) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    onClick = {
                        viewModel.clearCache()
                    }
                )
            }
        }
    }
}