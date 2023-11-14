package com.ojhdtapp.parabox.ui.message.chat

import android.Manifest
import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.core.util.launchSetting
import com.ojhdtapp.parabox.ui.common.clearFocusOnKeyboardDismiss
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.ImageSendingLayout
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.QuoteReplySendingLayout
import com.origeek.imageViewer.previewer.rememberPreviewerState
import com.origeek.imageViewer.viewer.detectTransformGestures
import org.apache.commons.io.FileUtils
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
    val hapticFeedback = LocalHapticFeedback.current
    val audioRecorder = LocalAudioRecorder.current
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
        shape = (RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp), verticalAlignment = Alignment.Bottom
        ) {
            val isRecording by remember(state) {
                derivedStateOf { state.enableAudioRecorder && state.audioRecorderState is AudioRecorderState.Recording || state.audioRecorderState is AudioRecorderState.Confirmed }
            }
            Crossfade(
                targetState = state.iconShrink,
                label = "icon_shrink",
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    .animateContentSize()
            ) {
                if (it) {
                    IconButton(onClick = { onEvent(MessagePageEvent.UpdateIconShrink(false)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.NavigateNext,
                            contentDescription = "expand"
                        )
                    }
                } else {
                    Row {
                        IconButton(
                            enabled = !isRecording,
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
                            enabled = !isRecording,
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
            AnimatedVisibility(
                visible = state.audioRecorderState is AudioRecorderState.Done,
                enter = expandHorizontally(),
                exit = shrinkHorizontally(),
            ) {
                Surface(
                    modifier = Modifier.padding(end = 16.dp),
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
                val inputBackground by animateColorAsState(targetValue = if (state.enableAudioRecorder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = inputBackground,
                ) {
                    AnimatedContent(
                        modifier = Modifier.animateContentSize(),
                        targetState = state.enableAudioRecorder, label = "audio/text",
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { it }).togetherWith(slideOutHorizontally { -it })
                            } else {
                                (slideInHorizontally { -it }).togetherWith(slideOutHorizontally { it })
                            }
                        }) {
                        if (it) {
                            // audio
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
                                        .pointerInput(Unit) {
                                            var targetFile: File? = null
                                            val press =
                                                PressInteraction.Press(Offset.Zero)
                                            var startTimestamp: Long = 0L
                                            var currentState: AudioRecorderState = AudioRecorderState.Recording
                                            detectTransformGestures(gestureStart = {
                                                startTimestamp = System.currentTimeMillis()
                                                targetFile = FileUtil.createTempFile(
                                                    context,
                                                    FileUtil.DEFAULT_AUDIO_NAME,
                                                    FileUtil.DEFAULT_AUDIO_EXTENSION
                                                )
                                                onEvent(MessagePageEvent.UpdateAudioRecorderState(AudioRecorderState.Recording))
                                                interactionSource.tryEmit(press)
                                                // record start
                                                audioRecorder.start(context, targetFile!!)
                                            },
                                                gestureEnd = {
                                                    // TODO: audio fail notice
                                                    audioRecorder.stop()
                                                    interactionSource.tryEmit(
                                                        PressInteraction.Release(
                                                            press
                                                        )
                                                    )

//                                                onStopRecording()
                                                    if (currentState is AudioRecorderState.Confirmed) {
                                                        if (System.currentTimeMillis() - startTimestamp > 1000) {
                                                            onEvent(MessagePageEvent.SendAudioMessage(audioFile = targetFile!!))
                                                        } else {

                                                        }
                                                        onEvent(
                                                            MessagePageEvent.UpdateAudioRecorderState(
                                                                AudioRecorderState.Ready
                                                            )
                                                        )
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
                                                }) { centroid, pan, zoom, rotation, event ->
                                                if (centroid.y < -150) {
                                                    if (currentState !is AudioRecorderState.Confirmed) {
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        currentState = AudioRecorderState.Confirmed
                                                        onEvent(
                                                            MessagePageEvent.UpdateAudioRecorderState(currentState)
                                                        )
                                                    }
                                                } else {
                                                    if (currentState !is AudioRecorderState.Recording) {
                                                        currentState = AudioRecorderState.Recording
                                                        onEvent(
                                                            MessagePageEvent.UpdateAudioRecorderState(currentState)
                                                        )
                                                    }
                                                }
                                                true
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(state.audioRecorderState.textResId),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            // text
                            Column(
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // quote reply
                                if (state.chosenQuoteReply != null) {
                                    QuoteReplySendingLayout(model = state.chosenQuoteReply, onClick = { /*TODO*/ },
                                        onCancel = {
                                            onEvent(MessagePageEvent.ChooseQuoteReply(null))
                                        })
                                }
                                // image
                                if (state.chosenImageList.isNotEmpty()) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                    ) {
                                        itemsIndexed(
                                            items = state.chosenImageList,
                                            key = { index, item -> item }) { index, item ->
                                            ImageSendingLayout(
                                                modifier = Modifier.animateItemPlacement(),
                                                model = item,
                                                previewIndex = index,
                                                onClick = { },
                                                onCancel = {
                                                    onEvent(MessagePageEvent.ChooseImageUri(item))
                                                }
                                            )
                                        }
                                    }
                                }
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
                                    shape = RoundedCornerShape(28.dp),
                                    colors = TextFieldDefaults.colors(
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
                Crossfade(targetState = state, label = "audio/text switch btn") {
                    if (!it.enableAudioRecorder && it.input.text.isEmpty() && it.chosenImageList.isEmpty() && it.chosenQuoteReply == null) {
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
                    } else if (it.enableAudioRecorder && state.audioRecorderState is AudioRecorderState.Ready) {
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
            AnimatedVisibility(visible = ((state.input.text.isNotEmpty() || state.chosenImageList.isNotEmpty()) && !state.enableAudioRecorder) || (state.audioRecorderState is AudioRecorderState.Done && state.enableAudioRecorder),
                enter = expandHorizontally(
                    expandFrom = Alignment.Start
                ) { width -> 0 },
                exit = shrinkHorizontally(
                    shrinkTowards = Alignment.Start
                ) { width -> 0 }
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}