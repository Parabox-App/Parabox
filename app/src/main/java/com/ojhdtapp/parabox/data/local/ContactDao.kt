package com.ojhdtapp.parabox.data.local

import androidx.room.Dao
import androidx.room.Insert
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.domain.model.Contact

@Dao
interface ContactDao {

    @Insert
    suspend fun insertContact(contact: ContactEntity)
}