package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "prism_user_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_VIEW_MODE = stringPreferencesKey("view_mode") // "GRID", "LIST"
        val KEY_SORT_TYPE = stringPreferencesKey("sort_type") // "NAME", "DATE", "SIZE", "TYPE"
        val KEY_SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val KEY_AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout") // "IMMEDIATE", "1_MIN", "5_MIN"
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val viewModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_VIEW_MODE] ?: "GRID"
    }

    val sortTypeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SORT_TYPE] ?: "DATE"
    }

    val sortAscendingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SORT_ASCENDING] ?: false
    }

    val autoLockTimeoutFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK_TIMEOUT] ?: "IMMEDIATE"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setViewMode(mode: String) {
        context.dataStore.edit { it[KEY_VIEW_MODE] = mode }
    }

    suspend fun setSort(type: String, ascending: Boolean) {
        context.dataStore.edit {
            it[KEY_SORT_TYPE] = type
            it[KEY_SORT_ASCENDING] = ascending
        }
    }

    suspend fun setAutoLockTimeout(timeout: String) {
        context.dataStore.edit { it[KEY_AUTO_LOCK_TIMEOUT] = timeout }
    }
}
