package com.ojhdtapp.parabox.domain.model

import com.ojhdtapp.parabox.data.local.entity.TagEntity

data class Tag(
    val value: String,
    val id: Long,
) {
    fun toTagEntity(): TagEntity = TagEntity(value, id)
}
