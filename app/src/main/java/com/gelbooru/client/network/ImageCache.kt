package com.gelbooru.client.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.Coil
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Two-tier image cache using Coil's Memory + Disk cache.
 * Provides manual cache inspection and management.
 */
class ImageCache(private val context: Context) {

    private val cacheDir: File = File(context.cacheDir, "gelbooru_images")

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    /**
     * Get the total size of disk cache in bytes.
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        calculateDirSize(cacheDir)
    }

    /**
     * Clear all cached images.
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if an image URL is cached on disk.
     */
    fun isCached(url: String): Boolean {
        val file = getCachedFile(url)
        return file.exists() && file.length() > 0
    }

    /**
     * Get a cached file for the given URL.
     */
    fun getCachedFile(url: String): File {
        val key = md5(url)
        return File(cacheDir, key)
    }

    /**
     * Get the number of cached files.
     */
    suspend fun getCachedFileCount(): Int = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.size ?: 0
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
