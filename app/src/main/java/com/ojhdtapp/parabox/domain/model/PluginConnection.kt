package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.parabox.data.local.entity.PluginConnectionEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class PluginConnection(val connectionType: Int, val objectId: Long) : Parcelable{
    fun toPluginConnectionEntity() : PluginConnectionEntity{
        return PluginConnectionEntity(
            connectionType = connectionType,
            objectId = objectId
        )
    }
}
