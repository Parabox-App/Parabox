package com.ojhdtapp.parabox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactChatCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.RecentQueryEntity

@Database(
    entities = [MessageEntity::class, ContactEntity::class, ChatEntity::class, RecentQueryEntity::class, ConnectionInfoEntity::class, ContactChatCrossRef::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val messageDao: MessageDao
    abstract val contactDao: ContactDao
    abstract val chatDao: ChatDao
    abstract val recentQueryDao: RecentQueryDao
    abstract val connectionInfoDao: ExtensionInfoDao
    abstract val contactChatCrossRefDao: ContactChatCrossRefDao
}
