package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.File
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFiles @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(query: String): Flow<Resource<List<File>>> {
        return repository.getFiles(query)
    }

    fun allStatic() : List<File>{
        return repository.getAllFilesStatic()
    }
}