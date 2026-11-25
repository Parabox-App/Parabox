package com.ojhdtapp.parabox

import com.ojhdtapp.parabox.ui.common.paneFocusAnchor

import FilePage
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.ktx.installStatus
import com.ojhdtapp.parabox.core.util.CacheUtil
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.DeviceUtil
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.LocalCacheUtil
import com.ojhdtapp.parabox.core.util.LocalFileUtil
import com.ojhdtapp.parabox.core.util.LocalPlayAppUpdateUtil
import com.ojhdtapp.parabox.core.util.NotificationUtil
import com.ojhdtapp.parabox.core.util.PlayAppUpdateUtil
import com.ojhdtapp.parabox.core.util.audio.AudioPlayer
import com.ojhdtapp.parabox.core.util.audio.AudioRecorder
import com.ojhdtapp.parabox.core.util.audio.LocalAudioPlayer
import com.ojhdtapp.parabox.core.util.audio.LocalAudioRecorder
import com.ojhdtapp.parabox.core.util.backup.LocalRoomBackup
import com.ojhdtapp.parabox.data.local.AppDatabase
import com.ojhdtapp.parabox.domain.cloud.LocalCloudService
import com.ojhdtapp.parabox.domain.service.ExtensionServiceConnection
import com.ojhdtapp.parabox.ui.MainSharedEffect
import com.ojhdtapp.parabox.ui.MainSharedEvent
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.RootNavigateTarget
import com.ojhdtapp.parabox.ui.common.ChangeLogBottomSheet
import com.ojhdtapp.parabox.ui.common.ChatPickerDialog
import com.ojhdtapp.parabox.ui.common.ContactPickerDialog
import com.ojhdtapp.parabox.ui.common.DateRangePickerDialog
import com.ojhdtapp.parabox.ui.common.FixedInsets
import com.ojhdtapp.parabox.ui.common.LayoutType
import com.ojhdtapp.parabox.ui.common.LocalFixedInsets
import com.ojhdtapp.parabox.ui.common.LocalSystemUiController
import com.ojhdtapp.parabox.ui.common.SystemUiController
import com.ojhdtapp.parabox.ui.common.enableTransparentNavigationBar
import com.ojhdtapp.parabox.ui.contact.ContactDetailPage
import com.ojhdtapp.parabox.ui.contact.ContactDetailPlaceholder
import com.ojhdtapp.parabox.ui.contact.ContactPage
import com.ojhdtapp.parabox.ui.contact.ContactPageViewModel
import com.ojhdtapp.parabox.ui.file.FilePageViewModel
import com.ojhdtapp.parabox.ui.message.ContactDetailDialog
import com.ojhdtapp.parabox.ui.message.MessageDialogsHost
import com.ojhdtapp.parabox.ui.message.MessagePage
import com.ojhdtapp.parabox.ui.message.MessagePageEvent
import com.ojhdtapp.parabox.ui.message.MessagePageViewModel
import com.ojhdtapp.parabox.ui.message.chat.ChatImagePreviewerEffectHost
import com.ojhdtapp.parabox.ui.message.chat.ChatInfoExtraPane
import com.ojhdtapp.parabox.ui.message.chat.ChatPage
import com.ojhdtapp.parabox.ui.message.chat.EmptyChatPage
import com.ojhdtapp.parabox.ui.message.chat.MyImagePreviewer
import com.ojhdtapp.parabox.ui.message.chat.rememberChatImagePreviewerState
import com.ojhdtapp.parabox.ui.navigation.AppNavTransitions
import com.ojhdtapp.parabox.ui.navigation.MenuRoute
import com.ojhdtapp.parabox.ui.navigation.MenuTab
import com.ojhdtapp.parabox.ui.navigation.RootRoute
import com.ojhdtapp.parabox.ui.navigation.SceneKeys
import com.ojhdtapp.parabox.ui.navigation.rememberAppPaneScaffoldDirective
import com.ojhdtapp.parabox.ui.navigation.suite.NavigationSuite
import com.ojhdtapp.parabox.ui.navigation.toLayoutType
import com.ojhdtapp.parabox.ui.navigation.toSetting
import com.ojhdtapp.parabox.ui.navigation.toTopLevelRoute
import com.ojhdtapp.parabox.ui.setting.SettingPageViewModel
import com.ojhdtapp.parabox.ui.setting.SettingPageWrapperUi
import com.ojhdtapp.parabox.ui.theme.AppTheme
import com.ojhdtapp.parabox.ui.theme.FontSize
import com.ojhdtapp.parabox.ui.theme.LocalFontSize
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxCloudService
import dagger.hilt.android.AndroidEntryPoint
import de.raphaelebner.roomdatabasebackup.core.RoomBackup
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var fileUtil: FileUtil

    @Inject
    lateinit var notificationUtil: NotificationUtil

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var cloudService: ParaboxCloudService

    @Inject
    lateinit var audioPlayer: AudioPlayer

    lateinit var roomBackup: RoomBackup

    private lateinit var extensionServiceConnection: ExtensionServiceConnection
    private lateinit var playAppUpdateUtil: PlayAppUpdateUtil

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomBackup = RoomBackup(this)
        extensionServiceConnection = ExtensionServiceConnection(baseContext)
        lifecycle.addObserver(extensionServiceConnection)
        lifecycle.addObserver(notificationUtil)

        if (DeviceUtil.isMIUI(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }

        val appUpdateActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
                if (result.resultCode != RESULT_OK) {
                    Log.d("parabox", "Update flow failed! Result code: ${result.resultCode}")
                }
            }
        val installStateUpdatedListener = InstallStateUpdatedListener { state ->
            when (state.installStatus) {
                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()
                    notificationUtil.sendPlayUpdateNotification(bytesDownloaded.toInt(), totalBytesToDownload.toInt())
                }
                InstallStatus.DOWNLOADED -> notificationUtil.sendPlayUpdateResultNotification(true)
                InstallStatus.FAILED -> notificationUtil.sendPlayUpdateResultNotification(false)
                InstallStatus.CANCELED -> notificationUtil.sendPlayUpdateResultNotification(false)
            }
        }
        playAppUpdateUtil = PlayAppUpdateUtil(
            this,
            appUpdateActivityResultLauncher,
            installStateUpdatedListener,
        )

        window.decorView.setViewTreeNavigationEventDispatcherOwner(this)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            DisposableEffect(darkTheme) {
                enableTransparentNavigationBar(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme },
                )
                onDispose {}
            }

            val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
            val fixedInsets = remember {
                FixedInsets(
                    statusBarHeight = systemBarsPadding.calculateTopPadding(),
                    navigationBarHeight = systemBarsPadding.calculateBottomPadding(),
                )
            }
            val mainSharedViewModel = hiltViewModel<MainSharedViewModel>()
            val messagePageViewModel = hiltViewModel<MessagePageViewModel>()
            val filePageViewModel = hiltViewModel<FilePageViewModel>()
            val contactPageViewModel = hiltViewModel<ContactPageViewModel>()
            val settingPageViewModel = hiltViewModel<SettingPageViewModel>()

            val mainSharedState by mainSharedViewModel.uiState.collectAsState()
            val messageState by messagePageViewModel.uiState.collectAsState()

            LaunchedEffect(mainSharedState.datastore.darkMode) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                    when (mainSharedState.datastore.darkMode) {
                        DataStoreKeys.DarkMode.YES -> manager.nightMode = UiModeManager.MODE_NIGHT_YES
                        DataStoreKeys.DarkMode.NO -> manager.nightMode = UiModeManager.MODE_NIGHT_NO
                        DataStoreKeys.DarkMode.FOLLOW_SYSTEM -> manager.nightMode = UiModeManager.MODE_NIGHT_AUTO
                    }
                } else {
                    when (mainSharedState.datastore.darkMode) {
                        DataStoreKeys.DarkMode.YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        DataStoreKeys.DarkMode.NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        DataStoreKeys.DarkMode.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    delegate.applyDayNight()
                }
            }

            AppTheme {
                CompositionLocalProvider(
                    values = arrayOf(
                        LocalNavigationEventDispatcherOwner provides this,
                        LocalFixedInsets provides fixedInsets,
                        LocalFileUtil provides fileUtil,
                        LocalAudioRecorder provides AudioRecorder,
                        LocalAudioPlayer provides audioPlayer,
                        LocalSystemUiController provides SystemUiController(this),
                        LocalMinimumInteractiveComponentSize provides 32.dp,
                        LocalFontSize provides FontSize(),
                        LocalRoomBackup provides roomBackup,
                        LocalCacheUtil provides CacheUtil(applicationContext),
                        LocalPlayAppUpdateUtil provides playAppUpdateUtil,
                        LocalCloudService provides cloudService,
                    ),
                ) {
                    val rootBackStack = rememberNavBackStack(RootRoute.Main)
                    val menuBackStack = rememberNavBackStack(MenuRoute.MessageList)
                    val lifecycleOwner = LocalLifecycleOwner.current

                    fun selectMenuTab(tab: MenuTab) {
                        val target = tab.toTopLevelRoute()
                        if (menuBackStack.size == 1 && menuBackStack.lastOrNull() == target) return
                        menuBackStack.clear()
                        menuBackStack.add(target)
                    }

                    fun openChatDetail() {
                        if (menuBackStack.firstOrNull() != MenuRoute.MessageList) {
                            menuBackStack.clear()
                            menuBackStack.add(MenuRoute.MessageList)
                        }
                        while (menuBackStack.size > 1 && menuBackStack.lastOrNull() != MenuRoute.ChatDetail) {
                            menuBackStack.removeLastOrNull()
                        }
                        if (menuBackStack.lastOrNull() != MenuRoute.ChatDetail) {
                            menuBackStack.add(MenuRoute.ChatDetail)
                        }
                    }

                    fun toggleChatInfo() {
                        if (menuBackStack.lastOrNull() == MenuRoute.ChatInfo) {
                            menuBackStack.removeLastOrNull()
                        } else {
                            openChatDetail()
                            menuBackStack.add(MenuRoute.ChatInfo)
                        }
                    }

                    fun closeChatInfo() {
                        if (menuBackStack.lastOrNull() == MenuRoute.ChatInfo) {
                            menuBackStack.removeLastOrNull()
                        }
                    }

                    fun openContactDetail() {
                        if (menuBackStack.firstOrNull() != MenuRoute.ContactList) {
                            menuBackStack.clear()
                            menuBackStack.add(MenuRoute.ContactList)
                        }
                        if (menuBackStack.lastOrNull() != MenuRoute.ContactDetail) {
                            menuBackStack.add(MenuRoute.ContactDetail)
                        }
                    }

                    fun popMenuBack() {
                        if (menuBackStack.size > 1) {
                            menuBackStack.removeLastOrNull()
                        }
                    }

                    fun openSettings(initialCategory: com.ojhdtapp.parabox.ui.setting.Setting? = null) {
                        if (rootBackStack.lastOrNull() !is RootRoute.Settings) {
                            rootBackStack.add(RootRoute.Settings(initialCategory))
                        }
                    }

                    fun exitSettings() {
                        if (rootBackStack.size > 1) {
                            rootBackStack.removeLastOrNull()
                        }
                    }

                    // Keep ViewModel infoAreaState in sync with Navigation 3 extraPane stack (including predictive back)
                    val isChatInfoActive = menuBackStack.lastOrNull() == MenuRoute.ChatInfo
                    LaunchedEffect(isChatInfoActive) {
                        if (messageState.chatDetail.infoAreaState.expanded != isChatInfoActive) {
                            messagePageViewModel.sendEvent(MessagePageEvent.OpenInfoArea(isChatInfoActive))
                        }
                    }

                    // Global shared navigation effects
                    LaunchedEffect(Unit) {
                        mainSharedViewModel.uiEffect
                            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                            .collectLatest { effect ->
                                when (effect) {
                                    is MainSharedEffect.MenuNavigate -> {
                                        while (rootBackStack.size > 1) {
                                            rootBackStack.removeLastOrNull()
                                        }
                                        val target = effect.target.toTopLevelRoute()
                                        menuBackStack.clear()
                                        menuBackStack.add(target)
                                    }
                                    is MainSharedEffect.RootNavigate -> {
                                        when (effect.target) {
                                            RootNavigateTarget.Menu -> {
                                                while (rootBackStack.size > 1) {
                                                    rootBackStack.removeLastOrNull()
                                                }
                                            }
                                            RootNavigateTarget.Setting -> openSettings()
                                        }
                                    }
                                    is MainSharedEffect.SettingNavigate -> {
                                        openSettings(effect.target.toSetting())
                                    }
                                    else -> Unit
                                }
                            }
                    }

                    // Global Predictive Back Dialogs & M3 ModalBottomSheet
                    ChatPickerDialog(
                        openDialog = mainSharedState.chatPicker.showDialog,
                        data = mainSharedState.chatPicker.result,
                        query = mainSharedState.chatPicker.query,
                        onQueryChange = { mainSharedViewModel.sendEvent(MainSharedEvent.PickChatQueryInput(it)) },
                        loadState = mainSharedState.chatPicker.loadState,
                        onConfirm = { mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(it)) },
                        onDismiss = { mainSharedViewModel.sendEvent(MainSharedEvent.PickChatDone(null)) },
                    )
                    ContactPickerDialog(
                        openDialog = mainSharedState.contactPicker.showDialog,
                        data = mainSharedState.contactPicker.result,
                        query = mainSharedState.contactPicker.query,
                        onQueryChange = { mainSharedViewModel.sendEvent(MainSharedEvent.PickContactQueryInput(it)) },
                        loadState = mainSharedState.contactPicker.loadState,
                        onConfirm = { mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(it)) },
                        onDismiss = { mainSharedViewModel.sendEvent(MainSharedEvent.PickContactDone(null)) },
                    )
                    DateRangePickerDialog(
                        openDialog = mainSharedState.openDateRangePicker,
                        onConfirm = { start, end -> mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(start to end)) },
                        onDismiss = { mainSharedViewModel.sendEvent(MainSharedEvent.PickDateRangeDone(null)) },
                    )
                    ContactDetailDialog(
                        contactDetailDialogState = mainSharedState.contactDetailDialogState,
                        onEvent = mainSharedViewModel::sendEvent,
                    )
                    ChangeLogBottomSheet(
                        changeLogBottomSheetState = mainSharedState.changeLogBottomSheetState,
                        onEvent = mainSharedViewModel::sendEvent,
                    )

                    // Unified Root Navigation 3 NavDisplay (Main <-> Settings with M3 Predictive Back)
                    NavDisplay(
                        modifier = Modifier.fillMaxSize(),
                        backStack = rootBackStack,
                        onBack = { exitSettings() },
                        transitionSpec = { AppNavTransitions.forwardTransition() },
                        popTransitionSpec = { AppNavTransitions.popTransition() },
                        predictivePopTransitionSpec = { swipeEdge -> AppNavTransitions.predictivePopTransition(swipeEdge) },
                        entryProvider = entryProvider {
                            entry<RootRoute.Main> {
                                val currentRoute = menuBackStack.lastOrNull() as? MenuRoute ?: MenuRoute.MessageList
                                val isCompactWidth = !currentWindowAdaptiveInfo().windowSizeClass
                                    .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
                                val paneDirective = rememberAppPaneScaffoldDirective()
                                val layoutType = paneDirective.toLayoutType()
                                val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
                                    backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
                                    directive = paneDirective,
                                )
                                val previewerState = rememberChatImagePreviewerState(messageState)
                                val messageLazyPagingItems = messagePageViewModel.messagePagingDataFlow.collectAsLazyPagingItems()

                                MessageDialogsHost(
                                    mainSharedViewModel = mainSharedViewModel,
                                    viewModel = messagePageViewModel,
                                )
                                ChatImagePreviewerEffectHost(
                                    viewModel = messagePageViewModel,
                                    state = messageState,
                                    layoutType = layoutType,
                                    previewerState = previewerState,
                                    messageLazyPagingItems = messageLazyPagingItems,
                                    onEvent = messagePageViewModel::sendEvent,
                                )

                                Box(modifier = Modifier.fillMaxSize()) {
                                    NavigationSuite(
                                        mainSharedViewModel = mainSharedViewModel,
                                        currentTab = currentRoute.tab,
                                        isAtTopLevel = currentRoute.isTopLevel,
                                        onSelectTab = ::selectMenuTab,
                                        onOpenSettings = { openSettings() },
                                    ) {
                                        NavDisplay(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .windowInsetsPadding(
                                                    WindowInsets.systemBars
                                                        .union(WindowInsets.displayCutout)
                                                        .only(WindowInsetsSides.Horizontal),
                                                )
                                                .padding(horizontal = if (layoutType == LayoutType.NORMAL) 0.dp else 16.dp),
                                            backStack = menuBackStack,
                                            sceneStrategy = listDetailStrategy,
                                            onBack = { popMenuBack() },
                                            transitionSpec = {
                                                val initialTab = initialState.entries.lastOrNull()
                                                    ?.metadata?.get(AppNavTransitions.MENU_TAB_METADATA_KEY) as? MenuTab
                                                val targetTab = targetState.entries.lastOrNull()
                                                    ?.metadata?.get(AppNavTransitions.MENU_TAB_METADATA_KEY) as? MenuTab
                                                if (initialTab != null && targetTab != null && initialTab != targetTab) {
                                                    AppNavTransitions.topLevelTabTransition(initialTab, targetTab, isCompactWidth)
                                                } else {
                                                    AppNavTransitions.forwardTransition()
                                                }
                                            },
                                            popTransitionSpec = {
                                                val initialTab = initialState.entries.lastOrNull()
                                                    ?.metadata?.get(AppNavTransitions.MENU_TAB_METADATA_KEY) as? MenuTab
                                                val targetTab = targetState.entries.lastOrNull()
                                                    ?.metadata?.get(AppNavTransitions.MENU_TAB_METADATA_KEY) as? MenuTab
                                                if (initialTab != null && targetTab != null && initialTab != targetTab) {
                                                    AppNavTransitions.topLevelTabTransition(initialTab, targetTab, isCompactWidth)
                                                } else {
                                                    AppNavTransitions.popTransition()
                                                }
                                            },
                                            predictivePopTransitionSpec = { swipeEdge -> AppNavTransitions.predictivePopTransition(swipeEdge) },
                                            entryProvider = entryProvider {
                                                entry<MenuRoute.MessageList>(
                                                    metadata = ListDetailSceneStrategy.listPane(
                                                        sceneKey = SceneKeys.MESSAGE,
                                                        detailPlaceholder = {
                                                            EmptyChatPage(layoutType = layoutType)
                                                        },
                                                    ) + AppNavTransitions.menuTabMetadata(MenuTab.Message),
                                                ) {
                                                    MessagePage(
                                                        modifier = Modifier.paneFocusAnchor(),
                                                        viewModel = messagePageViewModel,
                                                        mainSharedViewModel = mainSharedViewModel,
                                                        layoutType = layoutType,
                                                        onOpenChat = ::openChatDetail,
                                                    )
                                                }

                                                entry<MenuRoute.ChatDetail>(
                                                    metadata = ListDetailSceneStrategy.detailPane(
                                                        sceneKey = SceneKeys.MESSAGE,
                                                    ) + AppNavTransitions.menuTabMetadata(MenuTab.Message),
                                                ) {
                                                    ChatPage(
                                                        modifier = Modifier.paneFocusAnchor(),
                                                        viewModel = messagePageViewModel,
                                                        state = messageState,
                                                        mainSharedState = mainSharedState,
                                                        layoutType = layoutType,
                                                        previewerState = previewerState,
                                                        messageLazyPagingItems = messageLazyPagingItems,
                                                        onNavigateBack = ::popMenuBack,
                                                        onToggleInfoArea = ::toggleChatInfo,
                                                        onEvent = messagePageViewModel::sendEvent,
                                                        onMainSharedEvent = mainSharedViewModel::sendEvent,
                                                    )
                                                }

                                                entry<MenuRoute.ChatInfo>(
                                                    metadata = ListDetailSceneStrategy.extraPane(
                                                        sceneKey = SceneKeys.MESSAGE,
                                                    ) + AppNavTransitions.menuTabMetadata(MenuTab.Message),
                                                ) {
                                                    ChatInfoExtraPane(
                                                        modifier = Modifier.paneFocusAnchor(),
                                                        viewModel = messagePageViewModel,
                                                        state = messageState,
                                                        layoutType = layoutType,
                                                        previewerState = previewerState,
                                                        messageLazyPagingItems = messageLazyPagingItems,
                                                        onClose = ::closeChatInfo,
                                                        onEvent = messagePageViewModel::sendEvent,
                                                        onMainSharedEvent = mainSharedViewModel::sendEvent,
                                                    )
                                                }

                                                entry<MenuRoute.FileList>(
                                                    metadata = AppNavTransitions.menuTabMetadata(MenuTab.File),
                                                ) {
                                                    FilePage(modifier = Modifier.fillMaxSize())
                                                }

                                                entry<MenuRoute.ContactList>(
                                                    metadata = ListDetailSceneStrategy.listPane(
                                                        sceneKey = SceneKeys.CONTACT,
                                                        detailPlaceholder = {
                                                            ContactDetailPlaceholder()
                                                        },
                                                    ) + AppNavTransitions.menuTabMetadata(MenuTab.Contact),
                                                ) {
                                                    ContactPage(
                                                        modifier = Modifier.paneFocusAnchor(),
                                                        viewModel = contactPageViewModel,
                                                        mainSharedState = mainSharedState,
                                                        layoutType = layoutType,
                                                        onOpenContactDetail = ::openContactDetail,
                                                        onMainSharedEvent = mainSharedViewModel::sendEvent,
                                                    )
                                                }

                                                entry<MenuRoute.ContactDetail>(
                                                    metadata = ListDetailSceneStrategy.detailPane(
                                                        sceneKey = SceneKeys.CONTACT,
                                                    ) + AppNavTransitions.menuTabMetadata(MenuTab.Contact),
                                                ) {
                                                    ContactDetailPage(
                                                        modifier = Modifier.paneFocusAnchor(),
                                                        viewModel = contactPageViewModel,
                                                        mainSharedState = mainSharedState,
                                                        layoutType = layoutType,
                                                        onNavigateBack = ::popMenuBack,
                                                        onMainSharedEvent = mainSharedViewModel::sendEvent,
                                                    )
                                                }
                                            },
                                        )
                                    }

                                    // Fullscreen media previewer with Google-spec predictive back overlay
                                    MyImagePreviewer(
                                        previewerState = previewerState,
                                        state = messageState.chatDetail,
                                        layoutType = layoutType,
                                        onEvent = messagePageViewModel::sendEvent,
                                    )
                                }
                            }

                            entry<RootRoute.Settings> { key ->
                                SettingPageWrapperUi(
                                    modifier = Modifier.fillMaxSize(),
                                    initialCategory = key.initialCategory,
                                    mainSharedViewModel = mainSharedViewModel,
                                    viewModel = settingPageViewModel,
                                    onExitSettings = ::exitSettings,
                                )
                            }
                        },
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

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(extensionServiceConnection)
        lifecycle.removeObserver(notificationUtil)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.data?.host == "update") {
            playAppUpdateUtil.completeUpdate()
        }
    }

}
