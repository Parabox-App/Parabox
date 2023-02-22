package com.ojhdtapp.parabox.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.paging.PagingSource
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.HiltApplication
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.getContentString
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.entity.*
import com.ojhdtapp.parabox.data.remote.dto.*
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.*
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.ui.util.WorkingMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.awaitResponse
import java.io.IOException
import javax.inject.Inject

class MainRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    val context: Context,
    private val notificationUtil: NotificationUtil,
    private val fcmApiHelper: FcmApiHelper,
) : MainRepository {
    override suspend fun handleNewMessage(dto: ReceiveMessageDto) {
        coroutineScope {
            // Properties
            val allowForegroundNotification = context.dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION] ?: false
            }.first()
            val enableFcm = context.dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
            }.first()
            val fcmCloudStorage = context.dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE] ?: false
            }.first()
//            val fcmRole = context.dataStore.data.map { preferences ->
//                preferences[DataStoreKeys.SETTINGS_FCM_ROLE] ?: FcmConstants.Role.SENDER.ordinal
//            }.first()
            val workingMode = context.dataStore.data.map { preferences ->
                preferences[DataStoreKeys.SETTINGS_WORKING_MODE] ?: WorkingMode.NORMAL.ordinal
            }.first()

            val messageEntity = dto.toMessageEntity(context)
            val messageIdDeferred = async<Long> {
                database.messageDao.insertMessage(messageEntity)
            }
            // If MessageContent Type is File ...
            if (messageIdDeferred.await() != -1L) {
                dto.contents.filterIsInstance<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.File>().forEach {
                    database.fileDao.insertFile(
                        FileEntity(
                            url = it.url,
                            uriString = it.uri?.toString(),
                            name = it.name,
                            extension = it.extension,
                            size = it.size,
                            timestamp = it.lastModifiedTime,
                            profileName = dto.subjectProfile.name,
                            relatedContactId = dto.pluginConnection.objectId,
                            relatedMessageId = messageIdDeferred.await(),
                            cloudType = it.cloudType,
                            cloudId = it.cloudId
                        )
                    )
                }
            }
            val contactIdDeferred = async<Long> {
                database.contactDao.insertContact(
//                    dto.toContactEntityWithUnreadMessagesNumUpdate(
//                        database.contactDao
//                    )
                    dto.toContactEntity(context)
                )
            }
            val pluginConnectionDeferred = async<Long> {
                database.contactDao.insertPluginConnection(
                    dto.pluginConnection.toPluginConnection().toPluginConnectionEntity()
                )
            }
//            database.contactDao.updateHiddenState(ContactHiddenStateUpdate(dto.pluginConnection.objectId, false))
            if (pluginConnectionDeferred.await() != -1L) {
                database.contactDao.insertContactPluginConnectionCrossRef(
                    ContactPluginConnectionCrossRef(
                        contactId = dto.pluginConnection.objectId,
                        objectId = pluginConnectionDeferred.await()
                    )
                )
            }
            database.contactMessageCrossRefDao.insertContactMessageCrossRef(
                ContactMessageCrossRef(
                    contactId = dto.pluginConnection.objectId,
                    messageId = messageIdDeferred.await()
                )
            )
