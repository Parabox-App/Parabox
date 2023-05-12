package com.ojhdtapp.parabox.ui.guide

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.GuideFinishPageDestination
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.common.GuideNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Destination
@GuideNavGraph(start = false)
@Composable
fun GuideTermsPage(
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

    val listState = rememberLazyListState()
    var reachedBottom by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(listState.canScrollForward) {
        if(!listState.canScrollForward){
            reachedBottom = true
        }
    }

    Column(modifier = Modifier.systemBarsPadding()){
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(32.dp)) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = Icons.Outlined.Gavel,
                        contentDescription = "terms",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.terms),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item {
                Text(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    text = stringResource(id = R.string.terms_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                if (reachedBottom) {
                    mainNavController.navigate(GuideFinishPageDestination)
                } else {
                    coroutineScope.launch {
                        listState.animateScrollBy(1000f)
                    }
                }
            }) {
                if(reachedBottom){
                    Text(text = stringResource(R.string.agree_and_save))
                }else{
                    Text(text = stringResource(R.string.more))
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
}