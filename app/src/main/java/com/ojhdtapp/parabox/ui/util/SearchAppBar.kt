package com.ojhdtapp.parabox.ui.util

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchAppBar(
    modifier: Modifier = Modifier,
//    isActivated: Boolean = false,
    text: String,
    onTextChange: (text: String) -> Unit,
    placeholder: String,
) {
    var isActivated by remember {
        mutableStateOf(false)
    }
    val statusBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(isActivated) {
        if (isActivated) focusRequester.requestFocus()
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
                .clickable { isActivated = true },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animateDpAsState(targetValue = if (isActivated) 64.dp else 48.dp).value)
                        .align(Alignment.BottomCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isActivated = !isActivated },
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
        }
    }
}