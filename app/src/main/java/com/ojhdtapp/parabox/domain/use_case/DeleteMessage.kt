package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class DeleteMessage @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(messageId: Long){
        repository.deleteMessageById(messageId = messageId)
    }

    operator fun invoke(messageIdList: List<Long>){
        repository.deleteMessageById(messageIdList = messageIdList)
    }
}