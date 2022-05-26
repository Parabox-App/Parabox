package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.ContactEntity

data class Contact(
    val name: String,
    val avatar: ByteArray?,
    val latestMessage: String,
    val connection: PluginConnection,
){
    fun toContactEntity(id: Int): ContactEntity{
        return ContactEntity(
            name = name,
            avatar = avatar,
            latestMessage = latestMessage,
            connection = connection,
            contactId = id
        )
    }
}
