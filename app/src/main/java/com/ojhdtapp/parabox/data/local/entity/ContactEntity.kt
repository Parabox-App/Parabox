package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.LatestMessage
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity(tableName = "contact_entity")
data class ContactEntity(
    @Embedded val profile: Profile,
    @Embedded val latestMessage: LatestMessage?,
    val connections: List<PluginConnection>,
    val isHidden: Boolean = false,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        connections = connections,
        isHidden = isHidden,
        contactId = contactId
    )
}
