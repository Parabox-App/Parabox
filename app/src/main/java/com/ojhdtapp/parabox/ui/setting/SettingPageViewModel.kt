package com.ojhdtapp.parabox.ui.setting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ojhdtapp.parabox.core.util.CacheUtil
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.FileUtil
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.fcm.FcmApiHelper
import com.ojhdtapp.parabox.domain.fcm.FcmConstants
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import com.ojhdtapp.parabox.domain.use_case.UpdateContact
import com.ojhdtapp.parabox.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getContacts: GetContacts,
    val updateContact: UpdateContact,
    val fcmApiHelper: FcmApiHelper
) : ViewModel() {

    private val _contactLoadingState = mutableStateOf<Boolean>(true)
    val contactLoadingState: State<Boolean> = _contactLoadingState
    val contactStateFlow: StateFlow<List<Contact>> = getContacts.all()
        .map { resource ->
            when (resource) {
                is Resource.Error -> {
                    _contactLoadingState.value = false
                    emptyList()
                }

                is Resource.Loading -> {
                    _contactLoadingState.value = true
                    emptyList()
                }

                is Resource.Success -> {
                    _contactLoadingState.value = false
                    resource.data!!
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val googleLoginFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.GOOGLE_LOGIN] ?: false
        }

    val googleTotalSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.GOOGLE_TOTAL_SPACE] ?: 0L
        }

    val googleUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.GOOGLE_USED_SPACE] ?: 0L
        }
    val googleAppUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.GOOGLE_APP_USED_SPACE] ?: 0L
        }

    fun saveGoogleDriveAccount(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.GOOGLE_NAME] = account?.displayName ?: ""
                preferences[DataStoreKeys.GOOGLE_MAIL] = account?.email ?: ""
                preferences[DataStoreKeys.GOOGLE_LOGIN] = account != null
                preferences[DataStoreKeys.GOOGLE_AVATAR] = account?.photoUrl.toString()
                preferences[DataStoreKeys.SETTINGS_DEFAULT_BACKUP_SERVICE] = 0
            }
            GoogleDriveUtil.getDriveInformation(context)?.also {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.GOOGLE_WORK_FOLDER_ID] = it.workFolderId
                    preferences[DataStoreKeys.GOOGLE_TOTAL_SPACE] = it.totalSpace
                    preferences[DataStoreKeys.GOOGLE_USED_SPACE] = it.usedSpace
                    preferences[DataStoreKeys.GOOGLE_APP_USED_SPACE] = it.appUsedSpace
                }
            }
        }
    }

    // Cloud Service

    val defaultBackupServiceFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_DEFAULT_BACKUP_SERVICE] ?: 0
        }

    fun setDefaultBackupService(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_DEFAULT_BACKUP_SERVICE] = value
            }
        }
    }

    val autoBackupFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_AUTO_BACKUP] ?: false
        }

    fun setAutoBackup(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_AUTO_BACKUP] = value
            }
        }
    }

    fun onContactBackupChange(target: Contact, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.backup(target.contactId, value)
        }
    }

    val autoBackupFileMaxSizeFlow: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_AUTO_BACKUP_FILE_MAX_SIZE] ?: 10f
        }

    fun setAutoBackupFileMaxSize(value: Float) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_AUTO_BACKUP_FILE_MAX_SIZE] = value
            }
        }
    }

    val autoDeleteLocalFileFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_AUTO_DELETE_LOCAL_FILE] ?: false
        }

    fun setAutoDeleteLocalFile(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_AUTO_DELETE_LOCAL_FILE] = value
            }
        }
    }

    // Firebase
    val enableFCMFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
        }

    fun setEnableFCM(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ENABLE_FCM] = value
            }
        }
    }

    val fcmTokenFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FCM_TOKEN] ?: ""
        }

    private val _fcmStateFlow = MutableStateFlow<FcmConstants.Status>(FcmConstants.Status.Loading)
    val fcmStateFlow get() = _fcmStateFlow.asStateFlow()
    var isCheckingFCM = false
    fun checkFcmState() {
        if (!isCheckingFCM) {
            isCheckingFCM = true
            _fcmStateFlow.value = FcmConstants.Status.Loading

            viewModelScope.launch {
                val url = fcmUrlFlow.first()
                if (url.isNotBlank()) {
                    val httpUrl = "http://${url}/"
                    Log.d("parabox", "checkFcmState: $httpUrl")
                    fcmApiHelper.getVersion(httpUrl).also {
                        if (it.isSuccessful) {
                            _fcmStateFlow.value = FcmConstants.Status.Success(it.body()!!.version)
                        } else {
                            _fcmStateFlow.value = FcmConstants.Status.Failure
                        }
                        isCheckingFCM = false
                    }
                } else {
                    _fcmStateFlow.value = FcmConstants.Status.Failure
                    isCheckingFCM = false
                }
            }
        }
    }

    val fcmUrlFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_FCM_URL] ?: ""
        }

    fun setFCMUrl(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_URL] = value
            }
        }
    }

    val fcmHttpsFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_FCM_HTTPS] ?: false
        }

    fun setFCMHttps(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_HTTPS] = value
            }
        }
    }

    // Backup & Restore

    private val _cacheSizeStateFlow =
        MutableStateFlow(FileUtil.getSizeString(CacheUtil.getCacheSize(context)))
    val cacheSizeStateFlow: StateFlow<String> = _cacheSizeStateFlow.asStateFlow()
    private val _cleaningCache = mutableStateOf<Boolean>(false)
    val cleaningCache: State<Boolean> = _cleaningCache
    fun setCleaningCache(value: Boolean) {
        _cleaningCache.value = value
    }

    fun clearCache() {
        if (!cleaningCache.value) {
            setCleaningCache(true)
            viewModelScope.launch {
                CacheUtil.clearCache(context)
                delay(1000)
                _cacheSizeStateFlow.value = FileUtil.getSizeString(CacheUtil.getCacheSize(context))
                setCleaningCache(false)
            }
        }
    }

    // Notification
    private val _notificationPermissionGrantedStateFlow = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    )
    val notificationPermissionGrantedStateFlow: StateFlow<Boolean> =
        _notificationPermissionGrantedStateFlow.asStateFlow()

    fun onNotificationPermissionResult(value: Boolean) {
        _notificationPermissionGrantedStateFlow.tryEmit(value)
    }

    fun onContactNotificationChange(target: Contact, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.notificationState(target.contactId, value)
        }
    }

    // Interface
    val enableDynamicColorFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ENABLE_DYNAMIC_COLOR]
                ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        }

    fun setEnableDynamicColor(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ENABLE_DYNAMIC_COLOR] = value
            }
        }
    }

    val themeFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_THEME] ?: Theme.WILLOW
        }

    fun setTheme(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_THEME] = value
            }
        }
    }

    // Experimental
    val allowBubbleHomeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ALLOW_BUBBLE_HOME] ?: false
        }

    fun setAllowBubbleHome(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ALLOW_BUBBLE_HOME] = value
            }
        }
    }

    val allowForegroundNotificationFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION] ?: false
        }

    fun setAllowForegroundNotification(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ALLOW_FOREGROUND_NOTIFICATION] = value
            }
        }
    }

    private val _editUserNameDialogState = mutableStateOf<Boolean>(false)
    val editUserNameDialogState: State<Boolean> = _editUserNameDialogState
    fun setEditUserNameDialogState(value: Boolean) {
        _editUserNameDialogState.value = value
    }

    // Selected
    private val _selectedSetting = mutableStateOf<Int>(SettingPageState.INFO)
    val selectedSetting: State<Int> = _selectedSetting
    fun setSelectedSetting(value: Int) {
        _selectedSetting.value = value
    }

    // Privacy Dialog
    private val _showPrivacyDialog = mutableStateOf<Boolean>(false)
    val showPrivacyDialog: State<Boolean> = _showPrivacyDialog
    fun setShowPrivacyDialog(value: Boolean) {
        _showPrivacyDialog.value = value
    }

    // Terms Dialog
    private val _showTermsDialog = mutableStateOf<Boolean>(false)
    val showTermsDialog: State<Boolean> = _showTermsDialog
    fun setShowTermsDialog(value: Boolean) {
        _showTermsDialog.value = value
    }

    // Licenses
    val licenseList = listOf<License>(
        License(
            "Accompanist",
            "https://github.com/google/accompanist",
            "Apache License 2.0",
        ),
        License(
            "AndroidX",
            "https://developer.android.com/jetpack/androidx",
            "Apache License 2.0"
        ),
        License(
            "AndroidX DataStore",
            "https://developer.android.com/jetpack/androidx/releases/datastore",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Lifecycle",
            "https://developer.android.com/jetpack/androidx/releases/lifecycle",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Compose",
            "https://developer.android.com/jetpack/androidx/releases/compose",
            "Apache License 2.0"
        ),
        License(
            "AndroidX Compose Material",
            "https://developer.android.com/jetpack/androidx/releases/compose-material",
            "Apache License 2.0"
        ),
        License(
            "Coil",
            "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "Kotlin",
            "https://github.com/JetBrains/kotlin",
            "Apache License 2.0"
        ),
        License(
            "Android-Room-Database-Backup",
            "https://github.com/rafi0101/Android-Room-Database-Backup/blob/master/LICENSE",
            "MIT License"
        ),
        License(
            "ImageViewer",
            "https://github.com/jvziyaoyao/ImageViewer/blob/main/LICENSE",
            "MIT License"
        ),
        License(
            "Compose-Extended-Gestures",
            "https://github.com/SmartToolFactory/Compose-Extended-Gestures/blob/master/LICENSE.md",
            "Apache License 2.0"
        ),
        License(
            "Amplituda",
            "https://github.com/lincollincol/Amplituda/blob/master/LICENSE",
            "Apache License 2.0"
        )
    )
}