//            database.contactDao.getContactPluginConnectionCrossRefsByObjectId(dto.pluginConnection.objectId)
//                .map {
//                    ContactHiddenStateUpdate(contactId = it.contactId, isHidden = false)
//                }.let {
//                    database.contactDao.updateHiddenState(it)
//                }


            // Update Avatar or Anything Else Here
            database.contactDao.getPluginConnectionWithContacts(dto.pluginConnection.objectId).let { pluginConnectionWithContacts ->
                database.contactDao.updateContact(pluginConnectionWithContacts.contactList.map {
                    it.copy(
                        profile = dto.subjectProfile.toProfile(context).let{ newProfile ->
                            it.profile.copy(
                                name = newProfile.name,
                                avatar = newProfile.avatar,
                                avatarUri = newProfile.avatarUri,
                                id = newProfile.id
                            )
                        },
                        latestMessage =
                        if (it.latestMessage != null && it.latestMessage.timestamp < dto.timestamp) {
                            LatestMessage(
                                sender = dto.profile.name,
                                content = dto.contents.getContentString(),
                                timestamp = dto.timestamp,
                                unreadMessagesNum = it.latestMessage.unreadMessagesNum + 1
                            )
                        } else it.latestMessage
                            ?: LatestMessage(
                                sender = dto.profile.name,
                                content = dto.contents.getContentString(),
                                timestamp = dto.timestamp,
                                unreadMessagesNum = 1
                            ),
                        isHidden = false
                    )
                })
            }
            // Send Notification if Enabled ...
            // if FCM Enabled...
            if (contactIdDeferred.await() == -1L) {
                database.contactDao.getContactById(dto.pluginConnection.objectId).let {
                    if ((allowForegroundNotification || MainActivity.inBackground) && it?.enableNotifications == true && !it.isArchived) {
                        notificationUtil.sendNewMessageNotification(
                            message = dto.toMessage(context, messageIdDeferred.await()),
                            contact = dto.toContact(context),
                            channelId = dto.pluginConnection.connectionType.toString()
                        )
                    }
                    if (enableFcm && workingMode == WorkingMode.NORMAL.ordinal && it?.disableFCM != true) {
                        val dtoWithoutUri = dto.copy(
                            contents = dto.contents.saveLocalResourcesToCloud(
                                context
                            )
                        )
                        fcmApiHelper.pushReceiveDto(dtoWithoutUri).also {
                            if (it?.isSuccessful == true) {
                                Log.d("parabox", "fcm success")
                            } else {
                                Log.d("parabox", "fcm failed")
                            }
                        }
                    }
                }
            } else {
                if (allowForegroundNotification || MainActivity.inBackground) {
                    notificationUtil.sendNewMessageNotification(
                        message = dto.toMessage(context, messageIdDeferred.await()),
                        contact = dto.toContact(context),
                        channelId = dto.pluginConnection.connectionType.toString()
                    )
                }
                if (enableFcm && workingMode == WorkingMode.NORMAL.ordinal) {
                    fcmApiHelper.pushReceiveDto(dto).also {
                        if (it?.isSuccessful == true) {
                            Log.d("parabox", "fcm success")
                        } else {
                            Log.d("parabox", "fcm failed")
                        }
                    }
                }
            }

