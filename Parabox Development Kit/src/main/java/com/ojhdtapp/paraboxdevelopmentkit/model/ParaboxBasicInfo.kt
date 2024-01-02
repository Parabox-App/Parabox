package com.ojhdtapp.paraboxdevelopmentkit.model

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ParaboxBasicInfo(
    val name: String?,
    val avatar: ParaboxResourceInfo,
) : Parcelable
