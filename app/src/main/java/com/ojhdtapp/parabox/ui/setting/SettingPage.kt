package com.ojhdtapp.parabox.ui.setting

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainScreenSharedViewModel
import com.ojhdtapp.parabox.ui.util.SettingNavGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@SettingNavGraph(start = true)
@Composable
fun SettingPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    sharedViewModel: MainScreenSharedViewModel
) {
    Scaffold(modifier = modifier,
        bottomBar = {
            com.ojhdtapp.parabox.ui.util.NavigationBar(
                navController = navController,
                messageBadge = sharedViewModel.messageBadge.value,
                onSelfItemClick = {})
        }) {
        LazyColumn(contentPadding = it) {

        }
    }
}