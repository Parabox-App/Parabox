package com.ojhdtapp.parabox.ui.setting.detail

import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
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
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.common.TextInputDialog
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.parabox.ui.theme.Theme
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import kotlinx.coroutines.launch
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExtensionConfigSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    var showConfirmDialogIfModified by remember { mutableStateOf(false) }
    if (showConfirmDialogIfModified) {
        AlertDialog(
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
                    navigation.pop()
                }) {
                    Text(text = "保存并退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialogIfModified = false }) {
                    Text(text = "取消")
                }
            },
        )
    }
    BackHandler {
        if (state.configState.modified) {
            showConfirmDialogIfModified = true
        } else {
            navigation.pop()
        }
    }
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = state.configState.originalConnection?.name ?: "Connection",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = {
                        onEvent(SettingPageEvent.SubmitConnectionConfig)
                    }) {
                        Text(text = "保存")
                    }
                }
                Content(
                    modifier = Modifier.weight(1f),
                    state = state,
                    mainSharedState = mainSharedState,
                    layoutType = layoutType,
                    scaffoldNavigator = scaffoldNavigator,
                    navigation = navigation,
                    stackState = stackState,
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
                            navigation.pop()
                            if (state.configState.modified) {
                                showConfirmDialogIfModified = true
                            } else {
                                navigation.pop()
                            }
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            onEvent(SettingPageEvent.SubmitConnectionConfig)
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
                scaffoldNavigator = scaffoldNavigator,
                navigation = navigation,
                stackState = stackState,
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    scaffoldNavigator: ThreePaneScaffoldNavigator<Setting>,
    navigation: StackNavigation<DefaultSettingComponent.SettingConfig>,
    stackState: ChildStack<*, SettingComponent.SettingChild>,
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
                        state.configState.originalConnection?.extra?.getString(paraboxConfig.key)
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
                        onConfirm = {
                            coroutineScope.launch {
                                val result = paraboxConfig.onResult(it)
                                if (result is ParaboxInitActionResult.Error) {
                                    errMsg = result.message
                                } else {
                                    errMsg = null
                                    onEvent(SettingPageEvent.WriteConnectionConfigCache(paraboxConfig, it))
                                    openDialog = false
                                }
                            }
                        },
                        onDismiss = {
                            openDialog = false
                        }
                    )
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = paraboxConfig.description,
                        selected = false,
                        layoutType = layoutType,
                    ) {
                        openDialog = true
                    }
                }

                is ParaboxConfigItem.SwitchConfigItem -> {
                    var checked by remember {
                        mutableStateOf(
                            state.configState.originalConnection?.extra?.getBoolean(paraboxConfig.key)
                                ?: paraboxConfig.defaultValue
                        )
                    }
                    SettingItem(
                        title = paraboxConfig.title,
                        subTitle = paraboxConfig.description,
                        selected = false,
                        layoutType = layoutType,
                        trailingIcon = {
                            Switch(checked = checked, onCheckedChange = {
                                checked = it
                                onEvent(SettingPageEvent.WriteConnectionConfigCache(paraboxConfig, it))
                            })
                        }
                    ) {
                        checked = !checked
                        onEvent(SettingPageEvent.WriteConnectionConfigCache(paraboxConfig, checked))
                    }
                }

                is ParaboxConfigItem.SelectConfigItem -> {

                }
            }
        }
    }
}