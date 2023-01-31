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

    fun customizedProfileAndTag(id: Long, profile: Profile, tags: List<String>) {
        repository.updateCustomizedContactProfile(id, profile, tags)
    }

    fun tag(id: Long, tags: List<String>) {
        repository.updateContactTag(id, tags)
    }

    fun pinnedState(id: Long, value: Boolean) {
        repository.updateContactPinnedState(id, value)
    }

    fun notificationState(id: Long, value: Boolean) {
        repository.updateContactNotificationState(id, value)
    }

    fun archivedState(id: Long, value: Boolean){
        repository.updateContactArchivedState(id, value)
    }

    fun unreadMessagesNum(id: Long, value: Int){
        repository.updateContactUnreadMessagesNum(id, value)
    }

    fun backup(id: Long, value: Boolean){
        repository.updateContactBackupState(id, value)
    }

    fun disableFCM(id: Long, value: Boolean){
        repository.updateContactDisableFCMState(id, value)
    }
}