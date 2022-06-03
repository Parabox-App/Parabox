package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.ContactEntity

data class Contact(
    val profile: MessageProfile,
    val latestMessage: String?,
    val latestMessageTimestamp: Long?,
    val unreadMessagesNum: Int = 0,
    val connection: PluginConnection,
    val isHidden : Boolean = false
){
    fun toContactEntity(id: Int): ContactEntity{
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            latestMessageTimestamp = latestMessageTimestamp,
            unreadMessagesNum = unreadMessagesNum,
            connection = connection,
            contactId = id,
            isHidden = isHidden
        )
    }
}
