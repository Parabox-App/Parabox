package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HashTagEditor(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    textFieldValue: String,
    onValueChanged: (String) -> Unit,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    textFieldInteraction: MutableInteractionSource,
    readOnly: Boolean = false,
    message: String = "",
    errorMessage: String = "",
    shouldShowError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Default
    ),
    innerModifier: Modifier = Modifier,
    rowInteraction: MutableInteractionSource,
    listOfChips: SnapshotStateList<String>,
    onChipClick: (Int) -> Unit,
    isCompact: Boolean = true,
    onConfirmDelete: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val keyboardManager = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                indication = null,
                interactionSource = rowInteraction,
                onClick = {
                    focusRequester.requestFocus()
                    keyboardManager?.show()
                }
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.Center
            ) {

                TextFieldContent(
                    enabled = enabled,
                    textFieldValue = textFieldValue,
                    onValueChanged = onValueChanged,
                    lazyListState = lazyListState,
                    focusRequester = focusRequester,
                    textFieldInteraction = textFieldInteraction,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    focusManager = focusManager,
                    listOfChips = listOfChips,
                    innerModifier = innerModifier,
                    emphasizePlaceHolder = false,
                    onChipClick = onChipClick,
                    isCompact = isCompact,
                    onConfirmDelete = onConfirmDelete,
                )
            }

            ErrorSection(
                modifier = Modifier.padding(
                    horizontal = if (isCompact) 16.dp else 32.dp,
                ),
                message = message,
                errorMessage = errorMessage,
                shouldShowError = shouldShowError
            )
        }
    }
}

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun TextFieldContent(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    textFieldValue: String,
    onValueChanged: (String) -> Unit,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    textFieldInteraction: MutableInteractionSource,
    readOnly: Boolean,
    keyboardOptions: KeyboardOptions,
    focusManager: FocusManager,
    listOfChips: SnapshotStateList<String>,
    emphasizePlaceHolder: Boolean = false,
    innerModifier: Modifier,
    onChipClick: (Int) -> Unit,
    isCompact: Boolean,
    onConfirmDelete: Boolean,
) {
    Box {
        val isFocused = textFieldInteraction.collectIsFocusedAsState()
        val borderWidth = animateDpAsState(targetValue = if (enabled) 2.dp else 0.dp)
        val coroutineScope = rememberCoroutineScope()
        if (textFieldValue.isEmpty() && listOfChips.isEmpty()) {
            Text(
                modifier = Modifier
                    .padding(
                        horizontal = if (isCompact) 16.dp else 32.dp,
                    )
                    .align(alignment = Alignment.CenterStart),
                text = if (enabled) "要添加标签，请于此处输入后敲击空格或换行符" else "暂无标签",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
//                .border(
//                    width = borderWidth.value,
//                    brush = SolidColor(
//                        MaterialTheme.colorScheme.primary
//                    ),
//                    shape = RoundedCornerShape(4.dp)
//                )
            ,
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = if (isCompact) 16.dp else 32.dp,
                end = 64.dp
            )
        ) {
            itemsIndexed(items = listOfChips, key = { index, item -> item }) { index, item ->
                FilterChip(modifier = Modifier.animateItemPlacement(),
                    onClick = { if (enabled) onChipClick(index) },
                    selected = index == listOfChips.lastIndex && onConfirmDelete,
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = enabled,
                            enter = expandHorizontally(),
                            exit = shrinkHorizontally()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "close",
                                modifier = Modifier.size(ChipDefaults.LeadingIconSize)
                            )
                        }
                    },
                    label = { Text(text = item) })
            }
            item {
                BasicTextField(
                    enabled = enabled,
                    value = textFieldValue,
                    onValueChange = {
                        onValueChanged(it)
                        if (listOfChips.isNotEmpty()) {
                            coroutineScope.launch {
                                delay(200)
                                lazyListState.animateScrollToItem(listOfChips.lastIndex)
                            }
                        }
                    },
                    modifier = innerModifier
                        .focusRequester(focusRequester)
                        .width(IntrinsicSize.Min),
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier.wrapContentWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 4.dp)
                                        .wrapContentWidth(),
                                ) {
                                    innerTextField()
                                }
                            }
                        }
                    },
                    interactionSource = textFieldInteraction,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ErrorSection(
    modifier: Modifier = Modifier,
    message: String,
    errorMessage: String,
    shouldShowError: Boolean,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = shouldShowError,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .wrapContentHeight()
//    ) {
//
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .wrapContentHeight(),
//            horizontalAlignment = Alignment.End
//        ) {
//
//            if (message != null) {
//                Text(
//                    text = message,
//                    fontStyle = FontStyle.Italic,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//            if (errorMessage != null) {
//                AssistChip(onClick = {},
//                    colors = AssistChipDefaults.assistChipColors(
//                        containerColor = MaterialTheme.colorScheme.errorContainer,
//                        labelColor = MaterialTheme.colorScheme.onErrorContainer
//                    ),
//                    leadingIcon = {
//                        Icon(imageVector = Icons.Outlined.Error, contentDescription = "error")
//                    },
//                    label = { Text(text = errorMessage) })
//            }
//        }
//    }
}