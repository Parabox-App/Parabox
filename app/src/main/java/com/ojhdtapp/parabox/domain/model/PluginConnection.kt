package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ObjectIdUtil
import com.ojhdtapp.parabox.data.local.entity.PluginConnectionEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class PluginConnection(val connectionType: Int, val objectId: Long, val id: Long) :
    Parcelable {

    fun toSenderPluginConnection(): com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection =
        com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection(this.connectionType, ObjectIdUtil.getSendTargetType(objectId, connectionType.toString().length), this.id)

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
