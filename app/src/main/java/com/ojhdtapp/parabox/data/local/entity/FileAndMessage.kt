package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class FileAndMessage(
    @Embedded val file: FileEntity,
    @Relation(
        parentColumn = "fileId",
        entityColumn = "messageId"
    )
    val message: MessageEntity,
) {
    fun toFileAndMessage() = com.ojhdtapp.parabox.domain.model.FileAndMessage(
        file = file.toFile(),
        message = message.toMessage()
    )
}
