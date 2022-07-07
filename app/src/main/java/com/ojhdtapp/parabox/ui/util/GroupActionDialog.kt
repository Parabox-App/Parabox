package com.ojhdtapp.parabox.ui.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.GroupInfoPack

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    data: Resource<GroupInfoPack>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(modifier = modifier.fillMaxSize(), shape = RoundedCornerShape(16.dp)) {
                Scaffold(
                    topBar = {
                        SmallTopAppBar(title = { Text(text = "编组会话") },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "close"
                                    )
                                }
                            },
                            actions = {
                                TextButton(
                                    onClick = onConfirm,
                                    enabled = data is Resource.Success
                                ) {
                                    Text(text = "保存")
                                }
                            })
                    }
                ) {
                    when (data) {
                        is Resource.Loading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        is Resource.Error -> Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = data.message!!)
                        }
                        is Resource.Success -> GroupEditForm(paddingValues = it)
                    }

                }
            }
        }
    }
}

@Composable
fun GroupEditForm(modifier: Modifier = Modifier, paddingValues: PaddingValues) {
    LazyColumn(modifier = Modifier.padding(paddingValues)) {

    }
}
