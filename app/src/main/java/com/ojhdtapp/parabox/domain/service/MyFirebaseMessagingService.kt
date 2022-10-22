package com.ojhdtapp.parabox.domain.service

import android.util.Log
import androidx.datastore.preferences.core.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        Log.d("parabox", "Refreshed token: $token")
        GlobalScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.FCM_TOKEN] = token
            }
        }
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("parabox", "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("parabox", "Message data payload: ${remoteMessage.data}")

        }
        super.onMessageReceived(remoteMessage)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}