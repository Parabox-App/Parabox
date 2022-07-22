package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.model.Profile
import com.ojhdtapp.parabox.domain.repository.MainRepository
import javax.inject.Inject

class UpdateContact @Inject constructor(
    val repository: MainRepository
) {
    fun hiddenState(id: Long, value: Boolean) {
        repository.updateContactHiddenState(id, value)
    }

    fun profileAndTag(id: Long, profile: Profile, tags: List<String>) {
        repository.updateContactProfileAndTag(id, profile, tags)
    }

    fun tag(id: Long, tags: List<String>) {
        repository.updateContactTag(id, tags)
    }
}