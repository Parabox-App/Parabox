package com.ojhdtapp.parabox.ui.message.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import me.saket.cascade.CascadeDropdownMenu

@Composable
fun MessageDropdownMenu(
    modifier: Modifier = Modifier,
    message: Message,
    isMenuVisible: Boolean,
    onEvent: (event: MessagePageEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    CascadeDropdownMenu(
        expanded = isMenuVisible,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 0.dp),
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.refresh)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.reply)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.Reply,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.forward)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.Forward,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.multiple_select)) },
            onClick = {
                onEvent(MessagePageEvent.AddOrRemoveSelectedMessage(message))
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Checklist,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.add_meme)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.save_to_local)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.FileDownload,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.try_to_recall)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.copy_to_clipboard)) },
            onClick = {
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = null
                )
            })
    }
}