package com.player.models

data class VideoData(
    var id: String,
    val mediaUri: String,
    val previewImageUri: String,
    val aspectRatio: Float? = null,
    val type: String? = "video",
    val key: String?  = null,
)
