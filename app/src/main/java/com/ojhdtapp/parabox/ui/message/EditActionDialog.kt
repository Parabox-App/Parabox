package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.domain.model.Contact

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    contact: Contact,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var name by remember {
        mutableStateOf(contact.profile.name)
    }
    var nameError by remember {
        mutableStateOf(false)
    }

    var shouldShowAvatarSelector by remember {
        mutableStateOf(false)
    }
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            )
        ) {
            Surface(modifier = modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp)) {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(title = { Text(text = "编辑会话") },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "close"
                                    )
                                }
                            },
                            actions = {
                                TextButton(
                                    onClick = {
                                        if (name.isBlank()) {
                                            nameError = true
                                        }
                                        if (name.isNotBlank()) {
                                            onConfirm()
                                        }
                                    },
                                    enabled = true
                                ) {
                                    Text(text = "保存")
                                }
                            })
                    }
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = it
                    ) {

                    }
                }
            }
        }
    }
}