import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.buildChatEntity
import com.ojhdtapp.parabox.data.local.buildContactEntity
import com.ojhdtapp.parabox.data.local.buildMessageEntity
import com.ojhdtapp.parabox.data.local.buildOutgoingMessageEntity
import com.ojhdtapp.parabox.data.local.entity.ChatBasicInfoUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatLatestMessageIdUpdate
import com.ojhdtapp.parabox.data.local.entity.ChatUnreadMessagesNumUpdate
import com.ojhdtapp.parabox.data.local.entity.ContactBasicInfoUpdate
import com.ojhdtapp.parabox.data.local.entity.ContactChatCrossRef
import com.ojhdtapp.parabox.data.local.entity.ContactEntity
import com.ojhdtapp.parabox.data.local.entity.MessageSendResultUpdate
import com.ojhdtapp.parabox.data.local.entity.MessageVerifyStateUpdate
import com.ojhdtapp.parabox.data.local.entity.RecentQueryEntity
import com.ojhdtapp.parabox.data.local.entity.RecentQueryTimestampUpdate
import com.ojhdtapp.parabox.domain.model.Connection
import com.ojhdtapp.parabox.domain.model.RecentQuery
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MainRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
    val notificationUtil: NotificationUtil
) : MainRepository {
    override suspend fun receiveMessage(msg: ReceiveMessage, ext: Connection.ConnectionSuccess): ParaboxResult {
        Log.d("parabox", "receiving msg from ${ext.name}")
        return coroutineScope {
            try {
                val info = ext.toExtensionInfo()
                val allowForegroundNotification = context.getDataStoreValue(
                    DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION,
                    false
                )
                val chatEntity = buildChatEntity(msg, info)
                val contactEntity = buildContactEntity(msg, info)
                val chatIdDeferred = async {
                    db.chatDao.checkChat(chatEntity.pkg, info.connectionId, chatEntity.uid)
                        ?: db.chatDao.insertChat(chatEntity)
                }
                val contactIdDeferred = async {
                    db.contactDao.checkContact(contactEntity.pkg, info.connectionId, contactEntity.uid)
                        ?: db.contactDao.insertContact(contactEntity)
                }
                // crossRef contact and chat
                db.contactChatCrossRefDao.insertContactChatCrossRef(
                    ContactChatCrossRef(
                        contactIdDeferred.await(),
                        chatIdDeferred.await()
                    )
                )

                val messageEntity =
                    buildMessageEntity(msg, info, contactIdDeferred.await(), chatIdDeferred.await())
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

                if (contactIdDeferred.await() != -1L) {
                    launch(Dispatchers.IO) {
                        val originalContact = db.contactDao.getContactById(contactIdDeferred.await())
                        if (originalContact?.name?.isNotEmpty() != true || originalContact.avatar is ParaboxResourceInfo.ParaboxEmptyInfo) {
                            val basicInfo = try {
                                ext.getUserBasicInfo(msg.sender.uid)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            if (basicInfo != null) {
                                db.contactDao.updateBasicInfo(
                                    ContactBasicInfoUpdate(
                                        contactId = contactIdDeferred.await(),
                                        name = basicInfo.name ?: originalContact?.name,
                                        avatar = basicInfo.avatar.takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo } ?: originalContact?.avatar ?: ParaboxResourceInfo.ParaboxEmptyInfo
                                    )
                                )
                            }
                        }
                    }
                }

                if (chatIdDeferred.await() != -1L) {
                    launch(Dispatchers.IO) {
                        val originalChat = db.chatDao.getChatByIdWithoutObserve(chatIdDeferred.await())
                        if (originalChat?.name?.isNotEmpty() != true || originalChat.avatar is ParaboxResourceInfo.ParaboxEmptyInfo) {
                            val basicInfo = try {
                                when (originalChat?.type) {
                                    ParaboxChat.TYPE_PRIVATE -> ext.getUserBasicInfo(msg.sender.uid)
                                    ParaboxChat.TYPE_GROUP -> ext.getGroupBasicInfo(msg.chat.uid)
                                    else -> null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            if (basicInfo != null) {
                                db.chatDao.updateBasicInfo(
                                    ChatBasicInfoUpdate(
                                        chatId = chatIdDeferred.await(),
                                        name = basicInfo.name ?: originalChat?.name,
                                        avatar = basicInfo.avatar.takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo } ?: originalChat?.avatar ?: ParaboxResourceInfo.ParaboxEmptyInfo
                                    )
                                )
                            }
                        }
                        val originalNum = originalChat?.unreadMessageNum ?: 0
                        db.chatDao.updateUnreadMessageNum(
                            ChatUnreadMessagesNumUpdate(
                                chatId = chatIdDeferred.await(),
                                unreadMessageNum = originalNum + 1
                            )
                        )
                    }
                }

                context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0).also {
                    context.dataStore.edit { preferences ->
                        preferences[DataStoreKeys.MESSAGE_BADGE_NUM] = it + 1
                    }
                }
                notificationUtil.sendNewMessageNotification(
                    messageIdDeferred.await(),
                    contactIdDeferred.await(),
                    chatIdDeferred.await(),
                    ext.toExtensionInfo())
                ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            } catch (e: Exception) {
                ParaboxResult(ParaboxResult.ERROR_UNKNOWN, e.message ?: ParaboxResult.ERROR_UNKNOWN_MSG)
            }
        }
    }

    override suspend fun insertOutgoingMessage(
        msg: SendMessage,
        pkg: String,
        extensionId: Long,
        chatId: Long,
    ): Long = withContext(Dispatchers.IO) {
        val selfContactId = getOrCreateSelfContactId(pkg, extensionId)
        val messageEntity = buildOutgoingMessageEntity(
            msg = msg,
            pkg = pkg,
            extensionId = extensionId,
            senderId = selfContactId,
            chatId = chatId,
        )
        val messageId = db.messageDao.insertMessage(messageEntity)
        if (messageId > 0L) {
            db.chatDao.updateLatestMessageId(
                ChatLatestMessageIdUpdate(
                    chatId = chatId,
                    latestMessageId = messageId,
                )
            )
        }
        messageId
    }

    override suspend fun markMessageSendSuccess(messageId: Long, remoteUid: String?) = withContext(Dispatchers.IO) {
        if (!remoteUid.isNullOrBlank()) {
            db.messageDao.updateSendResult(
                MessageSendResultUpdate(
                    messageId = messageId,
                    verified = true,
                    uid = remoteUid,
                )
            )
        } else {
            db.messageDao.updateVerifiedState(
                MessageVerifyStateUpdate(
                    messageId = messageId,
                    verified = true,
                )
            )
        }
        Unit
    }

    override suspend fun resetMessageForRetry(messageId: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        db.messageDao.updateTimestampAndUnverify(messageId, timestamp)
        Unit
    }

    override suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        db.messageDao.deleteMessageById(messageId)
        Unit
    }

    private suspend fun getOrCreateSelfContactId(pkg: String, extensionId: Long): Long {
        val existing = db.contactDao.checkContact(pkg, extensionId, SELF_CONTACT_UID)
        if (existing != null && existing > 0L) return existing
        val localName = context.getDataStoreValue(DataStoreKeys.USER_NAME, "我").ifBlank { "我" }
        val localAvatarStr = context.getDataStoreValue(DataStoreKeys.USER_AVATAR, "")
        val localAvatar = if (localAvatarStr.isNotBlank()) {
            ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(android.net.Uri.parse(localAvatarStr))
        } else {
            ParaboxResourceInfo.ParaboxEmptyInfo
        }
        return db.contactDao.insertContact(
            ContactEntity(
                name = localName,
                avatar = localAvatar,
                isFriend = false,
                pkg = pkg,
                connectionId = extensionId,
                uid = SELF_CONTACT_UID,
            )
        )
    }

    companion object {
        private const val SELF_CONTACT_UID = "__parabox_self__"
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