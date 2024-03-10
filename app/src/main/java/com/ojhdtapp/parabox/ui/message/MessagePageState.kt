package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.input.TextFieldValue
import com.google.android.gms.maps.model.LatLng
import com.ojhdtapp.parabox.core.util.LoadState
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ChatTagsUpdate
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.filter.ChatFilter
import com.ojhdtapp.parabox.ui.base.UiState
import com.ojhdtapp.parabox.ui.message.chat.AudioRecorderState
import com.ojhdtapp.parabox.ui.message.chat.EditAreaMode
import com.ojhdtapp.parabox.ui.message.chat.ToolbarState
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import java.util.concurrent.ConcurrentHashMap

data class MessagePageState(
    val chatDetail: ChatDetail = ChatDetail(),
    val enabledChatFilterList: List<ChatFilter> = emptyList(),
    val selectedChatFilterLists: List<ChatFilter> = listOf(ChatFilter.Normal),
    val openEnabledChatFilterDialog: Boolean = false,
    val editingChatTags: ChatTagsUpdate? = null,
    val chatLatestMessageSenderCache: Map<Long, Resource<Contact>> = emptyMap()
): UiState{


    data class ChatDetail(
        val chat: Chat? = null,
        val selectedMessageList: List<Message> = emptyList(),
        val openDeleteMessageConfirmDialog: Boolean = false,
        val imagePreviewerState: ImagePreviewerState = ImagePreviewerState(),
        val editAreaState: EditAreaState = EditAreaState(),
        val atCache: Map<String, Resource<Contact>> = emptyMap(),
    )

    data class EditAreaState @OptIn(ExperimentalFoundationApi::class) constructor(
        val expanded: Boolean = false,
        val input: TextFieldState = TextFieldState(""),
        val memeList: List<Uri> = emptyList(),
        val chosenImageList: List<Uri> = emptyList(),
        val chosenAtId: Long? = null,
        val chosenQuoteReply: ChatPageUiModel.MessageWithSender? = null,
        val showVoicePermissionDeniedDialog: Boolean = false,
        val showLocationPermissionDeniedDialog: Boolean = false,
        val audioRecorderState: AudioRecorderState = AudioRecorderState.Ready,
        val locationPickerState: LocationPickerState = LocationPickerState(),
        val toolbarState: ToolbarState = ToolbarState.Tools,
        val iconShrink: Boolean = false,
        val mode: EditAreaMode = EditAreaMode.NORMAL
    )

    data class ImagePreviewerState(
        val showToolbar: Boolean = true,
        val expandMenu: Boolean = false,
        val imageSnapshotList: List<Pair<Long, ParaboxImage>> = emptyList(),
        @Deprecated("use effect instead")
        val targetElementIndex: Int = -1,
    )

    data class LocationPickerState(
        val currentLocation: LatLng = LatLng(0.0, 0.0),
        val selectedLocation: LatLng = LatLng(0.0, 0.0),
        val firstLocationGotten: Boolean = false,
        val selectedLocationAddress: String = "",
        val selectedLocationAddressLoadState: LoadState = LoadState.LOADING,
    )
}