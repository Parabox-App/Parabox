package com.ojhdtapp.parabox.ui.setting.detail

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.os.LocaleListCompat
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.optBooleanOrNull
import com.ojhdtapp.parabox.core.util.optStringOrNull
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.common.ParaboxAlertDialog
import com.ojhdtapp.parabox.ui.common.PredictiveBackGuardContainer
import com.ojhdtapp.parabox.ui.common.TextInputDialog
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.parabox.ui.theme.Theme
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionConfigSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onNavigateBack: () -> Unit,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val context = LocalContext.current
    var showConfirmDialogIfModified by remember { mutableStateOf(false) }
    if (showConfirmDialogIfModified) {
        ParaboxAlertDialog(
            onDismissRequest = { showConfirmDialogIfModified = false },
            title = {
                Text(text = "存在未应用的更改")
            },
            text = {
                Text(text = "是否保存更改并退出？连接将重启以应用更改。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialogIfModified = false
                    onEvent(SettingPageEvent.SubmitConnectionConfig)
                    onNavigateBack()
                }) {
                    Text(text = "保存并退出")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialogIfModified = false
                    onNavigateBack()
                }) {
                    Text(text = "舍弃更改")
                }
            },
        )
    }
    PredictiveBackGuardContainer(
        enabled = state.configState.modified,
        onIntercepted = { showConfirmDialogIfModified = true },
    ) {
        if (layoutType == LayoutType.SPLIT) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (state.configState.modified) {
                                showConfirmDialogIfModified = true
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            color = MaterialTheme.colorScheme.onSurface,
                            text = state.configState.originalConnection?.name ?: "Connection",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onEvent(SettingPageEvent.SubmitConnectionConfig)
                                Toast.makeText(context, "更改已应用", Toast.LENGTH_SHORT).show()
                            }) {
                            Text(text = "保存")
                        }
                    }
                    Content(
                        modifier = Modifier.weight(1f),
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = layoutType,
                        onEvent = onEvent,
                        onMainSharedEvent = onMainSharedEvent,
                    )
                }
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = state.configState.originalConnection?.name ?: "Connection",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (state.configState.modified) {
                                    showConfirmDialogIfModified = true
                                } else {
                                    onNavigateBack()
                                }
                            }) {
                                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                onEvent(SettingPageEvent.SubmitConnectionConfig)
                                Toast.makeText(context, "更改已应用", Toast.LENGTH_SHORT).show()
                            }) {
                                Text(text = "保存")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Content(
                    modifier = Modifier.padding(innerPadding),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    onEvent = onEvent,
                    onMainSharedEvent = onMainSharedEvent
                )
            }
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(modifier = modifier) {
        items(items = state.configState.configList) { paraboxConfig ->
            when (paraboxConfig) {
                is ParaboxConfigItem.Category -> {
                    SettingHeader(text = paraboxConfig.title)
                }

                is ParaboxConfigItem.TextInputConfigItem -> {
                    var openDialog by remember { mutableStateOf(false) }
                    val defaultValue =
                        state.configState.originalConnection?.extra?.optStringOrNull(paraboxConfig.key)
                            ?: paraboxConfig.defaultValue
                    var errMsg by remember {
                        mutableStateOf<String?>(null)
                    }
                    TextInputDialog(
                        openDialog = openDialog,
                        title = paraboxConfig.title,
                        description = paraboxConfig.description,
                        defaultValue = defaultValue,
                        label = paraboxConfig.label,
                        keyboardType = paraboxConfig.type.toComposeKeyboardType(),
                        errMsg = errMsg,
                        onConfirm = { value ->
                            onEvent(
                                SettingPageEvent.ValidateConnectionConfigItem(paraboxConfig, value) { result ->
                                    if (result is ParaboxInitActionResult.Error) {
                                        errMsg = result.message
                                    } else {
                                        errMsg = null
                                        onEvent(SettingPageEvent.WriteConnectionConfigCache(paraboxConfig, value))
                                        openDialog = false
                                    }
                                }
                            )
                        },
                        onDismiss = {
                            openDialog = false
                        }
                    )
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = state.configState.cacheExtra?.optStringOrNull(paraboxConfig.key)?.takeIf { it.isNotBlank() }
                            ?: paraboxConfig.description.takeIf { it.isNotBlank() } ?: "空",
                        selected = false,
                        layoutType = layoutType,
                    ) {
                        openDialog = true
                    }
                }

                is ParaboxConfigItem.SwitchConfigItem -> {
                    var checked by remember {
                        mutableStateOf(
                            state.configState.originalConnection?.extra?.optBooleanOrNull(paraboxConfig.key)
                                ?: paraboxConfig.defaultValue
                        )
                    }
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = paraboxConfig.description,
                        selected = false,
                        layoutType = layoutType,
                        trailingIcon = {
                            Switch(
                                checked = checked,
                                onCheckedChange = {
                                    checked = it
                                    onEvent(SettingPageEvent.WriteConnectionConfigCache(paraboxConfig, it))
                                })
                        }
                    ) {
                        checked = !checked
                        onEvent(
                            SettingPageEvent.WriteConnectionConfigCache(
                                paraboxConfig, checked
                            )
                        )
                    }
                }

                is ParaboxConfigItem.SelectConfigItem -> {
                    var openDialog by remember { mutableStateOf(false) }
                    val currentValue = state.configState.cacheExtra?.optStringOrNull(paraboxConfig.key)
                        ?: paraboxConfig.defaultValue?.let { paraboxConfig.options.getOrNull(it) }
                        ?: paraboxConfig.options.firstOrNull().orEmpty()
                    if (openDialog) {
                        ParaboxAlertDialog(
                            onDismissRequest = { openDialog = false },
                            title = { Text(text = paraboxConfig.title) },
                            text = {
                                Column {
                                    paraboxConfig.options.forEach { option ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onEvent(
                                                        SettingPageEvent.WriteConnectionConfigCache(
                                                            paraboxConfig,
                                                            option,
                                                        )
                                                    )
                                                    openDialog = false
                                                }
                                                .padding(vertical = 4.dp),
                                        ) {
                                            RadioButton(
                                                selected = currentValue == option,
                                                onClick = {
                                                    onEvent(
                                                        SettingPageEvent.WriteConnectionConfigCache(
                                                            paraboxConfig,
                                                            option,
                                                        )
                                                    )
                                                    openDialog = false
                                                },
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = option)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { openDialog = false }) {
                                    Text("关闭")
                                }
                            },
                        )
                    }
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = currentValue.ifBlank { paraboxConfig.description },
                        selected = false,
                        layoutType = layoutType,
                    ) {
                        openDialog = true
                    }
                }

                is ParaboxConfigItem.ActionConfigItem -> {
                    val actionContext = LocalContext.current
                    var openConfirm by remember { mutableStateOf(false) }
                    val executeAction = {
                        onEvent(
                            SettingPageEvent.ExecuteConnectionConfigAction(paraboxConfig) { res ->
                                val msg = if (res.code == ParaboxResult.SUCCESS) {
                                    res.message.ifBlank { "操作成功" }
                                } else {
                                    "操作失败：${res.message}"
                                }
                                Toast.makeText(actionContext, msg, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    val confirmModel = paraboxConfig.confirmModel
                    if (openConfirm && confirmModel != null) {
                        ParaboxAlertDialog(
                            onDismissRequest = { openConfirm = false },
                            title = { Text(text = confirmModel.title) },
                            text = { Text(text = confirmModel.description) },
                            confirmButton = {
                                TextButton(onClick = {
                                    openConfirm = false
                                    executeAction()
                                }) {
                                    Text(text = confirmModel.confirmText)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { openConfirm = false }) {
                                    Text(text = confirmModel.cancelText)
                                }
                            },
                        )
                    }
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = paraboxConfig.description,
                        selected = false,
                        layoutType = layoutType,
                    ) {
                        if (confirmModel != null) {
                            openConfirm = true
                        } else {
                            executeAction()
                        }
                    }
                }
            }
        }
    }
}