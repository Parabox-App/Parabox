package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(
    val profile: Profile,
    val latestMessage: LatestMessage?,
    val contactId: Long,
    val senderId: Long,
    val tags: List<String>,
    val isHidden : Boolean = false
) : Parcelable{
    // Unused
    fun toContactEntity(): ContactEntity{
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            senderId = senderId,
            contactId = contactId,
            isHidden = isHidden,
            tags = tags
        )
    }
}
