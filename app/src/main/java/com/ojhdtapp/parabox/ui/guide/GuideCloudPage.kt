package com.ojhdtapp.parabox.ui.guide

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.HyperlinkText
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideExtensionPageDestination
import com.ojhdtapp.parabox.ui.destinations.GuidePersonalisePageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.util.*
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideCloudPage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

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

    var showCloudDialog by remember {
        mutableStateOf(false)
    }

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
                                    text = stringResource(id = R.string.cloud_service_gd),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    BottomSheetScaffold(
        modifier = Modifier
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        sheetContent = {
            Row(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedButton(onClick = {
                    mainNavController.navigateUp()
                }) {
                    Text(text = stringResource(R.string.back))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        mainNavController.navigate(GuidePersonalisePageDestination)
                    },
                    enabled = true
                ) {
                    if (cloudService == 0) {
                        Text(text = stringResource(R.string.later))
                    } else {
                        Text(text = stringResource(R.string.cont))
                    }
                }
            }
        },
        sheetElevation = 0.dp,
        sheetBackgroundColor = Color.Transparent,
//        sheetPeekHeight = 56.dp,
        backgroundColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Icon(
                    modifier = Modifier
                        .padding(start = 32.dp, end = 32.dp, top = 32.dp)
                        .size(48.dp),
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = "cloud service",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    text = stringResource(R.string.setup_cloud_service),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_cloud_service_text_a),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_cloud_service_text_b),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                when (cloudService) {
                    GoogleDriveUtil.SERVICE_CODE -> {
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
                                            FaIcon(
                                                faIcon = FaIcons.GoogleDrive,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column() {
                                        Text(
                                            text = stringResource(id = R.string.cloud_service_gd),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        LinearProgressIndicator(
                                            progress = 0.6f,
                                            modifier = Modifier.padding(vertical = 4.dp)
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
                                    })
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = stringResource(id = R.string.cloud_service_not_connected),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                                text = stringResource(R.string.cloud_service_des),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
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
            }
            item {
                if(cloudService != 0){
                    Text(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        text = stringResource(id = R.string.setup_cloud_service_text_b),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}