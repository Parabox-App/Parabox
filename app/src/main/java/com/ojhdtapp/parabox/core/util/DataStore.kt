package com.ojhdtapp.parabox.core.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DataStoreKeys{
    val SEND_MESSAGE_ID = longPreferencesKey("send_message_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_AVATAR = stringPreferencesKey("user_avatar")

    val SETTINGS_DEFAULT_BACKUP_SERVICE = intPreferencesKey("settings_default_backup_service")
    val SETTINGS_AUTO_BACKUP = booleanPreferencesKey("settings_auto_backup")
    val SETTINGS_AUTO_DELETE_LOCAL_FILE = booleanPreferencesKey("settings_auto_delete_local_file")
    val SETTINGS_ENABLE_DYNAMIC_COLOR = booleanPreferencesKey("settings_enable_dynamic_color")
    val SETTINGS_THEME = intPreferencesKey("settings_theme")

    val GOOGLE_MAIL = stringPreferencesKey("google_mail")
    val GOOGLE_NAME = stringPreferencesKey("google_name")
    val GOOGLE_LOGIN = booleanPreferencesKey("google_login")
    val GOOGLE_AVATAR = stringPreferencesKey("google_avatar")
    val GOOGLE_WORK_FOLDER_ID = stringPreferencesKey("google_work_folder_id")
    val GOOGLE_TOTAL_SPACE = longPreferencesKey("google_total_space")
    val GOOGLE_USED_SPACE = longPreferencesKey("google_used_space")
    val GOOGLE_APP_USED_SPACE = longPreferencesKey("google_app_used_space")

    const val DEFAULT_USER_NAME = "Me"
}