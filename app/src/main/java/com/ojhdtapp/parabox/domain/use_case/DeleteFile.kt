package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class DeleteFile @Inject constructor(
    val repository: MainRepository
) {
    suspend operator fun invoke(fileId: Long) {
        repository.deleteFile(fileId)
    }
}