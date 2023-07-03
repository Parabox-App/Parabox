package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult

interface MainRepository {
    suspend fun receiveMessage(msg: ReceiveMessage, ext: ExtensionInfo): ParaboxResult

}