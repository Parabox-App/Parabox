package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.ContactEntity

data class Contact(
    val profile: MessageProfile,
    val latestMessage: Message?,
    val connection: PluginConnection,
    val isHidden : Boolean = false
){
    fun toContactEntity(id: Int): ContactEntity{
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            connection = connection,
            contactId = id,
            isHidden = isHidden
        )
    }
}
