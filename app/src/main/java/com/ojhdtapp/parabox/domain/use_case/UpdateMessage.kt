package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class UpdateMessage @Inject constructor(
    val repository: MainRepository
) {
    fun verifiedState(id: Long, value: Boolean) {
        repository.updateMessageVerifiedState(id, value)
    }
}