package com.ojhdtapp.parabox.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TextInputDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    title: String? = null,
    description: String? = null,
    defaultValue: String? = null,
    placeholder: String? = null,
    label: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    errMsg: String? = null,
    onConfirm: (value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (openDialog) {
        var input by remember {
            mutableStateOf(defaultValue ?: "")
        }
        AlertDialog(
            title = {
                if (title != null) {
                    Text(text = title)
                }
            },
            text = {
                Column {
                    if (description != null) {
                        Text(text = description)
                    }
                    TextField(
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .clearFocusOnKeyboardDismiss(),
                        value = input,
                        onValueChange = {
                            input = it
                        },
                        label = {
                            if (label != null) {
                                Text(text = label)
                            }
                        },
                        placeholder = {
                            if (placeholder != null) {
                                Text(text = placeholder)
                            }
                        },
                        isError = errMsg?.isNotEmpty() == true,
                        supportingText = {
                            AnimatedVisibility(
                                visible = errMsg?.isNotEmpty() == true,
                            ) {
                                Text(text = errMsg ?: "errMsg", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onConfirm(input)
                            }
                        ),
                        singleLine = true
                    )
                }
            },
            onDismissRequest = {
                onDismiss()
            },
            confirmButton = {
                TextButton(onClick = {
                    keyboardController?.hide()
                    onConfirm(input)
                }) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboardController?.hide()
                    onDismiss()
                }) {
                    Text(text = "取消")
                }
            }
        )
    }
}