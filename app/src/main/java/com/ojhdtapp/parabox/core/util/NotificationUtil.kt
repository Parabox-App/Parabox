package com.ojhdtapp.parabox.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.emptyPreferences
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ojhdtapp.parabox.MainActivity
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.model.message_content.Image
import com.ojhdtapp.parabox.domain.model.message_content.getContentString
import com.ojhdtapp.parabox.domain.notification.ReplyReceiver
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.ClassCastException

class NotificationUtil(
    val context: Context,
    val database: AppDatabase
) {
    companion object {
//        const val GROUP_KEY_NEW_MESSAGE = "group_new_message"
//        const val GROUP_KEY_INTERNAL = "group_internal"

        private const val REQUEST_CONTENT = 1
        private const val REQUEST_BUBBLE = 2

        const val KEY_TEXT_REPLY = "key_text_reply"
        val remoteInput: RemoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
            setLabel("输入回复")
            build()
        }
    }

    private val notificationManager: NotificationManager =
        context.getSystemService() ?: throw IllegalStateException()

    private val shortcutManager: ShortcutManager =
        context.getSystemService() ?: throw IllegalStateException()

    private val tempMessageMap =
        mutableMapOf<Long, CopyOnWriteArrayList<Pair<Message, Person>>>()

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

    suspend fun updateShortcuts(importantContact: Contact? = null) {
        // Truncate the list if we can't show all of our contacts.
        val maxCount = shortcutManager.maxShortcutCountPerActivity
        var contects = database.contactDao.getAllContacts().firstOrNull() ?: emptyList()
        if (contects.size > maxCount) {
            contects = contects.take(maxCount)
        }
        var shortcuts = contects.map {
            val icon = it.profile.avatar?.let { url ->
                withContext(Dispatchers.IO) {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false) // Disable hardware bitmaps.
                        .build()
                    val result = (loader.execute(request) as SuccessResult).drawable
                    val bitmap = (result as BitmapDrawable).bitmap
                    Icon.createWithAdaptiveBitmap(bitmap)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ShortcutInfo.Builder(context, it.contactId.toString())
                    .setLocusId(LocusId(it.contactId.toString()))
                    .setActivity(ComponentName(context, MainActivity::class.java))
                    .setShortLabel(it.profile.name)
                    .setLongLabel(it.profile.name)
                    .setIcon(icon)
                    .setLongLived(true)
                    .setCategories(setOf("com.ojhdtapp.parabox.bubbles.category.TEXT_SHARE_TARGET"))
                    .setIntent(Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("parabox://contact/${it.contactId}")
                    })
                    .setPerson(
                        Person.Builder()
                            .setName(it.profile.name)
                            .setIcon(icon)
                            .build()
                    )
                    .build()
            } else {
                ShortcutInfo.Builder(context, it.contactId.toString())
                    .setActivity(ComponentName(context, MainActivity::class.java))
                    .setShortLabel(it.profile.name)
                    .setLongLabel(it.profile.name)
                    .setIcon(icon)
                    .setCategories(setOf("com.ojhdtapp.parabox.bubbles.category.TEXT_SHARE_TARGET"))
                    .setIntent(Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse("parabox://contact/${it.contactId}")
                    })
                    .build()
            }
        }
        // Move the important contact to the front of the shortcut list.
        if (importantContact != null) {
            shortcuts =
                shortcuts.sortedByDescending { it.id == importantContact.contactId.toString() }
        }
        shortcutManager.addDynamicShortcuts(shortcuts)
    }

    suspend fun sendNewMessageNotification(
        message: Message,
        contact: Contact,
        channelId: String,
        isGroupSpecify: Boolean? = null
    ) {
        Log.d("parabox", "sendNotification at channel:${channelId}")
        updateShortcuts(contact)
        val isGroup = isGroupSpecify ?: (message.profile.name != contact.profile.name)
        val userNameFlow: Flow<String> = context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { settings ->
                settings[DataStoreKeys.USER_NAME] ?: "您"
            }
        val userAvatarFlow: Flow<String?> = context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { settings ->
                settings[DataStoreKeys.USER_AVATAR]
            }

        val launchPendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN.apply {
                    putExtra("contactId", contact.contactId)
                }
                addCategory(Intent.CATEGORY_LAUNCHER)
            }.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
        val replyPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                message.messageId.toInt(),
                Intent(context, ReplyReceiver::class.java).apply {
                    putExtra("contact", contact)
                    putExtra(
                        "sendTargetType",
                        if (isGroup) SendTargetType.GROUP else SendTargetType.USER
                    )
                },
                PendingIntent.FLAG_MUTABLE
            )

        val notificationBuilder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val userIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    userAvatarFlow.firstOrNull()?.let {
                        Icon.createWithAdaptiveBitmapContentUri(it)
                    } ?: Icon.createWithResource(context, R.drawable.avatar)
                } else {
                    userAvatarFlow.firstOrNull()?.let {
                        Icon.createWithContentUri(it)
                    } ?: Icon.createWithResource(context, R.drawable.avatar)
                }
                val user =
                    Person.Builder().setName(userNameFlow.firstOrNull()).setIcon(userIcon).build()

                val personIcon = message.profile.avatar?.let { url ->
                    withContext(Dispatchers.IO) {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false) // Disable hardware bitmaps.
                            .build()
                        val result = (loader.execute(request) as SuccessResult).drawable
                        val bitmap = (result as BitmapDrawable).bitmap
                        Icon.createWithAdaptiveBitmap(bitmap)
                    }
                }
                val person =
                    Person.Builder().setName(message.profile.name).setIcon(personIcon).build()
                val groupIcon = contact.profile.avatar?.let { url ->
                    withContext(Dispatchers.IO) {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false) // Disable hardware bitmaps.
                            .build()
                        val result = (loader.execute(request) as SuccessResult).drawable
                        val bitmap = (result as BitmapDrawable).bitmap
                        Icon.createWithAdaptiveBitmap(bitmap)
                    }
                }
                Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_android_black_24dp)
                    .setLargeIcon(groupIcon)
                    .setContentTitle(contact.profile.name)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setShortcutId(contact.contactId.toString())
                    .setContentIntent(launchPendingIntent)
                    .addPerson(person)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setWhen(message.timestamp)
                    .addAction(
                        Notification.Action
                            .Builder(
                                Icon.createWithResource(context, R.drawable.baseline_send_24),
                                "回复",
                                replyPendingIntent
                            )
                            .addRemoteInput(remoteInput)
                            .setAllowGeneratedReplies(true)
                            .build()
                    )
                    .setStyle(
                        Notification.MessagingStyle(user).apply {
                            // temp message
                            if (tempMessageMap[contact.contactId] == null) {
                                tempMessageMap[contact.contactId] =
                                    CopyOnWriteArrayList(arrayOf(message to person))
                            } else {
                                tempMessageMap[contact.contactId]?.run {
                                    add(message to person)
                                    if (size > 10) {
                                        this.removeAt(0)
                                    }
                                }
                            }

                            tempMessageMap[contact.contactId]?.forEach {
                                val m = Notification.MessagingStyle.Message(
                                    it.first.contents.getContentString(),
                                    it.first.timestamp,
                                    if (it.first.sentByMe) null else it.second
                                ).apply {
                                    it.first.contents.filterIsInstance<Image>().firstOrNull()?.let {
//                                        val mimetype = MimeTypeMap.getSingleton()
//                                            .getMimeTypeFromExtension(
//                                                MimeTypeMap.getFileExtensionFromUrl(
//                                                    it.url
//                                                )
//                                            )
                                        val mimetype = "image/png"
                                        val imageUri =
                                            it.uriString?.let { Uri.parse(it) } ?: try {
                                                val loader = ImageLoader(context)
                                                val request = ImageRequest.Builder(context)
                                                    .data(it.url)
                                                    .allowHardware(false) // Disable hardware bitmaps.
                                                    .build()
                                                val result =
                                                    (loader.execute(request) as SuccessResult).drawable
                                                FileUtil.getUriFromBitmap(
                                                    context,
                                                    (result as BitmapDrawable).bitmap
                                                )
                                            } catch (e: ClassCastException) {
                                                e.printStackTrace()
                                                null
                                            }
                                        Log.d("parabox", imageUri.toString())
                                        setData(mimetype, imageUri)
                                    }
                                }
                                if (it.first.timestamp < m.timestamp) {
                                    addHistoricMessage(m)
                                } else {
                                    addMessage(m)
                                }
                            }

                            isGroupConversation = isGroup
                            conversationTitle = contact.profile.name
                        }
                    ).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setLocusId(LocusId(contact.contactId.toString()))
                        }
                    }
            } else {
                val notificationBuilder = Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_android_black_24dp)
                    .setContentTitle(contact.profile.name)
                    .setContentText(message.contents.getContentString())
                    .setContentIntent(launchPendingIntent)
                    .setAutoCancel(true)
                val senderName = "Me"
                Notification.MessagingStyle(senderName)
                    .addMessage("Check this out!", Date().time, senderName)
                    .setConversationTitle(contact.profile.name)
                    .setBuilder(notificationBuilder)
                notificationBuilder
            }
        notificationManager.notify(contact.contactId.toInt(), notificationBuilder.build())
    }
}
