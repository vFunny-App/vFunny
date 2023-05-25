package com.videopager.data

interface ProgressListener {
    fun onProgress(page: Int, progress: Int)
}