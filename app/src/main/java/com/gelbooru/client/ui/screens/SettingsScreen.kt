package com.gelbooru.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gelbooru.client.data.model.ThemeMode
import com.gelbooru.client.data.repository.PreferencesRepository
import com.gelbooru.client.ui.components.TactileChip
import com.gelbooru.client.ui.theme.TactileTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    onBackClick: () -> Unit
) {
    val preferences by preferencesRepository.preferences.collectAsState(
        initial = com.gelbooru.client.data.model.UserPreferences()
    )
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TactileTheme.colors.surfaceBase)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicText(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = TactileTheme.colors.textPrimary
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBackClick) {
                Text("Done", color = TactileTheme.colors.accentPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content section
        SectionHeader("Content")

        SettingToggle(
            title = "Show NSFW Content",
            subtitle = "Display explicit and questionable posts",
            isOn = preferences.showNsfw,
            onToggle = { scope.launch { preferencesRepository.setNsfw(!preferences.showNsfw) } }
        )

        SettingToggle(
            title = "High Resolution",
            subtitle = "Load full resolution images when available",
            isOn = preferences.showHighRes,
            onToggle = { scope.launch { preferencesRepository.setHighRes(!preferences.showHighRes) } }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Appearance section
        SectionHeader("Appearance")

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            color = TactileTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = preferences.themeMode == mode
                TactileChip(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.clickable {
                        scope.launch { preferencesRepository.setThemeMode(mode) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Storage section
        SectionHeader("Storage")

        SettingItem(
            title = "Save Location",
            subtitle = "Pictures/${preferences.saveLocation}",
            onClick = { /* TODO: folder picker */ }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // About
        SectionHeader("About")
        Text(
            text = "Gelbooru Client v1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = TactileTheme.colors.textTertiary
        )
        Text(
            text = "Built with Kotlin & Jetpack Compose",
            style = MaterialTheme.typography.bodyMedium,
            color = TactileTheme.colors.textTertiary
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = TactileTheme.colors.textTertiary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    isOn: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TactileTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TactileTheme.colors.textSecondary
            )
        }
        Switch(
            checked = isOn,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = TactileTheme.colors.accentPrimary,
                uncheckedTrackColor = TactileTheme.colors.divider,
                checkedThumbColor = TactileTheme.colors.surfaceElevated,
                uncheckedThumbColor = TactileTheme.colors.textTertiary
            )
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TactileTheme.colors.textPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TactileTheme.colors.textSecondary
        )
    }
}
