package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.gms.maps.model.LatLng
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiEvent
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.EditAreaMode
import com.ojhdtapp.parabox.ui.message.chat.ToolbarState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import java.io.File

sealed interface MessagePageEvent : UiEvent {

    data class OpenEnabledChatFilterDialog(val open: Boolean) : MessagePageEvent
    data class UpdateEnabledChatFilterList(val list: List<ChatFilter>) : MessagePageEvent
    data class AddOrRemoveSelectedChatFilter(val filter: ChatFilter) : MessagePageEvent
    data class UpdateChatUnreadMessagesNum(val chatId: Long, val value: Int, val oldValue: Int) : MessagePageEvent
    data class UpdateChatHide(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatPin(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatArchive(val chatId: Long, val value: Boolean, val oldValue: Boolean) : MessagePageEvent
    data class UpdateChatTags(val chatId: Long, val value: List<String>, val oldValue: List<String>) : MessagePageEvent
    data class ShowChatDetail(val chatId: Long, val detail: Chat): MessagePageEvent
    data class UpdateEditingChatTags(val obj: ChatTagsUpdate?): MessagePageEvent
    data class LoadMessage(val chat: Chat?): MessagePageEvent
    data class OpenEditArea(val open: Boolean): MessagePageEvent
    data class UpdateEditAreaInput(val input: TextFieldValue): MessagePageEvent
    data class UpdateToolbarState(val state: ToolbarState): MessagePageEvent
    data class UpdateEditAreaMode(val mode: EditAreaMode): MessagePageEvent
    data class UpdateAudioRecorderState(val state: AudioRecorderState): MessagePageEvent
    data class ExpandImagePreviewerMenu(val expand: Boolean): MessagePageEvent
    data class ExpandImagePreviewerToolbar(val expand: Boolean): MessagePageEvent
    data class UpdateImagePreviewerSnapshotList(val list: List<Pair<Long, ParaboxImage>>, val targetElementIndex: Int) : MessagePageEvent
    data class AddMeme(val meme: Uri, val onSuccess: (path: File) -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class RemoveMeme(val meme: Uri, val onSuccess: () -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class SaveImageToLocal(val image: ParaboxImage,  val onSuccess: (path: File) -> Unit, val onFailure: () -> Unit): MessagePageEvent
    data class ChooseImageUri(val imageUri: Uri): MessagePageEvent
    data class ChooseQuoteReply(val model: ChatPageUiModel.MessageWithSender?): MessagePageEvent
    data class ShowVoicePermissionDeniedDialog(val open: Boolean): MessagePageEvent
    data class ShowLocationPermissionDeniedDialog(val open: Boolean): MessagePageEvent
    data class UpdateIconShrink(val shouldShrink: Boolean): MessagePageEvent
    object SendMessage: MessagePageEvent
    data class SendAudioMessage(val audioFile: File): MessagePageEvent
    data class SendMemeMessage(val imageUri: Uri): MessagePageEvent
    data class SendFileMessage(val fileUri: Uri, val size: Long, val name: String): MessagePageEvent
    data class AddOrRemoveSelectedMessage(val msg: Message) : MessagePageEvent
    object ClearSelectedMessage : MessagePageEvent
    data class UpdateLocation(val location: LatLng): MessagePageEvent
    data class UpdateSelectedLocation(val location: LatLng): MessagePageEvent
    data class UpdateSelectedLocationAddress(val address: String, val loadState: LoadState): MessagePageEvent
    data class QueryLatestMessageSenderOfChatWithCache(val senderId: Long): MessagePageEvent {
        override val lock: Boolean
            get() = true
    }
}
