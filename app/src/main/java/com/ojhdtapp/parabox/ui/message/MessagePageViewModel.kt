package com.ojhdtapp.parabox.ui.message

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.ojhdtapp.parabox.data.local.ExtensionInfo
import com.ojhdtapp.parabox.domain.repository.ChatRepository
import com.ojhdtapp.parabox.domain.repository.MainRepository
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val mainRepository: MainRepository,
    val chatRepository: ChatRepository
): ViewModel() {
    fun testFun(){
        viewModelScope.launch(Dispatchers.IO) {
            repeat(5) { num ->
                mainRepository.receiveMessage(
                    msg = ReceiveMessage(
                        contents = listOf(
                            ParaboxPlainText(text = "测试文本")
                        ),
                        sender = ParaboxContact(
                            name = "User_${num}",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            uid = "sender_${num}"
                        ),
                        chat = ParaboxChat(
                            name = "测试会话_${num}",
                            avatar = ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(url = "https://ojhdt.com/source/avatar.png"),
                            type = ParaboxChat.TYPE_PRIVATE,
                            uid = "group_${num}"
                        ),
                        timestamp = System.currentTimeMillis(),
                        uuid = "msg_${num}_1"
                    ),
                    ext = ExtensionInfo(
                        pkg = "parabox.extension.test",
                        name = "test_extension",
                        version = "1.0",
                        versionCode = 1,
                    )
                )
            }

        }
    }
}