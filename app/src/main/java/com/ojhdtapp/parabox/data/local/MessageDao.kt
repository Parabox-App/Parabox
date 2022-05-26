package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.domain.model.Message

@Dao
interface MessageDao {

    @Insert
    suspend fun insertMessage(message: MessageEntity)
}