package com.ojhdtapp.parabox.ui.message

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.ui.common.RoundedCornerCascadeDropdownMenu
import me.saket.cascade.CascadeDropdownMenu

@Composable
fun ChatDropdownMenu(
    modifier: Modifier = Modifier,
    chat: Chat,
    isMenuVisible: Boolean,
    onEvent: (event: MessagePageEvent) -> Unit,
    onDismiss: () -> Unit,
){
    CascadeDropdownMenu(
        expanded = isMenuVisible,
        onDismissRequest = onDismiss,
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        offset = DpOffset(16.dp, 0.dp),
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        if (chat.isPinned) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_not_pin)) },
                onClick = {
                    onEvent(MessagePageEvent.UpdateChatPin(
                        chat.chatId,
                        false,
                        chat.isPinned
                    ))
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = null
                    )
                })
        } else {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_pin)) },
                onClick = {
                    onEvent(
                        MessagePageEvent.UpdateChatPin(
                            chat.chatId,
                            true,
                            chat.isPinned
                        )
                    )
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Flag,
                        contentDescription = null
                    )
                })
        }
        if (chat.unreadMessageNum > 0) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_mark_as_read)) },
                onClick = {
                    onEvent(
                        MessagePageEvent.UpdateChatUnreadMessagesNum(
                            chat.chatId,
                            0,
                            chat.unreadMessageNum
                        )
                    )
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.MarkChatRead,
                        contentDescription = null
                    )
                })
        } else {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_mark_as_unread)) },
                onClick = {
                    onEvent(
                        MessagePageEvent.UpdateChatUnreadMessagesNum(
                            chat.chatId,
                            1,
                            chat.unreadMessageNum
                        )
                    )
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.MarkChatUnread,
                        contentDescription = null
                    )
                })
        }

        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = "标记已完成") },
            onClick = {
                onEvent(
                    MessagePageEvent.UpdateChatHide(
                        chat.chatId,
                        true,
                        chat.isHidden
                    )
                )
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Done,
                    contentDescription = null
                )
            })
        if (chat.isArchived) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_unarchive)) },
                onClick = {
                    onEvent(
                        MessagePageEvent.UpdateChatArchive(
                            chat.chatId,
                            false,
                            chat.isArchived
                        )
                    )
                    onDismiss()

                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Unarchive,
                        contentDescription = null
                    )
                })
        } else {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(text = stringResource(R.string.dropdown_menu_archive)) },
                onClick = {
                    onEvent(
                        MessagePageEvent.UpdateChatArchive(
                            chat.chatId,
                            true,
                            chat.isArchived
                        )
                    )
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Archive,
                        contentDescription = null
                    )
                })
        }
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.dropdown_menu_new_tag)) },
            onClick = {
                onEvent(
                    MessagePageEvent.UpdateEditingChatTags(
                        ChatTagsUpdate(
                            chat.chatId,
                            chat.tags
                        )
                    )
                )
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.NewLabel,
                    contentDescription = null
                )
            })
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(text = stringResource(R.string.dropdown_menu_info)) },
            onClick = {
                onEvent(
                    MessagePageEvent.ShowChatDetail(
                        chat.chatId,
                        chat
                    )
                )
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null
                )
            })
    }
}