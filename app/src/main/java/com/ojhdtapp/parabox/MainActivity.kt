package com.ojhdtapp.parabox

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
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
                        lifecycleScope.launch {
                            val res = pluginConn.connect()
                            Log.d("parabox", "connect status: $res")
                            repeatOnLifecycle(Lifecycle.State.STARTED) {
                                pluginConn.messageResFlow.collectLatest {
                                    viewModel.setMessage(it)
                                }
                                pluginConn.connectionStatusFlow.collect {
                                    viewModel.setSendAvailableState(it)
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