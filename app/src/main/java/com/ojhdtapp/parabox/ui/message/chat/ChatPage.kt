package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DismissibleBottomSheet
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawer
import com.ojhdtapp.parabox.ui.common.MyModalNavigationDrawerReverse
import com.ojhdtapp.parabox.ui.common.rememberMyDrawerState
import com.ojhdtapp.parabox.ui.message.MessageLayoutType
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    modifier: Modifier = Modifier,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    layoutType: MessageLayoutType,
    windowSize: WindowSizeClass,
) {
    val viewModel = hiltViewModel<MessagePageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current
    val state by viewModel.uiState.collectAsState()
    val sharedState by mainSharedViewModel.uiState.collectAsState()
    val sheetState = rememberMyDrawerState(initialValue = DrawerValue.Closed)
    val drawerState = rememberMyDrawerState(initialValue = DrawerValue.Closed)

    DismissibleBottomSheet(sheetContent = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.Red)
        )
    }, sheetHeight = 160.dp) {
        MyModalNavigationDrawerReverse(
            drawerContent = { Box(modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(Color.Green)) },
            drawerWidth = 360.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
            ) {
                Text(text = "HAHA")
            }
        }

    }
}