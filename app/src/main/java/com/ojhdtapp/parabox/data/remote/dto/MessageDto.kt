package com.ojhdtapp.parabox.data.remote.dto

import com.ojhdtapp.messagedto.MessageDto
import com.ojhdtapp.messagedto.message_content.getContentString
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.LatestMessage
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.message_content.Image
import com.ojhdtapp.parabox.domain.model.message_content.MessageContent
import com.ojhdtapp.parabox.domain.model.message_content.PlainText

fun MessageDto.toContactEntity(): ContactEntity {
    return ContactEntity(
        profile = subjectProfile.toProfile(),
        latestMessage = LatestMessage(
            content = contents.getContentString(),
            timestamp = timestamp,
            unreadMessagesNum = 0
        ),
        contactId = pluginConnection.objectId,
        senderId = pluginConnection.objectId,
        isHidden = false,
        isPinned = false,
        isArchived = false,
        enableNotifications = true,
        tags = emptyList()
    )
}

fun MessageDto.toMessageEntity(): MessageEntity {
    return MessageEntity(
        contents = contents.toMessageContentList(),
        profile = profile.toProfile(),
        timestamp = timestamp,
    )
}

fun com.ojhdtapp.messagedto.Profile.toProfile() : Profile{
    return Profile(this.name, this.avatar)
}

fun List<com.ojhdtapp.messagedto.message_content.MessageContent>.toMessageContentList() : List<MessageContent>{
    return this.map {
        it.toMessageContent()
    }
}

fun com.ojhdtapp.messagedto.message_content.MessageContent.toMessageContent() : MessageContent{
    return when(this){
        is com.ojhdtapp.messagedto.message_content.PlainText -> PlainText(this.text)
        is com.ojhdtapp.messagedto.message_content.Image -> Image(this.url)
        is com.ojhdtapp.messagedto.message_content.At -> com.ojhdtapp.parabox.domain.model.message_content.At(this.target, this.name)
        else -> PlainText(this.getContentString())
    }
}

fun com.ojhdtapp.messagedto.PluginConnection.toPluginConnection() : PluginConnection{
    return PluginConnection(this.connectionType, this.objectId, this.id)
}