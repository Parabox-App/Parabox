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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ojhdtapp.parabox.domain.plugin.Conn
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.theme.ParaboxTheme

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
                MessagePage() {
                    pluginConn.connect()
                }
            }
        }
    }
}