package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

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
        val selectedList = remember {
            mutableStateListOf<GetChatFilter>()
        }
        LaunchedEffect(Unit){
            selectedList.addAll(enabledList)
        }
        if (openCustomTagFilterDialog) {

        }
        AlertDialog(
            onDismissRequest = { /*TODO*/ },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "This area typically contains the supportive text " +
                                "which presents the details regarding the Dialog's purpose.",
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                onDismiss()
                            },
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                onConfirm(selectedList)
                            },
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }

}

