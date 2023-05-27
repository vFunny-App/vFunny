package com.videopager.watermark

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.videopager.DownloadWatermarkManager
import com.videopager.data.ProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.CoroutineContext


class AddWatermarkVideoBuilder(private val videoUrl: String) : DownloadWatermarkManager() {
    private var progressListener: ProgressListener? = null

    fun progressListener(progressListener: ProgressListener): AddWatermarkVideoBuilder {
        this.progressListener = progressListener
        return this
    }

    fun build() {
        val logoUrl = WATERMARK_LOGO_LINK
        val videoEndUrl = WATERMARK_END_SOUND_3
        val outputFilePath = createOutputFile()
        try {
            Log.d(TAG, "addWatermarkVideo2: Download Started!")

            val videoLogoOutputFile = File.createTempFile("videoLogoOutputFile", ".mp4")
                .apply { delete() }
            val videoEndFile = File.createTempFile("videoEndFile", ".mp4").apply { delete() }

            URL(videoEndUrl).openStream().use { inputStream ->
                Files.copy(
                    inputStream,
                    videoEndFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            val commandToConcatVideoAndEnd =
                "-i $videoUrl -i $logoUrl -filter_complex \"[1]colorchannelmixer=aa=1,format=rgba,colorchannelmixer=aa=1,scale=iw*0.5:-1[a];[0][a]overlay=x='if(lt(mod(t\\,24)\\,12)\\,W-w-W*10/100\\,W*10/100)':y='if(lt(mod(t+6\\,24)\\,12)\\,H-h-H*5/100\\,H*5/100)'\" -c:v libx264 -preset fast -crf 23 ${videoLogoOutputFile.absolutePath}"

            val tempVideoLogoOutputFile = File.createTempFile("temp1", ".ts").apply { delete() }
            val temp2 = File.createTempFile("temp2", ".ts").apply { delete() }

            val command2 =
                "-i ${videoLogoOutputFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${tempVideoLogoOutputFile.absolutePath}"
            val command3 =
                "-i ${videoEndFile.absolutePath} -c:v libx264 -preset fast -crf 23 -c copy -f mpegts ${temp2.absolutePath}"
            val command4 =
                "-i \"concat:${tempVideoLogoOutputFile.absolutePath}|${temp2.absolutePath}\" -s 1280x720 -c copy -bsf:a aac_adtstoasc $outputFilePath"

            FFmpegKit.executeAsync(commandToConcatVideoAndEnd) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    progressListener?.onProgress(0,25) // Update progress to 25%

                    FFmpegKit.executeAsync(command2) { session2 ->
                        if (ReturnCode.isSuccess(session2.returnCode)) {
                            progressListener?.onProgress(0,50) // Update progress to 50%

                            FFmpegKit.executeAsync(command3) { session3 ->
                                if (ReturnCode.isSuccess(session3.returnCode)) {
                                    progressListener?.onProgress(0,75) // Update progress to 75%

                                    FFmpegKit.executeAsync(command4) { session4 ->
                                        if (ReturnCode.isSuccess(session4.returnCode)) {
                                            progressListener?.onProgress(0,100) // Update progress to 100%
                                            Log.d(
                                                TAG,
                                                "addWatermarkVideo2: Download Finished!",
                                            )
                                        } else {
                                            if (File(outputFilePath).exists()) {
                                                File(outputFilePath).delete()
                                            }
                                            val command4_back =
                                                "-i ${videoLogoOutputFile.absolutePath} -c copy $outputFilePath"
                                            FFmpegKit.executeAsync(command4_back) { command4back ->
                                                if (ReturnCode.isSuccess(command4back.returnCode)) {
                                                    progressListener?.onProgress(0,100) // Update progress to 100%
                                                    Log.d(
                                                        TAG,
                                                        "addWatermarkVideo2: Download Finished!!",
                                                    )
                                                } else {
                                                    Log.e(
                                                        TAG,
                                                        "addWatermarkVideo2: Error Merging Logo Video",
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(
                                        TAG,
                                        "addWatermarkVideo2: Error Merging End Video!",
                                    )
                                }
                            }
                        } else {
                            Log.e(
                                TAG,
                                "addWatermarkVideo2: Error temping First Video!",
                            )
                        }
                    }
                } else {
                    Log.e(TAG, "addWatermarkVideo2: Error Adding Logo!")
                }
            }

            videoLogoOutputFile.deleteOnExit()
            videoEndFile.deleteOnExit()
            tempVideoLogoOutputFile.deleteOnExit()
            temp2.deleteOnExit()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "addWatermarkVideo: Error in Convert Execution!")
        }
    }
}
