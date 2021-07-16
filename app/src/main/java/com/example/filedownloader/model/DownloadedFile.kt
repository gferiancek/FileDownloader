package com.example.filedownloader.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadedFile(
    var title: String = "",
    var filePath: String = "",
    var status: String = "",
    var reason: String = "",
    var size: String = "",
    var statusColor: Int = 0
) : Parcelable