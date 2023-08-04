package com.ojhdtapp.parabox.ui.message.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.buildFileName
import com.ojhdtapp.parabox.core.util.launchSetting
import com.ojhdtapp.parabox.ui.common.clearFocusOnKeyboardDismiss
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditArea(
    modifier: Modifier = Modifier,
    state: MessagePageState.EditAreaState,
    onEvent: (e: MessagePageEvent) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val audioPermissionState =
        rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    if (state.showVoicePermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = {
                onEvent(MessagePageEvent.ShowVoicePermissionDeniedDialog(false))
            },
            icon = { Icon(Icons.Outlined.KeyboardVoice, contentDescription = null) },
            title = {
                Text(text = stringResource(R.string.request_permission))
            },
            text = {
                Text(
                    stringResource(id = R.string.audio_permission_text)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(MessagePageEvent.ShowVoicePermissionDeniedDialog(false))
                        audioPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text(stringResource(R.string.try_request_permission))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEvent(MessagePageEvent.ShowVoicePermissionDeniedDialog(false))
                        context.launchSetting()
                    }
                ) {
                    Text(stringResource(R.string.redirect_to_setting))
                }
            }
        )
    }
    Surface(
        shape = (RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp), verticalAlignment = Alignment.Bottom
        ) {
            val isRecording by remember {
                derivedStateOf { state.audioRecorderState is AudioRecorderState.Ready || state.audioRecorderState is AudioRecorderState.Done }
            }
            Crossfade(
                targetState = state.iconShrink,
                label = "icon_shrink",
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .animateContentSize()
            ) {
                if (it) {
                    IconButton(onClick = { onEvent(MessagePageEvent.UpdateIconShrink(false)) }) {
                        Icon(
                            imageVector = Icons.Outlined.NavigateNext,
                            contentDescription = "expand"
                        )
                    }
                } else {
                    Row {
                        IconButton(
                            enabled = isRecording,
                            onClick = {
                                keyboardController?.hide()
                                if (state.toolbarState == ToolbarState.Tools && state.expanded) {
                                    onEvent(MessagePageEvent.OpenEditArea(false))
                                } else {
                                    onEvent(MessagePageEvent.OpenEditArea(true))
                                }
                                onEvent(MessagePageEvent.UpdateToolbarState(ToolbarState.Tools))
                                onEvent(MessagePageEvent.EnableAudioRecorder(false))
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.AddCircleOutline,
                                contentDescription = "more"
                            )
                        }
                        IconButton(
                            enabled = isRecording,
                            onClick = {
                                keyboardController?.hide()
                                if (state.toolbarState == ToolbarState.Emoji && state.expanded) {
                                    onEvent(MessagePageEvent.OpenEditArea(false))
                                } else {
                                    onEvent(MessagePageEvent.OpenEditArea(true))
                                }
                                onEvent(MessagePageEvent.UpdateToolbarState(ToolbarState.Emoji))

                                onEvent(MessagePageEvent.EnableAudioRecorder(false))
                            }) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = "emoji"
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = state.audioRecorderState is AudioRecorderState.Done) {
                Surface(
                    modifier = Modifier.padding(end = 8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onClick = {
                        onEvent(MessagePageEvent.UpdateAudioRecorderState(AudioRecorderState.Ready))
                    }
                ) {
                    Box(
                        modifier = Modifier.size(TextFieldDefaults.MinHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = "clear",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier.zIndex(2f),
                    visible = state.enableAudioRecorder,
                    enter = expandHorizontally { 0 },
                    exit = shrinkHorizontally { 0 }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier.indication(
                                interactionSource,
                                LocalIndication.current
                            ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(TextFieldDefaults.MinHeight)
                                    .pointerInteropFilter {
                                        val press =
                                            PressInteraction.Press(Offset(it.x, it.y))
                                        when (it.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                onEvent(MessagePageEvent.UpdateAudioRecorderState(AudioRecorderState.Recording))
                                                interactionSource.tryEmit(press)
//                                                onStartRecording()
                                            }

                                            MotionEvent.ACTION_MOVE -> {
                                                if (it.y < -150) {
                                                    if (state.audioRecorderState !is AudioRecorderState.Confirmed) {
                                                        onEvent(
                                                            MessagePageEvent.UpdateAudioRecorderState(
                                                                AudioRecorderState.Confirmed
                                                            )
                                                        )
                                                    }
                                                } else {
                                                    if (state.audioRecorderState !is AudioRecorderState.Recording) {
                                                        onEvent(
                                                            MessagePageEvent.UpdateAudioRecorderState(
                                                                AudioRecorderState.Recording
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            MotionEvent.ACTION_UP -> {
                                                interactionSource.tryEmit(
                                                    PressInteraction.Release(
                                                        press
                                                    )
                                                )
//                                                onStopRecording()
                                                if (state.audioRecorderState is AudioRecorderState.Confirmed) {
//                                                    onClearRecording()
//                                                    sendAudio(context, packageNameList) {
//                                                        onSend(it)
//                                                        onAudioRecorderStateChanged(
//                                                            AudioRecorderState.Ready
//                                                        )
//                                                    }
                                                } else {
                                                    onEvent(
                                                        MessagePageEvent.UpdateAudioRecorderState(
                                                            AudioRecorderState.Done
                                                        )
                                                    )
                                                }
                                            }

                                            else -> false
                                        }
                                        true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(state.audioRecorderState.textResId),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(
                                visible = state.audioRecorderState is AudioRecorderState.Ready,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally(),
                            ) {
                                IconButton(onClick = {
                                    onEvent(MessagePageEvent.EnableAudioRecorder(false))
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Keyboard,
                                        contentDescription = "keyboard",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.animateContentSize()
                    ) {
                        // here!!!
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clearFocusOnKeyboardDismiss(),
                            value = state.input,
                            onValueChange = {
                                onEvent(MessagePageEvent.OpenEditArea(false))
                                onEvent(MessagePageEvent.UpdateEditAreaInput(it))
                            },
                            enabled = !state.enableAudioRecorder,
                            textStyle = MaterialTheme.typography.bodyLarge.merge(
                                TextStyle(color = MaterialTheme.colorScheme.onSurface)
                            ),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.input_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = state.input.text.isEmpty() && state.chosenImageList.isEmpty(),
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    IconButton(onClick = {
                                        if (audioPermissionState.status.isGranted) {
                                            onEvent(MessagePageEvent.EnableAudioRecorder(true))
                                            onEvent(MessagePageEvent.OpenEditArea(false))
                                        } else {
                                            onEvent(MessagePageEvent.ShowVoicePermissionDeniedDialog(true))
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.KeyboardVoice,
                                            contentDescription = "voice"
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
//                            val focusRequester = remember { FocusRequester() }
//                            BasicTextField(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .focusRequester(focusRequester)
//                                    .clearFocusOnKeyboardDismiss(),
//                                value = state.input,
//                                onValueChange = {
//                                    onEvent(MessagePageEvent.UpdateEditAreaInput(it))
//                                },
//                                enabled = !state.enableAudioRecorder,
//                                textStyle = MaterialTheme.typography.bodyLarge.merge(
//                                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
//                                ),
//                                decorationBox = { innerTextField ->
//                                    if (state.input.text.isEmpty()) {
//                                        Text(
//                                            text = stringResource(R.string.input_placeholder),
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                            maxLines = 1,
//                                            overflow = TextOverflow.Clip,
//                                        )
//                                    }
//                                    innerTextField()
//                                },
//                                cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
//                            )
                    }
                }
            }
            AnimatedVisibility(visible = ((state.input.text.isNotEmpty() || state.chosenImageList.isNotEmpty()) && !state.enableAudioRecorder) || (state.audioRecorderState is AudioRecorderState.Done && state.enableAudioRecorder),
                enter = expandHorizontally() { width -> 0 },
                exit = shrinkHorizontally() { width -> 0 }
            ) {
                FloatingActionButton(
                    onClick = {
                        onEvent(MessagePageEvent.SendMessage)
                    },
                    modifier = Modifier.padding(end = 16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Outlined.Send, contentDescription = "send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}