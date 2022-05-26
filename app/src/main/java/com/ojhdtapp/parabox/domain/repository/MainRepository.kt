package com.ojhdtapp.parabox.domain.repository

interface MainRepository {
    suspend fun receiveNewMessage()
}