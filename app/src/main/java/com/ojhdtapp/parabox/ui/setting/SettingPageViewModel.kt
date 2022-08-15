package com.ojhdtapp.parabox.ui.setting

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {
    private val _editUserNameDialogState = mutableStateOf<Boolean>(false)
    val editUserNameDialogState : State<Boolean> = _editUserNameDialogState
    fun setEditUserNameDialogState(value : Boolean){
        _editUserNameDialogState.value = value
    }

//    val userNameFlow: Flow<String> = context.dataStore.data
//        .catch { exception ->
//            if (exception is IOException) {
//                emit(emptyPreferences())
//            } else {
//                throw exception
//            }
//        }
//        .map { settings ->
//            settings[DataStoreKeys.USER_NAME] ?: "User"
//        }
//
//    fun setUserName(value: String) {
//        viewModelScope.launch {
//            context.dataStore.edit { settings ->
//                settings[DataStoreKeys.USER_NAME] = value
//            }
//        }
//    }
}