package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "video_library_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_BG_SYNC_ENABLED = booleanPreferencesKey("bg_sync_enabled")
        val KEY_SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_LAST_IMPORT_TIME = longPreferencesKey("last_import_time")
        val KEY_LAST_CONFIG_NAME = stringPreferencesKey("last_config_name")
    }

    val isBackgroundSyncEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BG_SYNC_ENABLED] ?: true
    }

    val syncIntervalHours: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SYNC_INTERVAL_HOURS] ?: 12
    }

    val isWifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WIFI_ONLY] ?: true
    }

    val lastImportTime: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_IMPORT_TIME]
    }

    val lastConfigName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_CONFIG_NAME]
    }

    suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BG_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setSyncIntervalHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL_HOURS] = hours
        }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun recordImport(configName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_CONFIG_NAME] = configName
            prefs[KEY_LAST_IMPORT_TIME] = System.currentTimeMillis()
        }
    }
}
