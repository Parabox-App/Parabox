package com.ojhdtapp.parabox.ui.file

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainScreenSharedViewModel
import com.ojhdtapp.parabox.ui.util.NavigationBar
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    navController: NavController,
    sharedViewModel: MainScreenSharedViewModel
) {
    val viewModel: FilePageViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    var searchBarState by remember {
        mutableStateOf(SearchAppBar.NONE)
    }
    LaunchedEffect(key1 = true) {
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is FilePageUiEvent.ShowSnackBar -> {
                    snackBarHostState.showSnackbar((it.message))
                }
            }
        }
    }
    Scaffold(modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            SearchAppBar(
                text = viewModel.searchText.value,
                onTextChange = viewModel::setSearchText,
                placeholder = "搜索文件",
                activateState = searchBarState,
                onActivateStateChanged = { searchBarState = it }
            )
        },
        bottomBar = {
            NavigationBar(
                navController = navController,
                messageBadge = sharedViewModel.messageBadge.value,
                onSelfItemClick = {
                })
        }) {
        LazyColumn(contentPadding = it, state = listState) {

        }
    }
}