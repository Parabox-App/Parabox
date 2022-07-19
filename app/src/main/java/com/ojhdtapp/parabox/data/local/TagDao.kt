package com.ojhdtapp.parabox.data.local

import androidx.room.*
import com.ojhdtapp.parabox.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert
    fun insertTag(tag: TagEntity): Long

    @Insert
    fun insertAllTags(tags: List<TagEntity>)

    @Query("SELECT * FROM tag_entity")
    fun queryAllTags(): Flow<List<TagEntity>>

    @Delete
    fun deleteTag(tag: TagEntity): Int

    @Query("DELETE FROM tag_entity WHERE value = :value")
    fun deleteTagByValue(value: String)

    @Query("DELETE FROM tag_entity")
    fun clearAllTags(): Int
}