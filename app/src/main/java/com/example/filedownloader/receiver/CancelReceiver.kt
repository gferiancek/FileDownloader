package com.example.filedownloader.receiver

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent

class CancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val downloadManager = context?.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        intent?.let {
            downloadManager.remove(intent.getLongExtra("download_id", 0))
        }
    }
}