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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss

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
                                    Image(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape),
                                        painter = painterResource(id = R.drawable.avatar),
                                        contentDescription = "avatar"
                                    )
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
                                        onValueChange = { name = it },
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
                                    visible = isEditing,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Text(
                                        text = "点击编辑会话名",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = contact?.latestMessage?.timestamp?.toDescriptiveTime()
                                        ?.let { "最近一次发言于$it" } ?: "无最近发言记录",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Crossfade(targetState = isEditing) {
                                if (it) {
                                    OutlinedButton(onClick = { if(!isEditing) {
                                        isEditing = true
                                    }else{

                                    }
                                    }) {
                                        Icon(
                                            Icons.Outlined.Done,
                                            contentDescription = "done",
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
                    item {

                        val hashTagLazyListState = rememberLazyListState()
                        val hashTagFocusRequester = remember { FocusRequester() }
                        val hashTagInteraction = remember { MutableInteractionSource() }
                        val rowInteraction = remember { MutableInteractionSource() }
                        HashTagEditor(
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
                            isCompact = isCompact,
                            onConfirmDelete = onConfirmDelete
                        )
                    }
                }
            }
        }
    }
}

