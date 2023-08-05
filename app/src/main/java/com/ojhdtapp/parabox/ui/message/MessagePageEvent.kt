package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiEvent
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.ToolbarState
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import java.io.File

sealed interface MessagePageEvent : UiEvent {
    object UpdateDataStore : MessagePageEvent
    data class OpenEnabledChatFilterDialog(val open: Boolean) : MessagePageEvent
    data class UpdateEnabledChatFilterList(val list: List<ChatFilter>) : MessagePageEvent
    data class AddOrRemoveSelectedChatFilter(val filter: ChatFilter) : MessagePageEvent
    data class GetChatPagingDataFlow(val filterList: List<ChatFilter>) : MessagePageEvent
    data class UpdateChatUnreadMessagesNum(val chatId: Long, val value: Int, val oldValue: Int) : MessagePageEvent
    data class UpdateChatHide(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatPin(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatArchive(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatTags(val chatId: Long, val value: List<String>, val oldValue: List<String>) : MessagePageEvent
    data class UpdateEditingChatTags(val obj: ChatTagsUpdate?): MessagePageEvent
    data class LoadMessage(val chat: Chat?): MessagePageEvent
    data class OpenEditArea(val open: Boolean): MessagePageEvent
    data class UpdateEditAreaInput(val input: TextFieldValue): MessagePageEvent
    data class UpdateToolbarState(val state: ToolbarState): MessagePageEvent
    data class EnableAudioRecorder(val enable: Boolean): MessagePageEvent
    data class UpdateAudioRecorderState(val state: AudioRecorderState): MessagePageEvent
    data class ExpandImagePreviewerMenu(val expand: Boolean): MessagePageEvent
    data class ExpandImagePreviewerToolbar(val expand: Boolean): MessagePageEvent
    data class AddMeme(val meme: Uri, val onSuccess: (path: File) -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class RemoveMeme(val meme: Uri, val onSuccess: () -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class SaveImageToLocal(val image: ParaboxImage,  val onSuccess: (path: File) -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class AddImageUriToChosenList(val imageUri: Uri): MessagePageEvent
    data class ShowVoicePermissionDeniedDialog(val open: Boolean): MessagePageEvent
    data class UpdateIconShrink(val shouldShrink: Boolean): MessagePageEvent
    object SendMessage: MessagePageEvent
    data class SendMemeMessage(val imageUri: Uri): MessagePageEvent
    data class SendFileMessage(val fileUri: Uri, val size: Long, val name: String): MessagePageEvent
}
