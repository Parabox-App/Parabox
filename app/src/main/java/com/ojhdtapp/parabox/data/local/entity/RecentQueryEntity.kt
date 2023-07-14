package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.parabox.domain.model.RecentQuery

@Entity(tableName = "recent_query_entity")
data class RecentQueryEntity(
    val value: String,
    val timestamp: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
) {
    fun toRecentQuery() : RecentQuery{
        return RecentQuery(
            value, timestamp, id
        )
    }
}

@Entity
data class RecentQueryTimestampUpdate(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)