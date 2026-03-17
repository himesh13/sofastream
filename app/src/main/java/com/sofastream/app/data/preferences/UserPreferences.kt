package com.sofastream.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sofastream_prefs")

class UserPreferences(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val JELLYFIN_URL = stringPreferencesKey("jellyfin_url")
        val JELLYFIN_TOKEN = stringPreferencesKey("jellyfin_token")
        val JELLYFIN_USER_ID = stringPreferencesKey("jellyfin_user_id")
        val JELLYFIN_USER_NAME = stringPreferencesKey("jellyfin_user_name")
        val JELLYSEERR_URL = stringPreferencesKey("jellyseerr_url")
        val JELLYSEERR_TOKEN = stringPreferencesKey("jellyseerr_token")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    }

    val jellyfinUrl: Flow<String> = dataStore.data.map { it[JELLYFIN_URL] ?: "" }
    val jellyfinToken: Flow<String> = dataStore.data.map { it[JELLYFIN_TOKEN] ?: "" }
    val jellyfinUserId: Flow<String> = dataStore.data.map { it[JELLYFIN_USER_ID] ?: "" }
    val jellyfinUserName: Flow<String> = dataStore.data.map { it[JELLYFIN_USER_NAME] ?: "" }
    val jellyseerrUrl: Flow<String> = dataStore.data.map { it[JELLYSEERR_URL] ?: "" }
    val jellyseerrToken: Flow<String> = dataStore.data.map { it[JELLYSEERR_TOKEN] ?: "" }
    val isSetupComplete: Flow<Boolean> = dataStore.data.map { it[IS_SETUP_COMPLETE] ?: false }

    suspend fun saveJellyfinCredentials(url: String, token: String, userId: String, userName: String) {
        dataStore.edit { prefs ->
            prefs[JELLYFIN_URL] = url
            prefs[JELLYFIN_TOKEN] = token
            prefs[JELLYFIN_USER_ID] = userId
            prefs[JELLYFIN_USER_NAME] = userName
        }
    }

    suspend fun saveJellyseerrCredentials(url: String, token: String) {
        dataStore.edit { prefs ->
            prefs[JELLYSEERR_URL] = url
            prefs[JELLYSEERR_TOKEN] = token
        }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[IS_SETUP_COMPLETE] = complete
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    fun getJellyfinUrlSync(): String = runBlocking { jellyfinUrl.first() }
    fun getJellyfinTokenSync(): String = runBlocking { jellyfinToken.first() }
    fun getJellyfinUserIdSync(): String = runBlocking { jellyfinUserId.first() }
    fun isSetupCompleteSync(): Boolean = runBlocking { isSetupComplete.first() }
    fun getJellyseerrUrlSync(): String = runBlocking { jellyseerrUrl.first() }
    fun getJellyseerrTokenSync(): String = runBlocking { jellyseerrToken.first() }
}
