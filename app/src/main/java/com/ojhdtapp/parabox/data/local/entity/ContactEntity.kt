package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity
data class ContactEntity(
    val name: String,
    val avatar: ByteArray?,
    val latestMessage: String?,
    @Embedded val connection: PluginConnection,
    @PrimaryKey val contactId: Int
){
    fun toContact() = Contact(
        name = name,
        avatar = avatar,
        latestMessage = latestMessage,
        connection = connection
    )
}
