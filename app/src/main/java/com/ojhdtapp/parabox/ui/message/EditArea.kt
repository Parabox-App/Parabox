package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ojhdtapp.parabox.ui.util.SearchAppBar
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class
)
@Composable
fun EditArea(
    modifier: Modifier = Modifier,
    onBottomSheetExpand: () -> Unit,
    onSend: (text: String) -> Unit,
    onTextFieldHeightChange: (height: Int) -> Unit
) {
    var audioState by remember {
        mutableStateOf(false)
    }
    var inputText by remember {
        mutableStateOf("")
    }
    var shouldToolbarShrink by remember {
        mutableStateOf(false)
    }
    Surface(
        modifier = modifier
//            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val relocation = remember { BringIntoViewRequester() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .bringIntoViewRequester(relocation)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                var isEditing by remember {
                    mutableStateOf(false)
                }
                Crossfade(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .animateContentSize(),
                    targetState = shouldToolbarShrink
                ) {
                    if (it) {
                        IconButton(onClick = { shouldToolbarShrink = false }) {
                            Icon(
                                imageVector = Icons.Outlined.NavigateNext,
                                contentDescription = "expand"
                            )
                        }
                    } else {
                        Row() {
                            IconButton(onClick = onBottomSheetExpand) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "more"
                                )
                            }
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "emoji"
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.zIndex(2f),
                        visible = audioState,
                        enter = expandHorizontally { 0 },
                        exit = shrinkHorizontally { 0 }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {}
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "长按录制", color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { audioState = false }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Keyboard,
                                        contentDescription = "keyboard",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            isEditing = true
                        }
                    ) {
                        val originalBoxHeight = with(LocalDensity.current) {
                            24.dp.toPx().toInt()
                        }
                        //temp
//                    val originalBoxHeight = 56
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .weight(1f)
                                    .padding(12.dp)
                                    .onSizeChanged { onTextFieldHeightChange(it.height - originalBoxHeight) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val scope = rememberCoroutineScope()
                                val focusRequester = remember { FocusRequester() }
                                LaunchedEffect(isEditing) {
                                    if (isEditing) focusRequester.requestFocus()
                                }
                                BasicTextField(
                                    modifier = Modifier
                                        .onFocusEvent {
                                            if (it.isFocused) scope.launch { delay(200); relocation.bringIntoView() }
                                        }
                                        .focusRequester(focusRequester)
                                        .clearFocusOnKeyboardDismiss() {
                                            isEditing = false
                                        },
                                    value = inputText,
                                    onValueChange = {
                                        if (it.length > 6) shouldToolbarShrink = true
                                        else if (it.isEmpty()) shouldToolbarShrink = false
                                        inputText = it
                                    },
                                    enabled = isEditing && !audioState,
                                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "输入内容",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    },
                                    cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
                                )
                            }
                            AnimatedVisibility(
                                visible = inputText.isEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    audioState = true
                                    isEditing = false
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardVoice,
                                        contentDescription = "voice"
                                    )
                                }
                            }
                        }
                    }

                }
                AnimatedVisibility(visible = inputText.isNotEmpty() && !audioState,
//                    enter = slideInHorizontally { width -> width },
//                    exit = slideOutHorizontally { width -> width }
                    enter = expandHorizontally() { width -> 0 },
                    exit = shrinkHorizontally() { width -> 0 }
                ) {
                    FloatingActionButton(
                        onClick = {
                            onSend(inputText)
                            inputText = ""
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = "send")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.padding(bottom = 8.dp),
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "相册", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.padding(bottom = 8.dp),
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "carema",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "拍摄", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.padding(bottom = 8.dp),
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = "file",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "文件", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            modifier = Modifier.padding(bottom = 8.dp),
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "位置", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}