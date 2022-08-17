package com.ojhdtapp.parabox.ui.file

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.destinations.ChatPageDestination
import com.ojhdtapp.parabox.ui.menu.MenuSharedViewModel
import com.ojhdtapp.parabox.ui.util.FileNavGraph
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.navigate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@FileNavGraph(start = true)
@Composable
fun FilePage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    drawerState: DrawerState
) {
    val viewModel: FilePageViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val snackBarHostState = remember { SnackbarHostState() }
    var searchBarState by remember {
        mutableStateOf(SearchAppBar.NONE)
    }
    val coroutineScope = rememberCoroutineScope()
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
                avatarUri = mainSharedViewModel.userAvatarFlow.collectAsState(initial = null).value,
                onActivateStateChanged = { searchBarState = it },
                sizeClass = sizeClass,
                onMenuClick = {
                    coroutineScope.launch {
                        drawerState.open()
                    }
                },
                onAvatarClick = {},
                onDropdownMenuItemEvent = {}
            )
        },
        bottomBar = {

        }) {
        LazyColumn(contentPadding = it, state = listState) {

        }
    }
}