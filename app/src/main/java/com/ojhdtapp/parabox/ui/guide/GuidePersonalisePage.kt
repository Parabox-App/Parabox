package com.ojhdtapp.parabox.ui.guide

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideTermsPageDestination
import com.ojhdtapp.parabox.ui.setting.EditUserNameDialog
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.common.GuideNavGraph
import com.ramcosta.composedestinations.annotation.Destination

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuidePersonalisePage(
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

    val userName =
        mainSharedViewModel.userNameFlow.collectAsState(initial = DataStoreKeys.DEFAULT_USER_NAME).value
    val avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value
    var showDialog by remember {
        mutableStateOf(false)
    }

    EditUserNameDialog(
        openDialog = showDialog,
        userName = userName,
        onConfirm = {
            showDialog = false
            mainSharedViewModel.setUserName(it)
        },
        onDismiss = { showDialog = false }
    )

    Column(modifier = Modifier.systemBarsPadding()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(32.dp)) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "personalise",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.setup_personalise_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(R.string.setup_personalise_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item{
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally){
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                AvatarUtil.getAvatar(
                                    context = context,
                                    uri = avatarUri?.let { Uri.parse(it) },
                                    url = null,
                                    name = null,
                                    backgroundColor = MaterialTheme.colorScheme.primary,
                                    textColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            )
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                            .build(),
                        contentDescription = "avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                onEvent(ActivityEvent.SetUserAvatar)
                            }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(shape = RoundedCornerShape(8.dp),onClick = { showDialog = true }){
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = userName, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = "edit", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
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
                Text(text = stringResource(R.string.back))
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                mainNavController.navigate(GuideTermsPageDestination)
            }) {
                Text(text = stringResource(id = R.string.cont))
            }
        }
    }
}

//    BottomSheetScaffold(
//        modifier = Modifier
//            .systemBarsPadding(),
//        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
//        sheetContent = {
//
//        },
//        sheetElevation = 0.dp,
//        sheetBackgroundColor = Color.Transparent,
////        sheetPeekHeight = 56.dp,
//        backgroundColor = Color.Transparent
//    ) { paddingValues ->
//
//    }