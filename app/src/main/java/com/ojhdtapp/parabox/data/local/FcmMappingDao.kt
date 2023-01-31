package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ojhdtapp.parabox.data.local.entity.FcmMapping
import com.ojhdtapp.parabox.data.local.entity.FcmMappingSessionIdUpdate

@Dao
interface FcmMappingDao {
    @Insert
    fun insertFcmMapping(fcmMapping: FcmMapping): Long

    @Query("SELECT * FROM fcmmapping WHERE uid = :uid LIMIT 1")
    fun getFcmMappingByUid(uid: String): FcmMapping?

    @Query("SELECT * FROM fcmmapping WHERE id = :id LIMIT 1")
    fun getFcmMappingById(id: Long): FcmMapping?

    @Update(entity = FcmMapping::class)
    fun updateSessionId(obj: FcmMappingSessionIdUpdate)

}