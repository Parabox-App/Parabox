package com.ojhdtapp.parabox

import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.audio.AudioRecorder
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.FixedInsets
import com.ojhdtapp.parabox.ui.common.LocalFixedInsets
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.SystemUiController
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

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val chatId = intent.data?.lastPathSegment?.toLongOrNull()
                if (chatId != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val chat = database.chatDao.getChatByIdWithoutObserve(chatId)?.toChat()
                        if (chat != null) {
                            messagePageViewModel.sendEvent(MessagePageEvent.LoadMessage(chat))
                        } else {
                            Toast.makeText(this@BubbleActivity, "加载消息列表失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@BubbleActivity, "加载消息列表失败", Toast.LENGTH_SHORT).show()
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
                        LocalFixedInsets provides fixedInsets,
                        LocalAudioRecorder provides AudioRecorder,
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
                        scaffoldNavigator = null,
                        layoutType = LayoutType.SINGLE_PAGE,
                        onEvent = messagePageViewModel::sendEvent,
                        onMainSharedEvent = mainSharedViewModel::sendEvent
                    )
                }
            }
        }
    }
}