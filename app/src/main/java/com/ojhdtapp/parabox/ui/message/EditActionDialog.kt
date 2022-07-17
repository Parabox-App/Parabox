package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.domain.model.Contact

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    contact: Contact?,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        var name by remember {
            mutableStateOf(contact?.profile?.name ?: "")
        }
        var nameError by remember {
            mutableStateOf(false)
        }

        var shouldShowAvatarSelector by remember {
            mutableStateOf(false)
        }
        Dialog(
            onDismissRequest = {
                name = ""
                onDismiss()
            }, properties = DialogProperties(
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
                                    onClick = {
                                        name = ""
                                        onDismiss()
                                    }
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
                        item {
                            val focusRequester = remember { FocusRequester() }
                            val focusManager = LocalFocusManager.current
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable {
                                            shouldShowAvatarSelector = !shouldShowAvatarSelector
                                        })
                                Spacer(modifier = Modifier.width(16.dp))
                                OutlinedTextField(
                                    modifier = Modifier.weight(1f),
                                    value = name, onValueChange = {
                                        name = it
                                        nameError = false
                                    },
                                    label = { Text(text = "会话名称") },
                                    isError = nameError,
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                    singleLine = true,
                                    trailingIcon = {
                                        var expanded by remember {
                                            mutableStateOf(false)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .wrapContentSize(Alignment.TopEnd)
                                        ) {

                                            IconButton(onClick = { expanded = !expanded }) {
                                                Crossfade(targetState = expanded) {
                                                    if (it) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ExpandLess,
                                                            contentDescription = "Shrink"
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ExpandMore,
                                                            contentDescription = "Expand"
                                                        )
                                                    }
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }) {
                                                contact?.profile?.name?.let {
                                                    DropdownMenuItem(
                                                        text = { Text(text = it) },
                                                        onClick = {
                                                            name = it
                                                            expanded = false
                                                        })
                                                }
                                            }
                                        }
                                    })
                            }
                        }
                        item {
                            Text(modifier = Modifier.padding(horizontal = 16.dp) ,text = "标签", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        item {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        item {
                            Text(modifier = Modifier.padding(horizontal = 16.dp) ,text = "配置项", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}