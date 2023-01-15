package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ojhdtapp.parabox.data.local.entity.FcmMapping

@Dao
interface FcmMappingDao {
    @Insert
    fun insertFcmMapping(fcmMapping: FcmMapping): Long

    @Query("SELECT * FROM fcmmapping WHERE uid = :uid LIMIT 1")
    fun getFcmMappingByUid(uid: String): FcmMapping?

    @Query("SELECT * FROM fcmmapping WHERE id = :id LIMIT 1")
    fun getFcmMappingById(id: Long): FcmMapping?

}