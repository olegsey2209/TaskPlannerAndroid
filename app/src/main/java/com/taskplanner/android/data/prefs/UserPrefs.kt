package com.taskplanner.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "taskplanner_prefs")

class UserPrefs(private val context: Context) {
    private val keyCurrentUserId = stringPreferencesKey("currentUserId")

    fun observeCurrentUserId(): Flow<String?> {
        return context.dataStore.data.map { prefs: Preferences ->
            prefs[keyCurrentUserId]
        }
    }

    suspend fun setCurrentUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[keyCurrentUserId] = userId
        }
    }
}

