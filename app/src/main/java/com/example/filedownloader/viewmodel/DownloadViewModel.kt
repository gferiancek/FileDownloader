package com.example.filedownloader.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.filedownloader.R
import com.example.filedownloader.model.DownloadedFile
import com.example.filedownloader.utils.getProgressUpdate
import com.example.filedownloader.utils.sendDownloadCompletedNotification
import com.example.filedownloader.utils.updateProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class DownloadViewModel(private val app: Application) : AndroidViewModel(app) {

    private var downloadId: Long = 0
    private var downloadManager: DownloadManager =
        app.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    private var notificationManager: NotificationManager =
        app.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int>
        get() = _progress

    private val _eventStartDownload = MutableLiveData<Boolean>()
    val eventStartDownload = _eventStartDownload

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var id: Long
            intent?.let {
                id = it.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                sendNotification(parseDownloadedFile(id))
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
    fun downloadData(url: String) {
        _progress.value = 0
        notificationManager.cancelAll()
        val fileName = URLUtil.guessFileName(url, null, null)

        val request = DownloadManager.Request(Uri.parse(url))
            .setDescription(url)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        downloadId = downloadManager.enqueue(request)
        viewModelScope.launch {
            downloadManager.getProgressUpdate(downloadId)
                .flowOn(Dispatchers.Default).collect { progress ->
                    _progress.value = progress
                    // progress is returned as -1 if the download has failed. Sometimes on a failed download
                    // the app would call updateProgress after onReceived had been called, and would
                    // replace the "Download Failed" notification with an ongoing notification that could
                    // not be canceled.  (Logic to cancel the ongoing notification is in onReceive,
                    // so if this is sent after that is triggered, it will never be cancelled and hitting
                    // the cancel action will not trigger onReceived again since the download is already finished.)
                    if (progress != -1) {
                        notificationManager.updateProgress(fileName, url, progress, downloadId, app)
                    }
                }
        }
    }


    /**
     * Fun that takes the downloadId and pulls the desired information from the DownloadManager Query for that id
     * and returns a DownloadedFile object with the extracted information.
     */
    fun parseDownloadedFile(downloadId: Long): DownloadedFile {
        val downloadQuery = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(downloadQuery)

        val downloadedFile = DownloadedFile()
        if (cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

            when (cursor.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val filePathIndex =
                        cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    downloadedFile.apply {
                        title = cursor.getString(titleIndex)
                        filePath = cursor.getString(filePathIndex)
                        status = app.getString(R.string.successful)
                        statusColor = Color.GREEN
                        size = android.text.format.Formatter.formatShortFileSize(
                            app,
                            cursor.getLong(sizeIndex)
                        )
                    }
                }
                DownloadManager.STATUS_FAILED -> {
                    val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                    downloadedFile.apply {
                        title = URLUtil.guessFileName(cursor.getString(uriIndex), null, null)
                        status = app.getString(R.string.failed)
                        statusColor = Color.RED
                        reason = "${cursor.getInt(reasonIndex)} error"
                    }
                }
            }
        }
        cursor.close()
        return downloadedFile
    }

    fun sendNotification(downloadedFile: DownloadedFile) {
        when (downloadedFile.status.isBlank()) {
            true -> {
                // status is only blank if the notification is cancelled, so if that is
                // the case we can cancel the notification and reset the progress.
                notificationManager.cancelAll()
                _progress.value = -1
            }
            false -> {
                // Cancels the ongoing notification from updateProgress, and replaces it with a new
                // one with information about the status of the download.
                notificationManager.cancelAll()
                // If filePath is blank, the download failed and we show the reason instead.
                val notificationMessage = when (downloadedFile.filePath.isNotBlank()) {
                    true -> downloadedFile.filePath
                    false -> downloadedFile.reason
                }
                notificationManager.sendDownloadCompletedNotification(
                    downloadedFile.title,
                    "Download ${downloadedFile.status}",
                    notificationMessage,
                    downloadedFile,
                    app
                )
            }
        }
    }

    fun onStartDownload() {
        _eventStartDownload.value = true
    }

    fun onStartDownloadCompleted() {
        _eventStartDownload.value = false
    }
}