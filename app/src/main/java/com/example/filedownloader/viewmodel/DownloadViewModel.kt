package com.example.filedownloader.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.filedownloader.utils.DownloadProgress
import kotlinx.coroutines.launch

class DownloadViewModel(private val app: Application) : AndroidViewModel(app) {

    private var downloadId : Long = 0
    private lateinit var downloadManager : DownloadManager
    var isDownloading = false
    // URL is set via Two-Way Binding with the text entered into the EditText
    var url = "https://speed.hetzner.de/100MB.bin"
    private val _eventReceived = MutableLiveData<String>()
    val eventReceived : LiveData<String>
        get() = _eventReceived

    private val _progress = MutableLiveData<String>()
    val progress : LiveData<String>
        get() = _progress

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var id: Long
            intent?.let {
                id = it.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val downloadQuery = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(downloadQuery)

                while (cursor.moveToNext()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val filePathIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val filePath = cursor.getString(filePathIndex)
                        _progress.value = "100% completed"
                        _eventReceived.value = filePath
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        _eventReceived.value = "Download Failed"
                    } else if (status == DownloadManager.STATUS_PAUSED) {
                        _progress.value = "Download paused"
                    }
                }
                isDownloading = false
                cursor.close()
            }
        }
    }

    init {
        app.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    fun downloadData() {
        _progress.value = "0"
        isDownloading = true
        val fileName = URLUtil.guessFileName(url,null,null)

        val request = DownloadManager.Request(Uri.parse(url))
            .setDescription(url)
            .setRequiresCharging(false)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        downloadManager = app.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        val downloadProgress = DownloadProgress(downloadManager)

        viewModelScope.launch {
            while (isDownloading) {
                _progress.value = "${downloadProgress.getProgressUpdate(downloadId)}% Completed"
            }
        }
    }

    fun onReceivedCompleted() {
        _eventReceived.value = ""
    }
}