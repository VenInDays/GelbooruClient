package com.gelbooru.client.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a single Gelbooru post parsed from HTML.
 */
data class GelbooruPost(
    val id: Int,
    val postId: Int = id,
    @SerializedName("preview_url")
    val previewUrl: String,
    @SerializedName("sample_url")
    val sampleUrl: String = "",
    @SerializedName("file_url")
    val fileUrl: String = "",
    @SerializedName("original_url")
    val originalUrl: String = "",
    val tags: List<String> = emptyList(),
    val score: Int = 0,
    val rating: PostRating = PostRating.SAFE,
    val source: String = "",
    val title: String = "",
    val hasNotes: Boolean = false,
    val hasComments: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0,
    val postUrl: String = ""
) {
    fun getBestImageUrl(): String {
        return when {
            originalUrl.isNotBlank() -> originalUrl
            fileUrl.isNotBlank() -> fileUrl
            sampleUrl.isNotBlank() -> sampleUrl
            else -> previewUrl
        }
    }

    fun getDisplayUrl(): String {
        return when {
            sampleUrl.isNotBlank() -> sampleUrl
            previewUrl.isNotBlank() -> previewUrl
            else -> fileUrl
        }
    }
}

enum class PostRating(val value: String) {
    SAFE("safe"),
    QUESTIONABLE("questionable"),
    EXPLICIT("explicit"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): PostRating {
            return entries.firstOrNull { it.value == value.lowercase() } ?: UNKNOWN
        }
    }
}

/**
 * Paginated result container for search results.
 */
data class SearchResult(
    val posts: List<GelbooruPost>,
    val currentPage: Int,
    val totalPages: Int,
    val totalPosts: Int = 0,
    val query: String = ""
) {
    val hasNextPage: Boolean get() = currentPage < totalPages
    val hasPreviousPage: Boolean get() = currentPage > 1
}
