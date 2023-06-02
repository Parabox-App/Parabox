package com.ojhdtapp.paraboxdevelopmentkit.model.message

import android.os.Parcelable
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo

sealed interface ParaboxMessageElement: Parcelable{
    fun contentToString(): String
}

//interface ParaboxRemoteMessageElement: ParaboxMessageElement{
//    val remoteInfo: ParaboxResourceInfo.ParaboxRemoteInfo
//}
//
//interface ParaboxLocalMessageElement: ParaboxMessageElement{
//    var localInfo: ParaboxResourceInfo.ParaboxLocalInfo
//    suspend fun upload()
//}