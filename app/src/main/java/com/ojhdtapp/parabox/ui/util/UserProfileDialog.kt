package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.ui.message.GroupInfoState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun UserProfileDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    userName: String = "",
    avatarUri: String? = null,
    sizeClass: WindowSizeClass,
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
                usePlatformDefaultWidth = false,
            )
        ) {
            val horizontalPadding = when(sizeClass.widthSizeClass){
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                WindowWidthSizeClass.Expanded -> 0.dp
                else -> 16.dp
            }
            Surface(modifier = modifier.widthIn(0.dp, 580.dp).padding(horizontal = horizontalPadding).animateContentSize(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
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