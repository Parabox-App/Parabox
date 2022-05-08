package com.ojhdtapp.parabox

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ojhdtapp.parabox.domain.plugin.Conn
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.theme.ParaboxTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MessagePageViewModel by viewModels()

        val pluginConn = Conn(
            this,
            "com.ojhdtapp.miraipluginforparabox",
            "com.ojhdtapp.miraipluginforparabox.domain.service.ConnService"
        ).also {
            viewModel.setPluginInstalledState(
                it.isInstalled()
            )
        }
        setContent {
            ParaboxTheme {
                MessagePage(
                    onConnectBtnClicked = {
                        pluginConn.connect()
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED){
                                pluginConn.connectionStateFlow.collect {
                                    Log.d("parabox", "connection state received")
                                    viewModel.setSendAvailableState(it)
                                }
                            }
                        }
                        lifecycleScope.launch {
                            repeatOnLifecycle(Lifecycle.State.STARTED){
                                repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    pluginConn.messageResFlow.collect {
                                        Log.d("parabox", "message received")
                                        viewModel.setMessage(it)
                                    }
                                }
                            }
                        }
                    },
                    onSendBtnClicked = {
                        pluginConn.send(
                            (0..10).random().toString()
                        )
                    }
                )
            }
        }
    }
}