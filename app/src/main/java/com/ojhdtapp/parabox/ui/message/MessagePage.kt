package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MessagePage(
    modifier: Modifier = Modifier,
    onConnectBtnClicked: () -> Unit,
    onSendBtnClicked: () -> Unit
) {
    val viewModel: MessagePageViewModel = hiltViewModel()
    val scaffoldState = rememberScaffoldState()
    LaunchedEffect(true) {
        viewModel.uiEventFlow.collectLatest {
            when (it) {
                is MessagePageUiEvent.ShowSnackBar -> {
                    scaffoldState.snackbarHostState.showSnackbar(it.message)
                }
            }
        }
    }
    Scaffold(
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                onConnectBtnClicked()
            }, enabled = viewModel.pluginInstalledState.value) {
                Text(text = "Connect")
            }
            Button(
                onClick = { onSendBtnClicked() },
                enabled = viewModel.sendAvailableState.value
            ) {
                Text(text = "Send")
            }
            Text(text = viewModel.message.value)
        }
    }
}