package com.example.filedownloader.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.filedownloader.MainActivity
import com.example.filedownloader.R
import com.example.filedownloader.receiver.CancelReceiver

const val NOTIFICATION_ID = 0
const val REQUEST_CODE = 0

fun NotificationManager.sendDownloadCompletedNotification(
    title: String,
    status: String,
    message: String,
    appContext: Context
) {
    val pendingIntent = NavDeepLinkBuilder(appContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.detailFragment)
        .createPendingIntent()

    val notificationBuilder = NotificationCompat.Builder(
        appContext,
        appContext.getString(R.string.download_complete_channel_id)
    )
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
                .setBigContentTitle(status)
        )
        .setSmallIcon(R.drawable.ic_baseline_download_24)
        .setContentTitle(title)
        .setContentText(status)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    notify(NOTIFICATION_ID, notificationBuilder.build())
}

fun NotificationManager.updateProgress(
    title: String,
    message: String,
    progress: Int,
    downloadId: Long,
    appContext: Context
) {
    val cancelIntent = Intent(appContext, CancelReceiver::class.java)
    cancelIntent.putExtra("download_id", downloadId)
    val cancelPendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_CODE,
        cancelIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificationBuilder = NotificationCompat.Builder(
        appContext,
        appContext.getString(R.string.download_complete_channel_id)
    )
        .setOngoing(true)
        .setProgress(100, progress, progress == 0)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_baseline_download_24)
        .setOnlyAlertOnce(true)
        .addAction(
            R.drawable.ic_baseline_cancel_24,
            appContext.getString(R.string.action_cancel),
            cancelPendingIntent
        )
    notify(NOTIFICATION_ID, notificationBuilder.build())
}