package com.ojhdtapp.parabox.domain.repository

import com.ojhdtapp.parabox.data.remote.dto.MessageDto

interface MainRepository {
    suspend fun handleNewMessage(dto: MessageDto)
}