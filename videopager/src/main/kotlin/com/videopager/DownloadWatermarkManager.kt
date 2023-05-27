package com.videopager

import android.os.Environment
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by shresthasaurabh86@gmail.com 21/05/2023.
 */
abstract class DownloadWatermarkManager {

    companion object {
        const val TAG: String = "WatermarkManager"

        const val WATERMARK_END_SOUND_3 =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio_3.mp4?alt=media&token=c8516689-d112-418b-96af-e8748ba6bf12"
        const val WATERMARK_LOGO_LINK =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_logo.png?alt=media&token=b6d7f2d9-d242-42b2-ae97-a85c82d19647"

    }
    fun createOutputFile(): String {
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dateFormat = SimpleDateFormat("h_mma_ddMMMyyyy", Locale.getDefault())
        return "$outputDir/vfunny_${dateFormat.format(Date())}.mp4"
    }
}