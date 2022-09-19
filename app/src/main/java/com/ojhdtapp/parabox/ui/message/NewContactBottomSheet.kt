package com.ojhdtapp.parabox.ui.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.SegmentedControl
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewContactBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState,
    sizeClass: WindowSizeClass,
    mainSharedViewModel: MainSharedViewModel,
    content: (@Composable () -> Unit)
) = ModalBottomSheetLayout(
    modifier = modifier,
    sheetState = sheetState,
//    sheetShape = RoundedCornerShape(24.dp),
    sheetBackgroundColor = Color.Transparent,
    sheetElevation = 0.dp,
    sheetContent = {
        val coroutineScope = rememberCoroutineScope()
        // Plugin List State
        val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
        var selectedPluginId by remember {
            mutableStateOf<Int?>(null)
        }
        var idInput by remember {
            mutableStateOf("")
        }
        val paddingModifier = if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            Modifier
        } else {
            Modifier.padding(start = 56.dp, end = 56.dp, top = 72.dp)
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = paddingModifier,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(640.dp)
                        .padding(horizontal = 32.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.size(32.dp, 4.dp))
                    }
                    Text(text = "发起会话", style = MaterialTheme.typography.headlineSmall)
                    SegmentedControl(
                        modifier = Modifier.padding(vertical = 16.dp),
                        items = listOf("私人会话", "群聊")
                    ) {

                    }
                    Row(
                        modifier = Modifier
                            .width(320.dp)
                            .height(56.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ManageAccounts,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                        if (pluginList.isEmpty()) {
                            Text(
                                text = "暂无可用发送出口",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                pluginList.forEach {
                                    FilterChip(
                                        modifier = Modifier
                                            .animateContentSize(),
                                        onClick = {
                                            selectedPluginId = it.connectionType
                                        },
                                        selected = selectedPluginId == it.connectionType,
                                        leadingIcon = {
                                            if (selectedPluginId == it.connectionType) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Done,
                                                    contentDescription = "",
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = it.icon,
                                                    contentDescription = "icon",
                                                    modifier = Modifier
                                                        .size(FilterChipDefaults.IconSize)
                                                        .clip(
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        },
                                        label = { Text(text = it.name) },
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outline.copy(
                                                alpha = 0.4f
                                            )
                                        )
                                    )
                                }

                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        modifier = Modifier
                            .width(320.dp)
                            .clearFocusOnKeyboardDismiss(),
                        value = idInput,
                        onValueChange = { idInput = it },
                        enabled = selectedPluginId != null,
                        label = { Text(text = "识别ID") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PersonAdd,
                                contentDescription = null
                            )
                        },
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                            }
                        }) {
                            Text(text = "取消")
                        }
                        Button(
                            onClick = { /*TODO*/ },
                            enabled = selectedPluginId != null && idInput.isNotBlank()
                        ) {
                            Text(text = "确定")
                        }
                    }
                }
            }
        }
    }) {
    content()
}