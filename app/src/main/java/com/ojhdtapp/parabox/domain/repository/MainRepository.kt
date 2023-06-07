package com.ojhdtapp.parabox.domain.repository

import androidx.paging.PagingSource
import coil.request.Tags
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessageResult
import kotlinx.coroutines.flow.Flow

interface MainRepository {
    suspend fun receiveMessage(msg: ReceiveMessage): ReceiveMessageResult

}