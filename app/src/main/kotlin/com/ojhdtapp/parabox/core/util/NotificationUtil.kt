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
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ojhdtapp.parabox.BubbleActivity
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.ImageUtil.getCircledBitmap
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.data.local.ConnectionInfo
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.receiver.MarkAsReadReceiver
import com.ojhdtapp.parabox.domain.receiver.ReplyReceiver
import com.ojhdtapp.parabox.domain.service.extension.DisconnectAlert
import com.ojhdtapp.parabox.domain.service.extension.ExtensionConnectionSnapshot
import com.ojhdtapp.parabox.domain.service.extension.ExtensionConnectionState
import com.ojhdtapp.parabox.domain.service.extension.ExtensionMonitorSummary
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject


class NotificationUtil @Inject constructor(
    @ApplicationContext val context: Context,
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

        const val EXTENSION_DISCONNECT_CHANNEL_ID = "extension_disconnect_channel"
        private const val EXTENSION_DISCONNECT_NOTIFICATION_BASE_ID = 20000
        private const val MAX_SERVICE_STATE_INBOX_LINES = 6

        const val KEY_TEXT_REPLY = "key_text_reply"

        const val PLAY_APP_UPDATE_CHANNEL_ID = "play_app_update_channel"
        const val PLAY_APP_UPDATE_NOTIFICATION_ID = 9997
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
                Log.d("shortcut", "shortcut id b:${chat.chatId}")
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
        connectionInfo: ConnectionInfo,
        fromChat: Boolean = false,
    ) {
        val message = database.messageDao.getMessageById(messageId)?.toMessage() ?: return
        val contact = database.contactDao.getContactById(contactId)?.toContact() ?: return
        val chat = database.chatDao.getChatByIdWithoutObserve(chatId)?.toChat() ?: return
        val channelId = "${connectionInfo.pkg}_${connectionInfo.connectionId}_${connectionInfo.alias}"
        createNotificationChannel(
            channelId = channelId,
            channelName = connectionInfo.alias,
            channelDescription = "来自${connectionInfo.name}扩展，${connectionInfo.alias}连接的消息"
        )
        Log.d("parabox", "sendNotification at channel:${channelId}")
        if (!context.getDataStoreValue(DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION, false) && isForeground) {
            return
        }
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
                Log.d("shortcut", "shortcut id a:${chat.chatId}")
                Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(chatIcon)
                    .setContentTitle(chat.name)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setShortcutId(chat.chatId.toString())
                    .setContentIntent(launchPendingIntent)
                    .addPerson(chatPerson)
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
                                    if (it.first.sentByMe) userPerson else it.second
                                ).apply {
                                    it.first.contents.filterIsInstance<ParaboxImage>().firstOrNull()?.let { img ->
                                        val model = img.resourceInfo.getModel()
                                        val bitmap = when (ExtensionMediaLoader.detectAnimatedMediaKind(img.fileName)) {
                                            AnimatedMediaKind.IMAGE -> ImageUtil.getBitmapWithCoil(context, model)
                                            AnimatedMediaKind.LOOPING_VIDEO -> ImageUtil.getVideoFrameBitmap(context, model)
                                            AnimatedMediaKind.TGS_LOTTIE -> null
                                        }
                                        val cacheKey = "${it.first.messageId}_${model}"
                                        val imageUri = bitmap?.let { bm ->
                                            ImageUtil.getImageUriFromBitmapWithCache(context, bm, cacheKey)
                                        }
                                        if (imageUri != null) {
                                            setData("image/png", imageUri)
                                        }
                                    }
                                }
                                if (it.first.timestamp < m.timestamp) {
                                    addHistoricMessage(m)
                                } else {
                                    addMessage(m)
                                }
                            }
                            isGroupConversation = true
                            conversationTitle = chat.name
                        }
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setLocusId(LocusId(chat.chatId.toString()))
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            setBubbleMetadata(
                                Notification.BubbleMetadata
                                    .Builder(
                                        PendingIntent.getActivity(
                                            context,
                                            REQUEST_BUBBLE,
                                            Intent(context, BubbleActivity::class.java)
                                                .setAction(Intent.ACTION_VIEW)
//                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                .setData(launchUri),
                                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                        ),
                                        chatIcon
                                    )
                                    // The height of the expanded bubble.
                                    .setDesiredHeightResId(R.dimen.bubble_height)
                                    .apply {
                                        // When the bubble is explicitly opened by the user, we can show the bubble
                                        // automatically in the expanded state. This works only when the app is in
                                        // the foreground.
                                        if (fromChat) {
                                            setAutoExpandBubble(true)
                                            setSuppressNotification(true)
                                        }
                                    }
                                    .build()
                            )
                        }
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

    private fun createMainLauncherPendingIntent(): PendingIntent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

    private fun ensureServiceStateChannel() {
        createNotificationChannel(
            SERVICE_STATE_CHANNEL_ID,
            context.getString(R.string.notification_service_state_channel_name),
            context.getString(R.string.notification_service_state_channel_des),
            NotificationManager.IMPORTANCE_MIN,
        )
    }

    private fun ensureExtensionDisconnectChannel() {
        createNotificationChannel(
            EXTENSION_DISCONNECT_CHANNEL_ID,
            context.getString(R.string.notification_extension_disconnect_channel_name),
            context.getString(R.string.notification_extension_disconnect_channel_des),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
    }

    private fun buildForegroundServiceNotification(
        title: String,
        text: String,
        detailLines: List<String> = emptyList(),
        summaryText: String? = null,
    ): Notification {
        val builder = NotificationCompat.Builder(context, SERVICE_STATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(createMainLauncherPendingIntent())
            .setTicker(title)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setGroup(GROUP_KEY_INTERNAL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (detailLines.isNotEmpty()) {
            val inboxStyle = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
            detailLines.forEach { line -> inboxStyle.addLine(line) }
            if (!summaryText.isNullOrBlank()) {
                inboxStyle.setSummaryText(summaryText)
            }
            builder.setStyle(inboxStyle)
        }

        return builder.build()
    }

    fun startForegroundService(service: Service) {
        ensureServiceStateChannel()
        val notification = buildForegroundServiceNotification(
            title = context.getString(R.string.notification_service_state_title),
            text = context.getString(R.string.notification_service_state_ticker),
        )
        try {
            ServiceCompat.startForeground(
                service,
                FOREGROUND_PLUGIN_SERVICE_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                } else 0,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) { }
        }
    }

    fun updateForegroundServiceNotification(summary: ExtensionMonitorSummary) {
        ensureServiceStateChannel()
        val title = when {
            summary.totalCount == 0 ->
                context.getString(R.string.notification_service_state_title)

            summary.activeCount == summary.totalCount ->
                context.getString(R.string.notification_service_state_title_all_connected, summary.activeCount)

            else ->
                context.getString(
                    R.string.notification_service_state_title_partial_connected,
                    summary.activeCount,
                    summary.totalCount,
                )
        }

        val contentText = when {
            summary.totalCount == 0 ->
                context.getString(R.string.notification_service_state_ticker)

            summary.activeCount == summary.totalCount ->
                context.getString(R.string.notification_service_state_all_normal)

            else -> buildList {
                if (summary.activeCount > 0) {
                    add(context.getString(R.string.notification_service_state_bucket_active, summary.activeCount))
                }
                if (summary.connectingCount > 0) {
                    add(context.getString(R.string.notification_service_state_bucket_connecting, summary.connectingCount))
                }
                if (summary.reconnectingCount > 0) {
                    add(context.getString(R.string.notification_service_state_bucket_reconnecting, summary.reconnectingCount))
                }
                if (summary.errorCount > 0) {
                    add(context.getString(R.string.notification_service_state_bucket_error, summary.errorCount))
                }
            }.joinToString(" · ")
        }

        val detailLines = buildList {
            summary.items.take(MAX_SERVICE_STATE_INBOX_LINES).forEach { item ->
                add(formatConnectionStatusLine(item))
            }
            val remaining = summary.items.size - MAX_SERVICE_STATE_INBOX_LINES
            if (remaining > 0) {
                add(context.getString(R.string.notification_service_state_more_items, remaining))
            }
        }

        val notification = buildForegroundServiceNotification(
            title = title,
            text = contentText,
            detailLines = detailLines,
            summaryText = contentText,
        )
        notificationManager.notify(
            FOREGROUND_PLUGIN_SERVICE_NOTIFICATION_ID,
            notification,
        )
    }

    fun sendExtensionDisconnectNotification(alert: DisconnectAlert) {
        ensureExtensionDisconnectChannel()
        val displayName = formatConnectionDisplayName(alert.alias, alert.extensionName)
        val reason = alert.reason.ifBlank {
            context.getString(R.string.notification_extension_disconnect_unknown_reason)
        }
        val title = context.getString(R.string.notification_extension_disconnect_title, displayName)
        val content = context.getString(R.string.notification_extension_disconnect_reason, reason)

        val notification = NotificationCompat.Builder(context, EXTENSION_DISCONNECT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(createMainLauncherPendingIntent())
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        notificationManager.notify(disconnectNotificationId(alert.connectionId), notification)
    }

    fun cancelExtensionDisconnectNotification(connectionId: Long) {
        notificationManager.cancel(disconnectNotificationId(connectionId))
    }

    private fun disconnectNotificationId(connectionId: Long): Int =
        EXTENSION_DISCONNECT_NOTIFICATION_BASE_ID + (connectionId.toInt() and 0xFFFF)

    private fun formatConnectionDisplayName(alias: String, extensionName: String): String =
        when {
            alias.isNotBlank() && extensionName.isNotBlank() && alias != extensionName ->
                "$alias ($extensionName)"
            alias.isNotBlank() -> alias
            else -> extensionName
        }

    private fun formatConnectionStatusLine(item: ExtensionConnectionSnapshot): String {
        val displayName = formatConnectionDisplayName(item.alias, item.extensionName)
        val statusText = when (val state = item.state) {
            is ExtensionConnectionState.Active ->
                context.getString(R.string.notification_service_state_status_active)

            is ExtensionConnectionState.Connecting ->
                context.getString(R.string.notification_service_state_status_connecting)

            is ExtensionConnectionState.Reconnecting -> {
                val base = context.getString(
                    R.string.notification_service_state_status_reconnecting_attempt,
                    state.attempt,
                )
                if (state.reason.isNotBlank()) "$base - ${state.reason}" else base
            }

            is ExtensionConnectionState.Error -> {
                val base = context.getString(R.string.notification_service_state_status_error)
                if (state.reason.isNotBlank()) "$base - ${state.reason}" else base
            }
        }
        return "$displayName · $statusText"
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

    fun sendPlayUpdateNotification(progressCurrent: Int, progressMax: Int) {
        createNotificationChannel(
            PLAY_APP_UPDATE_CHANNEL_ID,
            "更新",
            "应用更新",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val builder = Notification.Builder(context, PLAY_APP_UPDATE_CHANNEL_ID).apply {
            setContentTitle("Parabox 版本更新")
            setContentText("正在获取更新")
            setSmallIcon(R.drawable.ic_stat_name)
            setProgress(progressMax, progressCurrent, false)
        }
        notificationManager.notify(PLAY_APP_UPDATE_NOTIFICATION_ID, builder.build())
    }
    fun sendPlayUpdateResultNotification(isSuccess: Boolean) {
        createNotificationChannel(
            PLAY_APP_UPDATE_CHANNEL_ID,
            "更新",
            "应用更新",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val launchPendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "parabox://update".toUri()
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val builder = Notification.Builder(context, PLAY_APP_UPDATE_CHANNEL_ID).apply {
            setProgress(0, 0, false)
            setContentTitle("Parabox 版本更新")
            if (isSuccess) {
                setContentText("更新已就绪，等待重启")
                setContentIntent(launchPendingIntent)
                setActions(
                    Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.baseline_mark_chat_read_24),
                        "重启应用",
                        launchPendingIntent
                    ).build()
                )
            } else {
                setContentText("未知错误")
            }
            setSmallIcon(R.drawable.ic_stat_name)
        }
        notificationManager.notify(PLAY_APP_UPDATE_NOTIFICATION_ID, builder.build())
    }
}