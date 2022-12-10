package com.ojhdtapp.parabox.ui.message

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock

class MessagePageState {
}

data class ContactState(val isLoading: Boolean = true, val data: List<Contact> = emptyList())
data class ContactWithMessagesState(val isLoading: Boolean = true, val data: List<ContactWithMessages> = emptyList())
data class ArchivedContactState(val isHidden: Boolean = false, val data: List<Contact> = emptyList())
data class MessageState(
    val state: Int = MessageState.NULL,
    val contact: Contact? = null,
    val pluginConnectionList: List<PluginConnection> = emptyList(),
    val selectedPluginConnection: PluginConnection? = null
) {
    companion object {
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
//        const val ERROR = 3
    }
}

data class GroupInfoState(
    val state: Int = GroupInfoState.NULL,
    val resource: GroupEditResource? = null,
    val message: String? = null
) {
    companion object {
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
}

sealed class ContactTypeFilterState(
    val labelResId: Int,
    val contactCheck: (contact: Contact) -> Boolean
) {
    class All : ContactTypeFilterState(R.string.contact_type_filter_all_label, { _: Contact -> true })
    class Grouped :
        ContactTypeFilterState(R.string.contact_type_filter_grouped_label, { contact: Contact -> contact.contactId != contact.senderId })

    class Ungrouped :
        ContactTypeFilterState(R.string.contact_type_filter_ungrouped_label, { contact: Contact -> contact.contactId == contact.senderId })
}

sealed class ContactReadFilterState(val contactCheck: (contact: Contact) -> Boolean) {
    class All : ContactReadFilterState({ contact: Contact -> true })
    class Unread : ContactReadFilterState({ contact: Contact ->
        contact.latestMessage?.let { it.unreadMessagesNum > 0 } ?: false
    })
}

data class GroupEditResource(
    val name: List<String>,
    val avatar: List<String>,
    val avatarUri: List<Uri>,
    val pluginConnections: List<PluginConnection>
)

class AreaState{
    companion object{
        const val MessageArea = 0
        const val SearchArea = 1
        const val ArchiveArea = 2
    }
}

sealed class AudioRecorderState(
    val textResId: Int
){
    object Ready : AudioRecorderState(R.string.audio_recorder_state_text_ready)
    object Recording: AudioRecorderState(R.string.audio_recorder_state_text_recording)
    object Confirmed: AudioRecorderState(R.string.audio_recorder_state_text_confirmed)
    object Done : AudioRecorderState(R.string.audio_recorder_state_text_done)
}