package com.kontakti.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kontakti.data.sync.SyncDirection
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kontakti_prefs")

class TokenStore(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val ONBOARDED_KEY = booleanPreferencesKey("kontakti_onboarded")
    private val SYNC_DIRECTION_KEY = stringPreferencesKey("sync_direction")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val onboardedFlow: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDED_KEY] ?: false }
    val syncDirectionFlow: Flow<SyncDirection> = context.dataStore.data.map {
        val raw = it[SYNC_DIRECTION_KEY]
        if (raw != null) runCatching { SyncDirection.valueOf(raw) }.getOrDefault(SyncDirection.TWO_WAY)
        else SyncDirection.TWO_WAY
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    suspend fun markOnboarded() {
        context.dataStore.edit { it[ONBOARDED_KEY] = true }
    }

    suspend fun clearOnboarded() {
        context.dataStore.edit { it.remove(ONBOARDED_KEY) }
    }

    suspend fun saveSyncDirection(direction: SyncDirection) {
        context.dataStore.edit { it[SYNC_DIRECTION_KEY] = direction.name }
    }
}
