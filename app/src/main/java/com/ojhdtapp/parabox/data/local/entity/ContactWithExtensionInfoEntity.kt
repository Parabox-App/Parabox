package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.ojhdtapp.parabox.domain.model.ContactWithExtensionInfo

data class ContactWithExtensionInfoEntity(
    @Embedded
    val contact: ContactEntity,
    @Relation(parentColumn = "extensionId", entityColumn = "extensionId")
    val extensionInfo: ExtensionInfoEntity
) {
    fun toContactWithExtensionInfo(): ContactWithExtensionInfo {
        return ContactWithExtensionInfo(
            contact.toContact(),
            extensionInfo.toExtensionInfo()
        )
    }
}
