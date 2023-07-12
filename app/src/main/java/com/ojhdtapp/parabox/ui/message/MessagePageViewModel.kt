package com.ojhdtapp.parabox.ui.message

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.core.util.getDataStoreValueFlow
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.GetMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getChat: GetChat,
    val getContact: GetContact,
) : ViewModel() {
    private var _pageStateFlow = MutableStateFlow(MessagePageState())
    val pageStateFlow get() = _pageStateFlow.asStateFlow()

    // Datastore
    val enableSwipeToDismissFlow = context.getDataStoreValueFlow(DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS, false)

    private val chatLatestMessageSenderMap = mutableMapOf<Long, Resource<Contact>>()

    fun getLatestMessageSenderWithCache(senderId: Long?): Flow<Resource<Contact>> {
        return flow {
            if (senderId == null) {
                emit(Resource.Error("no sender"))
            } else {
                if (chatLatestMessageSenderMap[senderId] != null) {
                    emit(chatLatestMessageSenderMap[senderId]!!)
                } else {
                    emitAll(
                        getContact.byId(senderId).onEach {
                            if (it is Resource.Success) {
                                chatLatestMessageSenderMap[senderId] = it
                            }
                        }
                    )
                }
            }
        }
    }

    fun getChatPagingDataFlow(): Flow<PagingData<ChatWithLatestMessage>> {
        return getChat(pageStateFlow.value.getChatFilterList).cachedIn(viewModelScope)
    }

//    fun testFun(){
//        viewModelScope.launch(Dispatchers.IO) {
//            repeat(5) { num ->
//                mainRepository.receiveMessage(
//                    msg = ReceiveMessage(
//                        contents = listOf(
//                            ParaboxPlainText(text = "测试文本")
//                        ),
//                        sender = ParaboxContact(
//                            name = "User_${num}",
//                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
//                            uid = "sender_${num}"
//                        ),
//                        chat = ParaboxChat(
//                            name = "测试会话_${num}",
//                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
//                            type = ParaboxChat.TYPE_PRIVATE,
//                            uid = "group_${num}"
//                        ),
//                        timestamp = System.currentTimeMillis(),
//                        uuid = "msg_${num}_1"
//                    ),
//                    ext = ExtensionInfo(
//                        pkg = "parabox.extension.test",
//                        name = "test_extension",
//                        version = "1.0",
//                        versionCode = 1,
//                    )
//                )
//            }
//
//        }
//    }
}