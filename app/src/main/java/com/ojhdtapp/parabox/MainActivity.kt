package com.ojhdtapp.parabox

import FilePage
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.retainedComponent
import com.arkivanov.decompose.router.stack.pop
import com.ojhdtapp.parabox.core.util.*
import com.ojhdtapp.parabox.core.util.audio.AudioRecorder
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.domain.service.ExtensionServiceConnection
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.common.ChatPickerDialog
import com.ojhdtapp.parabox.ui.common.ContactPickerDialog
import com.ojhdtapp.parabox.ui.common.DateRangePickerDialog
import com.ojhdtapp.parabox.ui.common.DevicePosture
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.common.FixedInsets
import com.ojhdtapp.parabox.ui.common.LocalFixedInsets
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.SystemUiController
import com.ojhdtapp.parabox.ui.common.isBookPosture
import com.ojhdtapp.parabox.ui.common.isSeparating
import com.ojhdtapp.parabox.ui.contact.ContactPageViewModel
import com.ojhdtapp.parabox.ui.contact.ContactPageWrapperUI
import com.ojhdtapp.parabox.ui.file.FilePageViewModel
import com.ojhdtapp.parabox.ui.message.ContactDetailDialog
import com.ojhdtapp.parabox.ui.message.MessageAndChatPageWrapperUI
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.navigation.DefaultRootComponent
import com.ojhdtapp.parabox.ui.navigation.MenuComponent
import com.ojhdtapp.parabox.ui.navigation.RootComponent
import com.ojhdtapp.parabox.ui.navigation.slideWithOffset
import com.ojhdtapp.parabox.ui.navigation.suite.NavigationSuite
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.setting.SettingPageWrapperUi
import com.ojhdtapp.parabox.ui.theme.FontSize
import com.ojhdtapp.parabox.ui.theme.LocalFontSize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var notificationUtil: NotificationUtil

    private lateinit var extensionServiceConnection: ExtensionServiceConnection
    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalDecomposeApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bind Extension Service
        extensionServiceConnection = ExtensionServiceConnection(baseContext)
        lifecycle.addObserver(extensionServiceConnection)
        // notification util
        lifecycle.addObserver(notificationUtil)
        // Edge to Edge
        if (DeviceUtil.isMIUI(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.attributes = lp
        }

        // Device Posture
        val devicePostureFlow = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
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
        val root = retainedComponent {
            DefaultRootComponent(
                componentContext = it,
            )
        }

        setContent {
            // System Ui
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim,
                        darkScrim,
                    ) { darkTheme },
                )
                onDispose {}
            }

            // System Bars
            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding()
                )
            }
            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>()
            val messagePageViewModel = hiltViewModel<MessagePageViewModel>()
            val filePageViewModel = hiltViewModel<FilePageViewModel>()
            val contactPageViewModel = hiltViewModel<ContactPageViewModel>()
            val settingPageViewModel = hiltViewModel<SettingPageViewModel>()

            val mainSharedState by mainSharedViewModel.uiState.collectAsState()

            // darkMode
            LaunchedEffect(mainSharedState.datastore.darkMode) {
                if (BuildConfig.VERSION_CODE >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
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
//            val mainNavController = rememberNavController()
//            val mainNavHostEngine = rememberAnimatedNavHostEngine(
//                navHostContentAlignment = Alignment.TopCenter,
//                rootDefaultAnimations = RootNavGraphDefaultAnimations(
//                    enterTransition = { slideInHorizontally { it }},
//                    exitTransition = { slideOutHorizontally { -it }},
//                    popEnterTransition = { slideInHorizontally { -it }},
//                    popExitTransition = { slideOutHorizontally { it }},
//                    enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 0.9f) },
//                    exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 1.1f) },
//                    popEnterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), 1.1f) },
//                    popExitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), 0.9f) }
//                    enterTransition = { slideInHorizontally { 100 } + fadeIn() },
//                    exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
//                    popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
//                    popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
//                ),
//                defaultAnimationsForNestedNavGraph = mapOf(
//                    NavGraphs.guide to NestedNavGraphDefaultAnimations(
//                        enterTransition = { slideInHorizontally { it } },
//                        exitTransition = { slideOutHorizontally { -it } },
//                        popEnterTransition = { slideInHorizontally { -it } },
//                        popExitTransition = { slideOutHorizontally { it } },
//                        enterTransition = { slideInHorizontally { 100 } + fadeIn() },
//                        exitTransition = { slideOutHorizontally { -100 } + fadeOut() },
//                        popEnterTransition = { slideInHorizontally { -100 } + fadeIn() },
//                        popExitTransition = { slideOutHorizontally { 100 } + fadeOut() }
//                    )
//                )
//            )

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
                        LocalAudioRecorder provides AudioRecorder,
                        LocalSystemUiController provides SystemUiController(this),
                        LocalMinimumInteractiveComponentSize provides 32.dp,
                        LocalFontSize provides FontSize()
                    )
                ) {
                    val rootStackState by root.rootStack.subscribeAsState()
                    ChatPickerDialog(
                        openDialog = mainSharedState.chatPicker.showDialog,
                        data = mainSharedState.chatPicker.result,
                        query = mainSharedState.chatPicker.query,
                        onQueryChange = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatQueryInput(it))
                        },
                        loadState = mainSharedState.chatPicker.loadState,
                        onConfirm = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(it))
                        },
                        onDismiss = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(null))
                        }
                    )
                    ContactPickerDialog(openDialog = mainSharedState.contactPicker.showDialog,
                        data = mainSharedState.contactPicker.result,
                        query = mainSharedState.contactPicker.query,
                        onQueryChange = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactQueryInput(it))
                        },
                        loadState = mainSharedState.contactPicker.loadState,
                        onConfirm = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(it))
                        },
                        onDismiss = {
                            mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(null))
                        }
                    )
                    DateRangePickerDialog(openDialog = mainSharedState.openDateRangePicker, onConfirm = { start, end ->
                        mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(start to end))
                    }, onDismiss = {
                        mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(null))
                    })
                    ContactDetailDialog(
                        contactDetailDialogState = mainSharedState.contactDetailDialogState,
                        onEvent = mainSharedViewModel::sendEvent
                    )
                    Children(
                        stack = root.rootStack,
                        animation = predictiveBackAnimation(
                            backHandler = root.backHandler,
                            fallbackAnimation = stackAnimation(fade() + slideWithOffset(tween(), Orientation.Horizontal, 300f)),
                            selector = { backEvent, _, _ -> androidPredictiveBackAnimatable(backEvent) },
                            onBack = {
                                root.rootNav.pop()
                            },
                        ),
                    ) { child ->
                        when (val instance = child.instance) {
                            is RootComponent.RootChild.Menu -> {
                                val menuStackState by instance.component.menuStack.subscribeAsState()
                                NavigationSuite(
                                    rootNavigation = root.rootNav,
                                    rootStackState = rootStackState,
                                    menuNavigation = instance.component.menuNav,
                                    menuStackState = menuStackState
                                ) {
                                    Children(
                                        stack = instance.component.menuStack,
                                        animation = stackAnimation(animator = fade() + slideWithOffset( tween(), Orientation.Vertical, 80f)),
                                    ) { child ->
                                        when (child.instance) {
                                            is MenuComponent.MenuChild.Message -> {
                                                MessageAndChatPageWrapperUI(
                                                    modifier = Modifier.fillMaxSize(),
                                                    mainSharedViewModel = mainSharedViewModel,
                                                    viewModel = messagePageViewModel,
                                                    navigation = instance.component.menuNav,
                                                    stackState = menuStackState
                                                )
                                            }

                                            is MenuComponent.MenuChild.File -> {
                                                FilePage(modifier = Modifier.fillMaxSize())
                                            }

                                            is MenuComponent.MenuChild.Contact -> {
                                                ContactPageWrapperUI(
                                                    modifier = Modifier.fillMaxSize(),
                                                    mainSharedViewModel = mainSharedViewModel,
                                                    viewModel = contactPageViewModel,
                                                    navigation = instance.component.menuNav,
                                                    stackState = menuStackState
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            is RootComponent.RootChild.Setting -> {
                                SettingPageWrapperUi(
                                    modifier = Modifier.fillMaxSize(),
                                    component = instance.component,
                                    mainSharedViewModel = mainSharedViewModel,
                                    viewModel = settingPageViewModel,
                                    navigation = root.rootNav,
                                    stackState = rootStackState
                                )
                            }
                        }

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

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(extensionServiceConnection)
        lifecycle.removeObserver(notificationUtil)
    }

    private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
}