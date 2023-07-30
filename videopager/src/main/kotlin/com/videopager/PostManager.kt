package com.videopager

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.player.models.VideoData
import com.videopager.models.Post
import vfunny.shortvideovfunnyapp.Post.model.Language
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CompletableFuture


/**
 * Created by shresthasaurabh86@gmail.com 09/05/2023.
 */
class PostManager {

    lateinit var progressDialog: ProgressDialog

    companion object {
        private const val TAG: String = "PostManager"

        @JvmStatic
        val instance: PostManager by lazy { PostManager() }
    }


    fun showProgressDialog(context: Context, message: String) {
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle(message)
        progressDialog.show()
    }

    fun deleteFileFromStorage(context: Context, storageRef: StorageReference?) {
        val fileSegments = storageRef?.toString()?.split("/")
        if (fileSegments != null) {
            if (fileSegments.size == 2) {
                val folderName = fileSegments[0]
                val fileName = fileSegments[1]
                FirebaseStorage.getInstance().reference.child(folderName).child(fileName).delete()
                    .addOnFailureListener { e ->
                        handleDeleteFailure(context, e)
                    }
            }
        }
    }

    private fun handleDeleteFailure(context: Context, exception: Exception) {
        Log.e(TAG, "Delete failed: ${exception.message}")
        showToast(context, "Failed: ${exception.message}")
    }

    fun deletePostReferenceFromDatabase(context: Context, videoData: VideoData) {
        val databaseRef =
            FirebaseDatabase.getInstance().getReference("posts_${videoData.language!!.code}")
        databaseRef.child(videoData.key!!).removeValue().addOnSuccessListener {
            showToast(context, "Deleted")
        }.addOnFailureListener {
            Log.e(TAG, "Error removing post reference: $it")
            showToast(context, "Something went wrong removing post reference")
        }
    }

    fun deletePostReferenceFromDatabase(context: Context, videoData: Post) {
        val databaseRef =
            FirebaseDatabase.getInstance().getReference("posts_${videoData.language!!.code}")
        databaseRef.child(videoData.key!!).removeValue().addOnSuccessListener {
            showToast(context, "Deleted")
        }.addOnFailureListener {
            Log.e(TAG, "Error removing post reference: $it")
            showToast(context, "Something went wrong removing post reference")
        }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    fun dismissProgressDialog(progressDialog: DialogInterface?) {
        progressDialog?.dismiss()
        if (this.progressDialog.isShowing) {
            this.progressDialog.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun encodeHLS(
        context: Context,
        videoFile: String,
        language: Language,
        videoIndex: Int
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()

        Log.e(TAG, "Uploading . . . Item: $videoIndex")
        showProgressDialog(context, "Uploading . . . Item: $videoIndex")

        val tempDir = File(context.cacheDir, "temp_hls_$videoIndex")
        val outDirPath = tempDir.absolutePath
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dateTimeString = dateFormat.format(Date(currentTimeMillis))

        val masterName = "${dateTimeString}_$videoIndex"
        val playlistFileName = "$masterName.m3u8"
        val playlistFilePath = "$outDirPath/$playlistFileName"
        val storageRef = FirebaseStorage.getInstance().reference.child("stream/$language/$masterName/")

        tempDir.mkdirs()

        val arguments = "-y -i $videoFile" +
                " -preset ultrafast -g 48 -sc_threshold 0 " +
                "-map 0:0 -map 0:1 " +
                "-c:0 libx264 -b:0 1000k " +
                "-c:a copy " +
                "-var_stream_map 'v:0,a:0' " +
                "-f hls -hls_time 5 -hls_list_size 0 " +
                "-hls_segment_filename '$outDirPath/${masterName}_%d.ts' " +
                "-hls_playlist_type vod " +
                "$playlistFilePath"

        FFmpegKit.executeAsync(arguments) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                val itemCount = tempDir.listFiles()?.size
                if (itemCount == null || itemCount < 1) {
                    future.complete(Unit)
                    return@executeAsync
                }

                val tsFiles = tempDir.listFiles { file -> file.extension == "ts" }

                val downloadUrls = mutableListOf<String>()

                val uploadTasks = mutableListOf<CompletableFuture<Unit>>()

                tsFiles?.forEach { tsFile ->
                    val tsRef = storageRef.child(tsFile.name)
                    val tsFileUri = Uri.fromFile(tsFile)

                    val uploadTask = CompletableFuture<Unit>()

                    tsRef.putFile(tsFileUri)
                        .addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.metadata!!
                                .reference!!
                                .downloadUrl
                                .addOnCompleteListener { tsURL: Task<Uri> ->
                                    downloadUrls.add(tsURL.result.toString())
                                    if (downloadUrls.size == tsFiles.size) {
                                        // Update the playlist file with the new URLs
                                        updatePlaylistFile(
                                            playlistFilePath,
                                            downloadUrls,
                                            storageRef,
                                            videoFile,
                                            language,
                                            masterName,
                                            tempDir
                                        ).thenAccept {
                                            future.complete(Unit)
                                        }
                                    }
                                }
                            // Check if all files are uploaded and URLs are fetched
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "execute: $exception")
                            // Handle upload failure
                            future.completeExceptionally(exception)
                        }

                    uploadTasks.add(uploadTask)
                }

                CompletableFuture.allOf(*uploadTasks.toTypedArray())
                    .exceptionally { exception ->
                        future.completeExceptionally(exception)
                        null
                    }
            } else {
                future.completeExceptionally(Exception("FFmpeg execution failed"))
            }
        }

