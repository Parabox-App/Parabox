package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.common.MyFilterChip
import com.ojhdtapp.parabox.ui.common.clearFocusOnKeyboardDismiss

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditChatTagsDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    tags: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (openDialog) {


        var openCustomTagDialog by remember {
            mutableStateOf(false)
        }
        val tagList = remember {
            mutableStateListOf<String>()
        }
        LaunchedEffect(Unit) {
            tagList.addAll(tags)
        }
        NewChatTagDialog(
            openDialog = openCustomTagDialog,
            onConfirm = {
                tagList.add(it.trim())
                openCustomTagDialog = false
            },
            onDismiss = { openCustomTagDialog = false }
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(tagList)
                    },
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.NewLabel,
                    contentDescription = "new label"
                )
            },
            title = {
                Text(text = "快速添加标签")
            },
            text = {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tagList.forEach {
                            MyFilterChip(
                                selected = false,
                                label = { Text(text = it) },
                                trailingIcon = {
                                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "delete tag")
                                }
                            ) {
                                tagList.remove(it)
                            }
                        }
                        MyFilterChip(selected = false,
                            label = {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "new tag filter"
                                )
                            }
                        ) {
                            openCustomTagDialog = true
                        }
                    }
                    Text(text = "该会话已附加的标签，可手动增改。")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun NewChatTagDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    onConfirm: (text: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    if (openDialog) {
        var text by remember {
            mutableStateOf("")
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isBlank()) {
                            onDismiss()
                        } else {
                            onConfirm(text)
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.NewLabel,
                    contentDescription = "new label"
                )
            },
            title = {
                Text(text = "新增标签")
            },
            text = {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.clearFocusOnKeyboardDismiss(),
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("自定义标签") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (text.isBlank()) {
                                    onDismiss()
                                } else {
                                    onConfirm(text)
                                }
                            }
                        )
                    )
                    Text(text = "为该会话添加自定义标签，可用于筛选")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true
            ),
        )
    }
}

