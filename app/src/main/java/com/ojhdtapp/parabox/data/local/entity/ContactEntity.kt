package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
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
    @PrimaryKey val contactId: Int,
    val isHidden: Boolean = false
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        connections = connections,
        isHidden = isHidden
    )
}
