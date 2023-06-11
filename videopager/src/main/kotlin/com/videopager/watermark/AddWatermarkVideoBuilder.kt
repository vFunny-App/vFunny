package com.videopager.watermark

import android.app.Activity
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.videopager.DownloadWatermarkManager
import com.videopager.data.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AddWatermarkVideoBuilder(private val activity: Activity) : DownloadWatermarkManager() {
    private var videoUrl: String? = null
    private var progressListener: ProgressListener? = null
    private var page: Int = 0
    private var isCancelled: Boolean = false
    private lateinit var outputFilePath: File

    fun progressListener(progressListener: ProgressListener): AddWatermarkVideoBuilder {
        this.progressListener = progressListener
        return this
    }

    fun setPage(page: Int): AddWatermarkVideoBuilder {
        this.page = page
        return this
    }
    fun getOutputFilePath(): File {
        return outputFilePath
    }
    fun setVideoUrl(videoUrl: String): AddWatermarkVideoBuilder {
        this.videoUrl = videoUrl
        return this
    }

    fun cancel() {
        isCancelled = true
    }

    fun deleteOutput() {
        deleteAllTempFiles(activity.cacheDir)
    }

    suspend fun build() {
        try {
            var commandCount = 1
            val folder = activity.cacheDir
            outputFilePath = File(folder, "outputFilePath" + System.currentTimeMillis().toString() + ".mp4")
            val videoLogoOutputFile =
                File(folder, "videoLogoOutputFile" + System.currentTimeMillis().toString() + ".mp4")
            val videoEndFile =
                File(folder, "videoEndFile" + System.currentTimeMillis().toString() + ".mp4")

            if (isCancelled) {
                deleteTempFiles(folder)
                // Handle cancellation here, for example, cleanup resources
                return
            }

            withContext(Dispatchers.IO) {
                URL(WATERMARK_END_SOUND_3).openStream().use { inputStream ->
                    Files.copy(
                        inputStream,
                        videoEndFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }

            //Add logo to downloading video
            val command1 =
                "-i $videoUrl -i $WATERMARK_LOGO_LINK -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.5:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" -c:v libx264 -preset fast -crf 23 ${videoLogoOutputFile.absolutePath}"

            if (executeFfmpegCommand(command1, getDuration(videoUrl), commandCount)) {
                commandCount++
                val temp1 =
                    File(folder, "temp1" + System.currentTimeMillis().toString() + ".ts")
                val temp2 =
                    File(folder, "temp2" + System.currentTimeMillis().toString() + ".ts")
                //Convert downloaded logo video to .ts extension
                val command2 =
                    "-i ${videoLogoOutputFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp1.absolutePath}"
                if (executeFfmpegCommand(command2, getDuration(videoUrl), commandCount)) {
                    commandCount++
                    //Convert ending video to .ts extension
                    val command3 =
                        "-i ${videoEndFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp2.absolutePath}"
                    if (executeFfmpegCommand(
                            command3,
                            getDuration(videoUrl),
                            commandCount
                        )
                    ) {
                        commandCount++
                        //Join converted logo video and ending video together
                        val command4 =
                            "-i \"concat:${temp1.absolutePath}|${temp2.absolutePath}\" -s 1280x720 -c copy -bsf:a aac_adtstoasc $outputFilePath"
                        if (executeFfmpegCommand(
                                command4,
                                getDuration(videoUrl),
                                commandCount
                            )
                        ) {
                            progressListener?.onProgress(101, commandCount, null)
                        } else {
                            //if Join converted logo video and ending video together fails, output the converted log (without conversion)
                            val command4_back =
                                "-i ${videoLogoOutputFile.absolutePath} -c copy $outputFilePath"
                            if(executeFfmpegCommand(
                                command4_back,
                                getDuration(videoUrl),
                                commandCount
                                )){
                                    progressListener?.onProgress(101, commandCount, null)
                                }
                        }
                        deleteTempFiles(folder)
                    } else {
                        deleteTempFiles(folder)
                    }
                } else {
                    deleteTempFiles(folder)
                }
            } else {
                deleteTempFiles(folder)
            }
            videoLogoOutputFile.deleteOnExit()
            videoEndFile.deleteOnExit()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "addWatermarkVideo: Error in Convert Execution!")
        }
    }

    private suspend fun executeFfmpegCommand(
        exe: String,
        duration: Long,
        commandCount: Int,
    ): Boolean {
        return suspendCoroutine { continuation ->
            FFmpegKit.executeAsync(exe, { session ->
                val returnCode = session.returnCode
                val isSuccess = returnCode.isValueSuccess
                if (isSuccess) {
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }, { log ->
                progressListener?.onProgress(40, commandCount, log.message)
            }) { statistics ->
                val durationDone = statistics.time
                val percentageDone = (durationDone.toDouble() / duration.toDouble()) * 100
                progressListener?.onProgress(percentageDone.toInt(), commandCount, "Progress update")
                Log.d("@STATS", statistics.toString())
            }
        }
    }

    private fun deleteTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    deleteTempFiles(it)
                } else {
                    if(!it.nameWithoutExtension.startsWith("outputFilePath"))
                    it.delete()
                }
            }
        }
        return file.delete()
    }

    private fun deleteAllTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                if (it.isDirectory) {
                    deleteTempFiles(it)
                } else {
                    it.delete()
                }
            }
        }
        return file.delete()
    }

}
