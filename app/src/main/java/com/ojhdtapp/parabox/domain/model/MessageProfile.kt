package com.ojhdtapp.parabox.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageProfile(
    val name: String,
    val avatar: ByteArray?
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageProfile

        if (name != other.name) return false
        if (avatar != null) {
            if (other.avatar == null) return false
            if (!avatar.contentEquals(other.avatar)) return false
        } else if (other.avatar != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (avatar?.contentHashCode() ?: 0)
        return result
    }
}
