package com.ojhdtapp.parabox.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ojhdtapp.parabox.data.local.entity.ChatEntity
import com.ojhdtapp.parabox.data.local.entity.ContactChatCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.ConnectionInfoEntity
import com.ojhdtapp.parabox.data.local.entity.MessageEntity
import com.ojhdtapp.parabox.data.local.entity.RecentQueryEntity

@Database(
    entities = [MessageEntity::class, ContactEntity::class, ChatEntity::class, RecentQueryEntity::class, ConnectionInfoEntity::class, ContactChatCrossRef::class],
    version = 2,
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

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_message_entity_chatId_timestamp` ON `message_entity` (`chatId`, `timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_message_entity_pkg_extensionId_uid` ON `message_entity` (`pkg`, `extensionId`, `uid`)"
                )
            }
        }
    }
}
