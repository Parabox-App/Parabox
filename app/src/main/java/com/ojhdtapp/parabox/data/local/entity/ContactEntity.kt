package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity
data class ContactEntity(
    @Embedded val profile: MessageProfile,
    val latestMessage: Message?,
    @Embedded val connection: PluginConnection,
    @PrimaryKey val contactId: Int
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        connection = connection
    )
}
