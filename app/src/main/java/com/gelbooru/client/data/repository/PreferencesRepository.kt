package com.gelbooru.client.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.gelbooru.client.data.model.UserPreferences
import com.gelbooru.client.data.model.ThemeMode

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val SHOW_NSFW = booleanPreferencesKey("show_nsfw")
        val SHOW_HIGH_RES = booleanPreferencesKey("show_high_res")
        val DEFAULT_PAGE_COUNT = intPreferencesKey("default_page_count")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val SAVE_LOCATION = stringPreferencesKey("save_location")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SEARCH_QUERY = stringPreferencesKey("last_search_query")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            showNsfw = prefs[Keys.SHOW_NSFW] ?: false,
            showHighRes = prefs[Keys.SHOW_HIGH_RES] ?: true,
            defaultPageCount = prefs[Keys.DEFAULT_PAGE_COUNT] ?: 40,
            cacheEnabled = prefs[Keys.CACHE_ENABLED] ?: true,
            saveLocation = prefs[Keys.SAVE_LOCATION] ?: "Gelbooru",
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.name),
            lastSearchQuery = prefs[Keys.LAST_SEARCH_QUERY] ?: ""
        )
    }

    suspend fun setNsfw(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_NSFW] = enabled }
    }

    suspend fun setHighRes(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_HIGH_RES] = enabled }
    }

    suspend fun setSaveLocation(location: String) {
        context.dataStore.edit { it[Keys.SAVE_LOCATION] = location }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLastSearchQuery(query: String) {
        context.dataStore.edit { it[Keys.LAST_SEARCH_QUERY] = query }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
