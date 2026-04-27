package com.gelbooru.client.data.model

/**
 * User-configurable preferences for the app.
 */
data class UserPreferences(
    val showNsfw: Boolean = false,
    val showHighRes: Boolean = true,
    val defaultPageCount: Int = 40,
    val cacheEnabled: Boolean = true,
    val autoDownloadWifi: Boolean = false,
    val saveLocation: String = "Gelbooru",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val lastSearchQuery: String = ""
)

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Represents a tag suggestion from autocomplete.
 */
data class TagSuggestion(
    val name: String,
    val count: Int,
    val category: TagCategory = TagCategory.GENERAL
)

enum class TagCategory(val id: Int) {
    GENERAL(0),
    ARTIST(1),
    COPYRIGHT(3),
    CHARACTER(4),
    METADATA(5),
    UNKNOWN(-1);

    companion object {
        fun fromId(id: Int): TagCategory {
            return entries.firstOrNull { it.id == id } ?: UNKNOWN
        }
    }
}
