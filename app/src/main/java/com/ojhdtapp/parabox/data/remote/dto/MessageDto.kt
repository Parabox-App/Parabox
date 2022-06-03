package com.ojhdtapp.parabox.data.remote.dto

import android.os.Parcelable
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDto(
    val contents: List<MessageContent>,
    val profile: MessageProfile,
    val subjectProfile: MessageProfile,
    val id: Int,
    val timestamp: Long,
    val pluginConnection: PluginConnection
) : Parcelable {
    fun toContactEntity(): ContactEntity {
        return ContactEntity(
            profile = subjectProfile,
            latestMessage = contents.getContentString(),
            latestMessageTimestamp = timestamp,
            unreadMessagesNum = 0,
            connection = pluginConnection,
            contactId = pluginConnection.objectId,
        )
    }

    fun toMessageEntity(): MessageEntity {
        return MessageEntity(
            contents = contents,
            profile = profile,
            timestamp = timestamp,
            messageId = id
        )
    }

    fun getContactMessageCrossRef(): ContactMessageCrossRef {
        return ContactMessageCrossRef(
            pluginConnection.objectId,
            id
        )
    }
}