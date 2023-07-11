package com.ojhdtapp.parabox.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.parabox.ui.common.ContactNavGraph
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@ContactNavGraph(start = true)
@Composable
fun ContactPage(
    modifier: Modifier = Modifier
){
    val viewModel = hiltViewModel<ContactPageViewModel>()
    Scaffold() {
        LazyColumn(contentPadding = it){

        }
    }
}