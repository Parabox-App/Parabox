package com.ojhdtapp.parabox.ui.message

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    state: GroupInfoState,
    onDismiss: () -> Unit,
    onConfirm: (name: String, pluginConnections: List<PluginConnection>, senderId: Long) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var nameError by remember {
        mutableStateOf(false)
    }

    var shouldShowAvatarSelector by remember {
        mutableStateOf(false)
    }

    val selectedPluginConnection = remember(state) {
        mutableStateListOf<PluginConnection>().apply {
            state.resource?.let { addAll(it.pluginConnections) }
        }
    }
    var pluginConnectionNotSelectedError by remember {
        mutableStateOf(false)
    }

    var selectedSenderId by remember(state) {
        mutableStateOf(state.resource?.pluginConnections?.firstOrNull()?.objectId)
    }
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
                                    onClick = {
                                        if (name.isBlank()) {
                                            nameError = true
                                        }
                                        if (selectedPluginConnection.isEmpty()) {
                                            pluginConnectionNotSelectedError = true
                                        }
                                        if (name.isNotBlank() && selectedPluginConnection.isNotEmpty() && selectedSenderId != null) {
                                            onConfirm(
                                                name,
                                                selectedPluginConnection.toList(),
                                                selectedSenderId!!
                                            )
                                        }
                                    },
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
                            resource = state.resource!!,
                            name = name,
                            nameError = nameError,
                            shouldShowAvatarSelector = shouldShowAvatarSelector,
                            selectedPluginConnection = selectedPluginConnection,
                            pluginConnectionNotSelectedError = pluginConnectionNotSelectedError,
                            selectedSenderId = selectedSenderId,
                            onNameChange = {
                                name = it
                                nameError = false
                            },
                            onAvatarSelectorTrigger = { shouldShowAvatarSelector = it },
                            onSelectedPluginConnectionAdd = { selectedPluginConnection.add(it) },
                            onSelectedPluginConnectionRemove = { selectedPluginConnection.remove(it) },
                            onSelectedSenderIdChange = { selectedSenderId = it }
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
    resource: GroupEditResource,
    name: String,
    nameError: Boolean,
    shouldShowAvatarSelector: Boolean,
    selectedPluginConnection: List<PluginConnection>,
    pluginConnectionNotSelectedError: Boolean,
    selectedSenderId: Long?,
    onNameChange: (value: String) -> Unit,
    onAvatarSelectorTrigger: (value: Boolean) -> Unit,
    onSelectedPluginConnectionAdd: (target: PluginConnection) -> Unit,
    onSelectedPluginConnectionRemove: (target: PluginConnection) -> Unit,
    onSelectedSenderIdChange: (value: Long) -> Unit
) {

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
                        .clickable {
                            onAvatarSelectorTrigger(!shouldShowAvatarSelector)
                        })
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = name, onValueChange = {
                        onNameChange(it)
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
                                resource.name.forEach {
                                    DropdownMenuItem(text = { Text(text = it) }, onClick = {
                                        onNameChange(it)
                                        expanded = false
                                    })
                                }
                            }
                        }
                    })
            }
            AnimatedVisibility(visible = nameError) {
                Text(
                    modifier = Modifier.padding(start = 64.dp),
                    text = "请输入会话名称",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = shouldShowAvatarSelector,
//                enter = slideInVertically(),
//                exit = slideOutVertically()
            ) {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        resource.avatar.forEach {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(it)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar Selection",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onAvatarSelectorTrigger(false)
                                    }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                }, contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add Avatar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column() {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "消息源",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        text = "以下列出可用消息来源。\n勾选以将该来源应用于新建会话。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    resource.pluginConnections.forEach { conn ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedPluginConnection.contains(conn)) {
                                    onSelectedPluginConnectionRemove(conn)
                                } else {
                                    onSelectedPluginConnectionAdd(conn)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedPluginConnection.contains(conn),
                                onCheckedChange = {
                                    if (selectedPluginConnection.contains(conn)) {
                                        onSelectedPluginConnectionRemove(conn)
                                    } else {
                                        onSelectedPluginConnectionAdd(conn)
                                    }
                                })
                            Text(text = "${conn.connectionType} - ${conn.objectId}")
                        }
                    }
                    AnimatedVisibility(visible = pluginConnectionNotSelectedError) {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "请至少保留一个消息源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column() {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "默认发送出口",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        text = "以下列出可用消息发送出口，消息将默认尝试从该出口发送。\n该选项可稍后再作更改。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    resource.pluginConnections.forEach { conn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedSenderId == conn.objectId,
                                    onClick = {
                                        onSelectedSenderIdChange(conn.objectId)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier.padding(12.dp),
                                selected = selectedSenderId == conn.objectId,
                                onClick = null
                            )
                            Text(text = "${conn.connectionType} - ${conn.objectId}")
                        }
                    }
                }
            }
        }
    }
}
