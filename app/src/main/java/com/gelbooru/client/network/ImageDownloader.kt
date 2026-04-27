package com.gelbooru.client.network

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.gelbooru.client.data.model.DownloadStatus
import com.gelbooru.client.data.model.DownloadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class ImageDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun downloadImage(
        imageUrl: String,
        postId: Int,
        subfolder: String = "Gelbooru"
    ): Flow<DownloadTask> = flow {
        val fileName = generateFileName(imageUrl, postId)
        val mimeType = getMimeType(imageUrl)
        val task = DownloadTask(
            postId = postId,
            imageUrl = imageUrl,
            fileName = fileName,
            destinationPath = subfolder
        )

        emit(task.copy(status = DownloadStatus.DOWNLOADING, progress = 0f))

        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://gelbooru.com/")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(task.copy(status = DownloadStatus.FAILED))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(task.copy(status = DownloadStatus.FAILED))
                return@flow
            }

            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()

            val bytesWritten = writeToFile(inputStream, fileName, mimeType, subfolder, totalBytes)

            emit(task.copy(
                status = DownloadStatus.COMPLETED,
                progress = 1f,
                bytesDownloaded = bytesWritten,
                totalBytes = totalBytes
            ))
        } catch (e: Exception) {
            emit(task.copy(status = DownloadStatus.FAILED))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun downloadToCache(imageUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://gelbooru.com/")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) return@withContext null

            val extension = getExtensionFromUrl(imageUrl)
            val cacheFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.$extension")
            cacheFile.outputStream().use { output ->
                response.body!!.byteStream().copyTo(output)
            }
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    private fun writeToFile(
        inputStream: java.io.InputStream,
        fileName: String,
        mimeType: String,
        subfolder: String,
        totalBytes: Long
    ): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToMediaStore(inputStream, fileName, mimeType, subfolder, totalBytes)
        } else {
            @Suppress("DEPRECATION")
            writeToExternalStorage(inputStream, fileName, subfolder, totalBytes)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeToExternalStorage(
        inputStream: java.io.InputStream,
        fileName: String,
        subfolder: String,
        totalBytes: Long
    ): Long {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val destDir = File(picturesDir, subfolder)
        if (!destDir.exists()) destDir.mkdirs()

        val outFile = File(destDir, fileName)
        return outFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }.also {
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(outFile.absolutePath), null, null
            )
        }
    }

    private fun writeToMediaStore(
        inputStream: java.io.InputStream,
        fileName: String,
        mimeType: String,
        subfolder: String,
        totalBytes: Long
    ): Long {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subfolder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        var outputStream: OutputStream? = null
        return try {
            outputStream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open output stream")
            inputStream.copyTo(outputStream)
        } finally {
            outputStream?.close()
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }

    private fun generateFileName(url: String, postId: Int): String {
        val extension = getExtensionFromUrl(url)
        return "gelbooru_${postId}_${System.currentTimeMillis()}.$extension"
    }

    private fun getMimeType(url: String): String {
        val extension = getExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    }

    private fun getExtensionFromUrl(url: String): String {
        val cleaned = url.split("?")[0].split("#")[0]
        val dotIndex = cleaned.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < cleaned.length - 1) {
            cleaned.substring(dotIndex + 1).lowercase()
        } else "jpg"
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    }
}
