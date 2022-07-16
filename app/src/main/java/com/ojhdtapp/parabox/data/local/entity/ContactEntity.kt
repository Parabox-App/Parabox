package com.ojhdtapp.parabox.data.local.entity

import androidx.room.*
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.LatestMessage
import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.model.PluginConnection

@Entity(tableName = "contact_entity")
data class ContactEntity(
    @Embedded val profile: Profile,
    @Embedded val latestMessage: LatestMessage?,
    val senderId: Long,
    val isHidden: Boolean = false,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        isHidden = isHidden,
        contactId = contactId,
        senderId = senderId
    )
}
@Entity
data class ContactHiddenStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "isHidden")
    val isHidden: Boolean
)