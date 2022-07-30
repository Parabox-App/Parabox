package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object HashTagEditor {
    const val PADDING_NONE = 0
    const val PADDING_SMALL = 1
    const val PAdding_MEDIUM = 2
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HashTagEditor(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    textFieldValue: String,
    onValueChanged: (String) -> Unit,
    placeHolder: String = "",
    placeHolderWhenEnabled: String = "",
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    textFieldInteraction: MutableInteractionSource,
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
    selectedListOfChips: SnapshotStateList<String>?,
    onChipClick: (Int) -> Unit,
    onChipClickWhenEnabled: (Int) -> Unit,
    padding: Int = HashTagEditor.PADDING_SMALL,
    onConfirmDelete: Boolean = false,
    stickyChips: @Composable() (RowScope.() -> Unit)? = null
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
                    placeHolder = placeHolder,
                    placeHolderWhenEnabled = placeHolderWhenEnabled,
                    lazyListState = lazyListState,
                    focusRequester = focusRequester,
                    textFieldInteraction = textFieldInteraction,
                    keyboardOptions = keyboardOptions,
                    focusManager = focusManager,
                    listOfChips = listOfChips,
                    selectedListOfChips = selectedListOfChips,
                    innerModifier = innerModifier,
                    onChipClick = onChipClick,
                    onChipClickWhenEnabled = onChipClickWhenEnabled,
                    padding = padding,
                    onConfirmDelete = onConfirmDelete,
                    stickyChips = stickyChips
                )
            }

            ErrorSection(
                modifier = Modifier.padding(
                    horizontal = when (padding) {
                        HashTagEditor.PADDING_NONE -> 0.dp
                        HashTagEditor.PADDING_SMALL -> 16.dp
                        HashTagEditor.PAdding_MEDIUM -> 32.dp
                        else -> 0.dp
                    }
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
    placeHolder: String,
    placeHolderWhenEnabled: String,
    lazyListState: LazyListState,
    focusRequester: FocusRequester,
    textFieldInteraction: MutableInteractionSource,
    keyboardOptions: KeyboardOptions,
    focusManager: FocusManager,
    listOfChips: SnapshotStateList<String>,
    selectedListOfChips: SnapshotStateList<String>?,
    innerModifier: Modifier,
    onChipClick: (Int) -> Unit,
    onChipClickWhenEnabled: (Int) -> Unit,
    padding: Int,
    onConfirmDelete: Boolean,
    stickyChips: @Composable() (RowScope.() -> Unit)?,
) {
    Box {
        val isFocused = textFieldInteraction.collectIsFocusedAsState()
        val coroutineScope = rememberCoroutineScope()

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
                start = when (padding) {
                    HashTagEditor.PADDING_NONE -> 0.dp
                    HashTagEditor.PADDING_SMALL -> 16.dp
                    HashTagEditor.PAdding_MEDIUM -> 32.dp
                    else -> 0.dp
                },
                end = 0.dp
            )
        ) {
            stickyChips?.also {
                item {
                    Row() {
                        it()
                    }
                }
            }
            itemsIndexed(items = listOfChips, key = { index, item -> item }) { index, item ->
                FilterChip(modifier = Modifier
                    .animateItemPlacement()
                    .animateContentSize(),
                    onClick = { if (enabled) onChipClickWhenEnabled(index) else onChipClick(index) },
                    selected = if (enabled) (index == listOfChips.lastIndex && onConfirmDelete) else selectedListOfChips?.contains(
                        item
                    ) ?: false,
                    trailingIcon = {
//                        AnimatedVisibility(
//                            visible = enabled,
//                            enter = expandHorizontally(),
//                            exit = shrinkHorizontally()
//                        ) {
//                            Icon(
//                                imageVector = Icons.Outlined.Close,
//                                contentDescription = "close",
//                                modifier = Modifier.size(ChipDefaults.LeadingIconSize)
//                            )
//                        }
                        if (enabled) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "close",
                                modifier = Modifier.size(ChipDefaults.LeadingIconSize)
                            )
                        }
                    },
                    leadingIcon = {
                        if (!enabled && selectedListOfChips?.contains(item) == true) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = "",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
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
                                if (listOfChips.isNotEmpty())
                                    lazyListState.animateScrollToItem(listOfChips.lastIndex)
                            }
                        }
                    },
                    modifier = innerModifier
                        .focusRequester(focusRequester)
//                        .width(IntrinsicSize.Min)
                    ,
                    singleLine = false,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        if (textFieldValue.isEmpty() && listOfChips.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .align(alignment = Alignment.CenterStart),
                                text = if (enabled) placeHolderWhenEnabled else placeHolder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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