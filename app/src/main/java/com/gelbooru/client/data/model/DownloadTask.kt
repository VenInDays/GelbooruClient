package com.gelbooru.client.data.model

/**
 * Tracks the state of an image download operation.
 */
data class DownloadTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val postId: Int,
    val imageUrl: String,
    val fileName: String,
    val destinationPath: String,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Cache entry for a parsed post.
 */
data class CachedPost(
    val post: GelbooruPost,
    val cachedAt: Long = System.currentTimeMillis(),
    val ttl: Long = 30 * 60 * 1000 // 30 minutes
) {
    val isExpired: Boolean get() = System.currentTimeMillis() - cachedAt > ttl
}
