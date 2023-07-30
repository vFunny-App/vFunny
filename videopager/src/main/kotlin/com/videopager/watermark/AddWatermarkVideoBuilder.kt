package com.videopager.watermark

import android.os.Build
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.videopager.DownloadWatermarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AddWatermarkVideoBuilder : DownloadWatermarkManager() {
    private var videoUrl: String? = null
    private var page: Int = 0
    private var isCancelled: Boolean = false
    private lateinit var outputFilePath: File
    private var onCompletionListener: ((File) -> Unit)? = null
    private var onProgressListener: ((Int) -> Unit)? = null

    fun onCompletionListener(listener: (File) -> Unit): AddWatermarkVideoBuilder {
        this.onCompletionListener = listener
        return this
    }

    fun onProgressListener(progressListener: (Int) -> Unit): AddWatermarkVideoBuilder {
        this.onProgressListener = progressListener
        return this
    }

    fun setPage(page: Int): AddWatermarkVideoBuilder {
        this.page = page
        return this
    }

    fun setVideoUrl(videoUrl: String): AddWatermarkVideoBuilder {
        this.videoUrl = videoUrl
        return this
    }

    fun cancel() {
        isCancelled = true
    }

    fun reset() {
        isCancelled = false
    }

    suspend fun build() {
        try {
            var commandCount = 1
            outputFilePath = withContext(Dispatchers.IO) {
                File.createTempFile(
                    "outputFilePath" + System.currentTimeMillis().toString(),
                    ".mp4"
                ).also { it.delete() }
            }
            val videoLogoOutputFile =
                withContext(Dispatchers.IO) {
                    File.createTempFile(
                        "videoLogoOutputFile" + System.currentTimeMillis().toString(),
                        ".mp4"
                    ).also { it.delete() }
                }
            val videoEndFile =
                withContext(Dispatchers.IO) {
                    File.createTempFile(
                        "videoEndFile" + System.currentTimeMillis().toString(),
                        ".mp4"
                    ).also { it.delete() }
                }
            val temp1 =
                withContext(Dispatchers.IO) {
                    File.createTempFile("temp1" + System.currentTimeMillis().toString(), ".ts")
                        .also { it.delete() }
                }
            val temp2 =
                withContext(Dispatchers.IO) {
                    File.createTempFile("temp2" + System.currentTimeMillis().toString(), ".ts")
                        .also { it.delete() }
                }
            if (isCancelled) {
                return
            }
            GlobalScope.launch(Dispatchers.Main) {
                onProgressListener?.invoke(0)
            }
            withContext(Dispatchers.IO) {
                URL(WATERMARK_END_SOUND_3).openStream().use { inputStream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Files.copy(
                            inputStream,
                            videoEndFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    } else {
                        val outputFile = FileOutputStream(videoEndFile)
                        outputFile.use { outFile ->
                            val buffer = ByteArray(4096)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outFile.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }
            }
            //Add logo to downloading video
            val command1 =
                "-i $videoUrl -i $WATERMARK_LOGO_LINK -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.4:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" -c:v libx264 -preset fast -crf 23 ${videoLogoOutputFile.absolutePath}"

            if (executeFfmpegCommand(command1, getDuration(videoUrl), commandCount)) {
                commandCount++
                //Convert downloaded logo video to .ts extension
                val command2 =
                    "-i ${videoLogoOutputFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp1.absolutePath}"
                if (executeFfmpegCommand(
                        command2,
                        getDuration(videoLogoOutputFile.absolutePath),
                        commandCount
                    )
                ) {
                    commandCount++
                    //Convert ending video to .ts extension
                    val command3 =
                        "-i ${videoEndFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp2.absolutePath}"
                    if (executeFfmpegCommand(
                            command3,
                            getDuration(videoEndFile.absolutePath),
                            commandCount
                        )
                    ) {
                        commandCount++
                        //Join converted logo video and ending video together
                        val command4 =
                            "-i \"concat:${temp1.absolutePath}|${temp2.absolutePath}\" -s 1280x720 -c copy -bsf:a aac_adtstoasc $outputFilePath"
                        if (executeFfmpegCommand(
                                command4,
                                getDuration(temp1.absolutePath) + getDuration(temp2.absolutePath),
                                commandCount
                            )
                        ) {
                            GlobalScope.launch(Dispatchers.Main) {
                                onCompletionListener?.invoke(outputFilePath)
                            }
                        } else {
                            //if Join converted logo video and ending video together fails, output the converted log (without conversion)
                            val command4_back =
                                "-i ${videoLogoOutputFile.absolutePath} -c copy $outputFilePath"
                            if (executeFfmpegCommand(
                                    command4_back,
                                    getDuration(videoLogoOutputFile.absolutePath),
                                    commandCount
                                )
                            ) {
                                onCompletionListener?.invoke(outputFilePath)
                            }
                        }
                    }
                }
                temp1.deleteOnExit()
                temp2.deleteOnExit()
            }
            videoLogoOutputFile.deleteOnExit()
            videoEndFile.deleteOnExit()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "addWatermarkVideo: Error in Convert Execution!")
        }
        Log.e(TAG, "addWatermarkVideo: HELLOO")
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
            }, {
            }) { statistics ->
//                val durationDone = statistics.time
//                val percentageDone = (durationDone.toDouble() / duration.toDouble()) * 100
//                Log.d(
//                    "@STATS", "durationDone : $durationDone \n" +
//                            "duration : $duration \n" +
//                            "videoFrameNumber : ${statistics.videoFrameNumber} \n" +
//                            "percentageDone : $percentageDone"
//                )
//                GlobalScope.launch(Dispatchers.Main) {
//                    onProgressListener?.invoke(percentageDone.toInt())
//                }
            }
        }
    }
}
