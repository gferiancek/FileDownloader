package com.example.filedownloader.model

import android.os.FileUtils

data class DownloadedFile(
    var title: String = "",
    var filePath: String = "",
    var status: String = "",
    var reason: String = "",
    var size: String = ""
)
