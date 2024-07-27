package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo

data class ContactWithExtensionInfoEntity(
    @Embedded
    val contact: ContactEntity,
    @Relation(parentColumn = "connectionId", entityColumn = "connectionId")
    val connectionInfo: ConnectionInfoEntity
) {
    fun toContactWithExtensionInfo(): ContactWithExtensionInfo {
        return ContactWithExtensionInfo(
            contact.toContact(),
            connectionInfo.toConnectionInfo()
        )
    }
}
