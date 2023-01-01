package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.FileUtil.toSafeFilename
import com.ojhdtapp.parabox.core.util.FormUtil
import com.ojhdtapp.parabox.core.util.toDescriptiveTime
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.ui.util.HashTagEditor
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.SwitchPreference
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun EditActionDialog(
    modifier: Modifier = Modifier,
    showDialog: Boolean,
    contact: Contact?,
    sizeClass: WindowSizeClass,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onEvent: (event: EditActionDialogEvent) -> Unit
) {
    val context = LocalContext.current
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

        var selectedLocalAvatar by remember(contact){
            mutableStateOf<Uri?>(contact?.profile?.avatarUri?.let { Uri.parse(it) })
        }

        val imagePickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
                if (it != null) {
                    selectedLocalAvatar = it
                }
            }

        Dialog(
            onDismissRequest = {
                name = ""
                onDismiss()
            }, properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            val horizontalPadding = when (sizeClass.widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                WindowWidthSizeClass.Expanded -> 0.dp
                else -> 16.dp
            }
            Surface(
                modifier = modifier
                    .widthIn(0.dp, 580.dp)
                    .padding(horizontal = horizontalPadding)
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                var hashTagText by remember {
                    mutableStateOf("")
                }
                var hashTagError by remember {
                    mutableStateOf<String>("")
                }
                var hashTagShouldShowError by remember {
                    mutableStateOf(false)
                }
                val hashTagList = remember {
                    mutableStateListOf<String>()
                }
                var onConfirmDelete by remember {
                    mutableStateOf(false)
                }
                LaunchedEffect(key1 = true, block = {
                    contact?.tags?.also {
                        hashTagList.addAll(it)
                    }
                })
                LazyColumn(
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .height(176.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            bottomStart = 24.dp,
                                            bottomEnd = 24.dp
                                        )
                                    ),
                                painter = painterResource(id = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.drawable.background_dynamic else R.drawable.background),
                                contentDescription = "background",
                                contentScale = ContentScale.Crop
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
                                        .background(MaterialTheme.colorScheme.surface)
                                        .combinedClickable(
                                            enabled = true,
                                            onLongClick = {
                                                if (isEditing) selectedLocalAvatar = null
                                            },
                                            onClick = {
                                                if (isEditing) {
                                                    imagePickerLauncher.launch(
                                                        PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    ,
                                    contentAlignment = Alignment.Center
                                ) {

                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                AvatarUtil.getAvatar(
                                                    uri = selectedLocalAvatar ?:contact?.profile?.avatarUri?.let { Uri.parse(it) },
                                                    url = contact?.profile?.avatar,
                                                    name = contact?.profile?.name,
                                                    backgroundColor = MaterialTheme.colorScheme.primary,
                                                    textColor = MaterialTheme.colorScheme.onPrimary,
                                                )
                                            )
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                            .build(),
                                        contentDescription = "avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                    )
                                    Crossfade(targetState = isEditing) {
                                        if(it){
                                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = "edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
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
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clearFocusOnKeyboardDismiss(),
                                        value = name,
                                        onValueChange = {
                                            nameError = false
                                            name = it
                                        },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.merge(
                                            TextStyle(
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (name.isEmpty()) {
                                                Text(
                                                    text = stringResource(id = R.string.contact_name),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                            }
                                            innerTextField()
                                        },
                                        cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary),
                                        enabled = isEditing
                                    )
                                }
                                AnimatedVisibility(
                                    visible = isEditing || nameError,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    if (nameError) {
                                        Text(
                                            text = stringResource(R.string.contact_name_error_empty),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.edit_contact_name),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    modifier = Modifier.padding(top = 4.dp),
                                    text = contact?.latestMessage?.timestamp?.toDescriptiveTime(context)
                                        ?.let { stringResource(id = R.string.recent_speech_at, it) } ?: stringResource(R.string.no_chat_history),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCompact) {
                                Crossfade(targetState = isEditing) {
                                    if (it) {
                                        FloatingActionButton(
                                            onClick = {
                                                if (name.isEmpty()) {
                                                    nameError = true
                                                } else {
                                                    contact?.also {
                                                        onEvent(
                                                            EditActionDialogEvent.ProfileAndTagUpdate(
                                                                contactId = it.contactId,
                                                                profile = contact.profile.copy(
                                                                    name = name,
                                                                    avatarUri = selectedLocalAvatar?.let { it1 ->
                                                                        FileUtil.getUriByCopyingFileToPath(
                                                                            context,
                                                                            context.getExternalFilesDir("chat")!!,
                                                                            "Avatar_${name.toSafeFilename()}.png",
                                                                            it1
                                                                        )?.toString()
                                                                    }
                                                                ),
                                                                tags = hashTagList.toList()
                                                            )
                                                        )
                                                        isEditing = false
                                                    }
                                                }
                                            },
                                            elevation = FloatingActionButtonDefaults.elevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Outlined.Save,
                                                contentDescription = "save",
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        FloatingActionButton(
                                            onClick = { isEditing = !isEditing },
                                            elevation = FloatingActionButtonDefaults.elevation(
                                                defaultElevation = 0.dp,
                                                pressedElevation = 0.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "edit",
                                                modifier = Modifier.size(ButtonDefaults.IconSize),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                    }
                                }
                            } else {
                                Crossfade(targetState = isEditing) {
                                    if (it) {
                                        OutlinedButton(onClick = {
                                            if (name.isEmpty()) {
                                                nameError = true
                                            } else {
                                                contact?.also {
                                                    onEvent(
                                                        EditActionDialogEvent.ProfileAndTagUpdate(
                                                            contactId = it.contactId,
                                                            profile = Profile(
                                                                name = name,
                                                                avatar = contact.profile.avatar,
                                                                avatarUri = null,
                                                                id = contact.profile.id,
                                                            ),
                                                            tags = hashTagList.toList()
                                                        )
                                                    )
                                                    isEditing = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                Icons.Outlined.Save,
                                                contentDescription = "save",
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.save_change))
                                        }
                                    } else {
                                        Button(onClick = { isEditing = !isEditing }) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "edit",
                                                modifier = Modifier.size(ButtonDefaults.IconSize)
                                            )
                                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                            Text(stringResource(R.string.edit_msg))
                                        }

                                    }
                                }
                            }
                        }
                    }
                    item {

                        val hashTagLazyListState = rememberLazyListState()
                        val hashTagFocusRequester = remember { FocusRequester() }
                        val hashTagInteraction = remember { MutableInteractionSource() }
                        val rowInteraction = remember { MutableInteractionSource() }
                        HashTagEditor(
                            modifier = Modifier.padding(bottom = 8.dp),
                            textFieldValue = hashTagText,
                            enabled = isEditing,
                            onValueChanged = {
                                val values = FormUtil.splitPerSpaceOrNewLine(it)

                                if (values.size >= 2) {
                                    onConfirmDelete = false
                                    if (!FormUtil.checkTagMinimumCharacter(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_too_short)
                                        hashTagShouldShowError = true
                                    } else if (!FormUtil.checkTagMaximumCharacter(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_too_long)
                                        hashTagShouldShowError = true
                                    } else if (hashTagList.contains(values[0])) {
                                        hashTagError = context.getString(R.string.hash_tag_error_duplicate)
                                        hashTagShouldShowError = true
                                    } else {
                                        hashTagShouldShowError = false
                                    }

                                    if (!hashTagShouldShowError) {
                                        hashTagList.add(values[0])
                                        hashTagText = ""
                                    }
                                } else {
                                    hashTagText = it
                                }
                            },
                            placeHolder = stringResource(R.string.hash_tag_empty),
                            placeHolderWhenEnabled = stringResource(R.string.hash_tag_placeholder),
                            lazyListState = hashTagLazyListState,
                            focusRequester = hashTagFocusRequester,
                            textFieldInteraction = hashTagInteraction,
                            rowInteraction = rowInteraction,
                            errorMessage = hashTagError,
                            shouldShowError = hashTagShouldShowError,
                            listOfChips = hashTagList,
                            selectedListOfChips = null,
                            innerModifier = Modifier.onKeyEvent {
                                if (it.key.keyCode == Key.Backspace.keyCode && hashTagText.isBlank()) {
                                    if (onConfirmDelete) {
                                        hashTagList.removeLastOrNull()
                                        onConfirmDelete = false
                                    } else {
                                        onConfirmDelete = true
                                    }
                                }
                                false
                            },
                            onChipClick = {},
                            onChipClickWhenEnabled = { chipIndex ->
                                if (hashTagList.isNotEmpty()) {
                                    hashTagList.removeAt(chipIndex)
                                }
                            },
                            padding = if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) HashTagEditor.PADDING_SMALL else HashTagEditor.PADDING_MEDIUM,
                            onConfirmDelete = onConfirmDelete
                        )
                    }
                    item {
                        Divider(
                            modifier = Modifier.padding(horizontal = if (isCompact) 16.dp else 32.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.new_msg_notification_title),
                            subtitleOn = stringResource(R.string.new_msg_notification_on_subtitle),
                            subtitleOff = stringResource(R.string.new_msg_notification_off_subtitle),
                            checked = contact?.enableNotifications ?: false && contact?.isArchived == false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.EnableNotificationStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null && !contact.isArchived,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.pin_title),
                            subtitleOn = stringResource(R.string.pin_on_subtitle),
                            subtitleOff = stringResource(R.string.pin_off_subtitle),
                            checked = contact?.isPinned ?: false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.PinnedStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                    item {
                        SwitchPreference(
                            title = stringResource(R.string.archive_title),
                            subtitleOn = stringResource(R.string.archive_on_subtitle),
                            subtitleOff = stringResource(R.string.archive_off_subtitle),
                            checked = contact?.isArchived ?: false,
                            onCheckedChange = {
                                contact?.contactId?.let { id ->
                                    onEvent(
                                        EditActionDialogEvent.ArchivedStateUpdate(
                                            id, it
                                        )
                                    )
                                }
                            },
                            enabled = contact != null,
                            horizontalPadding = if (isCompact) 24.dp else 32.dp,
                        )
                    }
                    item {
                        AnimatedVisibility(
                            visible = isEditing && contact?.contactId != contact?.senderId,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            NormalPreference(title = stringResource(R.string.delete_contact_title), subtitle = stringResource(
                                                            R.string.delete_contact_subtitle),
                            warning = true,
                                horizontalPadding = if (isCompact) 24.dp else 32.dp) {
                                onEvent(EditActionDialogEvent.DeleteGrouped)
                            }
                        }
                    }
                }
            }
        }
    }
}