//        database.messageDao.insertMessage(dto.toMessageEntity())
//        database.contactDao.insertContact(dto.toContactEntityWithUnreadMessagesNumUpdate(database.contactDao))
//        database.contactMessageCrossRefDao.insertNewContactMessageCrossRef(dto.getContactMessageCrossRef())

        }
    }

    override suspend fun handleNewMessage(
        contents: List<com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.MessageContent>,
        pluginConnection: com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection,
        timestamp: Long,
        sendType: Int,
        withoutVerify: Boolean,
    ): Long {
        return coroutineScope {
            val userName = context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { settings ->
                    settings[DataStoreKeys.USER_NAME] ?: DataStoreKeys.DEFAULT_USER_NAME
                }.firstOrNull() ?: DataStoreKeys.DEFAULT_USER_NAME

            val messageIdDeferred = async<Long> {
                storeSendMessage(contents, timestamp, sendType, withoutVerify)
            }
            // Used to create new Conversation only
            val contactIdDeferred = async<Long> {
                database.contactDao.insertContact(
                    ContactEntity(
                        profile = Profile(
                            pluginConnection.id.toString(),
                            null,
                            null,
                            pluginConnection.id
                        ),
                        latestMessage = LatestMessage(
                            sender = userName,
                            content = contents.getContentString(),
                            timestamp = timestamp,
                            unreadMessagesNum = 0,
                        ),
                        contactId = pluginConnection.objectId,
                        senderId = pluginConnection.objectId,
                        isHidden = false,
                        isPinned = false,
                        isArchived = false,
                        enableNotifications = true,
                        tags = emptyList()
                    )
                )
            }
            val pluginConnectionDeferred = async<Long> {
                database.contactDao.insertPluginConnection(
                    pluginConnection.toPluginConnection().toPluginConnectionEntity()
                )
            }
//            database.contactDao.updateHiddenState(ContactHiddenStateUpdate(dto.pluginConnection.objectId, false))
            if (pluginConnectionDeferred.await() != -1L) {
                database.contactDao.insertContactPluginConnectionCrossRef(
                    ContactPluginConnectionCrossRef(
                        contactId = pluginConnection.objectId,
                        objectId = pluginConnectionDeferred.await()
                    )
                )
            }
            database.contactMessageCrossRefDao.insertContactMessageCrossRef(
                ContactMessageCrossRef(
                    contactId = pluginConnection.objectId,
                    messageId = messageIdDeferred.await()
                )
            )

            // Update Avatar or Anything Else Here
            launch(Dispatchers.IO) {
                database.contactDao.getPluginConnectionWithContacts(pluginConnection.objectId).let {
                    database.contactDao.updateContact(it.contactList.map {
                        it.copy(
                            latestMessage = LatestMessage(
                                sender = it.latestMessage?.sender ?: userName,
                                content = contents.getContentString(),
                                timestamp = timestamp,
                                unreadMessagesNum = (it.latestMessage?.unreadMessagesNum ?: 0) + 1,
                                sentByMe = true,
                            ),
                            isHidden = false
                        )
                    })
                }
            }
            messageIdDeferred.await()
        }
    }

    private suspend fun storeSendMessage(
        contents: List<MessageContent>,
        timestamp: Long,
        sendType: Int,
        withoutVerify: Boolean = false,
    ): Long {
        val sendId = context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { settings ->
                settings[DataStoreKeys.SEND_MESSAGE_ID] ?: 1L
            }.firstOrNull() ?: 1L
        context.dataStore.edit { settings ->
            settings[DataStoreKeys.SEND_MESSAGE_ID] = sendId + 1
        }
        return database.messageDao.insertMessage(
            MessageEntity(
                contents = contents.toMessageContentList(context),
                contentString = contents.getContentString(),
                profile = Profile("", null, null, null),
                timestamp = timestamp,
                messageId = sendId,
                sentByMe = true,
                verified = withoutVerify,
                sendType = sendType
            )
        ).let {
            if (it != -1L) it
            else {
                storeSendMessage(contents, timestamp, sendType, withoutVerify)
            }
        }
    }

    override suspend fun deleteGroupedContact(contactId: Long): Pair<ContactEntity?, List<ContactPluginConnectionCrossRef>> {
        return coroutineScope {
            val contactDeferred = async {
                database.contactDao.getContactById(contactId)
            }
            val connectionDeferred = async {
                database.contactDao.getContactPluginConnectionCrossRefsByContactId(contactId)
            }
            (contactDeferred.await() to connectionDeferred.await()).also {
                database.contactDao.deleteContact(contactId)
                database.contactDao.deleteContactPluginConnectionCrossRefByContactId(contactId)
            }
        }
    }

    override suspend fun restoreGroupedContact(pair: Pair<ContactEntity, List<ContactPluginConnectionCrossRef>>) {
        database.contactDao.insertContact(pair.first)
        pair.second.forEach {
            database.contactDao.insertContactPluginConnectionCrossRef(it)
        }
    }

    override fun updateMessageVerifiedState(id: Long, value: Boolean) {
        database.messageDao.updateVerifiedState(MessageVerifyStateUpdate(id, value))
    }

    override fun updateContactHiddenState(id: Long, value: Boolean) {
        database.contactDao.updateHiddenState(ContactHiddenStateUpdate(id, value))
    }

    override fun updateContactPinnedState(id: Long, value: Boolean) {
        database.contactDao.updatePinnedState(ContactPinnedStateUpdate(id, value))
    }

    override fun updateContactNotificationState(id: Long, value: Boolean) {
        database.contactDao.updateNotificationState(ContactNotificationStateUpdate(id, value))
    }

    override fun updateContactArchivedState(id: Long, value: Boolean) {
        database.contactDao.updateArchivedState(ContactArchivedStateUpdate(id, value))
    }

    override fun updateContactBackupState(id: Long, value: Boolean) {
        database.contactDao.updateShouldBackup(ContactShouldBackupUpdate(id, value))
    }

    override fun updateContactTag(id: Long, tag: List<String>) {
        database.contactDao.updateTag(ContactTagUpdate(id, tag))
    }

    override fun updateContactProfileAndTag(id: Long, profile: Profile, tags: List<String>) {
        database.contactDao.updateProfileAndTag(
            ContactProfileAndTagUpdate(
                id,
                profile.name,
                profile.avatar,
                profile.avatarUri,
                tags
            )
        )
    }

    override fun updateCustomizedContactProfile(id: Long, profile: Profile, tags: List<String>) {
        database.contactDao.updateCustomizedProfileAndTag(
            ContactCustomizedProfileAndTagUpdate(
                id,
                profile.customizedName,
                profile.customizedUri,
                tags
            )
        )
    }

    override fun updateContactUnreadMessagesNum(id: Long, value: Int) {
        database.contactDao.updateUnreadMessagesNum(ContactUnreadMessagesNumUpdate(id, value))
    }

    override fun updateContactDisableFCMState(id: Long, value: Boolean) {
        database.contactDao.updateDisableFCMState(ContactDisableFCMStateUpdate(id, value))
    }

    override fun getContactTags(): Flow<List<Tag>> {
        return database.tagDao.queryAllTags().map { it.map { it.toTag() } }
    }

    override fun deleteContactTag(value: String) {
        database.tagDao.deleteTagByValue(value)
    }

    override fun addContactTag(value: String) {
        database.tagDao.insertTag(TagEntity(value))
    }

    override fun checkContactTag(value: String): Boolean {
        return database.tagDao.hasTag(value)
    }

    override suspend fun getContactById(contactId: Long): Contact? {
        return database.contactDao.getContactById(contactId)?.toContact()
    }

    override fun getAllContacts(): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading<List<Contact>>())
            try {
                emitAll(
                    database.contactDao.getAllContacts().map {
                        Resource.Success(
                            it.map { it.toContact() }
                        )
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
            }
        }
    }

    override fun getAllHiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllHiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 })
            }.catch() {
                emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
            }
    }

    override fun getAllUnhiddenContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getAllUnhiddenContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 })
            }.catch {
                emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
            }
    }

    override fun getArchivedContacts(): Flow<Resource<List<Contact>>> {
        return database.contactDao.getArchivedContacts()
            .map<List<ContactEntity>, Resource<List<Contact>>> { contactEntityList ->
                Resource.Success(contactEntityList.map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 })
            }.catch {
                emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
            }
    }

    override fun getPersonalContacts(): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading<List<Contact>>())
            try {
                database.contactDao.getPersonalContacts()
                    .map {
                        it.toContact()
                    }.sortedByDescending { it.latestMessage?.timestamp ?: 0 }
                    .also { emit(Resource.Success(it)) }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
            }

        }
    }

    override fun getGroupContacts(limit: Int): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading<List<Contact>>())
