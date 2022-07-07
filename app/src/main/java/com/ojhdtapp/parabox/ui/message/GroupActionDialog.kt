package com.ojhdtapp.parabox.ui.message

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.PluginConnection

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    state: GroupInfoState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(modifier = modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp)) {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(title = { Text(text = "编组会话") },
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
                                    onClick = onConfirm,
                                    enabled = state.state == GroupInfoState.SUCCESS
                                ) {
                                    Text(text = "保存")
                                }
                            })
                    }
                ) {
                    when (state.state) {
                        GroupInfoState.LOADING -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        GroupInfoState.ERROR -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = state.message!!)
                        }
                        GroupInfoState.SUCCESS -> GroupEditForm(
                            paddingValues = it,
                            resource = state.resource!!
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditForm(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    resource: GroupEditResource
) {
    var name by remember {
        mutableStateOf("")
    }
    var selectedPluginConnection = remember {
        mutableStateListOf<PluginConnection>()
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
//        item{
//            Text(text = "基本信息", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
//        }
        item {
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            BackHandler(enabled = true) {
                focusManager.clearFocus()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { })
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = name, onValueChange = {
                        name = it
                    },
                    label = { Text(text = "会话名称") },
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
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
                                resource.name.forEach {
                                    DropdownMenuItem(text = { Text(text = it) }, onClick = {
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
            OutlinedCard() {
                Column() {
                    resource.pluginConnections.forEach { conn ->
                        var checked by remember {
                            mutableStateOf(false)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clickable { checked = !checked }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    checked = !checked
//                                    if (it) {
//                                        selectedPluginConnection.add(conn)
//                                    } else {
//                                        selectedPluginConnection.remove(conn)
//                                    }
                                })
                            Text(text = "${conn.connectionType} - ${conn.objectId}")
                        }
                    }
                }
            }
        }
    }
}
