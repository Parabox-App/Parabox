package com.ojhdtapp.parabox

import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.audio.AudioPlayer
import com.ojhdtapp.parabox.core.util.audio.AudioRecorder
import com.ojhdtapp.parabox.core.util.audio.LocalAudioPlayer
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.FixedInsets
import com.ojhdtapp.parabox.ui.common.LocalFixedInsets
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.SystemUiController
import com.ojhdtapp.parabox.ui.common.enableTransparentNavigationBar
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.message.chat.NormalChatPage
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.theme.FontSize
import com.ojhdtapp.parabox.ui.theme.LocalFontSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BubbleActivity : AppCompatActivity() {
    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var audioPlayer: AudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableTransparentNavigationBar()
        window.decorView.setViewTreeNavigationEventDispatcherOwner(this)
        setContent {
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding()
                )
            }
            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>()
            val messagePageViewModel = hiltViewModel<MessagePageViewModel>()
            val mainSharedState by mainSharedViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                Log.d("parabox", "new intent host=${intent.data?.host},scheme=${intent.data?.scheme},path=${intent.data?.host},query=${intent.data?.encodedQuery},pathSegments=${intent.data?.pathSegments?.joinToString(" ")}")
                if (intent.data?.host == "chat") {
                    val chatId = intent.data?.lastPathSegment?.toLongOrNull()
                    if (chatId != null && messagePageViewModel.uiState.value.chatDetail.chat?.chatId != chatId) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val chat = database.chatDao.getChatByIdWithoutObserve(chatId)?.toChat()
                            if (chat != null) {
                                messagePageViewModel.sendEvent(MessagePageEvent.LoadMessage(chat))
                            } else {
                                Toast.makeText(this@BubbleActivity, "加载消息列表失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (chatId == null) {
                        Toast.makeText(this@BubbleActivity, "加载消息列表失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            LaunchedEffect(mainSharedState.datastore.darkMode) {
                if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(UI_MODE_SERVICE) as UiModeManager
                    when (mainSharedState.datastore.darkMode) {
                        DataStoreKeys.DarkMode.YES -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_YES
                        }
                        DataStoreKeys.DarkMode.NO -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_NO
                        }
                        DataStoreKeys.DarkMode.FOLLOW_SYSTEM -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                        }
                    }
                } else {
                    when (mainSharedState.datastore.darkMode) {
                        DataStoreKeys.DarkMode.YES -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                        DataStoreKeys.DarkMode.NO -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }
                        DataStoreKeys.DarkMode.FOLLOW_SYSTEM -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                    delegate.applyDayNight()
                }
            }
            AppTheme {
                CompositionLocalProvider(
                    values = arrayOf(
                        LocalNavigationEventDispatcherOwner provides this,
                        LocalFixedInsets provides fixedInsets,
                        LocalAudioRecorder provides AudioRecorder,
                        LocalAudioPlayer provides audioPlayer,
                        LocalSystemUiController provides SystemUiController(this),
                        LocalMinimumInteractiveComponentSize provides 32.dp,
                        LocalFontSize provides FontSize()
                    )
                ) {
                    val state by messagePageViewModel.uiState.collectAsState()
                    NormalChatPage(
                        viewModel = messagePageViewModel,
                        state = state,
                        mainSharedState = mainSharedState,
                        layoutType = LayoutType.SINGLE_PAGE,
                        onNavigateBack = { finish() },
                        onEvent = messagePageViewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::audioPlayer.isInitialized) {
            audioPlayer.pause()
        }
    }
}