//            try {
            database.contactDao.getGroupContacts(limit)
                .map {
                    it.toContact()
                }.sortedByDescending { it.latestMessage?.timestamp ?: 0 }
                .also { emit(Resource.Success(it)) }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                emit(Resource.Error<List<Contact>>("获取数据时发生错误"))
//            }
        }
    }

    override suspend fun getShouldBackupContacts(): List<Contact> {
        return database.contactDao.getShouldBackupContacts().map { it.toContact() }
    }

    override fun getPluginConnectionByContactId(contactId: Long): List<PluginConnection> {
        return database.contactDao.getContactWithPluginConnections(contactId = contactId).pluginConnectionList.map { it.toPluginConnection() }
    }

    @OptIn(FlowPreview::class)
    override fun getSpecifiedContactWithMessages(contactId: Long): Flow<Resource<ContactWithMessages>> {
        return flow<Resource<ContactWithMessages>> {
            emit(Resource.Loading())
            emitAll(
                database.contactMessageCrossRefDao.getSpecifiedContactWithMessages(contactId)
                    .map<ContactWithMessagesEntity, Resource<ContactWithMessages>> {
                        Resource.Success(it.toContactWithMessages())
                    }.catch {
                        emit(Resource.Error<ContactWithMessages>(context.getString(R.string.error_query_data)))
                    }
            )
        }
//        return database.contactMessageCrossRefDao.getSpecifiedContactWithMessages(contactId)
//            .map<List<ContactWithMessagesEntity>, Resource<List<ContactWithMessages>>> { contactWithMessagesEntityList ->
//                Resource.Success(contactWithMessagesEntityList.map {
//                    it.toContactWithMessages()
//                })
//            }.catch {
//                emit(Resource.Error<List<ContactWithMessages>>("获取数据时发生错误"))
//            }
    }

    @OptIn(FlowPreview::class)
    override fun getSpecifiedListOfContactWithMessages(contactIds: List<Long>): Flow<Resource<List<ContactWithMessages>>> {
        return flow<Resource<List<ContactWithMessages>>> {
            emit(Resource.Loading())
            emitAll(
                database.contactMessageCrossRefDao.getSpecifiedListOfContactWithMessages(
                    contactIds
                )
                    .map<List<ContactWithMessagesEntity>, Resource<List<ContactWithMessages>>> { contactWithMessagesEntityList ->
                        Resource.Success(contactWithMessagesEntityList.map {
                            it.toContactWithMessages()
                        })
                    }.catch {
                        emit(Resource.Error<List<ContactWithMessages>>(context.getString(R.string.error_query_data)))
                    }
            )
        }
    }

    override fun getMessagesPagingSource(contactIds: List<Long>): PagingSource<Int, MessageEntity> {
        return database.messageDao.getMessagesPagingSource(contactIds)
    }

    override fun deleteMessageById(messageId: Long) {
        database.messageDao.deleteMessageById(messageId = messageId)
    }

    override fun deleteMessageById(messageIdList: List<Long>) {
        database.messageDao.deleteMessageById(messageIdList = messageIdList)
    }

    override fun getGroupInfoPack(contactIds: List<Long>): GroupInfoPack? {
        val contactList = mutableListOf<Contact>()
        val pluginConnectionList = mutableListOf<PluginConnection>()
        database.contactDao.getContactWithPluginConnectionsByList(contactIds).forEach {
            contactList.add(it.contact.toContact())
            pluginConnectionList.addAll(it.pluginConnectionList.map { it.toPluginConnection() })
        }
        return if (contactList.isEmpty() || pluginConnectionList.isEmpty()) null else GroupInfoPack(
            contacts = contactList,
            pluginConnectionsDistinct = pluginConnectionList.distinct()
        )
    }

    override suspend fun groupNewContact(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long,
        avatar: String?,
        avatarUri: String?,
        tags: List<String>,
        contactId: Long?
    ): Boolean {
        return coroutineScope {
            withContext(Dispatchers.IO) {
                try {
                    if (contactId != null) {
                        // Group Action don't need extra pluginConnection inserted,
                        // But Create Action need. (Under Ignore Strategy)
                        pluginConnections.forEach {
                            database.contactDao.insertPluginConnection(
                                it.toPluginConnectionEntity()
                            )
                        }
                        if (database.contactDao.isExist(contactId)) {
                            // Already Exist
                            throw IOException("id already exist")
                        }
                    }
                    val contactEntity = ContactEntity(
                        profile = Profile(
                            name = name,
                            avatar = avatar,
                            avatarUri = avatarUri,
                            id = null,
                        ),
                        latestMessage = LatestMessage("", "", System.currentTimeMillis(), 0),
                        senderId = senderId,
                        tags = tags,
                        contactId = contactId ?: 0
                    )
                    val returnedContactId = database.contactDao.insertContact(contactEntity)
                    pluginConnections.forEach { conn ->
                        database.contactDao.insertContactPluginConnectionCrossRef(
                            ContactPluginConnectionCrossRef(
                                contactId = returnedContactId,
                                objectId = conn.objectId
                            )
                        )
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

            }
        }
    }

    override fun getFiles(query: String): Flow<Resource<List<File>>> {
        return if (query.isBlank()) {
            database.fileDao.getAllFiles()
                .map<List<FileEntity>, Resource<List<File>>> { fileEntityList ->
                    Resource.Success(fileEntityList.map {
                        it.toFile()
                    }.sortedByDescending { it.timestamp ?: 0 })
                }.catch {
                    emit(Resource.Error(context.getString(R.string.error_query_data)))
                }
        } else {
            database.fileDao.queryFiles(query)
                .map<List<FileEntity>, Resource<List<File>>> { fileEntityList ->
                    Resource.Success(fileEntityList.map {
                        it.toFile()
                    }.sortedByDescending { it.timestamp ?: 0 })
                }.catch {
                    emit(Resource.Error(context.getString(R.string.error_query_data)))
                }
        }
    }

    override fun getAllFilesStatic(): List<File> {
        return database.fileDao.getAllFilesStatic().map { it.toFile() }
    }

    override suspend fun getFilesByContactIdsStatic(contactIds: List<Long>): List<File> {
        return database.fileDao.getShouldBackupFiles(contactIds).map { it.toFile() }
    }

    override fun updateDownloadingState(state: DownloadingState, target: File) {
        database.fileDao.updateDownloadingState(
            FileDownloadingStateUpdate(
                target.fileId,
                state
            )
        )
    }

    override fun updateDownloadInfo(path: String?, downloadId: Long?, target: File) {
        database.fileDao.updateDownloadInfo(
            FileDownloadInfoUpdate(
                target.fileId,
                path,
                downloadId
            )
        )
    }

    override fun updateCloudInfo(cloudType: Int?, cloudId: String?, targetId: Long) {
        database.fileDao.updateCloudInfo(
            FileCloudInfoUpdate(
                targetId,
                cloudType,
                cloudId
            )
        )
    }

    override suspend fun deleteFile(fileId: Long) {
        database.fileDao.deleteFileByFileId(fileId)
    }

    override fun queryContact(query: String): Flow<Resource<List<Contact>>> {
        return flow {
            emit(Resource.Loading<List<Contact>>())
            if (query.isNotBlank()) {
                try {
                    database.contactDao.queryContact(query)
                        .map { it.toContact() }.also {
                            emit(Resource.Success(it))
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(Resource.Error<List<Contact>>(context.getString(R.string.error_query_data)))
                }
            } else {
                emit(Resource.Success(emptyList()))
            }
        }
    }

    override fun queryContactWithMessages(query: String): Flow<Resource<List<ContactWithMessages>>> {
        return flow {
            emit(Resource.Loading<List<ContactWithMessages>>())
            if (query.isNotBlank()) {
                try {
                    database.messageDao.queryContactWithMessages(query)
                        .also {
                            Log.d("parabox", "queryContactWithMessages: $it")
                        }
                        .map {
                            ContactWithMessages(
                                it.key.toContact(),
                                it.value.map { it.toMessage() })
                        }
                        .also { emit(Resource.Success(it)) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emit(Resource.Error<List<ContactWithMessages>>(context.getString(R.string.error_query_data)))
                }
            } else {
                emit(Resource.Success(emptyList()))
            }
        }
    }
}