package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.data.local.entity.DownloadingState
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class UpdateFile @Inject constructor(
    val repository: MainRepository
) {
    fun downloadState(state: DownloadingState, target: File) {
        repository.updateDownloadingState(state, target)
    }

    fun downloadInfo(path: String?, downloadId: Long?, target: File) {
        repository.updateDownloadInfo(path, downloadId, target)
    }
}