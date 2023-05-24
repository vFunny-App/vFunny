package com.videopager

import android.app.Activity
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by shresthasaurabh86@gmail.com 21/05/2023.
 */
class DownloadWatermarkManager {

    companion object {
        private const val TAG: String = "WatermarkManager"

        const val WATERMARK_END =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end.mp4?alt=media&token=b39bcdc4-be50-49e0-8deb-024abb32ec8f"
        const val WATERMARK_END_SOUND =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio.mp4?alt=media&token=7ff3bd8e-12af-4828-9e59-cc5b50c353a5"
        const val WATERMARK_END_SOUND_2 =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio_2.mp4?alt=media&token=22e76999-c048-4429-bbe0-193b02fef79a"
        const val WATERMARK_END_SOUND_3 =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio_3.mp4?alt=media&token=c8516689-d112-418b-96af-e8748ba6bf12"
        const val WATERMARK_LOGO_LINK =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_logo.png?alt=media&token=b6d7f2d9-d242-42b2-ae97-a85c82d19647"

    }

    suspend fun addWatermarkVideo(
        videoUrl: String,
        context: Context,
    ) {
        val logoUrl = WATERMARK_LOGO_LINK
        val videoEndUrl = WATERMARK_END_SOUND_3
        val outputFilePath = createOutputFile()
        val videoLogoOutputFile =
            File.createTempFile("videoLogoOutputFile", ".mp4").also { it.delete() }
        val videoEndFile = File.createTempFile("videoEndFile", ".mp4").also { it.delete() }
        Files.copy(URL(videoEndUrl).openStream(), videoEndFile.toPath())
        // Write the paths to the video files
        val videoAndEndFiles = createVideoAndEndFiles(videoLogoOutputFile, videoEndFile)
        val commandToConcatVideoAndEnd =
            "-i $videoUrl -i $logoUrl -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.5:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" -c:v libx264 -preset fast -crf 23 ${videoLogoOutputFile.absolutePath}"

        val tempVideoLogoOutputFile = File.createTempFile("temp1", ".ts")
        val temp2 = File.createTempFile("temp2", ".ts")
        if (tempVideoLogoOutputFile.exists()) {
            tempVideoLogoOutputFile.delete()
        }
        if (temp2.exists()) {
            temp2.delete()
        }
        val command2 =
            "-i ${videoLogoOutputFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${tempVideoLogoOutputFile.absolutePath}"
        val command3 =
            "-i ${videoEndFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp2.absolutePath}"
        // now join
        val command4 =
            "-i \"concat:${tempVideoLogoOutputFile.absolutePath}|${temp2.absolutePath}\" -s 1280x720 -c copy -bsf:a aac_adtstoasc $outputFilePath"

        try {
            Looper.prepare()
            Toast.makeText(context, "Download Started!", Toast.LENGTH_SHORT).show()
            //Switch to IO thread as we will save a video to storage
            withContext(Dispatchers.IO) {
                Log.e(TAG, "addWatermarkVideo: Please wait while saving video :>")
                //Execute the command
                FFmpegKit.executeAsync(commandToConcatVideoAndEnd) { session ->
                    //Tell user if process is done successfully or not
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        Log.e(TAG, "Success Adding Logo")
                        FFmpegKit.executeAsync(command2) { session2 ->
                            //Tell user if process is done successfully or not
                            if (ReturnCode.isSuccess(session2.returnCode)) {
                                Log.e(TAG, "Success temping First Video")
                                FFmpegKit.executeAsync(command3) { session3 ->
                                    //Tell user if process is done successfully or not
                                    if (ReturnCode.isSuccess(session3.returnCode)) {
                                        Log.e(TAG, "Success temping End Video")
                                        FFmpegKit.executeAsync(command4) { session4 ->
                                            //Tell user if process is done successfully or not
                                            if (ReturnCode.isSuccess(session4.returnCode)) {
                                                Log.e(TAG, "Success Merging End Video")
                                                kotlin.run {
                                                    Looper.prepare()
                                                    Toast.makeText(
                                                        context,
                                                        "Download Finished!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    Log.e(
                                                        TAG,
                                                        "addWatermarkVideo: From UI Thread!  1"
                                                    )
                                                    videoAndEndFiles.deleteOnExit()
                                                    videoLogoOutputFile.deleteOnExit()
                                                    videoEndFile.deleteOnExit()
                                                }
                                            } else {
                                                if (File(outputFilePath).exists()) {
                                                    File(outputFilePath).delete()
                                                }
                                                val command4_back =
                                                    "-i ${videoLogoOutputFile.absolutePath} -c copy $outputFilePath"
                                                FFmpegKit.executeAsync(command4_back) { command4back ->
                                                    if (ReturnCode.isSuccess(command4back.returnCode)) {
                                                        Log.e(TAG, "Success Saving Logo Video")
                                                        kotlin.run {
                                                            videoAndEndFiles.deleteOnExit()
                                                            videoLogoOutputFile.deleteOnExit()
                                                            videoEndFile.deleteOnExit()
                                                        }
                                                    } else {
                                                        Log.e(TAG, "Error Merging Logo Video")
                                                        (context as Activity).runOnUiThread(Runnable {
                                                            kotlin.run {
                                                                Log.e(
                                                                    TAG,
                                                                    "addWatermarkVideo: From UI Thread!  69"
                                                                )
                                                                videoAndEndFiles.deleteOnExit()
                                                                videoLogoOutputFile.deleteOnExit()
                                                                videoEndFile.deleteOnExit()
                                                            }
                                                        })
                                                    }
                                                    Log.e(TAG, "Error Merging End Video")
                                                    (context as Activity).runOnUiThread(Runnable {
                                                        kotlin.run {
                                                            Log.e(
                                                                TAG,
                                                                "addWatermarkVideo: From UI Thread!  2"
                                                            )
                                                            videoAndEndFiles.deleteOnExit()
                                                            videoLogoOutputFile.deleteOnExit()
                                                            videoEndFile.deleteOnExit()
                                                        }
                                                    })
                                                }
                                            }
                                            //Return to main thread to call the Clear function
                                        }
                                    } else {
                                        (context as Activity).runOnUiThread(Runnable {
                                            kotlin.run {
                                                Log.e(TAG, "addWatermarkVideo: From UI Thread!  3")
                                                videoAndEndFiles.deleteOnExit()
                                                videoLogoOutputFile.deleteOnExit()
                                                videoEndFile.deleteOnExit()
                                            }
                                        })
                                    }
                                    //Return to main thread to call the Clear function
                                }
                            } else {
                                (context as Activity).runOnUiThread(Runnable {
                                    kotlin.run {
                                        Log.e(TAG, "addWatermarkVideo: From UI Thread!  4")
                                        videoAndEndFiles.deleteOnExit()
                                        videoLogoOutputFile.deleteOnExit()
                                        videoEndFile.deleteOnExit()
                                    }
                                })
                            }
                            //Return to main thread to call the Clear function
                        }
                    } else {
                        Log.e(TAG, "Error Adding Logo")
                        //Return to main thread to call the Clear function
                        (context as Activity).runOnUiThread(Runnable {
                            kotlin.run {
                                Log.e(TAG, "addWatermarkVideo: From UI Thread!  5")
                                videoAndEndFiles.deleteOnExit()
                                videoLogoOutputFile.deleteOnExit()
                                videoEndFile.deleteOnExit()
                            }
                        })
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(TAG, "addWatermarkVideo: Error in Convert Execution !")
        }
    }

    private fun createVideoAndEndFiles(videoLogoOutputFile: File, videoEndFile: File): File {
        val fileList = File.createTempFile("filelist", ".txt")
        val writer = fileList.bufferedWriter()
        writer.write("file '${videoLogoOutputFile}'")
        writer.newLine()
        writer.write("file '$videoEndFile'")
        writer.close()
        return fileList
    }

    private fun createOutputFile(): String {
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dateFormat = SimpleDateFormat("h_mma_ddMMMyyyy", Locale.getDefault())
        return "$outputDir/vfunny_${dateFormat.format(Date())}.mp4"
    }

    private fun checkCodecs(url1: String, url2: String): Boolean {

        val codec1 = getCodecName(url1)
        val codec2 = getCodecName(url2)

        return codec1 == codec2
    }

    private fun getCodecName(uri: String): String? {
        val mimeType = getMimeType(uri)
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        var codeName: String? = null
        val codecInfos = mediaCodecList.codecInfos
        try {
            for (codecInfo in codecInfos) {
                if (codecInfo.getCapabilitiesForType(mimeType)
                        ?.isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback) == true
                ) {
                    codeName = codecInfo.name
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "addWatermarkVideo: Error $e")
        }
        return codeName
    }

    private fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

}