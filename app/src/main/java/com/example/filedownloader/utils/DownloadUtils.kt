package com.example.filedownloader.utils

import android.app.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

fun getProgressUpdate(downloadManager: DownloadManager, id: Long) = flow {
    var progress = 0
    emit(progress)
    var isDownloading = true

    while (isDownloading) {
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)

        if (cursor.moveToNext()) {
            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

            val totalBytes = cursor.getInt(totalBytesIndex)
            val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
            val status = cursor.getInt(statusIndex)

            if (status == DownloadManager.STATUS_SUCCESSFUL ||
                status == DownloadManager.STATUS_FAILED
            ) {
                isDownloading = false
            }
            progress = when (totalBytes) {
                -1, 0 -> 0
                else -> ((bytesDownloaded * 100f) / totalBytes).toInt()
            }
            emit(progress)
            cursor.close()
        }
    }
}.flowOn(Dispatchers.Default)