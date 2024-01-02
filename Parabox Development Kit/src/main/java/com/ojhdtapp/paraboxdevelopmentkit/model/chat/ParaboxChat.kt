package com.ojhdtapp.paraboxdevelopmentkit.model.chat

import android.net.Uri
import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ParaboxChat(
    val basicInfo: ParaboxBasicInfo,
    val type: Int,
    val uid: String,
) : Parcelable{
    companion object{
        const val TYPE_GROUP = 0
        const val TYPE_PRIVATE = 1
        const val TYPE_OTHER = 2
    }
}
