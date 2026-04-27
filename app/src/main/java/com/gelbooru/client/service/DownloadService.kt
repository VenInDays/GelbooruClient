package com.gelbooru.client.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gelbooru.client.R
import com.gelbooru.client.data.model.DownloadStatus
import com.gelbooru.client.data.model.DownloadTask
import com.gelbooru.client.network.ImageDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Foreground service for downloading images in the background.
 * Shows progress notification during downloads.
 */
class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var downloader: ImageDownloader
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val CHANNEL_NAME = "Image Downloads"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_POST_ID = "extra_post_id"
        const val EXTRA_SUBFOLDER = "extra_subfolder"

        fun startDownload(context: Context, imageUrl: String, postId: Int, subfolder: String = "Gelbooru") {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, imageUrl)
                putExtra(EXTRA_POST_ID, postId)
                putExtra(EXTRA_SUBFOLDER, subfolder)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        downloader = ImageDownloader(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val imageUrl = intent?.getStringExtra(EXTRA_IMAGE_URL) ?: return START_NOT_STICKY
        val postId = intent.getIntExtra(EXTRA_POST_ID, 0)
        val subfolder = intent.getStringExtra(EXTRA_SUBFOLDER) ?: "Gelbooru"

        // Start as foreground service
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.downloading))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, 0, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start download
        serviceScope.launch {
            downloader.downloadImage(imageUrl, postId, subfolder)
                .catch { emit(DownloadTask(postId = postId, imageUrl = imageUrl, fileName = "", destinationPath = "", status = DownloadStatus.FAILED)) }
                .collect { task ->
                    updateNotification(task)
                    if (task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(task: DownloadTask) {
        val notification = when (task.status) {
            DownloadStatus.DOWNLOADING -> NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading))
                .setContentText("${task.fileName} - ${(task.progress * 100).toInt()}%")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, (task.progress * 100).toInt(), false)
                .setOngoing(true)
                .build()

            DownloadStatus.COMPLETED -> NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.download_complete))
                .setContentText(task.fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build()

            DownloadStatus.FAILED -> NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.download_failed))
                .setContentText(task.fileName)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build()

            else -> return
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows image download progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
