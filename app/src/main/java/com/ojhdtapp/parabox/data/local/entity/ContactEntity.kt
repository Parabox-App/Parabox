package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

@Entity(tableName = "contact_entity")
data class ContactEntity(
    val name: String?,
    val avatar: ParaboxResourceInfo,
    val pkg: String,
    val uid: String,
    val extensionId: Long,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
){
    fun toContact(): Contact{
        return Contact(
            name, avatar, pkg, uid, extensionId, contactId
        )
    }
}

@Entity
data class ContactBasicInfoUpdate(
    @ColumnInfo(name = "contactId")
    val contactId: Long,
    @ColumnInfo(name = "name")
    val name: String?,
    @ColumnInfo(name = "avatar")
    val avatar: ParaboxResourceInfo,
)