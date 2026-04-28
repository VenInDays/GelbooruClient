package com.gelbooru.client.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gelbooru.client.data.repository.PreferencesRepository
import com.gelbooru.client.ui.screens.GalleryScreen
import com.gelbooru.client.ui.screens.SettingsScreen
import com.gelbooru.client.ui.theme.GelbooruTheme
import com.gelbooru.client.ui.theme.TactileTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository(this)
        enableEdgeToEdge()

        setContent {
            // Observe theme preference reactively
            val preferences by preferencesRepository.preferences.collectAsState(
                initial = com.gelbooru.client.data.model.UserPreferences()
            )

            GelbooruTheme(themeMode = preferences.themeMode) {
                MainNavigation(preferencesRepository)
            }
        }
    }
}

@Composable
private fun MainNavigation(preferencesRepository: PreferencesRepository) {
    var currentScreen by remember { mutableStateOf("gallery") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TactileTheme.colors.surfaceBase)
    ) {
        when (currentScreen) {
            "gallery" -> GalleryScreen(
                onSettingsClick = { currentScreen = "settings" }
            )
            "settings" -> SettingsScreen(
                preferencesRepository = preferencesRepository,
                onBackClick = { currentScreen = "gallery" }
            )
        }
    }
}
