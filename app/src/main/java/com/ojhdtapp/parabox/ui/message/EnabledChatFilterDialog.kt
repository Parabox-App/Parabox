package com.ojhdtapp.parabox.ui.message

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnabledChatFilterDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    enabledList: List<GetChatFilter>,
    onConfirm: (List<GetChatFilter>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (openDialog) {
        val openCustomTagFilterDialog by remember {
            mutableStateOf(false)
        }
        if(openCustomTagFilterDialog){

        }
        AlertDialog(onDismissRequest = { /*TODO*/ }) {

        }
    }

}

