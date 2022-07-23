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
    val isHidden: Boolean = false,
    val isPinned: Boolean = false,
    val enableNotifications: Boolean = true,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return if (other is Contact) {
            contactId == other.contactId
        } else {
            super.equals(other)
        }
    }

    // Unused
    fun toContactEntity(): ContactEntity {
        return ContactEntity(
            profile = profile,
            latestMessage = latestMessage,
            senderId = senderId,
            contactId = contactId,
            isHidden = isHidden,
            isPinned = isPinned,
            enableNotifications = enableNotifications,
            tags = tags
        )
    }
}
