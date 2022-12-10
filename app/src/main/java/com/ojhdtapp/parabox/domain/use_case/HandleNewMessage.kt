package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class HandleNewMessage @Inject constructor(
    val repository: MainRepository
) {
    suspend operator fun invoke(dto: ReceiveMessageDto) {
        repository.handleNewMessage(dto)
    }

    suspend operator fun invoke(contents: List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>, pluginConnection: com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection, timestamp: Long, sendType: Int, withoutVerify: Boolean = false) : Long{
        return repository.handleNewMessage(contents, pluginConnection, timestamp, sendType, withoutVerify)
    }
}