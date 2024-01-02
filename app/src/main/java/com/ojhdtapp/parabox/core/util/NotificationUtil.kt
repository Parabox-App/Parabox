package com.ojhdtapp.parabox.core.util

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.emptyPreferences
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.AvatarUtil.getCircledBitmap
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.internal.notify
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


class NotificationUtil(
    val context: Context,
    val database: AppDatabase
) {
    companion object {
        const val GROUP_KEY_NEW_MESSAGE = "group_new_message"
        const val GROUP_KEY_INTERNAL = "group_internal"
        const val SUMMARY_ID = 9998

        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2

        const val SERVICE_STATE_CHANNEL_ID = "service_state_channel"
        private const val FOREGROUND_PLUGIN_SERVICE_NOTIFICATION_ID = 999

        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    private val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
        setLabel(context.getString(R.string.reply_label))
        build()
    }
    private val notificationManager: NotificationManager =
        context.getSystemService() ?: throw IllegalStateException()

    private val shortcutManager: ShortcutManager =
        context.getSystemService() ?: throw IllegalStateException()

    private val tempMessageMap =
        mutableMapOf<Long, CopyOnWriteArrayList<Pair<Message, Person>>>()

    fun openChannelSetting(channelId: String?) {
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
        if (context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ) != null
        ) {
            context.startActivity(intent)
        }
    }

    fun createNotificationChannel(
        channelId: String,
        channelName: String,
        channelDescription: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT
    ) {
        Log.d("parabox", "create notification channel, ID:${channelId}")
        if (notificationManager.getNotificationChannel(channelId) == null) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

//    suspend fun updateShortcuts(importantContact: Contact? = null) {
//        // Truncate the list if we can't show all of our contacts.
//        val maxCount = shortcutManager.maxShortcutCountPerActivity
//        var contacts = database.contactDao.getAllContacts().firstOrNull() ?: emptyList()
//        // Move the important contact to the front of the shortcut list.
//        if (importantContact != null) {
//            contacts =
//                contacts.sortedByDescending { it.contactId == importantContact.contactId }
//        }
//        if (contacts.size > maxCount) {
//            contacts = contacts.take(maxCount)
//        }
//        var shortcuts = contacts.map {
//            val icon = it.profile.let { profile ->
//                withContext(Dispatchers.IO) {
//                    if (profile.avatar != null || profile.avatarUri != null) {
//                        try {
//                            val loader = ImageLoader(context)
//                            val request = ImageRequest.Builder(context)
//                                .data(profile.avatarUri ?: profile.avatar)
//                                .allowHardware(false) // Disable hardware bitmaps.
//                                .build()
//                            val result = (loader.execute(request) as SuccessResult).drawable
//                            val bitmap = (result as BitmapDrawable).bitmap.getCircledBitmap()
//                            Icon.createWithAdaptiveBitmap(bitmap)
//                        } catch (e: ClassCastException) {
//                            e.printStackTrace()
//                            null
//                        }
//                    } else null
//                } ?: Icon.createWithAdaptiveBitmap(
//                    AvatarUtil.createNamedAvatarBm(
//                        width = 224,
//                        height = 224,
//                        backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
//                        textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
//                        name = profile.name.ifBlank { null }?.substring(0, 1)?.uppercase(Locale.getDefault())
//                    )
//                )
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                ShortcutInfo.Builder(context, it.contactId.toString())
//                    .setLocusId(LocusId(it.contactId.toString()))
//                    .setActivity(ComponentName(context, MainActivity::class.java))
//                    .setShortLabel(it.profile.name)
//                    .setLongLabel(it.profile.name)
//                    .setIcon(icon)
//                    .setLongLived(true)
//                    .setCategories(setOf("com.ojhdtapp.parabox.bubbles.category.TEXT_SHARE_TARGET"))
//                    .setIntent(Intent(context, MainActivity::class.java).apply {
//                        action = Intent.ACTION_VIEW
//                        data = Uri.parse("parabox://contact/${it.contactId}")
//                    })
//                    .setPerson(
//                        Person.Builder()
//                            .setName(it.profile.name)
//                            .setIcon(icon)
//                            .build()
//                    )
//                    .build()
//            } else {
//                ShortcutInfo.Builder(context, it.contactId.toString())
//                    .setActivity(ComponentName(context, MainActivity::class.java))
//                    .setShortLabel(it.profile.name)
//                    .setLongLabel(it.profile.name)
//                    .setIcon(icon)
//                    .setCategories(setOf("com.ojhdtapp.parabox.bubbles.category.TEXT_SHARE_TARGET"))
//                    .setIntent(Intent(context, MainActivity::class.java).apply {
//                        action = Intent.ACTION_VIEW
//                        data = Uri.parse("parabox://contact/${it.contactId}")
//                    })
//                    .build()
//            }
//        }
//        shortcutManager.dynamicShortcuts = shortcuts
//    }
//
//    suspend fun sendNewMessageNotification(
//        message: Message,
//        contact: Contact,
//        channelId: String,
//        isGroupSpecify: Boolean? = null,
//        fromChat: Boolean = false,
//    ) {
//        Log.d("parabox", "sendNotification at channel:${channelId}")
//        updateShortcuts(contact)
//        val contactIdUri = "parabox://contact/${contact.contactId}".toUri()
//        val isGroup = isGroupSpecify ?: (message.profile.name != contact.profile.name)
//        val userNameFlow: Flow<String> = context.dataStore.data
//            .catch { exception ->
//                if (exception is IOException) {
//                    emit(emptyPreferences())
//                } else {
//                    throw exception
//                }
//            }
//            .map { settings ->
//                settings[DataStoreKeys.USER_NAME] ?: context.getString(R.string.you)
//            }
//        val userAvatarFlow: Flow<String?> = context.dataStore.data
//            .catch { exception ->
//                if (exception is IOException) {
//                    emit(emptyPreferences())
//                } else {
//                    throw exception
//                }
//            }
//            .map { settings ->
//                settings[DataStoreKeys.USER_AVATAR]
//            }
//
//        val launchPendingIntent: PendingIntent =
//            Intent(context, MainActivity::class.java).apply {
//                action = Intent.ACTION_VIEW
//                data = contactIdUri
//                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            }.let {
//                PendingIntent.getActivity(
//                    context, 0, it,
//                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//                )
//            }
//        val replyPendingIntent: PendingIntent =
//            PendingIntent.getBroadcast(
//                context,
//                message.messageId.toInt(),
//                Intent(context, ReplyReceiver::class.java).apply {
//                    putExtra("contact", contact)
//                    putExtra(
//                        "sendTargetType",
//                        if (isGroup) SendTargetType.GROUP else SendTargetType.USER
//                    )
//                },
//                PendingIntent.FLAG_MUTABLE
//            )
//        val markAsReadPendingIntent: PendingIntent =
//            PendingIntent.getBroadcast(
//                context,
//                message.messageId.toInt(),
//                Intent(context, MarkAsReadReceiver::class.java).apply {
//                    putExtra("contact", contact)
//                },
//                PendingIntent.FLAG_MUTABLE
//            )
//
//        val notificationBuilder: Notification.Builder =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                val userIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    userAvatarFlow.firstOrNull()?.let {
//                        Icon.createWithAdaptiveBitmapContentUri(it)
//                    }
//                } else {
//                    userAvatarFlow.firstOrNull()?.let {
//                        Icon.createWithContentUri(it)
//                    }
//                } ?: Icon.createWithAdaptiveBitmap(
//                    AvatarUtil.createNamedAvatarBm(
//                        width = 224,
//                        height = 224,
//                        backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
//                        textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
//                        name = userNameFlow.firstOrNull()?.ifBlank { null }?.substring(0, 1)
//                            ?.uppercase(Locale.getDefault())
//                    )
//                )
//                val user =
//                    Person.Builder().setName(userNameFlow.firstOrNull()).setIcon(userIcon).build()
//
//                val personIcon = message.profile.let { profile ->
//                    withContext(Dispatchers.IO) {
//                        if (profile.avatar != null || profile.avatarUri != null) {
//                            try {
//                                val loader = ImageLoader(context)
//                                val request = ImageRequest.Builder(context)
//                                    .data(profile.avatarUri ?: profile.avatar)
//                                    .allowHardware(false) // Disable hardware bitmaps.
//                                    .build()
//                                val result = (loader.execute(request) as SuccessResult).drawable
//                                val bitmap = (result as BitmapDrawable).bitmap
//                                Icon.createWithAdaptiveBitmap(bitmap.getCircledBitmap())
//                            } catch (e: ClassCastException) {
//                                e.printStackTrace()
//                                null
//                            }
//                        } else null
//                    } ?: Icon.createWithAdaptiveBitmap(
//                        AvatarUtil.createNamedAvatarBm(
//                            width = 224,
//                            height = 224,
//                            backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
//                            textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
//                            name = profile.name.ifBlank { null }?.substring(0, 1)?.uppercase(Locale.getDefault())
//                        )
//                    )
//                }
//                val person =
//                    Person.Builder().setName(message.profile.name).setIcon(personIcon).build()
//                val groupIcon = contact.profile.let { profile ->
//                    withContext(Dispatchers.IO) {
//                        if (profile.avatar != null || profile.avatarUri != null) {
//                            try {
//                                val loader = ImageLoader(context)
//                                val request = ImageRequest.Builder(context)
//                                    .data(profile.avatarUri ?: profile.avatar)
//                                    .allowHardware(false) // Disable hardware bitmaps.
//                                    .build()
//                                val result = (loader.execute(request) as SuccessResult).drawable
//                                val bitmap = (result as BitmapDrawable).bitmap.getCircledBitmap()
//                                Icon.createWithAdaptiveBitmap(bitmap)
//                            } catch (e: ClassCastException) {
//                                e.printStackTrace()
//                                null
//                            }
//                        } else null
//                    } ?: Icon.createWithAdaptiveBitmap(
//                        AvatarUtil.createNamedAvatarBm(
//                            width = 224,
//                            height = 224,
//                            backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
//                            textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
//                            name = profile.name.ifBlank { null }?.substring(0, 1)?.uppercase(Locale.getDefault())
//                        )
//                    )
//                }
//                Notification.Builder(context, channelId)
//                    .setSmallIcon(R.drawable.ic_stat_name)
//                    .setLargeIcon(groupIcon)
//                    .setContentTitle(contact.profile.name)
//                    .setCategory(Notification.CATEGORY_MESSAGE)
//                    .setShortcutId(contact.contactId.toString())
//                    .setContentIntent(launchPendingIntent)
//                    .addPerson(person)
//                    .setShowWhen(true)
//                    .setAutoCancel(true)
//                    .setWhen(message.timestamp)
//                    .setGroup(GROUP_KEY_NEW_MESSAGE)
//                    .setActions(
//                        Notification.Action
//                            .Builder(
//                                Icon.createWithResource(context, R.drawable.baseline_send_24),
//                                context.getString(R.string.reply),
//                                replyPendingIntent
//                            )
//                            .addRemoteInput(remoteInput)
//                            .setAllowGeneratedReplies(true)
//                            .build(),
//                        Notification.Action.Builder(
//                            Icon.createWithResource(context, R.drawable.baseline_mark_chat_read_24),
//                            context.getString(R.string.mark_as_read),
//                            markAsReadPendingIntent
//                        ).build()
//                    )
//                    .setStyle(
//                        Notification.MessagingStyle(user).apply {
//                            // temp message
//                            if (tempMessageMap[contact.contactId] == null) {
//                                tempMessageMap[contact.contactId] =
//                                    CopyOnWriteArrayList(arrayOf(message to person))
//                            } else {
//                                tempMessageMap[contact.contactId]?.run {
//                                    add(message to person)
//                                    if (size > 6) {
//                                        this.removeAt(0)
//                                    }
//                                }
//                            }
//
//                            tempMessageMap[contact.contactId]?.forEach {
//                                val m = Notification.MessagingStyle.Message(
//                                    it.first.contents.getContentString(),
//                                    it.first.timestamp,
//                                    if (it.first.sentByMe) null else it.second
//                                ).apply {
//                                    it.first.contents.filterIsInstance<Image>().firstOrNull()?.let {
//                                        val mimetype = "image/"
//                                        val imageUri =
//                                            it.uriString?.let { Uri.parse(it) } ?: try {
//                                                val loader = ImageLoader(context)
//                                                val request = ImageRequest.Builder(context)
//                                                    .data(it.url)
//                                                    .allowHardware(false) // Disable hardware bitmaps.
//                                                    .build()
//                                                val result =
//                                                    (loader.execute(request) as SuccessResult).drawable
//                                                FileUtil.getUriFromBitmapWithCleanCache(
//                                                    context,
//                                                    (result as BitmapDrawable).bitmap
//                                                )
//                                            } catch (e: ClassCastException) {
//                                                e.printStackTrace()
//                                                null
//                                            }
////                                        Log.d("parabox", imageUri.toString())
//                                        setData(mimetype, imageUri)
//                                    }
//                                }
//                                if (it.first.timestamp < m.timestamp) {
//                                    addHistoricMessage(m)
//                                } else {
//                                    addMessage(m)
//                                }
//                            }
//
//                            isGroupConversation = isGroup
//                            conversationTitle = contact.profile.name
//                        }
//                    ).apply {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                            setLocusId(LocusId(contact.contactId.toString()))
//                        }
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            setBubbleMetadata(
//                                Notification.BubbleMetadata
//                                    .Builder(
//                                        PendingIntent.getActivity(
//                                            context,
//                                            REQUEST_BUBBLE,
//                                            Intent(context, BubbleActivity::class.java)
//                                                .setAction(Intent.ACTION_VIEW)
////                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                                                .setData(contactIdUri),
//                                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//                                        ),
//                                        personIcon
//                                    )
//                                    // The height of the expanded bubble.
//                                    .setDesiredHeightResId(R.dimen.bubble_height)
//                                    .apply {
//                                        // When the bubble is explicitly opened by the user, we can show the bubble
//                                        // automatically in the expanded state. This works only when the app is in
//                                        // the foreground.
//                                        if (fromChat) {
//                                            setAutoExpandBubble(true)
//                                            setSuppressNotification(true)
//                                        }
//                                    }
//                                    .build()
//                            )
//                        }
//                    }
//            } else {
//                Log.d("parabox", "old notification pattern")
//                val notificationBuilder = Notification.Builder(context, channelId)
//                    .setSmallIcon(R.drawable.ic_stat_name)
//                    .setContentTitle(contact.profile.name)
//                    .setContentText(message.contents.getContentString())
//                    .setContentIntent(launchPendingIntent)
//                    .setAutoCancel(true)
//                val senderName = context.getString(R.string.you)
//                Notification.MessagingStyle(senderName)
//                    .addMessage(message.contents.getContentString(), Date().time, senderName)
//                    .setConversationTitle(contact.profile.name)
//                    .setBuilder(notificationBuilder)
//                notificationBuilder
//            }
//
//        val messageBadgeNum = context.dataStore.data.map { preferences ->
//            preferences[DataStoreKeys.MESSAGE_BADGE_NUM] ?: 0
//        }.firstOrNull() ?: 0
//
//        val summaryNotificationBuilder = Notification.Builder(context, channelId)
//            .setSmallIcon(R.drawable.ic_stat_name)
//            .setContentTitle(context.getString(R.string.notification_group_title))
//            .setContentText(context.getString(R.string.notification_group_summary, messageBadgeNum))
//            .setShowWhen(true)
//            .setAutoCancel(true)
//            .setWhen(message.timestamp)
//            .setStyle(
//                Notification.InboxStyle()
//                    .addLine(message.contents.getContentString())
//                    .setBigContentTitle(context.getString(R.string.notification_group_summary, messageBadgeNum))
//                    .setSummaryText(context.getString(R.string.notification_group_summary, messageBadgeNum))
//            )
//            .setGroup(GROUP_KEY_NEW_MESSAGE)
//            .setGroupSummary(true)
//
//        notificationManager.notify(SUMMARY_ID, summaryNotificationBuilder.build())
//        notificationManager.notify(contact.contactId.toInt(), notificationBuilder.build())
//    }

    fun clearNotification(id: Int) {
        notificationManager.cancel(id)
    }

    fun startForegroundService(service: Service) {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        createNotificationChannel(
            SERVICE_STATE_CHANNEL_ID,
            context.getString(R.string.notification_service_state_channel_name),
            context.getString(R.string.notification_service_state_channel_des),
            NotificationManager.IMPORTANCE_MIN
        )
        val notification: Notification =
            NotificationCompat.Builder(context, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(context.getString(R.string.notification_service_state_title))
                .setContentIntent(pendingIntent)
                .setTicker(context.getString(R.string.notification_service_state_ticker))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setGroup(GROUP_KEY_INTERNAL)
                .setOnlyAlertOnce(true)
                .build()
        try {
            ServiceCompat.startForeground(
                service,
                FOREGROUND_PLUGIN_SERVICE_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                } else 0)
        } catch (e: Exception) {
            e.printStackTrace()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) { }
        }
    }

    fun updateForegroundServiceNotification(title: String, text: String) {
        val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification: Notification =
            Notification.Builder(context, SERVICE_STATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(title)
//                .setContentText(text)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .setGroup(GROUP_KEY_INTERNAL)
                .setTicker(title)
                .build()
        notificationManager.notify(
            FOREGROUND_PLUGIN_SERVICE_NOTIFICATION_ID,
            notification
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun canBubble(contact: Contact, channelId: String): Boolean {
        val channel = notificationManager.getNotificationChannel(
            channelId,
            contact.contactId.toString()
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationManager.bubblePreference == NotificationManager.BUBBLE_PREFERENCE_ALL
        } else {
            notificationManager.areBubblesAllowed()
        } || channel?.canBubble() == true
    }
}