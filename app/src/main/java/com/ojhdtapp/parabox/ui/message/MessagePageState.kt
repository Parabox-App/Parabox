package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.ToolbarState
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.Image
import kotlinx.coroutines.flow.Flow

data class MessagePageState(
    val chatPagingDataFlow: Flow<PagingData<ChatWithLatestMessage>>,
    val pinnedChatPagingDataFlow: Flow<PagingData<Chat>>,
    val currentChat: CurrentChat = CurrentChat(),
    val messagePagingDataFlow: Flow<PagingData<Message>>,
    val enabledChatFilterList: List<ChatFilter> = emptyList(),
    val selectedChatFilterLists: List<ChatFilter> = listOf(ChatFilter.Normal),
    val datastore: DataStore = DataStore(),
    val openEnabledChatFilterDialog: Boolean = false,
    val editingChatTags: ChatTagsUpdate? = null
): UiState{
    data class DataStore(
        val enableSwipeToDismiss: Boolean = false
    )

    data class CurrentChat(
        val chat: Chat? = null,
        val selectedMessageList: List<Message> = emptyList(),
        val openDeleteMessageConfirmDialog: Boolean = false,
        val imagePreviewerState: ImagePreviewerState = ImagePreviewerState(),
        val editAreaState: EditAreaState = EditAreaState(),
    )

    data class EditAreaState(
        val expanded: Boolean = false,
        val input: String = "",
        val memePathList: List<Uri> = emptyList(),
        val chosenImageList: List<Uri> = emptyList(),
        val chosenAudioUri: Uri? = null,
        val chosenFileUri: Uri? = null,
        val chosenAtId: Long? = null,
        val chosenQuoteReplyMessageId: Long? = null,
        val showVoicePermissionDeniedDialog: Boolean = false,
        val enableAudioRecorder: Boolean = false,
        val audioRecorderState: AudioRecorderState = AudioRecorderState.Ready,
        val toolbarState: ToolbarState = ToolbarState.Tools,
        val iconShrink: Boolean = false,
    )

    data class ImagePreviewerState(
        val showToolbar: Boolean = true,
        val expandMenu: Boolean = false,
    )
}