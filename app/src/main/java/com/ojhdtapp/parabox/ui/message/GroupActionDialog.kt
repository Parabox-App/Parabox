package com.ojhdtapp.parabox.ui.message

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.service.PluginService
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    state: GroupInfoState,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
    onConfirm: (name: String, pluginConnections: List<PluginConnection>, senderId: Long, avatar: String?, avatarUri: String?, tags: List<String>) -> Unit
) {
    val context = LocalContext.current
    if (showDialog) {
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

        var selectedAvatar by remember {
            mutableStateOf<String?>(null)
        }

        var selectedLocalAvatar by remember {
            mutableStateOf<Uri?>(null)
        }

        var selectedTags = remember {
            mutableStateListOf<String>()
        }

        Dialog(
            onDismissRequest = {
                name = ""
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
//                usePlatformDefaultWidth = sizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                usePlatformDefaultWidth = false
            )
        ) {
            val horizontalPadding = when (sizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                WindowWidthSizeClass.Expanded -> 0.dp
                else -> 16.dp
            }
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Surface(
                modifier = modifier
                    .widthIn(0.dp, 580.dp)
                    .padding(horizontal = horizontalPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = { Text(text = "编组会话") },
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
                                    if (selectedPluginConnection.isEmpty()) {
                                        pluginConnectionNotSelectedError = true
                                    }
                                    if (name.isNotBlank() && selectedPluginConnection.isNotEmpty() && selectedSenderId != null) {
                                        onConfirm(
                                            name,
                                            selectedPluginConnection.toList(),
                                            selectedSenderId!!,
                                            selectedAvatar,
                                            selectedLocalAvatar?.let { it1 ->
                                                FileUtil.getUriByCopyingFileToPath(
                                                    context,
                                                    context.getExternalFilesDir("chat")!!,
                                                    "Avatar_${name.replace("\\s+", "_")}.png",
                                                    it1
                                                )?.toString()
                                            },
                                            selectedTags.toList()
                                        )
                                    }
                                },
                                enabled = state.state == GroupInfoState.SUCCESS
                            ) {
                                Text(text = "保存")
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                5.dp
                            )
                        )
                    )
                    when (state.state) {
                        GroupInfoState.LOADING -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        GroupInfoState.ERROR -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = state.message!!)
                        }
                        GroupInfoState.SUCCESS -> GroupEditForm(
                            paddingValues = PaddingValues(0.dp),
                            resource = state.resource!!,
                            name = name,
                            nameError = nameError,
                            shouldShowAvatarSelector = shouldShowAvatarSelector,
                            selectedPluginConnection = selectedPluginConnection,
                            pluginConnectionNotSelectedError = pluginConnectionNotSelectedError,
                            selectedSenderId = selectedSenderId,
                            selectedAvatar = selectedAvatar,
                            selectedLocalAvatar = selectedLocalAvatar,
                            selectedTags = selectedTags,
                            onNameChange = {
                                name = it
                                nameError = false
                            },
                            onAvatarSelectorTrigger = { shouldShowAvatarSelector = it },
                            onSelectedPluginConnectionAdd = { selectedPluginConnection.add(it) },
                            onSelectedPluginConnectionRemove = { selectedPluginConnection.remove(it) },
                            onSelectedSenderIdChange = { selectedSenderId = it },
                            onSelectedAvatarChange = {
                                selectedAvatar = it
                                selectedLocalAvatar = null
                            },
                            onSelectedLocalAvatarChange = {
                                selectedAvatar = null
                                selectedLocalAvatar = it
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    selectedAvatar: String?,
    selectedLocalAvatar: Uri?,
    selectedTags: SnapshotStateList<String>,
    onNameChange: (value: String) -> Unit,
    onAvatarSelectorTrigger: (value: Boolean) -> Unit,
    onSelectedPluginConnectionAdd: (target: PluginConnection) -> Unit,
    onSelectedPluginConnectionRemove: (target: PluginConnection) -> Unit,
    onSelectedSenderIdChange: (value: Long) -> Unit,
    onSelectedAvatarChange: (value: String) -> Unit,
    onSelectedLocalAvatarChange: (value: Uri) -> Unit
) {

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val inputService = LocalTextInputService.current
    LaunchedEffect(true) {
        delay(300)
        inputService?.showSoftwareKeyboard()
        focusRequester.requestFocus()
    }
    var expanded by remember {
        mutableStateOf(false)
    }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
            if (it != null) {
                onSelectedLocalAvatarChange(it)
            }
        }
//    val imagePickerSLauncher =
//        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
//            if (it != null) {
//                onSelectedLocalAvatarChange(it)
//            }
//        }
    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            onAvatarSelectorTrigger(!shouldShowAvatarSelector)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedLocalAvatar ?: selectedAvatar)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                            .build(),
                        contentDescription = "avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                ExposedDropdownMenuBox(
                    modifier = Modifier.weight(1f),
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        modifier = Modifier
                            .focusRequester(focusRequester),
                        value = name, onValueChange = onNameChange,
                        label = { Text(text = "会话名称") },
                        isError = nameError,
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    if (resource.name.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            resource.name.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        onNameChange(selectionOption)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = nameError,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
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
                enter = expandVertically(),
                exit = shrinkVertically()
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
                                        onSelectedAvatarChange(it)
                                    }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        resource.avatarUri.forEach {
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
                                        onSelectedLocalAvatarChange(it)
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
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
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
            val coroutineScope = rememberCoroutineScope()
            val hashTagLazyListState = rememberLazyListState()
            val hashTagFocusRequester = remember { FocusRequester() }
            val hashTagInteraction = remember { MutableInteractionSource() }
            var hashTagText by remember {
                mutableStateOf("")
            }
            var hashTagError by remember {
                mutableStateOf<String>("")
            }
            var hashTagShouldShowError by remember {
                mutableStateOf(false)
            }
            var onConfirmDelete by remember {
                mutableStateOf(false)
            }
            val rowInteraction = remember { MutableInteractionSource() }
            Card(shape = RoundedCornerShape(24.dp)) {
                Column() {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "快速标签",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        text = "标签可帮助快速筛选，定位会话。\n可稍后再作更改。允许留空。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "当前标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HashTagEditor(
                        textFieldValue = hashTagText,
                        enabled = true,
                        onValueChanged = {
                            val values = FormUtil.splitPerSpaceOrNewLine(it)

                            if (values.size >= 2) {
                                onConfirmDelete = false
                                if (!FormUtil.checkTagMinimumCharacter(values[0])) {
                                    hashTagError = "标签应至少包含两个字符"
                                    hashTagShouldShowError = true
                                } else if (!FormUtil.checkTagMaximumCharacter(values[0])) {
                                    hashTagError = "标签长度不应超过50"
                                    hashTagShouldShowError = true
                                } else if (selectedTags.contains(values[0])) {
                                    hashTagError = "该标签已存在"
                                    hashTagShouldShowError = true
                                } else {
                                    hashTagShouldShowError = false
                                }

                                if (!hashTagShouldShowError) {
                                    selectedTags.add(values[0])
                                    hashTagText = ""
                                }
                            } else {
                                hashTagText = it
                            }
                        },
                        placeHolder = "暂无标签",
                        placeHolderWhenEnabled = "请输入标签后敲击空格或换行符",
                        lazyListState = hashTagLazyListState,
                        focusRequester = hashTagFocusRequester,
                        textFieldInteraction = hashTagInteraction,
                        rowInteraction = rowInteraction,
                        errorMessage = hashTagError,
                        shouldShowError = hashTagShouldShowError,
                        listOfChips = selectedTags,
                        selectedListOfChips = null,
                        innerModifier = Modifier.onKeyEvent {
                            if (it.key.keyCode == Key.Backspace.keyCode && hashTagText.isBlank()) {
                                if (onConfirmDelete) {
                                    selectedTags.removeLastOrNull()
                                    onConfirmDelete = false
                                } else {
                                    onConfirmDelete = true
                                }
                            }
                            false
                        },
                        onChipClick = {},
                        onChipClickWhenEnabled = { chipIndex ->
                            if (selectedTags.isNotEmpty()) {
                                selectedTags.removeAt(chipIndex)
                            }
                        },
                        padding = HashTagEditor.PADDING_SMALL,
                        onConfirmDelete = onConfirmDelete
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = "常用标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(listOf("工作", "课程", "家庭", "通知", "同学")) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (!selectedTags.contains(it)) {
                                        selectedTags.add(it)
                                        coroutineScope.launch {
                                            delay(100)
                                            if (selectedTags.isNotEmpty())
                                                hashTagLazyListState.animateScrollToItem(
                                                    selectedTags.lastIndex
                                                )
                                        }
                                    } else {
                                        hashTagError = "该标签已存在"
                                        hashTagShouldShowError = true
                                    }
                                },
                                label = { Text(text = it) })
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
                        val connectionName by remember {
                            mutableStateOf(PluginService.queryPluginConnectionName(conn.connectionType))
                        }
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
                            Text(text = "$connectionName - ${conn.id}")
                        }
                    }
                    AnimatedVisibility(
                        visible = pluginConnectionNotSelectedError,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
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
                        val connectionName by remember {
                            mutableStateOf(PluginService.queryPluginConnectionName(conn.connectionType))
                        }
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
                            Text(text = "$connectionName - ${conn.id}")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
