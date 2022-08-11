package com.ojhdtapp.parabox_dto

import android.os.Parcelable
import com.ojhdtapp.parabox.data.local.entity.PluginConnectionEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class PluginConnection(val connectionType: Int, val objectId: Long, val id: Long) :
    Parcelable {
    fun toPluginConnectionEntity(): PluginConnectionEntity {
        return PluginConnectionEntity(
            connectionType = connectionType,
            objectId = objectId,
            id = id,
        )
    }

    override fun equals(other: Any?): Boolean {
        return if (other is PluginConnection) {
            objectId == other.objectId
        } else
            super.equals(other)
    }

    override fun hashCode(): Int {
        var result = connectionType
        result = 31 * result + objectId.hashCode()
        return result
    }
}
