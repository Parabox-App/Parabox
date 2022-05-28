package com.ojhdtapp.parabox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ContactMessageCrossRef
import com.ojhdtapp.parabox.data.local.entity.MessageEntity

@Database(entities = [ContactEntity::class, MessageEntity::class, ContactMessageCrossRef::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val contactDao: ContactDao
    abstract val messageDao: MessageDao
    abstract val contactMessageCrossRefDao: ContactMessageCrossRefDao
}
