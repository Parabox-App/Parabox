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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.CacheUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.SliderPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val cacheSize = viewModel.cacheSizeStateFlow.collectAsState()

    var deleteFilesBeforeDays by remember {
        mutableStateOf(7f)
    }
    val canSaveSpace by remember(viewModel.cleaningFile.value) {
        derivedStateOf {
            FileUtil.getSizeString(
                CacheUtil.getChatFilesSizeBeforeTimestamp(
                    context,
                    System.currentTimeMillis() - deleteFilesBeforeDays.roundToInt() * 24 * 60 * 60 * 1000
                )
            )
        }
    }
    var showDeleteFileConfirmDialog by remember{
        mutableStateOf(false)
    }
    if(showDeleteFileConfirmDialog){
        AlertDialog(
            onDismissRequest = {
                showDeleteFileConfirmDialog = false
            },
            title = {
                Text(text = "删除会话资源")
            },
            text = {
                Text(text = "选中的会话资源（图片，语音）将被永久删除，无法恢复。是否确认？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteFileConfirmDialog = false
                        viewModel.clearFile(System.currentTimeMillis() - deleteFilesBeforeDays.roundToInt() * 24 * 60 * 60 * 1000)
                    }
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteFileConfirmDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
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
            item {
                PreferencesCategory(text = "会话资源管理")
            }
            item {
                SliderPreference(
                    title = "会话资源删除范围",
                    subTitle =
                    when (deleteFilesBeforeDays) {
                        0f -> "所有"
                        else -> "${deleteFilesBeforeDays.roundToInt()}天前"
                    },
                    value = deleteFilesBeforeDays,
                    valueRange = 0f..15f,
                    steps = 14,
                    enabled = !viewModel.cleaningFile.value,
                    onValueChange = { deleteFilesBeforeDays = it },
                )
            }
            item {
                NormalPreference(
                    title = "删除过期的会话资源",
                    subtitle = if (deleteFilesBeforeDays.roundToInt() == 0) "所有图片，语音将被删除\n预计可释放 $canSaveSpace 空间"
                    else "于 ${deleteFilesBeforeDays.roundToInt()} 天或更早前接收的图片，语音将被删除\n预计可释放 $canSaveSpace 空间",
                    enabled = !viewModel.cleaningFile.value,
                ) {
                    showDeleteFileConfirmDialog = true
                }
            }
        }
    }
}