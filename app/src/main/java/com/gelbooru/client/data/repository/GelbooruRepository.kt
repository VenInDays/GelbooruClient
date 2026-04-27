package com.gelbooru.client.data.repository

import android.content.Context
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.SearchResult
import com.gelbooru.client.data.model.UserPreferences
import com.gelbooru.client.scraping.GelbooruScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository that mediates between the scraping engine and the UI.
 * Provides a clean API for search, detail, and download operations.
 */
class GelbooruRepository(private val context: Context) {

    private val scraper = GelbooruScraper(context)

    /**
     * Search posts by tags.
     */
    suspend fun search(tags: String, page: Int, preferences: UserPreferences): SearchResult {
        return withContext(Dispatchers.IO) {
            scraper.searchPosts(tags, page, preferences)
        }
    }

    /**
     * Resolve the full-resolution image URL for a post.
     */
    suspend fun resolveFullImageUrl(post: GelbooruPost, preferences: UserPreferences): String {
        return withContext(Dispatchers.IO) {
            scraper.resolveFullImageUrl(post, preferences)
        }
    }

    /**
     * Download a post's image to the device gallery.
     * Returns a flow of progress (0f to 1f).
     */
    fun downloadImage(imageUrl: String, postId: Int, subfolder: String): Flow<Float> = flow {
        val downloader = com.gelbooru.client.network.ImageDownloader(context)
        downloader.downloadImage(imageUrl, postId, subfolder).collect { task ->
            emit(task.progress)
        }
    }.flowOn(Dispatchers.IO)
}
