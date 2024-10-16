package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.domain.repository.ChatRepository
import javax.inject.Inject

class UpdateChat @Inject constructor(
    private val chatRepository: ChatRepository
) {
    fun unreadMessagesNum(chatId: Long, value: Int): Boolean {
        return chatRepository.updateUnreadMessagesNum(chatId, value)
    }

    fun pin(chatId: Long, value: Boolean): Boolean {
        return chatRepository.updatePin(chatId, value)
    }

    fun hide(chatId: Long, value: Boolean): Boolean {
        return chatRepository.updateHide(chatId, value)
    }

    fun archive(chatId: Long, value: Boolean): Boolean {
        return chatRepository.updateArchive(chatId, value)
    }

    fun tags(chatId: Long, value: List<String>): Boolean {
        return chatRepository.updateTags(chatId, value)
    }

    fun notificationEnabled(chatId: Long, value: Boolean): Boolean {
        return chatRepository.updateNotificationEnabled(chatId, value)
    }
}