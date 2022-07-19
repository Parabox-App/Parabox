package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.model.Tag
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TagControl @Inject constructor(
    val repository: MainRepository
) {
    fun get(): Flow<List<Tag>> {
        return repository.getContactTags()
    }

    fun add(value: String) {
        repository.addContactTag(value)
    }

    fun delete(value: String) {
        repository.deleteContactTag(value)
    }
}