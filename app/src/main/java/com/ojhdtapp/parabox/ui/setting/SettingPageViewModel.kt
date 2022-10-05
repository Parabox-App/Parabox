package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.GoogleDriveUtil
import com.ojhdtapp.parabox.core.util.dataStore
import com.ojhdtapp.parabox.domain.use_case.GetContacts
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getContacts: GetContacts,
) : ViewModel() {

    val contactFlow : StateFlow<List<Context>> = getContacts.all()
        .onEach { result ->
            result.onSuccess { contacts ->
                if (contacts.isEmpty()) {
                    _isContactEmpty.value = true
                } else {
                    _isContactEmpty.value = false
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        .launchIn(viewModelScope)
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
    fun setDefaultBackupService(value: Int){
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
    fun setAutoBackup(value: Boolean){
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_AUTO_BACKUP] = value
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
    fun setAutoDeleteLocalFile(value: Boolean){
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.SETTINGS_AUTO_DELETE_LOCAL_FILE] = value
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
}