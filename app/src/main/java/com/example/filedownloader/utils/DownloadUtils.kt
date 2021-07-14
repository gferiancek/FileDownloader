package com.example.filedownloader.utils

import android.app.DownloadManager
import android.database.Cursor
import kotlinx.coroutines.flow.flow

fun DownloadManager.getProgressUpdate(id: Long) = flow {
    var progress = 0
    var isDownloading = true

    val query = DownloadManager.Query().setFilterById(id)
    lateinit var cursor: Cursor

    while (isDownloading) {
        // If we reuse the cursor within the loop, we don't receive any progress updates.  Because of that,
        // we reassign the cursor to the query to get the latest information about download progress.
        cursor = query(query)
        if (cursor.moveToFirst()) {
            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val bytesDownloadedIndex =
                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

            val totalBytes = cursor.getLong(totalBytesIndex)
            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)

            when (cursor.getInt(statusIndex)) {
                DownloadManager.STATUS_RUNNING -> {
                    progress = when (totalBytes) {
                        // COLUMN_TOTAL_SIZE_BYTES will return -1 if the total size isn't available,
                        // and if it is 0, we will have a divide by 0 error.  So in both cases
                        // we avoid the calculation and set progress to 0.
                        -1L, 0L -> 0
                        else -> ((bytesDownloaded * 100f) / totalBytes).toInt()
                    }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    progress = 100
                    isDownloading = false
                }
                DownloadManager.STATUS_FAILED -> {
                    // We return -1 on a failed download and use that in the ViewModel to check if we
                    // need to send a progress update. Without this check, the app will sometimes
                    // send the progress update AFTER onReceive has been called in the ViewModel,
                    // replacing the "Download Failed" notification with an ongoing progress notification
                    // that cannot be canceled. (Logic to cancel the ongoing notification is in onReceive,
                    // so if this is sent after that is triggered, it will never be cancelled and hitting
                    // the cancel action will not trigger onReceived again since the download is already finished.)
                    progress = -1
                    isDownloading = false
                }
            }
            emit(progress)
        }
    }
    cursor.close()
}
