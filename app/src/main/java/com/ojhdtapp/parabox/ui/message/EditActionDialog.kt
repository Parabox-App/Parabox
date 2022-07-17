package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
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
                            SmallTopAppBar(
                                title = { Text(text = "会话信息") },
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
                                colors = smallTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
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
                                    modifier = Modifier.padding(bottom = 4.dp),
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
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            innerTextField()
                                        },
                                        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary),
                                        enabled = isEditing
                                    )
                                }
                                Text(
                                    text = contact?.latestMessage?.timestamp?.toDescriptiveTime()
                                        ?.let { "最近一次发言于$it" } ?: "无最近发言记录",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Crossfade(targetState = isEditing) {
                                if (it) {
                                    OutlinedButton(onClick = { isEditing = !isEditing }) {
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
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = "标签",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
//                        item {
//                            val focusRequester = remember { FocusRequester() }
//                            val focusManager = LocalFocusManager.current
//                            Row(
//                                modifier = Modifier.padding(horizontal = 16.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .size(48.dp)
//                                        .clip(CircleShape)
//                                        .background(MaterialTheme.colorScheme.primary)
//                                        .clickable {
//                                            shouldShowAvatarSelector = !shouldShowAvatarSelector
//                                        })
//                                Spacer(modifier = Modifier.width(16.dp))
//                                OutlinedTextField(
//                                    modifier = Modifier.weight(1f),
//                                    value = name, onValueChange = {
//                                        name = it
//                                        nameError = false
//                                    },
//                                    label = { Text(text = "会话名称") },
//                                    isError = nameError,
//                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
//                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
//                                    singleLine = true,
//                                    trailingIcon = {
//                                        var expanded by remember {
//                                            mutableStateOf(false)
//                                        }
//                                        Box(
//                                            modifier = Modifier
//                                                .wrapContentSize(Alignment.TopEnd)
//                                        ) {
//
//                                            IconButton(onClick = { expanded = !expanded }) {
//                                                Crossfade(targetState = expanded) {
//                                                    if (it) {
//                                                        Icon(
//                                                            imageVector = Icons.Outlined.ExpandLess,
//                                                            contentDescription = "Shrink"
//                                                        )
//                                                    } else {
//                                                        Icon(
//                                                            imageVector = Icons.Outlined.ExpandMore,
//                                                            contentDescription = "Expand"
//                                                        )
//                                                    }
//                                                }
//                                            }
//                                            DropdownMenu(
//                                                expanded = expanded,
//                                                onDismissRequest = { expanded = false }) {
//                                                contact?.profile?.name?.let {
//                                                    DropdownMenuItem(
//                                                        text = { Text(text = it) },
//                                                        onClick = {
//                                                            name = it
//                                                            expanded = false
//                                                        })
//                                                }
//                                            }
//                                        }
//                                    })
//                            }
//                        }
//                        item {
//                            Text(modifier = Modifier.padding(horizontal = 16.dp) ,text = "标签", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
//                        }
//                        item {
//                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
//                        }
//                        item {
//                            Text(modifier = Modifier.padding(horizontal = 16.dp) ,text = "配置项", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
//                        }
                }
            }
        }
    }
}