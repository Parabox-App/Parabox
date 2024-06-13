package com.ojhdtapp.parabox.ui.setting.detail

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.LocalCacheUtil
import com.ojhdtapp.parabox.core.util.backup.LocalRoomBackup
import com.ojhdtapp.parabox.core.util.launchNotificationSetting
import com.ojhdtapp.parabox.core.util.toSizeString
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingLayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun StorageSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    BackHandler(enabled = layoutType != SettingLayoutType.SPLIT) {
        scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest)
    }
    if (layoutType == SettingLayoutType.SPLIT) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = "存储",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Content(
                    modifier = Modifier.weight(1f),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent,
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "存储",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scaffoldNavigator.navigateBack(BackNavigationBehavior.PopLatest) }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Content(
                modifier = Modifier.padding(innerPadding),
                state = state,
                mainSharedState = mainSharedState,
                layoutType = layoutType,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: SettingLayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val roomBackup = LocalRoomBackup.current
    val cacheUtil = LocalCacheUtil.current
    val appDatabase = (LocalContext.current as? MainActivity)?.appDatabase
    val cacheSize by cacheUtil.cacheSizeStateFlow.collectAsState()
    var cleaningCache by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(Unit) {
        cacheUtil.getCacheSize()
    }
    LazyColumn(modifier = modifier) {
        item {
            SettingHeader(
                text = stringResource(id = R.string.backup_and_restore),
            )
        }
        item {
            SettingItem(
                title = stringResource(R.string.backup_title),
                subTitle = stringResource(R.string.backup_subtitle),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = "backup"
                    )
                },
                selected = false, layoutType = layoutType
            ) {
                if (appDatabase != null) {
                    roomBackup.database(appDatabase)
                        .enableLogDebug(true)
                        .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                        .apply {
                            onCompleteListener { success, message, exitCode ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.backup_text),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.backup_failed, exitCode),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .backup()
                }
            }
        }
        item {
            SettingItem(
                title = stringResource(R.string.restore_title),
                subTitle = stringResource(R.string.restore_subtitle),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = "restore"
                    )
                },
                selected = false, layoutType = layoutType
            ) {
                if (appDatabase != null) {
                    roomBackup.database(appDatabase)
                        .enableLogDebug(true)
                        .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
                        .apply {
                            onCompleteListener { success, message, exitCode ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.restore_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.restore_failed, exitCode),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .restore()
                }
            }
        }
        item {
            SettingHeader(
                text = "空间管理",
            )
        }
        item {
            SettingItem(
                title = stringResource(R.string.clean_cache_title),
                subTitle = stringResource(id = R.string.clean_cache_subtitle, cacheSize.toSizeString()),
                disabled = cleaningCache,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = "clean cache",
                        tint = if (cleaningCache) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                },
                selected = false, layoutType = layoutType
            ) {
                if (!cleaningCache) {
                    coroutineScope.launch(Dispatchers.IO) {
                        cleaningCache = true
                        cacheUtil.clearCache()
                        delay(1500)
                        cleaningCache = false
                        cacheUtil.getCacheSize()
                    }
                }
            }
        }
    }
}