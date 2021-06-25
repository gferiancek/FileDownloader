package com.example.filedownloader.utils

import android.app.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadProgress(private val downloadManager: DownloadManager) {

    suspend fun getProgressUpdate(id: Long): Int {
        var progress = 0
        withContext(Dispatchers.Default) {
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager.query(query)

            while (cursor.moveToNext()) {
                val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytes = cursor.getInt(totalBytesIndex)
                val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)

                progress = when (totalBytes) {
                    0 -> 0
                    else -> ((bytesDownloaded * 100f) / totalBytes).toInt()
                }
            }
            cursor.close()
        }
        return progress
    }
}