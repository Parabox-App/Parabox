package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.RecentQueryEntity
import com.ojhdtapp.parabox.data.local.entity.RecentQueryTimestampUpdate

@Dao
interface RecentQueryDao {
    @Insert
    fun insertRecentQuery(recentQuery: RecentQueryEntity): Long

    @Query("DELETE FROM recent_query_entity WHERE id = :id")
    fun deleteRecentQueryById(id: Long): Int

    @Query("SELECT * FROM recent_query_entity")
    fun getAllRecentQuery(): List<RecentQueryEntity>

    @Update(entity = RecentQueryEntity::class)
    fun updateTimestamp(obj: RecentQueryTimestampUpdate): Int
}