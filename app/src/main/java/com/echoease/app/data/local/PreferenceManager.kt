package com.echoease.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferenceManager(private val context: Context) {
    private val LAST_FLAG_TIMESTAMP = longPreferencesKey("last_flag_timestamp")

    val lastFlagTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_FLAG_TIMESTAMP] ?: 0L
    }

    suspend fun updateLastFlagTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FLAG_TIMESTAMP] = timestamp
        }
    }
}
