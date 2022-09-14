package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class UpdateDownloadingState @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(state: DownloadingState, target: File, path: String){
        repository.updateDownloadingState(state, target, path)
    }
}