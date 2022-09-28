package com.ojhdtapp.parabox.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.use_case.UpdateContact
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MarkAsReadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateContact: UpdateContact

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            val contact = intent.getParcelableExtra<Contact>("contact")
            if (contact == null) {
                return@launch
            } else {
                updateContact.unreadMessagesNum(contact.contactId, 0)
                notificationUtil.clearNotification(contact.contactId.toInt())
            }
        }
    }
}