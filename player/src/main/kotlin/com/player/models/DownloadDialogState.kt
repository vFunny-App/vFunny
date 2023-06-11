package com.player.models

data class DownloadDialogState(
    var progress: Int,
    var title: String? = "No Logs",
    val isShowing: Boolean,
)
