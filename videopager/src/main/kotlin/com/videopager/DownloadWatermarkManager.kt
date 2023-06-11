package com.videopager

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformationSession
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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

        private val root = Environment.getExternalStorageDirectory().toString()
        private val app_folder: String = "$root/vFunny/"

    }

    fun getDuration(videoUrl: String?): Long {
        val ffprobeCommand : MediaInformationSession = FFprobeKit.getMediaInformation(videoUrl)
        return ffprobeCommand.duration
    }

}