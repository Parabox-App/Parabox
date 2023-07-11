package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

@Entity(tableName = "contact_entity")
data class ContactEntity(
    val name: String,
    val avatar: ParaboxResourceInfo,
    val pkg: String,
    val uid: String,
    @PrimaryKey(autoGenerate = true) val contactId: Long = 0,
){
    fun toContact(): Contact{
        return Contact(
            name, avatar, pkg, uid, contactId
        )
    }
}