package com.videopager

import android.app.Activity
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
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

        @JvmStatic
        val instance: DownloadWatermarkManager by lazy { DownloadWatermarkManager() }
    }

    suspend fun addWatermarkVideo(
        videoUrl: String,
        watermarLogokUrl: String,
        watermarkEndVideoUrl: String,
        context: Context,
    ) {
        val videoPath = videoUrl
        val logoPath = watermarLogokUrl
        val videoEndPath = watermarkEndVideoUrl
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dateFormat = SimpleDateFormat("h_mma_ddMMMyyyy", Locale.getDefault())
        val outputFilePath = "$outputDir/vfunny_${dateFormat.format(Date())}.mp4"
        val outputFilePath2 = "$outputDir/vfunny2_${dateFormat.format(Date())}.mp4"
        val fileList = File.createTempFile("filelist", ".txt")
        val videoFile = File.createTempFile("videoFile", ".mp4")
        val videoEndFile = File.createTempFile("videoEndFile", ".mp4")
        val writer = fileList.bufferedWriter()
// Download the first video
        if (videoFile.exists()) {
            videoFile.delete()
        }
        val video1URL = URL(videoPath)
        Files.copy(video1URL.openStream(), videoFile.toPath())
// Download the second video
        if (videoEndFile.exists()) {
            videoEndFile.delete()
        }
        val video2URL = URL(videoEndPath)
        Files.copy(video2URL.openStream(), videoEndFile.toPath())
        // Write the paths to the video files
        writer.write("file '${videoFile.absolutePath}'")
        writer.newLine()
        writer.write("file '${videoEndFile.absolutePath}'")
        // Close the writer
        writer.close()

        val command =
            "-i $videoPath -i $logoPath -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.5:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" $outputFilePath"

        val command2 = "-f concat -safe 0 -i ${fileList.absolutePath} -c copy $outputFilePath2"

        try {
            //Switch to IO thread as we will save a video to storage
            withContext(Dispatchers.IO) {
                Log.e(TAG, "addWatermarkVideo: Please wait while saving video :>")
                //Execute the command
                FFmpegKit.executeAsync(command2) {
                    //Tell user if process is done successfully or not
                    if (ReturnCode.isSuccess(it.returnCode)) {
                        Log.e(TAG, "addWatermarkVideo: Video Saved Successfully :)")
                    } else {
                        Log.e(TAG, "addWatermarkVideo: Error happened while saving !!")
                    }
                    //Return to main thread to call the Clear function
                    (context as Activity).runOnUiThread(Runnable {
                        kotlin.run {
                            Log.e(TAG, "addWatermarkVideo: From UI Thread!! !")
                        }
                    })
                }
                //Execute the command
                FFmpegKit.executeAsync(command) {
                    //Tell user if process is done successfully or not
                    if (ReturnCode.isSuccess(it.returnCode)) {
                        Log.e(TAG, "addWatermarkVideo: Video Saved Successfully :)")
                    } else {
                        Log.e(TAG, "addWatermarkVideo: Error happened while saving !!")
                    }
                    //Return to main thread to call the Clear function
                    (context as Activity).runOnUiThread(Runnable {
                        kotlin.run {
                            Log.e(TAG, "addWatermarkVideo: From UI Thread!! !")
                        }
                    })
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(TAG, "addWatermarkVideo: Error in Convert Execution !")
        } finally {
            fileList.deleteOnExit()
            videoFile.deleteOnExit()
            videoEndFile.deleteOnExit()
        }
    }


    suspend fun addWatermarkVideo2(
        videoUrl: String,
        logoUrl: String,
        videoEndUrl: String,
        context: Context,
    ) {
        val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dateFormat = SimpleDateFormat("h_mma_ddMMMyyyy", Locale.getDefault())
        val outputFilePath = "$outputDir/vfunny_${dateFormat.format(Date())}.mp4"

        val videoLogoOutputFile = File.createTempFile("videoLogoOutputFile", ".mp4")
        val videoEndFile = File.createTempFile("videoEndFile", ".mp4")
// Download the first video
// Download the second video
        if (videoEndFile.exists()) {
            videoEndFile.delete()
        }
        if (videoLogoOutputFile.exists()) {
            videoLogoOutputFile.delete()
        }
        Files.copy(URL(videoEndUrl).openStream(), videoEndFile.toPath())
        // Write the paths to the video files
        val fileList = File.createTempFile("filelist", ".txt")
        val writer = fileList.bufferedWriter()
        writer.write("file '${videoLogoOutputFile}'")
        writer.newLine()
        writer.write("file '$videoEndFile'")
        writer.close()
        val command =
            "-i $videoUrl -i $logoUrl -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.5:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" ${videoLogoOutputFile.absolutePath}"

        val command2 = "-f concat -safe 0 -i $fileList -c copy $outputFilePath"

        try {
            //Switch to IO thread as we will save a video to storage
            withContext(Dispatchers.IO) {
                Log.e(TAG, "addWatermarkVideo: Please wait while saving video :>")
                //Execute the command
                FFmpegKit.executeAsync(command) { session ->
                    //Tell user if process is done successfully or not
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        Log.e(TAG, "Success Adding Logo")
                        FFmpegKit.executeAsync(command2) { session2 ->
                            //Tell user if process is done successfully or not
                            if (ReturnCode.isSuccess(session2.returnCode)) {
                                Log.e(TAG, "Success adding End Video")
                            } else {
                                Log.e(TAG, "Error adding End Video")
                            }
                            //Return to main thread to call the Clear function
                            (context as Activity).runOnUiThread(Runnable {
                                kotlin.run {
                                    Log.e(TAG, "addWatermarkVideo: From UI Thread!! !")
                                    fileList.deleteOnExit()
                                    videoLogoOutputFile.deleteOnExit()
                                    videoEndFile.deleteOnExit()
                                }
                            })
                        }
                    } else {
                        Log.e(TAG, "Error Adding Logo")
                        //Return to main thread to call the Clear function
                        (context as Activity).runOnUiThread(Runnable {
                            kotlin.run {
                                Log.e(TAG, "addWatermarkVideo: From UI Thread!! !")
                                fileList.deleteOnExit()
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