        return future
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun updatePlaylistFile(
        playlistFilePath: String,
        downloadUrls: List<String>,
        storageRef: StorageReference,
        videoFile: String,
        language: Language,
        masterName: String,
        tempDir: File
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()

        val playlistFile = File(playlistFilePath)
        val updatedPlaylistLines = mutableListOf<String>()

        try {
            val reader = BufferedReader(FileReader(playlistFile))
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.trim().endsWith(".ts")) {
                        val fileName = line.substringAfterLast("/")
                        val matchingUrl = downloadUrls.find { url ->
                            url.contains(fileName)
                        }
                        if (matchingUrl != null) {
                            val updatedLine = line.replace(line, matchingUrl)
                            updatedPlaylistLines.add(updatedLine)
                        } else {
                            updatedPlaylistLines.add(line)
                        }
                    } else {
                        updatedPlaylistLines.add(line)
                    }
                }
            }

            val writer = FileWriter(playlistFile)
            updatedPlaylistLines.forEach { updatedLine ->
                writer.write(updatedLine)
                writer.write("\n")
            }
            writer.close()

            val m3u8Ref = storageRef.child(playlistFile.name)
            val m3u8FileUri = Uri.fromFile(playlistFile)

            m3u8Ref.putFile(m3u8FileUri)
                .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    tempDir.deleteRecursively()
                    taskSnapshot.metadata!!
                        .reference!!
                        .downloadUrl
                        .addOnCompleteListener { videoUri: Task<Uri> ->
                            createThumbnailUploadTask(videoFile, language, masterName)
                                .addOnSuccessListener { thumbnailSnapshot: UploadTask.TaskSnapshot ->
                                    thumbnailSnapshot.metadata!!
                                        .reference!!.downloadUrl.addOnCompleteListener { thumbnailUri: Task<Uri> ->
                                            uploadPost(
                                                videoUri.result.toString(),
                                                thumbnailUri.result.toString(),
                                                language
                                            )
                                            future.complete(Unit)
                                        }
                                }
                        }
                }
                .addOnFailureListener {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    tempDir.deleteRecursively()
                    future.completeExceptionally(it)
                    Log.e(TAG, "execute: $it")
                }
                .addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->
                    val progress =
                        100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                    progressDialog.setMessage("Uploading file: " + progress.toInt() + "%")
                }
        } catch (e: IOException) {
            e.printStackTrace()
            future.completeExceptionally(e)
        }

        return future
    }
    private fun createThumbnailUploadTask(
        filePath: String,
        language: Language,
        masterName: String
    ): UploadTask {
        val thumbnailPath: String = filePath
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(thumbnailPath)
        val imageBitmap = retriever.getFrameAtTime(0)
        val imgref =
            FirebaseStorage.getInstance().reference.child("thumbs/${language.name}/$masterName.jpg")
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()
        val metadata =
            StorageMetadata.Builder().setContentType("image/jpg").build()
        return imgref.putBytes(bytes, metadata)
    }

    fun uploadPost(videoUrl: String, thumbnail: String, language: Language) {
        // Create a new Post object with the provided video URL, thumbnail URL, and current timestamp
        val newPost =
            Post(image = thumbnail, video = videoUrl, timestamp = System.currentTimeMillis())
        // Get a reference to the posts node in the Realtime Database for the specified language
        FirebaseDatabase.getInstance().getReference("posts_${language.code}")
            // Push the new post to the Realtime Database
            .push().setValue(newPost)
    }
}