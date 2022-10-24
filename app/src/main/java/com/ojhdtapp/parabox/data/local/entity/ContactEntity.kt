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
    val isPinned : Boolean = false,
    val isArchived: Boolean = false,
    val enableNotifications : Boolean = true,
    val shouldBackup: Boolean = false,
    val disableFCM: Boolean = false,
    val tags: List<String>,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
){
    fun toContact() = Contact(
        profile = profile,
        latestMessage = latestMessage,
        isHidden = isHidden,
        isPinned = isPinned,
        isArchived = isArchived,
        enableNotifications = enableNotifications,
        shouldBackup = shouldBackup,
        disableFCM = disableFCM,
        contactId = contactId,
        senderId = senderId,
        tags = tags
    )
}
@Entity
data class ContactHiddenStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "isHidden")
    val isHidden: Boolean
)

@Entity
data class ContactProfileAndTagUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "avatar")
    val avatar: String?,
    @ColumnInfo(name = "tags")
    val tags: List<String>,
)

@Entity
data class ContactTagUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "tags")
    val tags: List<String>,
)

@Entity
data class ContactPinnedStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean,
)

@Entity
data class ContactNotificationStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "enableNotifications")
    val enableNotifications: Boolean,
)

@Entity
data class ContactArchivedStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "isArchived")
    val isArchived: Boolean,
)

@Entity
data class ContactUnreadMessagesNumUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "unreadMessagesNum")
    val unreadMessagesNum: Int,
)

@Entity
data class ContactShouldBackupUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "shouldBackup")
    val shouldBackup: Boolean,
)
@Entity
data class ContactDisableFCMStateUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "disableFCM")
    val disableFCM: Boolean,
)