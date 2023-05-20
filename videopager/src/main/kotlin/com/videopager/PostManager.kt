package com.videopager

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.player.models.VideoData

/**
 * Created by shresthasaurabh86@gmail.com 09/05/2023.
 */
class PostManager {

    lateinit var progressDialog : ProgressDialog

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

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    fun dismissProgressDialog(progressDialog: DialogInterface?) {
        progressDialog?.dismiss()
        if(this.progressDialog.isShowing) {
            this.progressDialog.dismiss()
        }
    }

}