package com.videopager.data

interface ProgressListener {
    fun onProgress(progress: Int, count: Int? = 1, log: String? = "no logs")
}