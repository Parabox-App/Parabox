package com.ojhdtapp.parabox.ui.setting.detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedState
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.setting.SettingPageEvent
import com.ojhdtapp.parabox.ui.setting.SettingPageState
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxExtensionConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionAddSettingPage(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onNavigateBack: () -> Unit,
    onEvent: (SettingPageEvent) -> Unit,
    onMainSharedEvent: (MainSharedEvent) -> Unit,
) {
    var finishedSuccessfully by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            if (!finishedSuccessfully) {
                onEvent(SettingPageEvent.InitNewExtensionConnectionDone(false))
            }
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
                    onFinish = {
                        finishedSuccessfully = true
                        onEvent(SettingPageEvent.InitNewExtensionConnectionDone(true))
                        onNavigateBack()
                    },
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
                            text = state.initActionState.name ?: "Extension",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
                onFinish = {
                    finishedSuccessfully = true
                    onEvent(SettingPageEvent.InitNewExtensionConnectionDone(true))
                    onNavigateBack()
                },
                onEvent = onEvent,
                onMainSharedEvent = onMainSharedEvent
            )
        }
    }
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    state: SettingPageState,
    mainSharedState: MainSharedState,
    layoutType: LayoutType,
    onFinish: () -> Unit,
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
                                    when (paraboxInitAction) {
                                        is ParaboxInitAction.InfoAction -> {
                                            if (paraboxInitAction.errMsg.isNotEmpty()) {
                                                Text(
                                                    text = paraboxInitAction.errMsg,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            WizardStepButtons(
                                                currentIndex = state.initActionState.currentIndex,
                                                totalSteps = state.initActionState.actionList.size,
                                                isLoading = state.initActionState.isLoading,
                                                onRevert = { onEvent(SettingPageEvent.RevertExtensionInitAction) },
                                                onSubmit = { onEvent(SettingPageEvent.SubmitExtensionInitActionResult("ok")) },
                                            )
                                        }

                                        is ParaboxInitAction.TextInputAction -> {
                                            var text by remember(paraboxInitAction.key) {
                                                mutableStateOf(paraboxInitAction.defaultValue.orEmpty())
                                            }
                                            OutlinedTextField(
                                                value = text,
                                                onValueChange = { text = it },
                                                label = { Text(text = paraboxInitAction.label) },
                                                isError = paraboxInitAction.errMsg.isNotEmpty(),
                                                supportingText = {
                                                    AnimatedVisibility(visible = paraboxInitAction.errMsg.isNotEmpty()) {
                                                        Text(text = paraboxInitAction.errMsg, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    keyboardType = paraboxInitAction.type.toComposeKeyboardType(),
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        if (!state.initActionState.isLoading) {
                                                            onEvent(SettingPageEvent.SubmitExtensionInitActionResult(text))
                                                        }
                                                    }
                                                ),
                                            )
                                            WizardStepButtons(
                                                currentIndex = state.initActionState.currentIndex,
                                                totalSteps = state.initActionState.actionList.size,
                                                isLoading = state.initActionState.isLoading,
                                                onRevert = { onEvent(SettingPageEvent.RevertExtensionInitAction) },
                                                onSubmit = { onEvent(SettingPageEvent.SubmitExtensionInitActionResult(text)) },
                                            )
                                        }

                                        is ParaboxInitAction.TextInputWithImageAction -> {
                                            var text by remember(paraboxInitAction.key) {
                                                mutableStateOf(paraboxInitAction.defaultValue.orEmpty())
                                            }
                                            AsyncImage(
                                                model = paraboxInitAction.image.getModel(),
                                                contentDescription = paraboxInitAction.title,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp),
                                            )
                                            OutlinedTextField(
                                                value = text,
                                                onValueChange = { text = it },
                                                label = { Text(text = paraboxInitAction.label) },
                                                isError = paraboxInitAction.errMsg.isNotEmpty(),
                                                supportingText = {
                                                    AnimatedVisibility(visible = paraboxInitAction.errMsg.isNotEmpty()) {
                                                        Text(text = paraboxInitAction.errMsg, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                },
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    keyboardType = paraboxInitAction.type.toComposeKeyboardType(),
                                                    imeAction = ImeAction.Done
                                                ),
                                            )
                                            WizardStepButtons(
                                                currentIndex = state.initActionState.currentIndex,
                                                totalSteps = state.initActionState.actionList.size,
                                                isLoading = state.initActionState.isLoading,
                                                onRevert = { onEvent(SettingPageEvent.RevertExtensionInitAction) },
                                                onSubmit = { onEvent(SettingPageEvent.SubmitExtensionInitActionResult(text)) },
                                            )
                                        }

                                        is ParaboxInitAction.SelectAction -> {
                                            var selectedIndex by remember(paraboxInitAction.key) {
                                                mutableIntStateOf(paraboxInitAction.defaultValue ?: 0)
                                            }
                                            Column {
                                                paraboxInitAction.options.forEachIndexed { optIdx, option ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { selectedIndex = optIdx }
                                                            .padding(vertical = 4.dp),
                                                    ) {
                                                        RadioButton(
                                                            selected = selectedIndex == optIdx,
                                                            onClick = { selectedIndex = optIdx },
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                            if (paraboxInitAction.errMsg.isNotEmpty()) {
                                                Text(
                                                    text = paraboxInitAction.errMsg,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            WizardStepButtons(
                                                currentIndex = state.initActionState.currentIndex,
                                                totalSteps = state.initActionState.actionList.size,
                                                isLoading = state.initActionState.isLoading,
                                                onRevert = { onEvent(SettingPageEvent.RevertExtensionInitAction) },
                                                onSubmit = { onEvent(SettingPageEvent.SubmitExtensionInitActionResult(selectedIndex)) },
                                            )
                                        }

                                        is ParaboxInitAction.SwitchAction -> {
                                            var checked by remember(paraboxInitAction.key) {
                                                mutableStateOf(paraboxInitAction.defaultValue)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Switch(checked = checked, onCheckedChange = { checked = it })
                                                Text(
                                                    text = if (checked) "已开启" else "已关闭",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                            }
                                            if (paraboxInitAction.errMsg.isNotEmpty()) {
                                                Text(
                                                    text = paraboxInitAction.errMsg,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            WizardStepButtons(
                                                currentIndex = state.initActionState.currentIndex,
                                                totalSteps = state.initActionState.actionList.size,
                                                isLoading = state.initActionState.isLoading,
                                                onRevert = { onEvent(SettingPageEvent.RevertExtensionInitAction) },
                                                onSubmit = { onEvent(SettingPageEvent.SubmitExtensionInitActionResult(checked)) },
                                            )
                                        }

                                        is ParaboxInitAction.ExternalAuthAction -> {
                                            val context = LocalContext.current
                                            val launcher = rememberLauncherForActivityResult(
                                                contract = ActivityResultContracts.StartActivityForResult()
                                            ) { res ->
                                                if (res.resultCode == Activity.RESULT_OK) {
                                                    val payload = res.data?.getStringExtra(
                                                        ParaboxExtensionConstants.EXTRA_AUTH_RESULT_KEY
                                                    ) ?: res.data?.dataString.orEmpty()
                                                    onEvent(SettingPageEvent.SubmitExtensionInitActionResult(payload))
                                                }
                                            }
                                            Button(
                                                enabled = !state.initActionState.isLoading,
                                                onClick = {
                                                    val intent = when {
                                                        paraboxInitAction.activityAction.isNotBlank() -> Intent(
                                                            paraboxInitAction.activityAction
                                                        ).apply {
                                                            if (paraboxInitAction.targetPackage.isNotBlank()) {
                                                                setPackage(paraboxInitAction.targetPackage)
                                                            }
                                                        }
                                                        paraboxInitAction.authUrl.isNotBlank() -> Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(paraboxInitAction.authUrl),
                                                        )
                                                        else -> null
                                                    }
                                                    if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                                                        launcher.launch(intent)
                                                    }
                                                }
                                            ) {
                                                Text(text = paraboxInitAction.buttonLabel)
                                            }
                                            if (paraboxInitAction.errMsg.isNotEmpty()) {
                                                Text(
                                                    text = paraboxInitAction.errMsg,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            if (state.initActionState.currentIndex > 0) {
                                                OutlinedButton(onClick = {
                                                    onEvent(SettingPageEvent.RevertExtensionInitAction)
                                                }) {
                                                    Text(text = stringResource(R.string.last_step))
                                                }
                                            }
                                        }
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
                                    Button(onClick = onFinish) {
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

@Composable
private fun WizardStepButtons(
    currentIndex: Int,
    totalSteps: Int,
    isLoading: Boolean,
    onRevert: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (currentIndex > 0) {
            OutlinedButton(enabled = !isLoading, onClick = onRevert) {
                Text(text = stringResource(R.string.last_step))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (currentIndex <= totalSteps - 1) {
            Button(enabled = !isLoading, onClick = onSubmit) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = stringResource(R.string.next_step))
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