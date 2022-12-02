package com.ojhdtapp.parabox.ui.setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.ojhdtapp.parabox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserNameDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    userName: String = "",
    onConfirm: (result: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (openDialog) {
        var editUserName by remember {
            mutableStateOf(userName)
        }
        var editUserNameError by remember {
            mutableStateOf(false)
        }
        AlertDialog(
            modifier = modifier,
            icon = {
                Icon(imageVector = Icons.Outlined.AccountCircle, contentDescription = "account")
            },
            title = { Text(text = stringResource(R.string.user_name_title)) },
            text = {
                OutlinedTextField(
//                    modifier = Modifier.fillMaxWidth(),
                    value = editUserName,
                    onValueChange = {
                        editUserNameError = false
                        editUserName = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.user_name)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editUserName.isNotBlank()) {
                                onConfirm(editUserName)
                            } else {
                                editUserNameError = true
                            }
                        }
                    ),
                    isError = editUserNameError
                )
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    if (editUserName.isNotBlank()) {
                        onConfirm(editUserName)
                    } else {
                        editUserNameError = true
                    }
                }) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editUserName = ""
                    editUserNameError = false
                    onDismiss()
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            })
    }
}