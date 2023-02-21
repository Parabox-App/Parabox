package com.ojhdtapp.parabox.ui.setting

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.OnedriveUtil
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ojhdtapp.parabox.ui.util.RoundedCornerDropdownMenu
import com.ojhdtapp.parabox.ui.util.SliderPreference
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun CloudPage(
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
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    var showCloudDialog by remember {
        mutableStateOf(false)
    }
    var showContactDialog by remember {
        mutableStateOf(false)
    }


    val cloudService by viewModel.cloudServiceFlow.collectAsState(initial = 0)
    val cloudTotalSpace by viewModel.cloudTotalSpaceFlow.collectAsState(initial = 0L)
    val cloudUsedSpace by viewModel.cloudUsedSpaceFlow.collectAsState(initial = 0L)
    val cloudUsedSpacePercent = remember {
        derivedStateOf {
            if (cloudTotalSpace == 0L) 0 else (cloudUsedSpace * 100 / cloudTotalSpace).toInt()
        }
    }
    val cloudAppUsedSpace by viewModel.cloudAppUsedSpaceFlow.collectAsState(initial = 0L)
    val cloudAppUsedSpacePercent = remember {
        derivedStateOf {
            if (cloudTotalSpace == 0L) 0 else (cloudAppUsedSpace * 100 / cloudTotalSpace).toInt()
        }
    }
    val autoBackup by viewModel.autoBackupFlow.collectAsState(initial = false)
    val autoBackupFileMaxSize by viewModel.autoBackupFileMaxSizeFlow.collectAsState(initial = 10f)
    val autoDeleteLocalFile by viewModel.autoDeleteLocalFileFlow.collectAsState(initial = false)
    val gDriveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (result.data != null) {
                    val googleSignInAccount = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    googleSignInAccount.addOnCompleteListener { task ->
                        showCloudDialog = false
                        if (task.isSuccessful) {
                            val account = task.result
                            if (account != null) {
                                viewModel.saveGoogleDriveAccount(account)
                                coroutineScope.launch {
                                    snackBarHostState.showSnackbar(context.getString(R.string.connect_gd_success))
                                }
                            }
                        } else {
                            viewModel.saveGoogleDriveAccount(null)
                            coroutineScope.launch {
                                snackBarHostState.showSnackbar(context.getString(R.string.connect_cloud_service_cancel))
                            }
                        }
                    }
                } else {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(context.getString(R.string.device_not_support))
                    }
                }
            } else {
                coroutineScope.launch {
                    snackBarHostState.showSnackbar(context.getString(R.string.device_not_support))
                }
            }
        }

    // Cloud Dialog
    if (showCloudDialog) {
        AlertDialog(
            onDismissRequest = { showCloudDialog = false },
            confirmButton = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "select cloud storage"
                )
            },
            title = { Text(text = stringResource(id = R.string.connect_cloud_service)) },
            text = {
                LazyColumn() {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                val signInIntent =
                                    (context as MainActivity).getGoogleLoginAuth().signInIntent
                                gDriveLauncher.launch(signInIntent)
                            }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    faIcon = FaIcons.GoogleDrive,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.cloud_service_save_to_gd),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val res = (context as MainActivity).msSignIn()
                                    showCloudDialog = false
                                    if (res) {
                                        snackBarHostState.showSnackbar(context.getString(R.string.connect_od_successful))
                                    } else {
                                        snackBarHostState.showSnackbar(context.getString(R.string.operation_canceled))
                                    }
                                }
                            }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    faIcon = FaIcons.Microsoft,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.cloud_service_od),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                (context as MainActivity).msLoginIn()
                            }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FaIcon(
                                    modifier = Modifier.padding(16.dp),
                                    faIcon = FaIcons.Microsoft,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Microsoft OneDrive",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    // Contact Dialog
    ContactListDialog(
        modifier = Modifier,
        showDialog = showContactDialog,
        contactList = viewModel.contactStateFlow.collectAsState().value.filter { it.contactId == it.senderId },
        contactCheck = { it.shouldBackup },
        onValueChange = { target, value ->
            viewModel.onContactBackupChange(target, value)
        },
        loading = viewModel.contactLoadingState.value,
        sizeClass = sizeClass,
        onDismiss = {
            showContactDialog = false
        }
    )

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
                title = { Text(stringResource(id = R.string.connect_cloud_service)) },
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
                if (cloudService != 0) {
                    var expanded by remember {
                        mutableStateOf(false)
                    }
                    Box(modifier = Modifier.wrapContentSize()) {
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
                                        when (cloudService) {
                                            GoogleDriveUtil.SERVICE_CODE -> {
                                                FaIcon(
                                                    faIcon = FaIcons.GoogleDrive,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            OnedriveUtil.SERVICE_CODE -> {
                                                FaIcon(
                                                    faIcon = FaIcons.Microsoft,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            else -> {
                                                FaIcon(
                                                    faIcon = FaIcons.Cloud,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column() {
                                    when (cloudService) {
                                        GoogleDriveUtil.SERVICE_CODE -> {
                                            Text(
                                                text = stringResource(id = R.string.cloud_service_gd),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        OnedriveUtil.SERVICE_CODE -> {
                                            Text(
                                                text = stringResource(R.string.cloud_service_od),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = stringResource(id = R.string.cloud_service),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = cloudUsedSpacePercent.value.toFloat() / 100,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .clip(CircleShape),
                                        )
                                    Text(
                                        text = stringResource(
                                            id = R.string.cloud_service_used_space,
                                            cloudUsedSpacePercent.value,
                                            FileUtil.getSizeString(
                                                cloudUsedSpace
                                            ),
                                            FileUtil.getSizeString(
                                                cloudTotalSpace
                                            )
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.cloud_service_app_used_space,
                                            cloudAppUsedSpacePercent.value,
                                            FileUtil.getSizeString(
                                                cloudAppUsedSpace
                                            )
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        RoundedCornerDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.sign_out_cloud_service)) },
                                onClick = {
                                    expanded = false
                                    when (cloudService) {
                                        GoogleDriveUtil.SERVICE_CODE -> {
                                            (context as MainActivity).getGoogleLoginAuth().signOut()
                                                .addOnCompleteListener {
                                                    viewModel.saveGoogleDriveAccount(null)
                                                    coroutineScope.launch {
                                                        snackBarHostState.showSnackbar(
                                                            context.getString(
                                                                R.string.signed_out_cloud_service
                                                            )
                                                        )
                                                    }
                                                }
                                        }
                                        OnedriveUtil.SERVICE_CODE -> {
                                            coroutineScope.launch {
                                                val res = (context as MainActivity).msSignOut()
                                                if(res){
                                                    snackBarHostState.showSnackbar(
                                                        context.getString(
                                                            R.string.signed_out_cloud_service
                                                        )
                                                    )
                                                } else {
                                                    snackBarHostState.showSnackbar(
                                                        context.getString(
                                                            R.string.unknown_error
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        else -> {

                                        }
                                    }

                                })
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = stringResource(id = R.string.cloud_service_not_connected),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                            text = stringResource(R.string.cloud_service_des),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(
                            onClick = {
                                showCloudDialog = true
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = "cloud",
                                modifier = Modifier
                                    .size(ButtonDefaults.IconSize),
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = stringResource(id = R.string.connect_cloud_service)
                            )
                        }
                    }
                }


            }
            item {
                PreferencesCategory(text = stringResource(R.string.cloud_service_settings))
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.auto_backup_title),
                    subtitleOff = stringResource(R.string.auto_backup_subtitle_off),
                    subtitleOn = stringResource(R.string.auto_backup_subtitle_on),
                    checked = autoBackup && cloudService != 0,
                    onCheckedChange = viewModel::setAutoBackup,
                    enabled = cloudService != 0
                )
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.auto_backup_target_contacts_title),
                    subtitle = stringResource(R.string.auto_backup_target_contacts_subtitle),
                    enabled = cloudService != 0
                ) {
                    showContactDialog = true
                }
            }
            item {
                SliderPreference(
                    title = stringResource(R.string.auto_backup_file_max_size_title),
                    subTitle =
                    when (autoBackupFileMaxSize) {
                        100f -> stringResource(R.string.no_limit)
                        else -> "${autoBackupFileMaxSize.toInt()}MB"
                    },
                    value = autoBackupFileMaxSize,
                    valueRange = 10f..100f,
                    steps = 8,
                    enabled = autoBackup && cloudService != 0,
                    onValueChange = viewModel::setAutoBackupFileMaxSize,
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.auto_backup_then_delete_title),
                    subtitleOff = stringResource(R.string.auto_backup_then_delete_subtitle_off),
                    subtitleOn = stringResource(R.string.auto_backup_then_delete_subtitle_on),
//                    checked = autoDeleteLocalFile && autoBackup && defaultBackupService != 0,
                    checked = true,
                    onCheckedChange = viewModel::setAutoDeleteLocalFile,
//                    enabled = autoBackup && defaultBackupService != 0,
                    enabled = false
                )
            }
            item {
                Column(modifier = Modifier.padding(24.dp, 16.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.auto_backup_info),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}