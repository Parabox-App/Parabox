package com.ojhdtapp.parabox.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ContactPickerDialog(
    modifier: Modifier = Modifier,
    openDialog: Boolean,
    data: List<Contact>,
    query: String,
    onQueryChange: (query: String) -> Unit,
    loadState: LoadState,
    onConfirm: (contact: Contact) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    if (openDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = modifier,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = true
            ),
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "联系人列表",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Surface(
                        shape = SearchBarDefaults.inputFieldShape,
                        tonalElevation = SearchBarDefaults.TonalElevation,
                        shadowElevation = SearchBarDefaults.ShadowElevation,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.height(SearchBarDefaults.InputFieldHeight),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(modifier = Modifier.padding(horizontal = 16.dp),imageVector = Icons.Outlined.Search, contentDescription = "search")
                            BasicTextField(
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .clearFocusOnKeyboardDismiss(),
                                value = query,
                                onValueChange = onQueryChange,
                                enabled = true,
                                textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "搜索会话",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                },
                                cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(0.dp, 400.dp)
                            .clip(MaterialTheme.shapes.extraLarge),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        when (loadState) {
                            LoadState.LOADING -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(176.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            LoadState.ERROR -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(176.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "发生错误")
                                    }
                                }
                            }

                            LoadState.SUCCESS -> {
                                item {
                                    if (data.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(176.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "暂无联系人")
                                        }
                                    }
                                }
                                items(items = data, key = { it.contactId }) {
                                    PickerItem(avatarModel = it.avatar.getModel(), title = it.name, onClick = {
                                        onConfirm(it)
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}