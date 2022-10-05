package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.RoundedCornerDropdownMenu
import com.ojhdtapp.parabox.ui.util.SimpleMenuPreference
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudPage(
    modifier: Modifier = Modifier,
    viewModel: SettingPageViewModel,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    // Google Drive
    val gDriveLogin by viewModel.googleLoginFlow.collectAsState(initial = false)
    val gDriveTotalSpace by viewModel.googleTotalSpaceFlow.collectAsState(initial = 0L)
    val gDriveUsedSpace by viewModel.googleUsedSpaceFlow.collectAsState(initial = 0L)
    val gDriveUsedSpacePercent = remember{
        derivedStateOf {
            if (gDriveTotalSpace == 0L) 0 else (gDriveUsedSpace * 100 / gDriveTotalSpace).toInt()
        }
    }
    val gDriveAppUsedSpace by viewModel.googleAppUsedSpaceFlow.collectAsState(initial = 0L)
    val gDriveAppUsedSpacePercent = remember{
        derivedStateOf {
            if (gDriveTotalSpace == 0L) 0 else (gDriveAppUsedSpace * 100 / gDriveTotalSpace).toInt()
        }
    }
    val selectableService by remember{
        derivedStateOf{
            buildMap<Int, String> {
                put(0, "无")
                if(gDriveLogin) put(GoogleDriveUtil.SERVICE_CODE, "Google Drive")
            }
        }
    }
    val defaultBackupService by viewModel.defaultBackupServiceFlow.collectAsState(initial = 0)
    val autoBackup by viewModel.autoBackupFlow.collectAsState(initial = false)
    val autoDeleteLocalFile by viewModel.autoDeleteLocalFileFlow.collectAsState(initial = false)
    val gDriveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (result.data != null) {
                    val googleSignInAccount = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    googleSignInAccount.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val account = task.result
                            if (account != null) {
                                viewModel.saveGoogleDriveAccount(account)
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar("成功连接 Google Drive")
                                }
                            }
                        } else {
                            viewModel.saveGoogleDriveAccount(null)
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar("连接取消")
                            }
                        }
                    }
                }
            }
        }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
                title = { Text("连接云端服务") },
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
        LazyColumn(
            contentPadding = it
        ) {
            item {
                if (!gDriveLogin) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = "未连接云端服务",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            modifier = Modifier.padding(vertical = 16.dp),
                            text = "连接云端服务可将您的会话文件备份至云端",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FilledTonalButton(
                            onClick = {
                                val signInIntent =
                                    (context as MainActivity).getGoogleLoginAuth().signInIntent
                                gDriveLauncher.launch(signInIntent)
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = "cloud",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(ButtonDefaults.IconSize),
                            )
                            Text(text = "连接云端服务")
                        }
                    }
                }
            }
            item {
                if (gDriveLogin) {
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Box(modifier = Modifier.wrapContentSize()){
                        OutlinedCard(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), onClick = {
                            expanded = true
                        }) {
                            Row(modifier = Modifier.padding(16.dp)) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        FaIcon(
                                            faIcon = FaIcons.GoogleDrive,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column() {
                                    Text(
                                        text = "Google Drive",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    LinearProgressIndicator(
                                        progress = 0.6f,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = "已使用 ${gDriveUsedSpacePercent.value}% 的存储空间（${FileUtil.getSizeString(gDriveUsedSpace)} / ${FileUtil.getSizeString(gDriveTotalSpace)}）",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "其中应用使用 ${gDriveAppUsedSpacePercent.value}%（${FileUtil.getSizeString(gDriveAppUsedSpace)}）",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        RoundedCornerDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false}) {
                            DropdownMenuItem(text = { Text(text = "退出登录") }, onClick = {
                                expanded = false
                                (context as MainActivity).getGoogleLoginAuth().signOut()
                                    .addOnCompleteListener {
                                        viewModel.saveGoogleDriveAccount(null)
                                        coroutineScope.launch {
                                            snackBarHostState.showSnackbar("已退出登录")
                                        }
                                    }
                            })
                        }
                    }
                }
            }
            item {
                PreferencesCategory(text = "服务设置")
            }
            item {
                SimpleMenuPreference(
                    title = "默认云端服务",
                    optionsMap = selectableService,
                    selectedKey = defaultBackupService,
                    onSelect = viewModel::setDefaultBackupService)
            }
            item {
                SwitchPreference(
                    title = "自动备份",
                    subtitleOff = "于合适网络环境下自动下载文件，使用默认云端服务备份",
                    subtitleOn = "启用",
                    initialChecked = autoBackup,
                    onCheckedChange = viewModel::setAutoBackup,
                    enabled = defaultBackupService != 0)
            }
            item {
                NormalPreference(title = "目标会话", subtitle = "对选中会话应用自动备份", enabled = defaultBackupService != 0) {}
            }
            item {
                SwitchPreference(
                    title = "同时删除本地文件",
                    subtitleOff = "备份完成后自动删除本地文件",
                    subtitleOn = "启用",
                    initialChecked = autoDeleteLocalFile,
                    onCheckedChange = viewModel::setAutoDeleteLocalFile,
                    enabled = autoBackup && defaultBackupService != 0
                )
            }
        }
    }
}