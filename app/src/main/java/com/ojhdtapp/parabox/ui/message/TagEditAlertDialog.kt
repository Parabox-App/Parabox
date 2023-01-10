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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import com.ojhdtapp.parabox.ui.util.MyFilterChip
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
    val context = LocalContext.current
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
            title = { Text(text = stringResource(R.string.add_tag)) },
            text = {
                Column() {
                    val hashTagLazyListState = rememberLazyListState()
                    val hashTagFocusRequester = remember { FocusRequester() }
                    val hashTagInteraction = remember { MutableInteractionSource() }
                    val rowInteraction = remember { MutableInteractionSource() }
                    Text(
                        modifier = Modifier,
                        text = stringResource(id = R.string.current_tags),
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
                                val values = FormUtil.splitTwoSpacesOrNewLine(it)

                                if (values.size >= 2) {
                                    onConfirmDelete = false
                                    if (!FormUtil.checkTagMinimumCharacter(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_too_short)
                                        hashTagShouldShowError = true
                                    } else if (!FormUtil.checkTagMaximumCharacter(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_too_long)
                                        hashTagShouldShowError = true
                                    } else if (hashTagList.contains(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_duplicate)
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
                            placeHolder = stringResource(R.string.hash_tag_empty),
                            placeHolderWhenEnabled = stringResource(R.string.hash_tag_placeholder_short),
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
                            onConfirmDelete = onConfirmDelete,
                            chipContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                    Text(
                        modifier = Modifier,
                        text = stringResource(R.string.common_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf(
                            context.getString(R.string.tag_example_work),
                            context.getString(R.string.tag_example_class),
                            context.getString(R.string.tag_example_family),
                            context.getString(R.string.tag_example_notification),
                            context.getString(R.string.tag_example_classmate))) {
                            MyFilterChip(
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
                                        hashTagError = context.getString(R.string.hash_tag_error_duplicate)
                                        hashTagShouldShowError = true
                                    }
                                },
                                label = { Text(text = it) },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
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
                    Text(text = stringResource(R.string.save_and_exit))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}