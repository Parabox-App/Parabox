package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagEditAlertDialog(
    modifier: Modifier = Modifier, showDialog: Boolean,
    contact: Contact?,
    sizeClass: WindowSizeClass,
    onConfirm: (id: Long, tags: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        val coroutineScope = rememberCoroutineScope()
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
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.NewLabel,
                    contentDescription = "new_label"
                )
            },
            title = { Text(text = "添加标签") },
            text = {
                Column() {
                    val hashTagLazyListState = rememberLazyListState()
                    val hashTagFocusRequester = remember { FocusRequester() }
                    val hashTagInteraction = remember { MutableInteractionSource() }
                    val rowInteraction = remember { MutableInteractionSource() }
                    Text(
                        modifier = Modifier,
                        text = "当前标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
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
                            placeHolderWhenEnabled = "请输入标签后敲击空格或换行符",
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
                            padding = HashTagEditor.PADDING_NONE,
                            onConfirmDelete = onConfirmDelete
                        )
                    }
                    Text(
                        modifier = Modifier,
                        text = "常用标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("工作", "课程", "家庭", "通知", "同学")) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (!hashTagList.contains(it)) {
                                        hashTagList.add(it)
                                        coroutineScope.launch {
                                            delay(100)
                                            if (hashTagList.isNotEmpty())
                                                hashTagLazyListState.animateScrollToItem(hashTagList.lastIndex)
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
            },
            confirmButton = {
                TextButton(onClick = {
                    contact?.let {
                        onConfirm(it.contactId, hashTagList.toList())
                    }
                }) {
                    Text(text = "保存并退出")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "取消")
                }
            }
        )
    }
}