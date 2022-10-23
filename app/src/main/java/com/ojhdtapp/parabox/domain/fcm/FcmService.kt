package com.ojhdtapp.parabox.domain.fcm

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface FcmService {
    @GET
    suspend fun getVersion(@Url url: String) : Response<FcmConnectionResponse>
}