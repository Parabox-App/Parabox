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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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
import com.ojhdtapp.parabox.domain.model.Country
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.navigation.DefaultSettingComponent
import com.ojhdtapp.parabox.ui.navigation.SettingComponent
import com.ojhdtapp.parabox.ui.setting.Setting
import com.ojhdtapp.parabox.ui.setting.SettingHeader
import com.ojhdtapp.parabox.ui.setting.SettingItem
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.parabox.ui.theme.Theme
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import me.saket.cascade.CascadeDropdownMenu

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ExtensionAddSettingPage(
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
    BackHandler {
        navigation.pop()
        onEvent(SettingPageEvent.InitNewExtensionConnectionDone(false))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        text = state.initActionState.name ?: "Extension",
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            text =  state.initActionState.name ?: "Extension",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navigation.pop()
                            onEvent(SettingPageEvent.InitNewExtensionConnectionDone(false))
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "back")
                        }
                    },
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
) =
    LazyColumn(modifier = modifier) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.initActionState.actionList.forEachIndexed { index, paraboxInitAction ->
                    Row() {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${index + 1}", color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = paraboxInitAction.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedVisibility(visible = state.initActionState.currentIndex == index) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (paraboxInitAction.description.isNotEmpty()) {
                                        Text(
                                            text = paraboxInitAction.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    LaunchedEffect(Unit) {
                                        onEvent(SettingPageEvent.CheckShouldExtensionInitActionSkip)
                                    }
                                    when (paraboxInitAction) {
                                        is ParaboxInitAction.InfoAction -> {
                                           // 什么都不用做
                                        }
                                        is ParaboxInitAction.LoadingAction -> {
                                            LaunchedEffect(Unit) {
                                                onEvent(SettingPageEvent.SubmitExtensionInitActionResult(""))
                                            }
                                            if (paraboxInitAction.errMsg.isNotEmpty()) {
                                                ContainedLoadingIndicator()
                                            } else {
                                                Column {
                                                    Text(
                                                        text = paraboxInitAction.errMsg,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row {
                                                        if (state.initActionState.currentIndex > 0) {
                                                            OutlinedButton(onClick = {
                                                                onEvent(SettingPageEvent.RevertExtensionInitAction)
                                                            }) {
                                                                Text(text = stringResource(R.string.last_step))
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                        }
                                                        Button(
                                                            enabled = !paraboxInitAction.isLoading,
                                                            onClick = {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult(""))
                                                        }) {
                                                            if (paraboxInitAction.isLoading) {
                                                                CircularWavyProgressIndicator(
                                                                    modifier = Modifier.size(24.dp),
                                                                    stroke = Stroke(
                                                                        width = with(LocalDensity.current) {
                                                                            2.dp.toPx()
                                                                        },
                                                                        cap = StrokeCap.Round
                                                                    ),
                                                                    trackStroke = Stroke(
                                                                        width = with(LocalDensity.current) {
                                                                            2.dp.toPx()
                                                                        },
                                                                        cap = StrokeCap.Round
                                                                    )
                                                                )
                                                            } else {
                                                                Text(text = stringResource(R.string.refresh))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is ParaboxInitAction.TextInputAction -> {
                                            var text by remember { mutableStateOf("") }
                                            OutlinedTextField(
                                                value = text,
                                                onValueChange = { text = it },
                                                label = {
                                                    Text(text = paraboxInitAction.label)
                                                },
                                                isError = paraboxInitAction.errMsg.isNotEmpty(),
                                                supportingText = {
                                                    AnimatedVisibility(
                                                        visible = paraboxInitAction.errMsg.isNotEmpty(),
                                                    ) {
                                                        Text(text = paraboxInitAction.errMsg, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    keyboardType = paraboxInitAction.type.toComposeKeyboardType(),
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        onEvent(SettingPageEvent.SubmitExtensionInitActionResult(text))
                                                    }),
                                            )
                                            Row() {
                                                if (state.initActionState.currentIndex > 0) {
                                                    OutlinedButton(onClick = {
                                                        onEvent(SettingPageEvent.RevertExtensionInitAction)
                                                    }) {
                                                        Text(text = stringResource(R.string.last_step))
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                if (state.initActionState.currentIndex <= state.initActionState.actionList.size - 1) {
                                                    Button(
                                                        enabled = !paraboxInitAction.isLoading,
                                                        onClick = {
                                                        onEvent(SettingPageEvent.SubmitExtensionInitActionResult(text))
                                                    }) {
                                                        if (paraboxInitAction.isLoading) {
                                                            CircularWavyProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                stroke = Stroke(
                                                                    width = with(LocalDensity.current) {
                                                                        2.dp.toPx()
                                                                    },
                                                                    cap = StrokeCap.Round
                                                                ),
                                                                trackStroke = Stroke(
                                                                    width = with(LocalDensity.current) {
                                                                        2.dp.toPx()
                                                                    },
                                                                    cap = StrokeCap.Round
                                                                )
                                                            )
                                                        } else {
                                                            Text(text = stringResource(R.string.next_step))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is ParaboxInitAction.PhoneInputAction -> {
                                            var phoneNumber by remember { mutableStateOf("") }
                                            var selectedCountry by remember {
                                                mutableStateOf(Country.countryList.first())
                                            }
                                            var isMenuVisible by remember {
                                                mutableStateOf(false)
                                            }
                                            OutlinedTextField(
                                                value = phoneNumber,
                                                onValueChange = { phoneNumber = it },
                                                label = {
                                                    Text(text = "电话号码")
                                                },
                                                isError = paraboxInitAction.errMsg.isNotEmpty(),
                                                supportingText = {
                                                    AnimatedVisibility(
                                                        visible = paraboxInitAction.errMsg.isNotEmpty(),
                                                    ) {
                                                        Text(text = paraboxInitAction.errMsg, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier.size(56.dp, 48.dp)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .clickable{
                                                            isMenuVisible = true
                                                        },
                                                        contentAlignment = Alignment.Center
                                                    ){
                                                        CascadeDropdownMenu(
                                                            expanded = isMenuVisible,
                                                            onDismissRequest = { isMenuVisible = false },
                                                            offset = DpOffset(16.dp, 0.dp),
                                                            properties = PopupProperties(
                                                                dismissOnBackPress = true,
                                                                dismissOnClickOutside = true,
                                                                focusable = true
                                                            ),
                                                            shape = MaterialTheme.shapes.medium,
                                                        ) {
                                                            Country.countryList.forEach {
                                                                androidx.compose.material3.DropdownMenuItem(
                                                                    text = { Text(text = it.name) },
                                                                    leadingIcon = {
                                                                        Text(text = "${it.flag} ")
                                                                    },
                                                                    onClick = {
                                                                        phoneNumber = ""
                                                                        selectedCountry = it
                                                                        isMenuVisible = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                        Text(text = selectedCountry.code)
                                                    }
                                                },
                                                prefix = {
                                                    Text(text = selectedCountry.flag)
                                                },
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (phoneNumber.isNotBlank()) {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult("${selectedCountry.code}$phoneNumber"))
                                                        } else {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult(""))
                                                        }
                                                    }),
                                            )
                                            Row() {
                                                if (state.initActionState.currentIndex > 0) {
                                                    OutlinedButton(onClick = {
                                                        onEvent(SettingPageEvent.RevertExtensionInitAction)
                                                    }) {
                                                        Text(text = stringResource(R.string.last_step))
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                if (state.initActionState.currentIndex <= state.initActionState.actionList.size - 1) {
                                                    Button(
                                                        enabled =!paraboxInitAction.isLoading,
                                                        onClick = {
                                                        if (phoneNumber.isNotBlank()) {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult("${selectedCountry.code}$phoneNumber"))
                                                        } else {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult(""))
                                                        }
                                                    }) {
                                                        if (paraboxInitAction.isLoading) {
                                                            CircularWavyProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                stroke = Stroke(
                                                                    width = with(LocalDensity.current) {
                                                                        2.dp.toPx()
                                                                    },
                                                                    cap = StrokeCap.Round
                                                                ),
                                                                trackStroke = Stroke(
                                                                    width = with(LocalDensity.current) {
                                                                        2.dp.toPx()
                                                                    },
                                                                    cap = StrokeCap.Round
                                                                )
                                                            )
                                                        } else {
                                                            Text(text = stringResource(R.string.next_step))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
                Row() {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${state.initActionState.actionList.size + 1}", color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "完成",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedVisibility(visible = state.initActionState.currentIndex == state.initActionState.actionList.size) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "已完成新增扩展连接的配置工作",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row() {
                                    if (state.initActionState.currentIndex > 0) {
                                        OutlinedButton(onClick = {
                                            onEvent(SettingPageEvent.RevertExtensionInitAction)
                                        }) {
                                            Text(text = stringResource(R.string.last_step))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Button(onClick = {
                                        navigation.pop()
                                        onEvent(SettingPageEvent.InitNewExtensionConnectionDone(true))
                                    }) {
                                        Text(text = "完成")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

fun ParaboxInitAction.KeyboardType.toComposeKeyboardType(): KeyboardType {
    return when (this) {
        ParaboxInitAction.KeyboardType.TEXT -> KeyboardType.Text
        ParaboxInitAction.KeyboardType.NUMBER ->KeyboardType.Number
        ParaboxInitAction.KeyboardType.EMAIL -> KeyboardType.Email
        ParaboxInitAction.KeyboardType.PASSWORD -> KeyboardType.Password
    }
}