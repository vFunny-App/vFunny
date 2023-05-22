package vfunny.shortvideovfunnyapp.Live.ui.migrate

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.ImageLoader
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import vfunny.shortvideovfunnyapp.Post.model.Language
import com.videopager.models.Post
import vfunny.shortvideovfunnyapp.R
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created on 13/05/2023.
 * Copyright by Shresthasaurabh86@gmail.com
 */

class MigratePostAdapter(
    var context: Context,
    private val imageLoader: ImageLoader,
    private val itemWidth: Int,
    private val mData: ArrayList<Post>,
) : RecyclerView.Adapter<MigratePostAdapter.MigratePostViewHolder>() {

    companion object {
        private val simpleDateFormat = SimpleDateFormat("d/M/yy h:mm a", Locale.ENGLISH)
        private const val TAG: String = "LISTING"
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int,
    ): MigratePostViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.migrate_item_list, viewGroup, false)
        return MigratePostViewHolder(view, imageLoader)
    }


    override fun onBindViewHolder(holder: MigratePostViewHolder, position: Int) {
        val currentPost = mData[position]
        Log.e(TAG, "onBindViewHolder: position $position == ${currentPost.key}")
        holder.setItem(currentPost)
        // Set LayoutParams to adjust item width
        val layoutParams = holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
        layoutParams.width = itemWidth // divide by number of columns
        holder.itemView.layoutParams = layoutParams
        holder.itemView.setOnClickListener {
            val postRef = currentPost.key?.let { it1 ->
                FirebaseDatabase.getInstance().getReference("posts").child(it1)
            }
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_migrate_video, null)
            val videoView = dialogView.findViewById<PlayerView>(R.id.item_player_view)
            val dateTextView = dialogView.findViewById<TextView>(R.id.date_tv)
            val migrateBtn = dialogView.findViewById<Button>(R.id.migrate_btn)
            currentPost.timestamp?.let {
                try {
                    if (it is Long) {
                        dateTextView.text = "Time : ${simpleDateFormat.format(0 - it)}"
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error getting date $e", Toast.LENGTH_SHORT).show()
                }
            }

            if (currentPost.video == null || currentPost.image == null) {
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
                val dialog = AlertDialog.Builder(context).setView(dialogView)
                    .setPositiveButton("Delete") { dialog, _ ->
                        deleteItem(currentPost, postRef, position, context = context)
                        dialog.dismiss()
                    }.setNegativeButton("Cancel", null).create()
                dialog.show()
                return@setOnClickListener
            }
            // Set the video URI
            val player = SimpleExoPlayer.Builder(context).build()
            videoView.player = player
            val videoUri = Uri.parse(currentPost.video)
            val mediaItem = MediaItem.fromUri(videoUri)
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(context))
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
            migrateBtn.setOnClickListener {
                showListDialog(context, currentPost, position)
            }
            // Show the dialog
            val dialog = AlertDialog.Builder(context).setView(dialogView)
                .setPositiveButton("Delete") { dialog, _ ->
                    deleteItem(currentPost, postRef, position, context)
                    player.release()
                    dialog.dismiss()
                }.setNegativeButton("Cancel") { dialog, _ ->
                    player.release()
                    dialog.dismiss()
                }.create()
            dialog.show()
        }
    }

    private fun showListDialog(context: Context, post: Post, adapterPostion: Int) {
        val langList = Language.getAllLanguages()
        val listItems = langList.map { it.name }.toTypedArray()

        val mBuilder = AlertDialog.Builder(context)
        mBuilder.setTitle("Choose a language")
        mBuilder.setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
            showConfirmationDialog(context, langList[i], post, adapterPostion, dialogInterface)
        }
        mBuilder.setNeutralButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        val mDialog = mBuilder.create()
        mDialog.show()
    }

    private fun showConfirmationDialog(
        context: Context,
        item: Language,
        post: Post,
        adapterPostion: Int,
        dialogInterface: DialogInterface,
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Are you sure you want to set Language to ${item.name}?")
            .setCancelable(false).setPositiveButton("Yes") { dialog, _ ->
                migratePostToLang(context, post, item, adapterPostion)
                dialog.dismiss()
                dialogInterface.dismiss()
            }.setNegativeButton("No") { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }


    /**
     * * It gets references to the RTDB and RTDB posts.
     * *  It creates a ProgressDialog to show during the migration process.
     * * It adds the post data to the RTDB collection using the add method. This returns a Task object that can be used to listen for success or failure events.
     * * On success, it updates the "migrated" field in the RTDB post using the updateChildren method. This also returns a Task object that can be used to listen for success or failure events.
     * * On success of updating the RTDB post, it dismisses the progress dialog and shows a success message.
     * * On failure at any step, it dismisses the progress dialog and shows an error message.
     */
    private fun migratePostToLang(
        context: Context,
        post: Post,
        language: Language,
        adapterPostion: Int,
    ) {
        // Get references to the ORIGINAL RTDB and NEW LANGUAGE RTDB posts
        Log.e(TAG, "post: $post")
        Log.e(TAG, "language: $language")
        Log.e(TAG, "adapterPostion: $adapterPostion")
        val rtdbPostRef =
            post.key?.let { FirebaseDatabase.getInstance().reference.child("posts").child(it) }
        val firestorePostRef =
            FirebaseDatabase.getInstance().reference.child("posts_${language.code}")

        // Create a progress dialog to show during the migration process
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Migrating post...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Add the post data to Firestore
        val post2 = post.copy()
        post2.timestamp
        post2.key = null
        if (post2.timestamp is Long && (post2.timestamp as Long) < 0) post2.timestamp =
            (0 - (post2.timestamp as Long))
        else {
            post2.timestamp = System.currentTimeMillis()
        }
        Log.e(TAG, "migratePostFromRtdbToFirestore: $post2")
        firestorePostRef.push().setValue(post2).addOnSuccessListener {
                Log.e(TAG, "migratePostFromRtdbToFirestore: $it")
                // On success, update the "migrated" field in the RTDB post and remove the post from the adapter
                rtdbPostRef?.updateChildren(mapOf("migrated" to true))?.addOnSuccessListener {
                        Log.e(TAG, "migratePostFromRtdbToFirestore: $it")
                        // On success, dismiss the progress dialog and show a success message
                        progressDialog.dismiss()
                        if (adapterPostion < mData.size) {
                            mData.removeAt(adapterPostion)
                            notifyItemRemoved(adapterPostion)
                        } else {
                            Toast.makeText(context,
                                "$adapterPostion ! ${mData.size}",
                                Toast.LENGTH_LONG).show()
                        }
                        Toast.makeText(context, "Post migrated successfully!", Toast.LENGTH_SHORT)
                            .show()
                    }?.addOnFailureListener { e ->
                        // On failure, dismiss the progress dialog and show an error message
                        progressDialog.dismiss()
                        Toast.makeText(context,
                            "Failed to update RTDB post: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                // On failure, dismiss the progress dialog and show an error message
                progressDialog.dismiss()
                Toast.makeText(context,
                    "Failed to migrate post to Firestore: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteItem(
        videoData: Post,
        postRef: DatabaseReference?,
        viewPosition: Int,
        context: Context,
    ) {
        val builder = AlertDialog.Builder(context)
        val storage = FirebaseStorage.getInstance()
        val storageReference = FirebaseStorage.getInstance().reference
        val thumbnailRef = videoData.video?.let { storage.getReferenceFromUrl(it) }
        val videoRef = videoData.image?.let { storage.getReferenceFromUrl(it) }
        builder.setCancelable(false)
        builder.setMessage("Do you want to delete this item?")
            .setPositiveButton("Yes") { dialog, id ->
                // User clicked Yes button
                val progressDialog = ProgressDialog(context)
                progressDialog.setTitle("Deleting...")
                progressDialog.show()
                if (thumbnailRef != null && thumbnailRef.toString().contains("/")) {
                    val thumbnailSegments = thumbnailRef.toString().split("/")
                    if (thumbnailSegments.size == 2) {
                        val thumbnailFolderName = thumbnailSegments[0]
                        val thumbnailFileName = thumbnailSegments[1]
                        storageReference.child(thumbnailFolderName).child(thumbnailFileName)
                            .delete().addOnFailureListener { e: java.lang.Exception ->
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                                Toast.makeText(context, "Failed " + e.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                }
                if (videoRef != null && videoRef.toString().contains("/")) {
                    val videoSegments = videoRef.toString().split("/")
                    if (videoSegments.size == 2) {
                        val videoFolderName = videoSegments[0]
                        val videoFileName = videoSegments[1]
                        storageReference.child(videoFolderName).child(videoFileName).delete()
                            .addOnFailureListener { e ->
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                                Toast.makeText(context, "Failed " + e.message, Toast.LENGTH_SHORT)
                                    .show()
                                Log.e("TAG", "Failed : " + e.message)
                            }
                    }
                }
                if (postRef != null) {
                    postRef.removeValue().addOnSuccessListener {
                        notifyItemRemoved(viewPosition)
                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Log.e(TAG, "deleteItem: $postRef")
                        Log.e(TAG, "deleteItem: $it")
                        Toast.makeText(context,
                            "Something went wrong removing post reference",
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context,
                        "Something went wrong removing post null reference",
                        Toast.LENGTH_SHORT).show()
                }
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }.setNegativeButton("No") { _, _ ->
                // User cancelled the dialog
                Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        builder.create().show()
    }

    override fun getItemCount(): Int {
        return mData.size
    }


    inner class MigratePostViewHolder(itemView: View, imageLoader: ImageLoader) :
        RecyclerView.ViewHolder(itemView) {
        var previewImage: ImageView
        var imageLoader: ImageLoader

        init {
            previewImage = itemView.findViewById(R.id.preview_image)
            this.imageLoader = imageLoader
        }

        fun setItem(post: Post) {
            // Load the image using Coil
            previewImage.load(post.image, imageLoader) {
                precision(Precision.EXACT)
                scale(Scale.FIT)
                placeholder(R.drawable.logo)
                error(R.drawable.btn_bg_danger)
            }
        }
    }
}