package com.ojhdtapp.parabox.ui.guide

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.guru.fontawesomecomposelib.FaIcon
import com.guru.fontawesomecomposelib.FaIcons
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.HyperlinkText
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideCloudPageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.common.*
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideFCMReceiverPage(
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

    var currentStep by rememberSaveable {
        mutableStateOf(1)
    }

    val fcmEnabled by viewModel.enableFCMStateFlow.collectAsState(initial = false)
    val token by viewModel.fcmTokenFlow.collectAsState(initial = "")
    val loopbackToken by viewModel.fcmLoopbackTokenFlow.collectAsState(initial = "")

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

    val cloudService by viewModel.cloudServiceFlow.collectAsState(initial = 0)

    val cloudStorage =
        viewModel.fcmCloudStorageFlow.collectAsState(initial = FcmConstants.CloudStorage.NONE.ordinal)

    val tencentCOSSecretId = viewModel.tencentCOSSecretIdFlow.collectAsState(initial = "")
    val tencentCOSSecretKey = viewModel.tencentCOSSecretKeyFlow.collectAsState(initial = "")
    val tencentCOSBucket = viewModel.tencentCOSBucketFlow.collectAsState(initial = "")
    val tencentCOSRegion = viewModel.tencentCOSRegionFlow.collectAsState(initial = "")

    val qiniuKODOAccessKey = viewModel.qiniuKODOAccessKeyFlow.collectAsState(initial = "")
    val qiniuKODOSecretKey = viewModel.qiniuKODOSecretKeyFlow.collectAsState(initial = "")
    val qiniuKODOBucket = viewModel.qiniuKODOBucketFlow.collectAsState(initial = "")
    val qiniuKODODomain = viewModel.qiniuKODODomainFlow.collectAsState(initial = "")

    Column(modifier = Modifier.systemBarsPadding()){
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Icon(
                    modifier = Modifier
                        .padding(start = 32.dp, end = 32.dp, top = 32.dp)
                        .size(48.dp),
                    imageVector = Icons.Outlined.Reply,
                    contentDescription = "fcm receive",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    text = stringResource(R.string.setup_fcm_receiver_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_fcm_receiver_text_a),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_fcm_receiver_text_b),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_fcm_receiver_text_c),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                SwitchPreference(
                    title = stringResource(R.string.enable),
                    checked = fcmEnabled,
                    onCheckedChange = viewModel::setEnableFCM,
                    enabled = token.isNotBlank(),
                    horizontalPadding = 32.dp
                )
            }
            if (token.isBlank()) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.padding(start = 32.dp),
                            text = stringResource(R.string.fcm_unavailable),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { onEvent(ActivityEvent.QueryFCMToken) }) {
                            Text(text = stringResource(R.string.refresh))
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = fcmEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1
                        Row {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "1", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_fcm_receiver_step_a_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 1) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.setup_fcm_receiver_step_a_text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row {
                                            Button(onClick = { currentStep = 2 }) {
                                                Text(text = stringResource(R.string.next_step))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2
                        Row {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "2", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_fcm_receiver_step_b_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 2) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = stringResource(R.string.setup_fcm_receiver_step_b_text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                if (token.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(token))
                                                    coroutineScope.launch {
                                                        snackBarHostState.showSnackbar(
                                                            context.getString(
                                                                R.string.save_to_clipboard
                                                            )
                                                        )
                                                    }
                                                }
                                            }) {
                                            Icon(
                                                modifier = Modifier.size(18.dp),
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "copy to clipboard"
                                            )
                                            Text(
                                                modifier = Modifier.padding(start = 8.dp),
                                                text = stringResource(R.string.copy_to_clipboard)
                                            )
                                        }
                                        Row {
                                            OutlinedButton(onClick = { currentStep = 1 }) {
                                                Text(text = stringResource(R.string.last_step))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                currentStep = 3
                                            }) {
                                                Text(text = stringResource(R.string.next_step))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 3
                        Row {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "3", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_fcm_receiver_step_c_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 3) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        var tempToken by remember {
                                            mutableStateOf(loopbackToken)
                                        }
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = stringResource(R.string.setup_fcm_receiver_step_c_text),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        OutlinedTextField(
                                            value = tempToken, onValueChange = { tempToken = it },
                                            label = { Text(text = stringResource(id = R.string.fcm_token)) },
                                        )
                                        Row {
                                            OutlinedButton(onClick = { currentStep = 2 }) {
                                                Text(text = stringResource(R.string.last_step))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                currentStep = 4
                                                viewModel.setFcmLoopbackToken(tempToken)
                                            }) {
                                                Text(text = stringResource(R.string.next_step))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 4
                        Row {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "4", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_fcm_receiver_step_d_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 4) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        var selectedTagIndex by remember {
                                            mutableStateOf(0)
                                        }
                                        var tempTencentCOSSecretId by remember {
                                            mutableStateOf(tencentCOSSecretId.value)
                                        }
                                        var tempTencentCOSSecretKey by remember {
                                            mutableStateOf(tencentCOSSecretKey.value)
                                        }
                                        var tempTencentCOSRegion by remember {
                                            mutableStateOf(tencentCOSRegion.value)
                                        }
                                        var tempTencentCOSBucket by remember {
                                            mutableStateOf(tencentCOSBucket.value)
                                        }
                                        var tempQiniuKODOAccessKey by remember {
                                            mutableStateOf(qiniuKODOAccessKey.value)
                                        }
                                        var tempQiniuKODOSecretKey by remember {
                                            mutableStateOf(qiniuKODOSecretKey.value)
                                        }
                                        var tempQiniuKODOBucket by remember {
                                            mutableStateOf(qiniuKODOBucket.value)
                                        }
                                        var tempQiniuKODODomain by remember {
                                            mutableStateOf(qiniuKODODomain.value)
                                        }
                                        val cloudStorageText by remember {
                                            derivedStateOf {
                                                when (cloudStorage.value) {
                                                    FcmConstants.CloudStorage.NONE.ordinal -> context.getString(
                                                        R.string.not_set
                                                    )
                                                    FcmConstants.CloudStorage.TENCENT_COS.ordinal -> context.getString(R.string.fcm_cloud_storage_tencent_cos)
                                                    FcmConstants.CloudStorage.QINIU_KODO.ordinal -> context.getString(R.string.fcm_cloud_storage_qiniu_kodo)
                                                    FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal -> context.getString(R.string.fcm_cloud_storage_google_drive)
                                                    else -> context.getString(R.string.not_set)
                                                }
                                            }
                                        }
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = stringResource(R.string.setup_fcm_receiver_step_d_text_a),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = stringResource(R.string.setup_fcm_receiver_step_d_text_b, cloudStorageText),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        // Tab
                                        ScrollableTabRow(selectedTabIndex = selectedTagIndex) {
                                            Tab(
                                                selected = selectedTagIndex == 0,
                                                onClick = { selectedTagIndex = 0 }) {
                                                Text(
                                                    text = stringResource(R.string.fcm_cloud_storage_tencent_cos),
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    color = if (selectedTagIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.6f
                                                    )
                                                )
                                            }
                                            Tab(
                                                selected = selectedTagIndex == 1,
                                                onClick = { selectedTagIndex = 1 }) {
                                                Text(
                                                    text = stringResource(R.string.fcm_cloud_storage_qiniu_kodo),
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    color = if (selectedTagIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.6f
                                                    )
                                                )
                                            }
                                            Tab(
                                                selected = selectedTagIndex == 2,
                                                onClick = { selectedTagIndex = 2 }) {
                                                Text(
                                                    text = stringResource(R.string.fcm_cloud_storage_google_drive),
                                                    modifier = Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    color = if (selectedTagIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.6f
                                                    )
                                                )
                                            }
                                        }
                                        AnimatedContent(
                                            modifier = Modifier.fillMaxWidth(),
                                            targetState = selectedTagIndex,
                                            transitionSpec = {
                                                if (targetState > initialState) {
                                                    slideInHorizontally(
                                                        initialOffsetX = { it },
                                                        animationSpec = tween(300)
                                                    ) + fadeIn(animationSpec = tween(300)) with slideOutHorizontally(
                                                        targetOffsetX = { -it },
                                                        animationSpec = tween(300)
                                                    ) + fadeOut(animationSpec = tween(300))
                                                } else {
                                                    slideInHorizontally(
                                                        initialOffsetX = { -it },
                                                        animationSpec = tween(300)
                                                    ) + fadeIn(animationSpec = tween(300)) with slideOutHorizontally(
                                                        targetOffsetX = { it },
                                                        animationSpec = tween(300)
                                                    ) + fadeOut(animationSpec = tween(300))
                                                }
                                            }
                                        ) { index ->
                                            when (index) {
                                                0 -> {
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.fcm_cloud_storage_temp_folder_notice),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        OutlinedTextField(
                                                            value = tempTencentCOSSecretId,
                                                            onValueChange = {
                                                                tempTencentCOSSecretId = it
                                                            },
                                                            label = { Text(text = "SecretId") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_tencent_cos_secretid_supporting_text)
                                                            ) },
                                                        )
                                                        OutlinedTextField(
                                                            value = tempTencentCOSSecretKey,
                                                            onValueChange = {
                                                                tempTencentCOSSecretKey = it
                                                            },
                                                            label = { Text(text = "SecretKey") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_tencent_cos_secretkey_supporting_text)
                                                            ) },
                                                        )
                                                        OutlinedTextField(
                                                            value = tempTencentCOSRegion,
                                                            onValueChange = {
                                                                tempTencentCOSRegion = it
                                                            },
                                                            label = { Text(text = "Region") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_tencent_cos_region_supporting_text)
                                                            ) }
                                                        )
                                                        OutlinedTextField(
                                                            value = tempTencentCOSBucket,
                                                            onValueChange = {
                                                                tempTencentCOSBucket = it
                                                            },
                                                            label = { Text(text = "Bucket") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_tencent_cos_bucket_supporting_text)
                                                            ) },
                                                        )
                                                    }
                                                }
                                                1 -> {
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.fcm_cloud_storage_temp_folder_notice),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        OutlinedTextField(
                                                            value = tempQiniuKODOAccessKey,
                                                            onValueChange = {
                                                                tempQiniuKODOAccessKey = it
                                                            },
                                                            label = { Text(text = "AccessKey") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_qiniu_kodo_accesskey_supporting_text)
                                                            ) },
                                                        )
                                                        OutlinedTextField(
                                                            value = tempQiniuKODOSecretKey,
                                                            onValueChange = {
                                                                tempQiniuKODOSecretKey = it
                                                            },
                                                            label = { Text(text = "SecretKey") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_qiniu_kodo_secretkey_supporting_text)
                                                            ) },
                                                        )
                                                        OutlinedTextField(
                                                            value = tempQiniuKODOBucket,
                                                            onValueChange = {
                                                                tempQiniuKODOBucket = it
                                                            },
                                                            label = { Text(text = "Bucket") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_qiniu_kodo_bucket_supporting_text)
                                                            ) },
                                                        )
                                                        OutlinedTextField(
                                                            value = tempQiniuKODODomain,
                                                            onValueChange = {
                                                                tempQiniuKODODomain = it
                                                            },
                                                            label = { Text(text = "Domain") },
                                                            singleLine = true,
                                                            supportingText = { Text(text = stringResource(
                                                                R.string.fcm_cloud_storage_qiniu_kodo_domain_supporting_text)
                                                            ) },
                                                        )
                                                    }
                                                }
                                                2 -> {
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.fcm_cloud_storage_temp_folder_notice),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        if(cloudService == GoogleDriveUtil.SERVICE_CODE){
                                                            Text(
                                                                text = stringResource(R.string.google_drive_connected),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }else{
                                                            FilledTonalButton(
                                                                onClick = {
                                                                    val signInIntent =
                                                                        (context as MainActivity).getGoogleLoginAuth().signInIntent
                                                                    gDriveLauncher.launch(signInIntent)
                                                                }) {
                                                                FaIcon(
                                                                    faIcon = FaIcons.GoogleDrive,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    size = ButtonDefaults.IconSize,
                                                                )
                                                                Text(
                                                                    modifier = Modifier.padding(start = 8.dp),
                                                                    text = stringResource(R.string.connect_to_google_drive)
                                                                )
                                                            }
                                                        }
                                                    }

                                                }
                                            }
                                        }
                                        Row {
                                            OutlinedButton(onClick = { currentStep = 3 }) {
                                                Text(text = stringResource(R.string.last_step))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = {
                                                when (selectedTagIndex) {
                                                    0 -> {
                                                        if (tempTencentCOSSecretId.isNotBlank() && tempTencentCOSSecretKey.isNotBlank() && tempTencentCOSRegion.isNotBlank() && tempTencentCOSBucket.isNotBlank()) {
                                                            viewModel.setTencentCOSSecretId(
                                                                tempTencentCOSSecretId
                                                            )
                                                            viewModel.setTencentCOSSecretKey(
                                                                tempTencentCOSSecretKey
                                                            )
                                                            viewModel.setTencentCOSRegion(
                                                                tempTencentCOSRegion
                                                            )
                                                            viewModel.setTencentCOSBucket(
                                                                tempTencentCOSBucket
                                                            )
                                                            viewModel.setFCMCloudStorage(
                                                                FcmConstants.CloudStorage.TENCENT_COS.ordinal
                                                            )
                                                        }
                                                    }
                                                    1 -> {
                                                        if (tempQiniuKODOAccessKey.isNotBlank() && tempQiniuKODOSecretKey.isNotBlank() && tempQiniuKODOBucket.isNotBlank() && tempQiniuKODODomain.isNotBlank()) {
                                                            viewModel.setQiniuKODOAccessKey(
                                                                tempQiniuKODOAccessKey
                                                            )
                                                            viewModel.setQiniuKODOSecretKey(
                                                                tempQiniuKODOSecretKey
                                                            )
                                                            viewModel.setQiniuKODOBucket(
                                                                tempQiniuKODOBucket
                                                            )
                                                            viewModel.setQiniuKODODomain(
                                                                tempQiniuKODODomain
                                                            )
                                                            viewModel.setFCMCloudStorage(
                                                                FcmConstants.CloudStorage.QINIU_KODO.ordinal
                                                            )
                                                        }
                                                    }
                                                    2 -> {
                                                        viewModel.setFCMCloudStorage(
                                                            FcmConstants.CloudStorage.GOOGLE_DRIVE.ordinal
                                                        )
                                                    }
                                                }
                                                currentStep = 5
                                            }) {
                                                if ((selectedTagIndex == 0 && tempTencentCOSSecretId.isNotBlank() && tempTencentCOSSecretKey.isNotBlank() && tempTencentCOSRegion.isNotBlank() && tempTencentCOSBucket.isNotBlank())
                                                    || (selectedTagIndex == 1 && tempQiniuKODOAccessKey.isNotBlank() && tempQiniuKODOSecretKey.isNotBlank() && tempQiniuKODOBucket.isNotBlank() && tempQiniuKODODomain.isNotBlank())
                                                    || (selectedTagIndex == 2 && cloudService == GoogleDriveUtil.SERVICE_CODE)
                                                ) {
                                                    Text(text = stringResource(R.string.save_and_enable))
                                                } else {
                                                    Text(text = stringResource(R.string.later))
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 5
                        Row {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary
                            ) {
                                Box(
                                    modifier = Modifier.size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "5", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_fcm_receiver_step_e_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AnimatedVisibility(visible = currentStep == 5) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            text = stringResource(R.string.setup_fcm_receiver_step_e_text_a),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        HyperlinkText(
                                            fullText = stringResource(R.string.setup_fcm_receiver_step_e_text_b),
                                            hyperLinks = mapOf(stringResource(id = R.string.documentation) to "https://docs.parabox.ojhdt.dev/"),
                                            linkTextColor = MaterialTheme.colorScheme.primary,
                                            textStyle = MaterialTheme.typography.bodyMedium,
                                            textColor = MaterialTheme.colorScheme.onSurface

                                        )
                                        Row {
                                            OutlinedButton(onClick = { currentStep = 4 }) {
                                                Text(text = stringResource(R.string.last_step))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
                Text(text = stringResource(id = R.string.back))
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    mainNavController.navigate(GuideCloudPageDestination)
                },
                enabled = fcmEnabled && loopbackToken.isNotBlank()
            ) {
                if (fcmEnabled && loopbackToken.isNotBlank()) {
                    Text(text = stringResource(id = R.string.cont))
                } else {
                    Text(text = stringResource(id = R.string.unfinished))
                }
            }
        }
    }
}