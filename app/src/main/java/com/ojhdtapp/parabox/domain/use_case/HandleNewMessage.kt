package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.messagedto.ReceiveMessageDto
import com.ojhdtapp.messagedto.SendMessageDto
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class HandleNewMessage @Inject constructor(
    val repository: MainRepository
) {
    suspend operator fun invoke(dto: ReceiveMessageDto) {
        repository.handleNewMessage(dto)
    }

    suspend operator fun invoke(dto : SendMessageDto){
        repository.handleNewMessage(dto)
    }
}