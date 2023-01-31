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
import com.ojhdtapp.parabox.ui.util.WorkingMode
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

    val cloudTotalSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_TOTAL_SPACE] ?: 0L
        }

    val cloudUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_USED_SPACE] ?: 0L
        }
    val cloudAppUsedSpaceFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.CLOUD_APP_USED_SPACE] ?: 0L
        }

    fun saveGoogleDriveAccount(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.GOOGLE_NAME] = account?.displayName ?: ""
                preferences[DataStoreKeys.GOOGLE_MAIL] = account?.email ?: ""
                preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = if(account == null) 0 else GoogleDriveUtil.SERVICE_CODE
                preferences[DataStoreKeys.GOOGLE_AVATAR] = account?.photoUrl.toString()
            }
            GoogleDriveUtil.getDriveInformation(context)?.also {
                context.dataStore.edit { preferences ->
                    preferences[DataStoreKeys.GOOGLE_WORK_FOLDER_ID] = it.workFolderId
                    preferences[DataStoreKeys.CLOUD_TOTAL_SPACE] = it.totalSpace
                    preferences[DataStoreKeys.CLOUD_USED_SPACE] = it.usedSpace
                    preferences[DataStoreKeys.CLOUD_APP_USED_SPACE] = it.appUsedSpace
                }
            }
        }
    }

    // Working Mode
    val workingModeFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_WORKING_MODE] ?: WorkingMode.NORMAL.ordinal
        }

    fun setWorkingMode(mode: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_WORKING_MODE] = mode
            }
        }
    }

    // Cloud Service

    val cloudServiceFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_CLOUD_SERVICE] ?: 0
        }

    fun setCloudService(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_CLOUD_SERVICE] = value
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
    val enableFCMStateFlow: StateFlow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ENABLE_FCM] ?: false
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

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
                fcmApiHelper.getVersion().also {
                    if (it?.isSuccessful == true) {
                        _fcmStateFlow.value = FcmConstants.Status.Success(it.body()!!.version)
                    } else {
                        _fcmStateFlow.value = FcmConstants.Status.Failure
                    }
                    isCheckingFCM = false
                }
            }
        }
    }

    val enableFcmCustomUrlFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ENABLE_FCM_CUSTOM_URL] ?: false
        }

    fun setEnableFcmCustomUrl(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ENABLE_FCM_CUSTOM_URL] = value
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

