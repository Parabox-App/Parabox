package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    contact: Contact?,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onEvent: (event: EditActionDialogEvent) -> Unit
) {
    if (showDialog) {
        val isCompact = sizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        var name by remember {
            mutableStateOf(contact?.profile?.name ?: "")
        }
        var nameError by remember {
            mutableStateOf(false)
        }

        var isEditing by remember {
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
            Surface(
                modifier = modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp)
            ) {
                var hashTagText by remember {
                    mutableStateOf("")
                }
                var hashTagError by remember {
                    mutableStateOf<String>("")
                }
                var hashTagShouldShowError by remember {
                    mutableStateOf(false)
                }
                val hashTagList = remember {
                    mutableStateListOf<String>()
                }
                var onConfirmDelete by remember {
                    mutableStateOf(false)
                }
                LaunchedEffect(key1 = true, block = {
                    contact?.tags?.also {
                        hashTagList.addAll(it)
                    }
                })
                LazyColumn(
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .height(276.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            bottomStart = 24.dp,
                                            bottomEnd = 24.dp
                                        )
                                    ),
                                painter = painterResource(id = R.drawable.bg),
                                contentDescription = "background",
                                contentScale = ContentScale.Crop
                            )
//                            SmallTopAppBar(
//                                title = { Text(text = "会话信息") },
//                                navigationIcon = {
//                                    IconButton(
//                                        onClick = {
//                                            name = ""
//                                            onDismiss()
//                                        }
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Outlined.Close,
//                                            contentDescription = "close"
//                                        )
//                                    }
//                                },
//                                colors = smallTopAppBarColors(
//                                    containerColor = Color.Transparent
//                                )
//                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .width(if (isCompact) 84.dp else 96.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(contact?.profile?.avatar ?: R.drawable.avatar)
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                            .build(),
                                        contentDescription = "avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                    )
//                                    Image(
//                                        modifier = Modifier
//                                            .size(64.dp)
//                                            .clip(CircleShape),
//                                        painter = painterResource(id = R.drawable.avatar),
//                                        contentDescription = "avatar"
//                                    )
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isCompact) 16.dp else 32.dp,
                                    vertical = 8.dp
                                )
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clearFocusOnKeyboardDismiss(),
                                        value = name,
                                        onValueChange = {
                                            nameError = false
                                            name = it
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.merge(
                                            TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (name.isEmpty()) {
                                                Text(
                                                    text = "会话名",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            }
                                            innerTextField()
                                        },
                                        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary),
                                        enabled = isEditing
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isEditing || nameError,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    if (nameError) {
                                        Text(
                                            text = "会话名不可为空",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = "点击编辑会话名",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = contact?.latestMessage?.timestamp?.toDescriptiveTime()
                                        ?.let { "最近一次发言于$it" } ?: "无最近发言记录",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCompact) {
                                Crossfade(targetState = isEditing) {
                                    if (it) {
                                        FloatingActionButton(
                                            onClick = {
                                                if (name.isEmpty()) {
                                                    nameError = true
                                                } else {
                                                    contact?.also {
                                                        onEvent(
                                                            EditActionDialogEvent.ProfileAndTagUpdate(
                                                                contactId = it.contactId,
                                                                profile = Profile(
                                                                    name = name,
                                                                    avatar = null
                                                                ),
                                                                tags = hashTagList.toList()
                                                            )
                                                        )
                                                        isEditing = false
                                                    }
                                                }
                                            },
                                            elevation = FloatingActionButtonDefaults.elevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Outlined.Save,
                                                contentDescription = "save",
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        FloatingActionButton(
                                            onClick = { isEditing = !isEditing },
                                            elevation = FloatingActionButtonDefaults.elevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "edit",
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                    }
                                }
                            } else {
                                Crossfade(targetState = isEditing) {
                                    if (it) {
                                        OutlinedButton(onClick = {
                                            if (name.isEmpty()) {
                                                nameError = true
                                            } else {
                                                contact?.also {
                                                    onEvent(
                                                        EditActionDialogEvent.ProfileAndTagUpdate(
                                                            contactId = it.contactId,
                                                            profile = Profile(
                                                                name = name,
                                                                avatar = null
                                                            ),
                                                            tags = hashTagList.toList()
                                                        )
                                                    )
                                                    isEditing = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                Icons.Outlined.Save,
                                                contentDescription = "save",
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text("保存更改")
                                        }
                                    } else {
                                        Button(onClick = { isEditing = !isEditing }) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "edit",
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text("编辑信息")
                                        }

                                    }
                                }
                            }
                        }
                    }
                    item {

                        val hashTagLazyListState = rememberLazyListState()
                        val hashTagFocusRequester = remember { FocusRequester() }
                        val hashTagInteraction = remember { MutableInteractionSource() }
                        val rowInteraction = remember { MutableInteractionSource() }
                        HashTagEditor(
                            modifier = Modifier.padding(bottom = 8.dp),
                            textFieldValue = hashTagText,
                            enabled = isEditing,
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
                                    } else if (hashTagList.contains(values[0])) {
                                        hashTagError = "该标签已存在"
                                        hashTagShouldShowError = true
                                    } else {
                                        hashTagShouldShowError = false
                                    }

                                    if (!hashTagShouldShowError) {
                                        hashTagList.add(values[0])
                                        hashTagText = ""
                                    }
                                } else {
                                    hashTagText = it
                                }
                            },
                            placeHolder = "暂无标签",
                            placeHolderWhenEnabled = "要添加标签，请于此处输入后敲击空格或换行符",
                            lazyListState = hashTagLazyListState,
                            focusRequester = hashTagFocusRequester,
                            textFieldInteraction = hashTagInteraction,
                            rowInteraction = rowInteraction,
                            errorMessage = hashTagError,
                            shouldShowError = hashTagShouldShowError,
                            listOfChips = hashTagList,
                            selectedListOfChips = null,
                            innerModifier = Modifier.onKeyEvent {
                                if (it.key.keyCode == Key.Backspace.keyCode && hashTagText.isBlank()) {
                                    if (onConfirmDelete) {
                                        hashTagList.removeLastOrNull()
                                        onConfirmDelete = false
                                    } else {
                                        onConfirmDelete = true
                                    }
                                }
                                false
                            },
                            onChipClick = {},
                            onChipClickWhenEnabled = { chipIndex ->
                                if (hashTagList.isNotEmpty()) {
                                    hashTagList.removeAt(chipIndex)
                                }
                            },
                            padding = if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) HashTagEditor.PADDING_SMALL else HashTagEditor.PAdding_MEDIUM,
                            onConfirmDelete = onConfirmDelete
                        )
                    }
                    item {
                        Divider(
                            modifier = Modifier.padding(horizontal = if (isCompact) 16.dp else 32.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    item {
                        SwitchPreference(
                            title = "新消息通知",
                            subtitleOn = "开启",
                            subtitleOff = "你将不会收到新消息通知",
                            initialChecked = contact?.enableNotifications ?: false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.EnableNotificationStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                    item {
                        SwitchPreference(
                            title = "置顶聊天",
                            subtitleOn = "该聊天将始终固定于列表顶部",
                            subtitleOff = "不置顶该聊天",
                            initialChecked = contact?.isPinned ?: false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.PinnedStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                    item {
                        SwitchPreference(
                            title = "归档",
                            subtitleOn = "归档后，你将不会收到新消息通知",
                            subtitleOff = "禁用",
                            initialChecked = contact?.isArchived ?: false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.ArchivedStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                }
            }
        }
    }
}

