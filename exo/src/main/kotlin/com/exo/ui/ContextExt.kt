package com.exo.ui

import android.content.Context
import android.view.LayoutInflater

internal val Context.layoutInflater: LayoutInflater get() = LayoutInflater.from(this)

internal const val VIDEO_LIST = "VIDEO_LIST"
internal const val VIDEO_URL = "VIDEO_URL"
internal const val MAX_CACHED_VIDEOS = 3
