package vfunny.shortvideovfunnyapp.Live.ui

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import vfunny.shortvideovfunnyapp.Post.model.Post
import vfunny.shortvideovfunnyapp.R
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    var context: Context,
    private val imageLoader: ImageLoader,
    private val itemWidth: Int,
    private val mData: ArrayList<Post>
) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    companion object {
        private val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
        private const val TAG: String = "LISTING"
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int,
    ): PostViewHolder {
        val view =
            LayoutInflater.from(viewGroup.context).inflate(R.layout.item_list, viewGroup, false)
        return PostViewHolder(view, imageLoader)
    }


    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        Log.e(TAG, "onBindViewHolder: position $position == ${mData[position].timestamp}")
        Log.e(TAG, "onBindViewHolder: $position == ${mData[position].image}")
        holder.setItem(mData[position])
        // Set LayoutParams to adjust item width
        val layoutParams = holder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
        layoutParams.width = itemWidth // divide by number of columns
        holder.itemView.layoutParams = layoutParams
        holder.itemView.setOnClickListener {
            val postRef =
                mData[position].key?.let { it1 ->
                    FirebaseDatabase.getInstance().getReference("posts").child(it1)
                }
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_video, null)
            val videoView = dialogView.findViewById<PlayerView>(R.id.item_player_view)
            val dateTextView = dialogView.findViewById<TextView>(R.id.date_tv)
            mData[position].timestamp?.let {
                try {
                    if(it is Long) {
                        dateTextView.text = "Time : ${simpleDateFormat.format(0 - it)}"
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error getting date $e", Toast.LENGTH_SHORT).show()
                }
            }

            if (mData[position].video == null || mData[position].image == null) {
                Toast.makeText(
                    context,
                    "Something went wrong",
                    Toast.LENGTH_SHORT
                ).show()
                val dialog = AlertDialog.Builder(context).setView(dialogView)
                    .setPositiveButton("Delete") { dialog, _ ->
                        deleteItem(mData[position], postRef, position, context = context)
                        dialog.dismiss()
                    }.setNegativeButton("Cancel", null).create()
                dialog.show()
                return@setOnClickListener
            }
            // Set the video URI
            val player = SimpleExoPlayer.Builder(context).build()
            videoView.player = player
            val videoUri = Uri.parse(mData[position].video)
            val mediaItem = MediaItem.fromUri(videoUri)
            val mediaSource =
                ProgressiveMediaSource.Factory(DefaultDataSourceFactory(context))
                    .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
            // Show the dialog
            val dialog = AlertDialog.Builder(context).setView(dialogView)
                .setPositiveButton("Delete") { dialog, _ ->
                    deleteItem(mData[position], postRef, position, context)
                    player.release()
                    dialog.dismiss()
                }.setNegativeButton("Cancel") { dialog, _ ->
                    player.release()
                    dialog.dismiss()
                }.create()
            dialog.show()
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
                                Toast.makeText(
                                    context,
                                    "Failed " + e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                if (videoRef != null && videoRef.toString().contains("/")) {
                    val videoSegments = videoRef.toString().split("/")
                    if (videoSegments.size == 2) {
                        val videoFolderName = videoSegments[0]
                        val videoFileName = videoSegments[1]
                        storageReference.child(videoFolderName).child(videoFileName)
                            .delete().addOnFailureListener { e ->
                                if (progressDialog.isShowing) {
                                    progressDialog.dismiss()
                                }
                                Toast.makeText(
                                    context,
                                    "Failed " + e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
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
                        Toast.makeText(
                            context,
                            "Something went wrong removing post reference",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Something went wrong removing post null reference",
                        Toast.LENGTH_SHORT
                    ).show()
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


    inner class PostViewHolder(itemView: View, imageLoader: ImageLoader) :
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