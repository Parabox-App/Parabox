package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.MessageProfile
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity(tableName = "contact_entity")
data class ContactEntity(
    @Embedded val profile: MessageProfile,
    val latestMessage: String?,
    val latestMessageTimestamp: Long?,
    val unreadMessagesNum: Int = 0,
    @Embedded val connection: PluginConnection,
    @PrimaryKey val contactId: Int,
    val isHidden: Boolean = false
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        latestMessageTimestamp = latestMessageTimestamp,
        unreadMessagesNum = unreadMessagesNum,
        connection = connection,
        isHidden = isHidden
    )
}
