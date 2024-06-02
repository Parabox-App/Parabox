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
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.ImageUtil.getCircledBitmap
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Extension
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.receiver.MarkAsReadReceiver
import com.ojhdtapp.parabox.domain.receiver.ReplyReceiver
import com.ojhdtapp.parabox.ui.message.chat.contents_layout.model.ChatPageUiModel
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


class NotificationUtil(
    val context: Context,
    val database: AppDatabase
): DefaultLifecycleObserver {
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

    private var isForeground = false

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isForeground = false
    }

    private val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
        setLabel(context.getString(R.string.reply_label))
        build()
    }
    private val notificationManager: NotificationManager =
        context.getSystemService<NotificationManager>() ?: throw IllegalStateException()

    private val shortcutManager: ShortcutManager =
        context.getSystemService<ShortcutManager>() ?: throw IllegalStateException()

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

    suspend fun updateShortcuts() {
        val maxCount = shortcutManager.maxShortcutCountPerActivity
        var chatList = database.chatDao.getChatWithLimit(maxCount)
        val shortcutList =
            chatList.map { chat ->
                val avatarBitmap = ImageUtil.getBitmapWithCoil(context, chat.avatar.getModel())
                    ?: ImageUtil.createNamedAvatarBm(
                        backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorSecondary),
                        textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnSecondary),
                        name = chat.name ?: "name"
                    )
                val icon = Icon.createWithAdaptiveBitmap(avatarBitmap.getCircledBitmap())
                val builder = ShortcutInfo.Builder(context, chat.chatId.toString())
                    .setActivity(ComponentName(context, MainActivity::class.java))
                    .setShortLabel(chat.name ?: "name")
                    .setLongLabel(chat.name ?: "name")
                    .setIcon(icon)
                    .setCategories(setOf("com.ojhdtapp.parabox.bubbles.category.TEXT_SHARE_TARGET"))
                    .setIntent(Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("parabox://chat/${chat.chatId}")
                    })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setLocusId(LocusId(chat.chatId.toString()))
                        .setLongLived(true)
                        .setPerson(
                            Person.Builder()
                                .setName(chat.name ?: "name")
                                .setIcon(icon)
                                .build()
                        )
                }
                builder.build()
            }
        shortcutManager.dynamicShortcuts = shortcutList
    }

    suspend fun sendNewMessageNotification(
        messageId: Long,
        contactId: Long,
        chatId: Long,
        extensionInfo: ExtensionInfo,
        fromChat: Boolean = false,
    ) {
        val message = database.messageDao.getMessageById(messageId)?.toMessage() ?: return
        val contact = database.contactDao.getContactById(contactId)?.toContact() ?: return
        val chat = database.chatDao.getChatByIdWithoutObserve(chatId)?.toChat() ?: return
        val channelId = "${extensionInfo.pkg}_${extensionInfo.extensionId}_${extensionInfo.alias}"
        createNotificationChannel(
            channelId = channelId,
            channelName = extensionInfo.alias,
            channelDescription = "来自${extensionInfo.name}扩展，${extensionInfo.alias}连接的消息"
        )
        Log.d("parabox", "sendNotification at channel:${channelId}")

        updateShortcuts()
        val launchUri = "parabox://chat/${chat.chatId}".toUri()

        val userName = context.getDataStoreValue(DataStoreKeys.USER_NAME, context.getString(R.string.you))
        val userAvatarModel = context.getDataStoreValue(DataStoreKeys.USER_AVATAR, "")
        val userAvatarBitmap = ImageUtil.getBitmapWithCoil(context, userAvatarModel)
            ?: ImageUtil.createNamedAvatarBm(
                backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorSecondary),
                textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnSecondary),
                name = userName
            )
        val userIcon = Icon.createWithAdaptiveBitmap(userAvatarBitmap.getCircledBitmap())

        val senderAvatarBitmap = ImageUtil.getBitmapWithCoil(context, contact.avatar.getModel())
            ?: ImageUtil.createNamedAvatarBm(
                backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
                textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
                name = contact.name
            )
        val senderIcon = Icon.createWithAdaptiveBitmap(senderAvatarBitmap.getCircledBitmap())

        val chatAvatarBitmap = ImageUtil.getBitmapWithCoil(context, chat.avatar.getModel())
            ?: ImageUtil.createNamedAvatarBm(
                backgroundColor = context.getThemeColor(com.google.android.material.R.attr.colorPrimary),
                textColor = context.getThemeColor(com.google.android.material.R.attr.colorOnPrimary),
                name = chat.name
            )
        val chatIcon = Icon.createWithAdaptiveBitmap(chatAvatarBitmap.getCircledBitmap())
        val launchPendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = launchUri
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val replyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                message.messageId.toInt(),
                Intent(context, ReplyReceiver::class.java).apply {
                    putExtra("chat", chat)
                },
                getPendingIntentFlags(true)
            )
        val markAsReadPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                message.messageId.toInt(),
                Intent(context, MarkAsReadReceiver::class.java).apply {
                    putExtra("chat", chat)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notificationBuilder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val userPerson =
                    Person.Builder().setName(userName).setIcon(userIcon).build()
                val senderPerson = Person.Builder().setName(contact.name).setIcon(senderIcon).build()
                val chatPerson = Person.Builder().setName(chat.name).setIcon(chatIcon).build()
                Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(chatIcon)
                    .setContentTitle(chat.name)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setShortcutId(chat.chatId.toString())
                    .setContentIntent(launchPendingIntent)
                    .addPerson(senderPerson)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setWhen(message.timestamp)
                    .setGroup(GROUP_KEY_NEW_MESSAGE)
                    .setActions(
                        Notification.Action
                            .Builder(
                                Icon.createWithResource(context, R.drawable.baseline_send_24),
                                context.getString(R.string.reply),
                                replyPendingIntent
                            )
                            .addRemoteInput(remoteInput)
                            .setAllowGeneratedReplies(true)
                            .build(),
                        Notification.Action.Builder(
                            Icon.createWithResource(context, R.drawable.baseline_mark_chat_read_24),
                            context.getString(R.string.mark_as_read),
                            markAsReadPendingIntent
                        ).build()
                    )
                    .setStyle(
                        Notification.MessagingStyle(userPerson).apply {
                            // temp message
                            if (tempMessageMap[chat.chatId] == null) {
                                tempMessageMap[chat.chatId] =
                                    CopyOnWriteArrayList(arrayOf(message to senderPerson))
                            } else {
                                tempMessageMap[chat.chatId]?.run {
                                    add(message to senderPerson)
                                    if (size > 6) {
                                        this.removeAt(0)
                                    }
                                }
                            }

                            tempMessageMap[chat.chatId]?.forEach {
                                val m = Notification.MessagingStyle.Message(
                                    it.first.contentString,
                                    it.first.timestamp,
                                    if (it.first.sentByMe) null else it.second
                                ).apply {
                                    it.first.contents.filterIsInstance<ParaboxImage>().firstOrNull()?.let {
                                        val mimetype = "image/"
                                        val bitmap = ImageUtil.getBitmapWithCoil(context, it.resourceInfo.getModel())
                                        val imageUri = if (bitmap != null) {
                                            ImageUtil.getImageUriFromBitmap(context, bitmap, it.fileName)
                                        } else {
                                            Uri.parse("android.resource://${context.packageName}/${R.drawable.image_lost}")
                                        }
                                        setData(mimetype, imageUri)
                                    }
                                }
                                if (it.first.timestamp < m.timestamp) {
                                    addHistoricMessage(m)
                                } else {
                                    addMessage(m)
                                }
                            }

                            isGroupConversation = chat.type == SendTargetType.GROUP
                            conversationTitle = chat.name
                        }
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setLocusId(LocusId(chat.chatId.toString()))
                        }
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            setBubbleMetadata(
//                                Notification.BubbleMetadata
//                                    .Builder(
//                                        PendingIntent.getActivity(
//                                            context,
//                                            REQUEST_BUBBLE,
//                                            Intent(context, BubbleActivity::class.java)
//                                                .setAction(Intent.ACTION_VIEW)
//                                                .setData(launchUri),
//                                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
//                                        ),
//                                        chatIcon
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
                    }
            } else {
                Log.d("parabox", "old notification pattern")
                val notificationBuilder = Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(contact.name)
                    .setContentText(message.contentString)
                    .setContentIntent(launchPendingIntent)
                    .setAutoCancel(true)
                val senderName = context.getString(R.string.you)
                Notification.MessagingStyle(senderName)
                    .addMessage(message.contentString, Date().time, senderName)
                    .setConversationTitle(chat.name)
                    .setBuilder(notificationBuilder)
                notificationBuilder
            }

        val messageBadgeNum = context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0)

        val summaryNotificationBuilder = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(context.getString(R.string.notification_group_title))
            .setContentText(context.getString(R.string.notification_group_summary, messageBadgeNum))
            .setShowWhen(true)
            .setAutoCancel(true)
            .setWhen(message.timestamp)
            .setStyle(
                Notification.InboxStyle()
                    .addLine(message.contentString)
                    .setBigContentTitle(context.getString(R.string.notification_group_summary, messageBadgeNum))
                    .setSummaryText(context.getString(R.string.notification_group_summary, messageBadgeNum))
            )
            .setGroup(GROUP_KEY_NEW_MESSAGE)
            .setGroupSummary(true)

        notificationManager.notify(SUMMARY_ID, summaryNotificationBuilder.build())
        notificationManager.notify(chat.chatId.toInt(), notificationBuilder.build())
    }

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
            NotificationCompat.Builder(context, SERVICE_STATE_CHANNEL_ID)
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

    private fun getPendingIntentFlags(isMutable: Boolean = false) =
        when {
            isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            !isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }
}