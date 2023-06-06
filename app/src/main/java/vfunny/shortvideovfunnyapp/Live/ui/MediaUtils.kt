package vfunny.shortvideovfunnyapp.Live.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.videopager.PostManager.Companion.instance
import vfunny.shortvideovfunnyapp.Post.model.Language
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * Created on 26/05/2017.
 * Copyright by Shresthasaurabh86@gmail.com
 */
object MediaUtils {
    const val REQUEST_VIDEO_PICK = 1001
    private const val PERMISSIONS_REQUEST_STORAGE = 1002
    private var currentItemIndex: Int = 0


    fun storagePermissionGrant(activity: Activity?): Boolean {
        return if (ContextCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // request the permission
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_STORAGE
            )
            false
        } else {
            true
        }
    }

    fun openVideoLibrary(activity: Activity, requestCode: Int): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            intent.type = "video/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.startActivityForResult(
                Intent.createChooser(intent, "Select video(s)"),
                requestCode
            )
            true
        } catch (e: Error) {
            Log.e("TAG", "addClick: getPackageManager null")
            Log.e("TAG", "addClick: ERROR $e")
            false
        }
    }

    fun uploadPhoto(filePath: Uri?, language: Language?, context: Context) {
        if (filePath != null) {
            val videoPath = getFilePathFromContentUri(filePath, context.contentResolver) ?: return
            instance.encodeHLS(context, videoPath, language!!, 0)
        }
    }

    fun uploadMultiplePhoto(uriList: List<Uri?>, language: Language?, context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        Log.e("TAG", "uploadPhoto: userId : $userId")
        val itemCount = uriList.size
        for (i in 0 until itemCount) {
            val filePath = uriList[i]
            val videoPath = getFilePathFromContentUri(filePath, context.contentResolver) ?: return
            instance.encodeHLS(context, videoPath, language!!, i)
        }
    }


    fun uploadMultiplePhoto(
        uriList: List<Uri>,
        language: Language,
        context: Context,
        completionCallback: () -> Unit
    ) {
        if (currentItemIndex >= uriList.size) {
            // All items processed, invoke the completion callback
            completionCallback()
            return
        }

        val filePath = uriList[currentItemIndex]
        val videoPath = getFilePathFromContentUri(filePath, context.contentResolver)
        if (videoPath == null) {
            // Skip the current item if the video path is null
            currentItemIndex++
            uploadMultiplePhoto(uriList, language, context, completionCallback)
            return
        }

        instance.encodeHLS(context, videoPath, language, currentItemIndex)
            .thenAccept {
                currentItemIndex++
                uploadMultiplePhoto(uriList, language, context, completionCallback)
            }
    }

    private fun createTempOutputDirectory(): File? {
        try {
            val tempDir = File.createTempFile("temp_", java.lang.Long.toString(System.nanoTime()))
            if (tempDir.delete() && tempDir.mkdirs()) {
                return tempDir
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun getFilePathFromContentUri(contentUri: Uri?, contentResolver: ContentResolver): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = contentResolver.query(contentUri!!, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val filePath = cursor.getString(columnIndex)
            cursor.close()
            return filePath
        }
        return null
    }

    private fun showRetryDialog(context: Context, failedUris: List<Uri?>, language: Language) {
        Log.e("TAG", "Failed to upload file: showRetryDialog starting")
        MaterialAlertDialogBuilder(context).setTitle("Retry Failed Uploads?")
            .setMessage("Some files failed to upload. Do you want to retry uploading them?")
            .setPositiveButton("OK") { dialog: DialogInterface, which: Int ->
                uploadMultiplePhoto(failedUris, language, context)
                dialog.dismiss()
            }.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
                showFileNamesDialog(context, failedUris)
                dialog.dismiss()
            }.show()
    }

    fun showFileNamesDialog(context: Context, uriList: List<Uri?>) {
        val stringBuilder = StringBuilder()
        for (uri in uriList) {
            stringBuilder.append(getAbsolutePath(uri, context)).append("\n")
        }
        MaterialAlertDialogBuilder(context).setTitle("Failed File Names")
            .setMessage(stringBuilder.toString())
            .setPositiveButton("COPY TO CLIPBOARD") { dialog: DialogInterface?, which: Int ->
                // Copy all the file names to clipboard
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("File Names", stringBuilder.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "File names copied to clipboard", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Exit") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            .show()
    }

    private fun createThumbnailUploadTask(filePath: Uri?, context: Context?): UploadTask? {
        if (filePath == null) {
            Log.e("TAG", "createThumbnailUploadTask: null filepath")
        }
        if (context == null) {
            Log.e("TAG", "createThumbnailUploadTask: null context")
        }
        val thumbnailPath = getAbsolutePath(filePath, context)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(thumbnailPath)
        val imageBitmap = retriever.getFrameAtTime(0)
        if (imageBitmap != null) {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val imgref =
                storageRef.child(userId + "/ac" + System.currentTimeMillis() + ".jpg")
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            val metadata =
                StorageMetadata.Builder().setContentType("image/jpg").build()
            return imgref.putBytes(bytes, metadata)
        }
        return null
    }

    private fun getAbsolutePath(uri: Uri?, context: Context?): String {
        val filePathColumn = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = context!!.contentResolver.query(uri!!, filePathColumn, null, null, null)
        cursor?.moveToFirst()
        val columnIndex = cursor!!.getColumnIndex(filePathColumn[0])
        return cursor.getString(columnIndex)
    }
}