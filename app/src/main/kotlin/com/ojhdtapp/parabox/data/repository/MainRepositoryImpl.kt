import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.room.withTransaction
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.ExtensionResourceUri
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MainRepositoryImpl @Inject constructor(
    val context: Context,
    private val db: AppDatabase,
    val notificationUtil: NotificationUtil
) : MainRepository {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingBadgeDelta = AtomicInteger(0)
    @Volatile
    private var badgeFlushJob: Job? = null
    private val inFlightContactRefresh = ConcurrentHashMap.newKeySet<Long>()
    private val inFlightChatRefresh = ConcurrentHashMap.newKeySet<Long>()

    private fun scheduleBadgeIncrement() {
        pendingBadgeDelta.incrementAndGet()
        if (badgeFlushJob?.isActive == true) return
        badgeFlushJob = backgroundScope.launch {
            delay(BADGE_FLUSH_DEBOUNCE_MS)
            val delta = pendingBadgeDelta.getAndSet(0)
            if (delta > 0) {
                runCatching {
                    context.dataStore.edit { preferences ->
                        val current = preferences[DataStoreKeys.MESSAGE_BADGE_NUM] ?: 0
                        preferences[DataStoreKeys.MESSAGE_BADGE_NUM] = current + delta
                    }
                }
            }
        }
    }

    override suspend fun receiveMessage(msg: ReceiveMessage, ext: Connection.ConnectionSuccess): ParaboxResult {
        return withContext(Dispatchers.IO) {
            try {
                val info = ext.toExtensionInfo()
                if (msg.uuid.isNotBlank()) {
                    val existingMessageId = db.messageDao.checkMessage(info.pkg, info.connectionId, msg.uuid)
                    if (existingMessageId != null && existingMessageId > 0L) {
                        return@withContext ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
                    }
                }

                var resolvedChatId = -1L
                var resolvedContactId = -1L
                var resolvedMessageId = -1L
                var needContactInfoRefresh = false
                var needChatInfoRefresh = false
                var originalContactName: String? = null
                var originalContactAvatar: ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo
                var originalChatName: String? = null
                var originalChatAvatar: ParaboxResourceInfo = ParaboxResourceInfo.ParaboxEmptyInfo
                var originalChatType: Int = msg.chat.type

                db.withTransaction {
                    val chatEntity = buildChatEntity(msg, info)
                    resolvedChatId = db.chatDao.checkChat(chatEntity.pkg, info.connectionId, chatEntity.uid)
                        ?: db.chatDao.insertChat(chatEntity)

                    resolvedContactId = if (msg.sentByMe) {
                        getOrCreateSelfContactId(info.pkg, info.connectionId)
                    } else {
                        val contactEntity = buildContactEntity(msg, info)
                        db.contactDao.checkContact(contactEntity.pkg, info.connectionId, contactEntity.uid)
                            ?: db.contactDao.insertContact(contactEntity)
                    }

                    if (!msg.sentByMe && resolvedContactId != -1L && resolvedChatId != -1L) {
                        db.contactChatCrossRefDao.insertContactChatCrossRef(
                            ContactChatCrossRef(resolvedContactId, resolvedChatId)
                        )
                    }

                    val messageEntity = buildMessageEntity(msg, info, resolvedContactId, resolvedChatId)
                    resolvedMessageId = db.messageDao.insertMessage(messageEntity)

                    if (resolvedChatId != -1L) {
                        db.chatDao.updateLatestMessageId(
                            ChatLatestMessageIdUpdate(
                                chatId = resolvedChatId,
                                latestMessageId = resolvedMessageId,
                            )
                        )
                        val originalChat = db.chatDao.getChatByIdWithoutObserve(resolvedChatId)
                        originalChatName = originalChat?.name
                        originalChatAvatar = originalChat?.avatar ?: ParaboxResourceInfo.ParaboxEmptyInfo
                        originalChatType = originalChat?.type ?: msg.chat.type
                        val chatAvatarNeedsRefresh = originalChat == null ||
                            originalChat.avatar is ParaboxResourceInfo.ParaboxEmptyInfo ||
                            ExtensionResourceUri.isExpiredTdlibFileProviderUri(originalChat.avatar)
                        val incomingChatAvatar = msg.chat.basicInfo.avatar
                            .takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo }
                        if (chatAvatarNeedsRefresh && incomingChatAvatar != null && originalChat?.name?.isNotEmpty() == true) {
                            db.chatDao.updateBasicInfo(
                                ChatBasicInfoUpdate(
                                    chatId = resolvedChatId,
                                    name = originalChat.name,
                                    avatar = incomingChatAvatar,
                                )
                            )
                        } else if (originalChat?.name?.isNotEmpty() != true || chatAvatarNeedsRefresh) {
                            needChatInfoRefresh = true
                        }

                        val originalNum = originalChat?.unreadMessageNum ?: 0
                        if (msg.sentByMe) {
                            if (originalNum > 0) {
                                db.chatDao.updateUnreadMessageNum(
                                    ChatUnreadMessagesNumUpdate(
                                        chatId = resolvedChatId,
                                        unreadMessageNum = 0,
                                    )
                                )
                            }
                        } else {
                            db.chatDao.updateUnreadMessageNum(
                                ChatUnreadMessagesNumUpdate(
                                    chatId = resolvedChatId,
                                    unreadMessageNum = originalNum + 1,
                                )
                            )
                        }
                    }

                    if (!msg.sentByMe && resolvedContactId != -1L) {
                        val originalContact = db.contactDao.getContactById(resolvedContactId)
                        originalContactName = originalContact?.name
                        originalContactAvatar = originalContact?.avatar ?: ParaboxResourceInfo.ParaboxEmptyInfo
                        val contactAvatarNeedsRefresh = originalContact == null ||
                            originalContact.avatar is ParaboxResourceInfo.ParaboxEmptyInfo ||
                            ExtensionResourceUri.isExpiredTdlibFileProviderUri(originalContact.avatar)
                        val incomingContactAvatar = msg.sender.basicInfo.avatar
                            .takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo }
                        if (contactAvatarNeedsRefresh && incomingContactAvatar != null && originalContact?.name?.isNotEmpty() == true) {
                            db.contactDao.updateBasicInfo(
                                ContactBasicInfoUpdate(
                                    contactId = resolvedContactId,
                                    name = originalContact.name,
                                    avatar = incomingContactAvatar,
                                )
                            )
                        } else if (originalContact?.name?.isNotEmpty() != true || contactAvatarNeedsRefresh) {
                            needContactInfoRefresh = true
                        }
                    }
                }

                if (needContactInfoRefresh && resolvedContactId != -1L && inFlightContactRefresh.add(resolvedContactId)) {
                    backgroundScope.launch {
                        try {
                            val basicInfo = runCatching { ext.getUserBasicInfo(msg.sender.uid) }.getOrNull()
                            if (basicInfo != null) {
                                db.contactDao.updateBasicInfo(
                                    ContactBasicInfoUpdate(
                                        contactId = resolvedContactId,
                                        name = basicInfo.name ?: originalContactName,
                                        avatar = basicInfo.avatar.takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo }
                                            ?: originalContactAvatar,
                                    )
                                )
                            }
                        } finally {
                            inFlightContactRefresh.remove(resolvedContactId)
                        }
                    }
                }

                if (needChatInfoRefresh && resolvedChatId != -1L && inFlightChatRefresh.add(resolvedChatId)) {
                    backgroundScope.launch {
                        try {
                            val basicInfo = runCatching {
                                when (originalChatType) {
                                    ParaboxChat.TYPE_PRIVATE -> ext.getUserBasicInfo(msg.chat.uid.ifBlank { msg.sender.uid })
                                    ParaboxChat.TYPE_GROUP -> ext.getGroupBasicInfo(msg.chat.uid)
                                    else -> null
                                }
                            }.getOrNull()
                            if (basicInfo != null) {
                                db.chatDao.updateBasicInfo(
                                    ChatBasicInfoUpdate(
                                        chatId = resolvedChatId,
                                        name = basicInfo.name ?: originalChatName,
                                        avatar = basicInfo.avatar.takeIf { it !is ParaboxResourceInfo.ParaboxEmptyInfo }
                                            ?: originalChatAvatar,
                                    )
                                )
                            }
                        } finally {
                            inFlightChatRefresh.remove(resolvedChatId)
                        }
                    }
                }

                if (!msg.sentByMe) {
                    scheduleBadgeIncrement()
                    notificationUtil.enqueueNewMessageNotification(
                        messageId = resolvedMessageId,
                        contactId = resolvedContactId,
                        chatId = resolvedChatId,
                        connectionInfo = info,
                    )
                }
                ParaboxResult(ParaboxResult.SUCCESS, ParaboxResult.SUCCESS_MSG)
            } catch (e: Exception) {
                Log.e("parabox", "receiveMessage failed", e)
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
        private const val BADGE_FLUSH_DEBOUNCE_MS = 500L
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