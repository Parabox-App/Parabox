package com.ojhdtapp.parabox.ui.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.ui.message.GroupInfoState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    userName: String = "",
    avatarUri: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (openDialog) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        SmallTopAppBar(title = {},
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "close"
                                    )
                                }
                            })
                    }
                }
            }
        }
    }
}