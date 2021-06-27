package com.example.filedownloader.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.example.filedownloader.MainActivity
import com.example.filedownloader.R

const val NOTIFICATION_ID = 0
fun NotificationManager.sendNotification(title: String, message: String, appContext: Context) {

    val pendingIntent = NavDeepLinkBuilder(appContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_graph)
        .setDestination(R.id.detailFragment)
        .createPendingIntent()

    val notifBuilder = NotificationCompat.Builder(
        appContext,
        appContext.getString(R.string.download_complete_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    notify(NOTIFICATION_ID, notifBuilder.build())
}