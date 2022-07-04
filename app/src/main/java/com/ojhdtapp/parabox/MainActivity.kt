package com.ojhdtapp.parabox

import android.os.Bundle
import androidx.compose.ui.unit.LayoutDirection
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ojhdtapp.parabox.domain.plugin.Conn
import com.ojhdtapp.parabox.ui.MainScreenSharedViewModel
import com.ojhdtapp.parabox.ui.NavGraphs
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.util.FixedInsets
import com.ojhdtapp.parabox.ui.util.LocalFixedInsets
import com.ojhdtapp.parabox.ui.util.SharedAxisZTransition
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.NestedNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.rememberNavHostEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val pluginConn = Conn(
//            this,
//            "com.ojhdtapp.miraipluginforparabox",
//            "com.ojhdtapp.miraipluginforparabox.domain.service.ConnService"
//        ).also {
//            viewModel.setPluginInstalledState(
//                it.isInstalled()
//            )
//        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            // System Ui
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = isSystemInDarkTheme()
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !useDarkIcons
                )
            }

            // System Bars
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding()
                )
            }

            // Destination
            val navController = rememberAnimatedNavController()
            val navHostEngine = rememberAnimatedNavHostEngine(
                navHostContentAlignment = Alignment.TopCenter,
                rootDefaultAnimations = RootNavGraphDefaultAnimations(),
                defaultAnimationsForNestedNavGraph = mapOf(
//                    NavGraphs.message to NestedNavGraphDefaultAnimations(
//                        enterTransition = { scaleIn(tween(200), 0.9f) + fadeIn(tween(200)) },
//                        exitTransition = { scaleOut(tween(200), 1.1f) + fadeOut(tween(200)) },
//                        popEnterTransition = {scaleIn(tween(200), 1.1f) + fadeIn(tween(200))},
//                        popExitTransition = {scaleOut(tween(200), 0.9f) + fadeOut(tween(200))}
//                    )
                )
            )
            AppTheme {
                CompositionLocalProvider(values = arrayOf(LocalFixedInsets provides fixedInsets)) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        engine = navHostEngine,
                        navController = navController,
                        dependenciesContainerBuilder = {
                            dependency(NavGraphs.message){
                                val parentEntry = remember(navBackStackEntry){
                                    navController.getBackStackEntry(NavGraphs.message.route)
                                }
                                hiltViewModel<MessagePageViewModel>(parentEntry)
                            }
                            dependency(hiltViewModel<MainScreenSharedViewModel>(this@MainActivity))
                        }
                    )
                }

//                MessagePage(
//                    onConnectBtnClicked = {
//                        pluginConn.connect()
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                pluginConn.connectionStateFlow.collect {
//                                    Log.d("parabox", "connection state received")
//                                    viewModel.setSendAvailableState(it)
//                                }
//                            }
//                        }
//                        lifecycleScope.launch {
//                            repeatOnLifecycle(Lifecycle.State.STARTED){
//                                repeatOnLifecycle(Lifecycle.State.STARTED) {
//                                    pluginConn.messageResFlow.collect {
//                                        Log.d("parabox", "message received")
//                                        viewModel.setMessage(it)
//                                    }
//                                }
//                            }
//                        }
//                    },
//                    onSendBtnClicked = {
//                        pluginConn.send(
//                            (0..10).random().toString()
//                        )
//                    }
//                )
            }
        }
    }
}