package com.ojhdtapp.paraboxdevelopmentkit.model.contact

import android.net.Uri
import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ParaboxContact(
    val name: String?,
    val avatar: ParaboxResourceInfo,
    val uid: String,
) : Parcelable{
    companion object{
        const val TYPE_GROUP = 0
        const val TYPE_PRIVATE = 1
        const val TYPE_OTHER = 2
    }
}
