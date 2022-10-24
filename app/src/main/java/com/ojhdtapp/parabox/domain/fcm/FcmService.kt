package com.ojhdtapp.parabox.domain.fcm

import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface FcmService {
    @GET
    suspend fun getVersion(@Url url: String) : Response<FcmConnectionResponse>

    @Headers("Content-Type: application/json")
    @POST
    suspend fun pushReceiveDto(@Url url: String, @Body fcmReceiveModel: FcmReceiveModel ) : Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST
    suspend fun pushSendDto(@Url url: String, @Body fcmSendModel: FcmSendModel ) : Response<ResponseBody>
}