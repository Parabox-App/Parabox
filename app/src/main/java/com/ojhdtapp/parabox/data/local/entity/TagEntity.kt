package com.ojhdtapp.parabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.Tag

@Entity(tableName = "tag_entity")
data class TagEntity(
    val value: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
) {
    fun toTag(): Tag = Tag(value, id)
}