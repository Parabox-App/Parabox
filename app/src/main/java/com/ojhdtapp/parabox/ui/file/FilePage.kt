package com.ojhdtapp.parabox.ui.file

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.util.NavigationBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController
) {
    Scaffold(modifier = modifier,
        bottomBar = { NavigationBar(navController = navController) }) {
        LazyColumn(contentPadding = it) {

        }
    }
}