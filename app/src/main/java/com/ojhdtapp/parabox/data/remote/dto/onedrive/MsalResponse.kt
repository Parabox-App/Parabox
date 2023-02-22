package com.ojhdtapp.parabox.data.remote.dto.onedrive

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
/**
 * @Author laoyuyu
 * @Description
 * @Date 2021/2/6
 **/
@Keep
class MsalResponse<T> {
    @SerializedName("value")
    val value: T? = null
        get() {
            // https://docs.microsoft.com/zh-cn/graph/errors
            if (error?.code == "unauthenticated") {
//                OneDriveUtil.initOneDrive {
//                    if (it) {
//                        OneDriveUtil.loadAccount()
//                        return@initOneDrive
//                    }
//                }
            }
            return field
        }

    @SerializedName("error")
    val error: MsalErrorInfo? = null
}

@Keep
data class MsalErrorInfo(
    val code: String, // https://docs.microsoft.com/zh-cn/graph/errors#code-property
    val message: String
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}