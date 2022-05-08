package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MessagePage(modifier: Modifier = Modifier, onConnectBtnClicked : () -> Unit) {
    val viewModel: MessagePageViewModel = hiltViewModel()
    val text by remember {
        mutableStateOf("Text")
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                onConnectBtnClicked()
            }, enabled = viewModel.pluginInstalledState.value) {
                Text(text = "Connect")
            }
            Text(text = text)
        }

    }
}