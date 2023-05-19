package com.ojhdtapp.parabox.ui.contact

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
}