package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

object SearchAppBar {
    const val NONE = 0
    const val SEARCH = 1
    const val SELECT = 2
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchAppBar(
    modifier: Modifier = Modifier,
    activateState: Int = SearchAppBar.NONE,
    onActivateStateChanged: (value: Int) -> Unit,
    text: String,
    onTextChange: (text: String) -> Unit,
    placeholder: String,
    selectedNum: String = "0"
) {
    val isActivated = activateState != SearchAppBar.NONE
    val statusBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(activateState) {
        if (activateState == SearchAppBar.SEARCH) focusRequester.requestFocus()
        else keyboardController?.hide()
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                statusBarHeight + 64.dp
            )
            .padding(
                PaddingValues(
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(
                        targetValue = if (isActivated) 0.dp else 8.dp + statusBarHeight
                    ).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 16.dp).value,
                    animateDpAsState(targetValue = if (isActivated) 0.dp else 8.dp).value
                )
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(animateIntAsState(targetValue = if (isActivated) 0 else 50).value))
                .clickable {
                    if (activateState == SearchAppBar.NONE)
                        onActivateStateChanged(SearchAppBar.SEARCH)
                },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                if (activateState == SearchAppBar.SELECT) {
                    SelectContentField(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        isActivated = isActivated,
                        onActivateStateChanged = onActivateStateChanged,
                        selectedNum = selectedNum
                    )
                } else {
                    SearchContentField(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        isActivated = isActivated,
                        onActivateStateChanged = onActivateStateChanged,
                        placeholder = placeholder,
                        focusRequester = focusRequester,
                        text = text,
                        onTextChange = onTextChange,
                        keyboardController = keyboardController
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchContentField(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivateStateChanged: (value: Int) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    text: String,
    onTextChange: (text: String) -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                onActivateStateChanged(
                    if (isActivated) SearchAppBar.NONE else SearchAppBar.SEARCH
                )
            },
        ) {
            Icon(
                imageVector = if (isActivated) Icons.Outlined.ArrowBack else Icons.Outlined.Search,
                contentDescription = "search",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            value = text,
            onValueChange = { onTextChange(it.trim()) },
            enabled = isActivated,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
        AnimatedVisibility(visible = !isActivated) {
            IconButton(onClick = { /*TODO*/ }) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectContentField(
    modifier: Modifier = Modifier,
    isActivated: Boolean,
    onActivateStateChanged: (value: Int) -> Unit,
    selectedNum: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onActivateStateChanged(SearchAppBar.NONE) },
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "close",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(targetState = selectedNum) { num ->
            Text(text = num, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /*TODO*/ }) {
            Icon(imageVector = Icons.Outlined.Group, contentDescription = "group")
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(imageVector = Icons.Outlined.Archive, contentDescription = "archive")
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "more")
        }
    }
}