//    val fcmRoleFlow = context.dataStore.data
//        .catch { exception ->
//            if (exception is IOException) {
//                emit(emptyPreferences())
//            } else {
//                throw exception
//            }
//        }
//        .map { settings ->
//            settings[DataStoreKeys.SETTINGS_FCM_ROLE] ?: FcmConstants.Role.SENDER.ordinal
//        }
//
//    fun setFCMRole(value: Int) {
//        viewModelScope.launch {
//            context.dataStore.edit { preferences ->
//                preferences[DataStoreKeys.SETTINGS_FCM_ROLE] = value
//            }
//        }
//    }

    val fcmTargetTokensFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FCM_TARGET_TOKENS] ?: emptySet()
        }

    fun setFcmTargetTokens(value: Set<String>) {
        Log.d("parabox", "setFcmTargetTokens: $value")
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.FCM_TARGET_TOKENS] = value
            }
        }
    }

    val fcmLoopbackTokenFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.FCM_LOOPBACK_TOKEN] ?: ""
        }

    fun setFcmLoopbackToken(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.FCM_LOOPBACK_TOKEN] = value
            }
        }
    }

    val fcmCloudStorageFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE] ?: FcmConstants.CloudStorage.NONE.ordinal
        }

    fun setFCMCloudStorage(value: Int) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_CLOUD_STORAGE] = value
            }
        }
    }

    fun onContactDisableFCMChange(target: Contact, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            updateContact.disableFCM(target.contactId, value)
        }
    }

    val fcmEnableCacheFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_FCM_ENABLE_CACHE] ?: false
        }

    fun setFcmEnableCache(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_FCM_ENABLE_CACHE] = value
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

    private val _cleaningFile = mutableStateOf<Boolean>(false)
    val cleaningFile: State<Boolean> = _cleaningFile
    fun setCleaningFile(value: Boolean) {
        _cleaningFile.value = value
    }

    fun clearFile(timestamp: Long) {
        if (!cleaningFile.value) {
            setCleaningFile(true)
            viewModelScope.launch {
                CacheUtil.deleteChatFilesBeforeTimestamp(context, timestamp)
                delay(3000)
                setCleaningFile(false)
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
    val entityExtractionFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ML_KIT_ENTITY_EXTRACTION] ?: true
        }
    fun setEntityExtraction(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ML_KIT_ENTITY_EXTRACTION] = value
            }
        }
    }

    val smartReplyFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ML_KIT_SMART_REPLY] ?: true
        }

    fun setSmartReply(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ML_KIT_SMART_REPLY] = value
            }
        }
    }

    val translationFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.SETTINGS_ML_KIT_TRANSLATION] ?: true
        }

    fun setTranslation(value: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_ML_KIT_TRANSLATION] = value
            }
        }
    }

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

    // Tencent COS
    val tencentCOSSecretIdFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.TENCENT_COS_SECRET_ID] ?: ""
        }

    fun setTencentCOSSecretId(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.TENCENT_COS_SECRET_ID] = value
            }
        }
    }

    val tencentCOSSecretKeyFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.TENCENT_COS_SECRET_KEY] ?: ""
        }

    fun setTencentCOSSecretKey(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.TENCENT_COS_SECRET_KEY] = value
            }
        }
    }

    val tencentCOSBucketFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.TENCENT_COS_BUCKET] ?: ""
        }

    fun setTencentCOSBucket(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.TENCENT_COS_BUCKET] = value
            }
        }
    }

    val tencentCOSRegionFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.TENCENT_COS_REGION] ?: ""
        }

    fun setTencentCOSRegion(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.TENCENT_COS_REGION] = value
            }
        }
    }

    val qiniuKODOAccessKeyFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.QINIU_KODO_ACCESS_KEY] ?: ""
        }

    fun setQiniuKODOAccessKey(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.QINIU_KODO_ACCESS_KEY] = value
            }
        }
    }

    val qiniuKODOSecretKeyFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.QINIU_KODO_SECRET_KEY] ?: ""
        }

    fun setQiniuKODOSecretKey(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.QINIU_KODO_SECRET_KEY] = value
            }
        }
    }

    val qiniuKODOBucketFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.QINIU_KODO_BUCKET] ?: ""
        }

    fun setQiniuKODOBucket(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.QINIU_KODO_BUCKET] = value
            }
        }
    }

    val qiniuKODODomainFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { settings ->
            settings[DataStoreKeys.QINIU_KODO_DOMAIN] ?: ""
        }

    fun setQiniuKODODomain(value: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.QINIU_KODO_DOMAIN] = value
            }
        }
    }
    // Licenses
    val licenseList = listOf<License>(
        License(
            "Accompanist",
            "https://github.com/google/accompanist/blob/main/LICENSE",
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
            "Android Room-Database Backup",
            "https://github.com/rafi0101/Android-Room-Database-Backup/blob/master/LICENSE",
            "MIT License"
        ),
        License(
            "ImageViewer",
            "https://github.com/jvziyaoyao/ImageViewer/blob/main/LICENSE",
            "MIT License"
        ),
        License(
            "Compose Extended Gestures",
            "https://github.com/SmartToolFactory/Compose-Extended-Gestures/blob/master/LICENSE.md",
            "Apache License 2.0"
        ),
        License(
            "Amplituda",
            "https://github.com/lincollincol/Amplituda/blob/master/LICENSE",
            "Apache License 2.0"
        ),
        License(
            "Retrofit",
            "https://github.com/square/retrofit/blob/master/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "Compose Destinations",
            "https://github.com/raamcosta/compose-destinations/blob/main/LICENSE.txt",
            "Apache License 2.0"
        ),
        License(
            "Gson",
            "https://github.com/google/gson/blob/master/LICENSE",
            "Apache License 2.0"
        ),
    )
}