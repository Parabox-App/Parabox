package com.ojhdtapp.parabox.ui.contact

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState

@Composable
fun ContactDetailPage(
    viewModel: ContactPageViewModel,
    mainNavController: NavController,
    mainSharedState: MainSharedState,
    layoutType: ContactLayoutType,
    onMainSharedEvent: (MainSharedEvent) -> Unit
) {}