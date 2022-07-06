package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.ContactEntity

data class Contact(
    val profile: Profile,
    val latestMessage: LatestMessage?,
    val contactId: Long,
    val senderId: Long,
    val isHidden : Boolean = false
){
    // Unused
    fun toContactEntity(): ContactEntity{
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            senderId = senderId,
            contactId = contactId,
            isHidden = isHidden,
        )
    }
}
