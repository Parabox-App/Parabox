package com.ojhdtapp.parabox.domain.fcm

import javax.inject.Inject

class FcmApiHelper @Inject constructor(
    private val fcmService: FcmService
) {
    suspend fun getVersion(url: String) = fcmService.getVersion(url)
}