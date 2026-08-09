package com.glowup.ai.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.glowup.ai.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class AuthStore(private val context: Context) {

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTH_TOKEN]
    }

    val userName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_NAME]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_EMAIL]
    }

    val userAge: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_AGE]
    }

    val isLoggedIn: Flow<Boolean> = authToken.map { !it.isNullOrBlank() }

    suspend fun saveAuth(token: String, name: String, age: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTH_TOKEN] = token
            prefs[PreferencesKeys.USER_NAME] = name
            prefs[PreferencesKeys.USER_AGE] = age
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.map { prefs ->
            prefs[PreferencesKeys.AUTH_TOKEN]
        }.firstOrNull()
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.AUTH_TOKEN)
            prefs.remove(PreferencesKeys.USER_NAME)
            prefs.remove(PreferencesKeys.USER_EMAIL)
            prefs.remove(PreferencesKeys.USER_AGE)
        }
    }

    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey(Constants.KEY_AUTH_TOKEN)
        val USER_NAME = stringPreferencesKey(Constants.KEY_USER_NAME)
        val USER_EMAIL = stringPreferencesKey(Constants.KEY_USER_EMAIL)
        val USER_AGE = intPreferencesKey(Constants.KEY_USER_AGE)
    }
}
