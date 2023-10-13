import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.data.local.buildChatEntity
import com.ojhdtapp.parabox.data.local.buildContactEntity
import com.ojhdtapp.parabox.data.local.buildMessageEntity
import com.ojhdtapp.parabox.data.local.entity.ChatLatestMessageIdUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import com.ojhdtapp.parabox.data.local.entity.RecentQueryEntity
import com.ojhdtapp.parabox.data.local.entity.RecentQueryTimestampUpdate
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MainRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
) : MainRepository {
    override suspend fun receiveMessage(msg: ReceiveMessage, ext: ExtensionInfo): ParaboxResult {
        Log.d("parabox", "receiving msg from ${ext.name}")
        coroutineScope {
            val allowForegroundNotification = context.getDataStoreValue(
                DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION,
                false
            )
            val chatEntity = buildChatEntity(msg, ext)
            val contactEntity = buildContactEntity(msg, ext)
            val chatIdDeferred = async {
                db.chatDao.checkChat(chatEntity.pkg, chatEntity.uid)
                    ?: db.chatDao.insertChat(chatEntity)
            }
            val contactIdDeferred = async {
                db.contactDao.checkContact(contactEntity.pkg, contactEntity.uid)
                    ?: db.contactDao.insertContact(contactEntity)
            }
            val messageEntity =
                buildMessageEntity(msg, ext, contactIdDeferred.await(), chatIdDeferred.await())
            val messageIdDeferred = async {
                db.messageDao.insertMessage(messageEntity)
            }
            Log.d(
                "parabox",
                "chatId:${chatIdDeferred.await()};contactId:${contactIdDeferred.await()};messageId:${messageIdDeferred.await()}"
            )
            db.chatDao.updateLatestMessageId(
                ChatLatestMessageIdUpdate(
                    chatId = chatIdDeferred.await(),
                    latestMessageId = messageIdDeferred.await()
                )
            )
            if (chatIdDeferred.await() != -1L) {
                val originalNum =
                    db.chatDao.getChatById(chatIdDeferred.await())?.unreadMessageNum ?: 0
                db.chatDao.updateUnreadMessageNum(
                    ChatUnreadMessagesNumUpdate(
                        chatId = chatIdDeferred.await(),
                        unreadMessageNum = originalNum + 1
                    )
                )
            }

            context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0).also {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.MESSAGE_BADGE_NUM] = it + 1
                }
            }
        }

        return ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
    }

    override fun getRecentQuery(): Flow<Resource<List<RecentQuery>>> {
        return flow {
            emit(Resource.Loading())
            try {
                emit(
                    Resource.Success(db.recentQueryDao.getAllRecentQuery().map { it.toRecentQuery() })
                )
            } catch (e: Exception) {
                emit(Resource.Error("unknown error"))
            }
        }
    }

    override suspend fun submitRecentQuery(value: String): Boolean {
        return db.recentQueryDao.getRecentQueryByValue(value)?.let {
            return db.recentQueryDao.updateTimestamp(
                RecentQueryTimestampUpdate(it.id, System.currentTimeMillis())
            ) == 1
        } ?: kotlin.run {
            db.recentQueryDao.insertRecentQuery(
                RecentQueryEntity(value, System.currentTimeMillis())
            )
            true
        }
    }

    override suspend fun deleteRecentQuery(id: Long): Boolean {
        return db.recentQueryDao.deleteRecentQueryById(id) == 1
    }
}