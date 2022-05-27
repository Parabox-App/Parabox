package com.ojhdtapp.parabox.data.remote.dto

import android.os.Parcelable
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageDto(
    val contents: List<MessageContent>,
    val profile: MessageProfile,
    val timestamp: Long,
    val pluginConnection: PluginConnection
) : Parcelable {
    fun toMessageEntity(id: Int): MessageEntity {
        return MessageEntity(
            contents = contents,
            profile = profile,
            timestamp = timestamp,
            messageId = id
        )
    }

    fun toContactEntity(id: Int): ContactEntity {
        return ContactEntity(
            profile.name,
            profile.avatar,
            contents.lastOrNull()?.getContentString() ?: "",
            pluginConnection,
            id
        )
    }
}