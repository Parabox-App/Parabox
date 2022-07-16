package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.data.remote.dto.MessageDto
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class UpdateContactHiddenState @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(id: Long, value: Boolean) {
        repository.updateContactHiddenState(id, value)
    }
}