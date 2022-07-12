package com.ojhdtapp.parabox.ui.message

import android.os.Parcelable
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.GroupInfoPack
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.chat.ChatBlock
import kotlinx.parcelize.Parcelize

class MessagePageState {
}

data class ContactState(val isLoading: Boolean = true, val data: List<Contact> = emptyList())
data class MessageState(val state: Int = MessageState.NULL, val profile: Profile? = null, val data: Map<Long, List<ChatBlock>>? = null, val message: String? = null) {
    companion object{
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
}

data class GroupInfoState(val state:Int = GroupInfoState.NULL, val resource: GroupEditResource? = null, val message: String? = null){
    companion object{
        const val NULL = 0
        const val LOADING = 1
        const val SUCCESS = 2
        const val ERROR = 3
    }
}

data class GroupEditResource(val name: List<String>, val avatar: List<ByteArray>, val pluginConnections: List<PluginConnection>)