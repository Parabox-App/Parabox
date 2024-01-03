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
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewLabel
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
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.common.MyFilterChip
import com.ojhdtapp.parabox.ui.common.clearFocusOnKeyboardDismiss

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnabledChatFilterDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    enabledList: List<ChatFilter>,
    onConfirm: (List<ChatFilter>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (openDialog) {


        var openCustomTagFilterDialog by remember {
            mutableStateOf(false)
        }
        val selectedList = remember {
            mutableStateListOf<ChatFilter>()
        }
        LaunchedEffect(Unit) {
            selectedList.addAll(enabledList)
        }
        NewChatFilterDialog(
            openDialog = openCustomTagFilterDialog,
            onConfirm = {
                selectedList.add(ChatFilter.Tag(it.trim()))
                openCustomTagFilterDialog = false
            },
            onDismiss = { openCustomTagFilterDialog = false }
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(selectedList)
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
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "enable filter"
                )
            },
            title = {
                Text(text = "编辑分组")
            },
            text = {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChatFilter.allFilterList.forEach {
                            MyFilterChip(
                                selected = it in selectedList,
                                label = { Text(text = stringResource(id = it.labelResId)) }) {
                                if (selectedList.contains(it)) {
                                    selectedList.remove(it)
                                } else {
                                    selectedList.add(it)
                                }
                            }
                        }
                        selectedList.filterIsInstance<ChatFilter.Tag>().forEach {
                            MyFilterChip(selected = true, label = { Text(text = it.label) }) {
                                selectedList.remove(it)
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
                            openCustomTagFilterDialog = true
                        }
                    }
                    Text(text = "选中标签将于主页顶部显示。")
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
fun NewChatFilterDialog(
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
                Text(text = "新增标签筛选")
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
                    Text(text = "新建自定义标签筛选，启用后仅命中该标签的会话被展示。")
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

