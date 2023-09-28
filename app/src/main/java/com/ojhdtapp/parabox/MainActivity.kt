package com.ojhdtapp.parabox

import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.mlkit.nl.entityextraction.*
import com.google.mlkit.nl.smartreply.*
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.audio.AudioRecorder
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.destinations.MenuPageDestination
import com.ojhdtapp.parabox.domain.service.ExtensionService
import com.ojhdtapp.parabox.domain.service.ExtensionServiceConnection
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.common.FixedInsets
import com.ojhdtapp.parabox.ui.common.LocalFixedInsets
import com.ojhdtapp.parabox.ui.common.isBookPosture
import com.ojhdtapp.parabox.ui.common.isSeparating
import com.ojhdtapp.parabox.ui.menu.MenuPage
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.NestedNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var extensionServiceConnection : ExtensionServiceConnection
    private fun collectDarkModeFlow() {
        lifecycleScope.launch {
            dataStore.data.collectLatest {
                val darkMode = it[DataStoreKeys.SETTINGS_DARK_MODE]
                if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                    when (darkMode) {
                        DataStoreKeys.DARK_MODE.YES.ordinal -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_YES
                        }

                        DataStoreKeys.DARK_MODE.NO.ordinal -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_NO
                        }

                        else -> {
                            manager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                        }
                    }
                } else {
                    when (darkMode) {
                        DataStoreKeys.DARK_MODE.YES.ordinal -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }

                        DataStoreKeys.DARK_MODE.NO.ordinal -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        else -> {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        }
                    }
                    delegate.applyDayNight()
                }
            }
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class,
        ExperimentalMaterial3WindowSizeClassApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply Dark Mode
        collectDarkModeFlow()
        // Bind Extension Service
        extensionServiceConnection = ExtensionServiceConnection(baseContext)
        lifecycle.addObserver(extensionServiceConnection)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Device Posture
        val devicePostureFlow = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this)
            .flowWithLifecycle(this.lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
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

            val mainNavController = rememberNavController()
            val mainNavHostEngine = rememberAnimatedNavHostEngine(
                navHostContentAlignment = Alignment.TopCenter,
                rootDefaultAnimations = RootNavGraphDefaultAnimations(
//                    enterTransition = { slideInHorizontally { it }},
//                    exitTransition = { slideOutHorizontally { -it }},
//                    popEnterTransition = { slideInHorizontally { -it }},
//                    popExitTransition = { slideOutHorizontally { it }},
//                    enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
//                    exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
//                    popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 1.1f) },
//                    popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 0.9f) }
                    enterTransition = { slideInHorizontally { 100 } + fadeIn() },
                    exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
                    popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
                    popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
                ),
                defaultAnimationsForNestedNavGraph = mapOf(
                    NavGraphs.guide to NestedNavGraphDefaultAnimations(
                        enterTransition = { slideInHorizontally { it } },
                        exitTransition = { slideOutHorizontally { -it } },
                        popEnterTransition = { slideInHorizontally { -it } },
                        popExitTransition = { slideOutHorizontally { it } },
//                        enterTransition = { slideInHorizontally { 100 } + fadeIn() },
//                        exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
//                        popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
//                        popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
                    )
                )
            )
            // Shared ViewModel
            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>(this)

            // Screen Sizes
            val sizeClass = calculateWindowSizeClass(activity = this)
            val devicePosture = devicePostureFlow.collectAsState().value


//            val shouldShowNav = menuNavController.appCurrentDestinationAsState().value in listOf(
//                MessagePageDestination,
//                FilePageDestination,
//                SettingPageDestination
//            )

            // Navigate to guide
//            LaunchedEffect(Unit) {
//                // read from datastore
//                val isFirstLaunch = !mainSharedViewModel.guideLaunchedStateFlow.value
//                        && dataStore.data.first()[DataStoreKeys.IS_FIRST_LAUNCH] ?: true
//                if (isFirstLaunch) {
//                    mainSharedViewModel.launchedGuide()
//                    mainNavController.navigate(GuideWelcomePageDestination) {
//                        popUpTo(NavGraphs.root) {
//                            saveState = true
//                        }
//                        launchSingleTop = true
//                        restoreState = true
//                    }
//                }
//            }
            AppTheme {
                CompositionLocalProvider(
                    values = arrayOf(
                        LocalFixedInsets provides fixedInsets,
                        LocalAudioRecorder provides AudioRecorder
                    )) {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        engine = mainNavHostEngine,
                        navController = mainNavController,
                        dependenciesContainerBuilder = {
                            dependency(mainSharedViewModel)
                            dependency(devicePosture)
                            dependency(sizeClass)
                        }){
                    }

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

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}