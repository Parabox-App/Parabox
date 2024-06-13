package com.ojhdtapp.parabox.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MarkAsReadReceiver : BroadcastReceiver() {
    @Inject
    lateinit var updateChat: UpdateChat

    @Inject
    lateinit var appDatabase: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        GlobalScope.launch(Dispatchers.IO) {
            val chat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("chat", Chat::class.java)
            } else {
                intent.getSerializableExtra("chat") as? Chat
            }
            if (chat != null) {
                updateChat.unreadMessagesNum(chat.chatId, 0)
                context.getDataStoreValue(DataStoreKeys.MESSAGE_BADGE_NUM, 0).also {
                    context.dataStore.edit { preferences ->
                        preferences[DataStoreKeys.MESSAGE_BADGE_NUM] = (it - chat.unreadMessageNum).coerceAtLeast(0)
                    }
                }
            }
        }
    }
}