package com.example.filedownloader.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.filedownloader.utils.getProgressUpdate
import com.example.filedownloader.utils.sendNotification
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DownloadViewModel(private val app: Application) : AndroidViewModel(app) {

    private var downloadId: Long = 0
    private lateinit var downloadManager: DownloadManager
    var isDownloading = false

    // URL is set via Two-Way Binding with the text entered into the EditText
    var url = "https://speed.hetzner.de/100MB.bin"

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<String>
        get() = _progress.map { progress ->
            when (progress) {
                0 -> ""
                else -> "$progress% completed"
            }
        }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isDownloading = false
            var id: Long
            intent?.let {
                id = it.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val downloadQuery = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(downloadQuery)

                while (cursor.moveToNext()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)
                    val nameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val notificationTitle = cursor.getString(nameIndex)

                    val notificationMessage = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val filePathIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val filePath = cursor.getString(filePathIndex)
                            filePath
                        }
                        DownloadManager.STATUS_PAUSED -> "Download Paused"
                        else -> "Download Failed"
                    }

                    val notificationManager = ContextCompat.getSystemService(
                        app,
                        NotificationManager::class.java
                    ) as NotificationManager
                    notificationManager.sendNotification(
                        notificationTitle,
                        notificationMessage,
                        app
                    )

                }
                cursor.close()
            }
        }
    }

    init {
        app.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    /**
     * Function that builds a request for the downloadmanager, downloads that data, and then
     * keeps track of the download progress via DownloadUtils' getProgressUpdate function.
     */
    fun downloadData() {
        val fileName = URLUtil.guessFileName(url, null, null)

        val request = DownloadManager.Request(Uri.parse(url))
            .setDescription(url)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        downloadManager = app.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        viewModelScope.launch {
            getProgressUpdate(downloadManager, downloadId).collect { progress ->
                _progress.value = progress
            }
        }
    }
}