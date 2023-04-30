package vfunny.shortvideovfunnyapp.Live.ui

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import com.firebase.ui.database.paging.DatabasePagingOptions
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter
import com.firebase.ui.database.paging.LoadingState
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import vfunny.shortvideovfunnyapp.Login.data.Post
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.databinding.ListActivityBinding

open class ListActivity : AppCompatActivity() {
    private val TAG: String = "ListActivity"
    private lateinit var mAdapter: FirebaseRecyclerPagingAdapter<Post, PostViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manual dependency injection
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: Starting ListActivity")
        val binding = ListActivityBinding.inflate(layoutInflater)
        binding.mRecyclerView.setHasFixedSize(true)
        binding.mRecyclerView.layoutManager = StaggeredGridLayoutManager(4, RecyclerView.VERTICAL)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val itemWidth = screenWidth / 4
        //Initialize Database
        val query =
            FirebaseDatabase.getInstance().reference.child("posts").orderByChild("timestamp")

        //Initialize PagedList Configuration
        val config: PagedList.Config =
            PagedList.Config.Builder().setEnablePlaceholders(true).setPrefetchDistance(5)
                .setPageSize(10).build()

        //Initialize FirebasePagingOptions
        val options: DatabasePagingOptions<Post> =
            DatabasePagingOptions.Builder<Post>().setLifecycleOwner(this)
                .setQuery(query, config, Post::class.java).build()

        //Initialize Adapter
        mAdapter = object : FirebaseRecyclerPagingAdapter<Post, PostViewHolder>(options) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): PostViewHolder {
                return PostViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_list, parent, false), this@ListActivity.imageLoader)
            }

            override fun onBindViewHolder(viewHolder: PostViewHolder, position: Int, model: Post) {
                viewHolder.setItem(model)
                // Set LayoutParams to adjust item width
                val layoutParams =
                    viewHolder.itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
                layoutParams.width = itemWidth // divide by number of columns
                viewHolder.itemView.layoutParams = layoutParams
                viewHolder.itemView.setOnClickListener {
                    val postRef = getRef(position)
                    val dialogView =
                        LayoutInflater.from(this@ListActivity).inflate(R.layout.dialog_video, null)
                    val videoView = dialogView.findViewById<PlayerView>(R.id.item_player_view)

                    if (model.video == null || model.image == null) {
                        Toast.makeText(this@ListActivity,
                            "Something went wrong",
                            Toast.LENGTH_SHORT).show()
                        val dialog = AlertDialog.Builder(this@ListActivity).setView(dialogView)
                            .setPositiveButton("Delete") { dialog, _ ->
                                deleteItem(model, postRef)
                                dialog.dismiss()
                            }.setNegativeButton("Cancel", null).create()
                        dialog.show()
                        return@setOnClickListener
                    }
                    // Set the video URI
                    val player = SimpleExoPlayer.Builder(this@ListActivity).build()
                    videoView.player = player
                    val videoUri = Uri.parse(model.video)
                    val mediaItem = MediaItem.fromUri(videoUri)
                    val mediaSource =
                        ProgressiveMediaSource.Factory(DefaultDataSourceFactory(this@ListActivity))
                            .createMediaSource(mediaItem)
                    player.setMediaSource(mediaSource)
                    player.prepare()
                    player.play()
                    // Show the dialog
                    val dialog = AlertDialog.Builder(this@ListActivity).setView(dialogView)
                        .setPositiveButton("Delete") { dialog, _ ->
                            deleteItem(model, postRef)
                            player.release()
                            dialog.dismiss()
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            player.release()
                            dialog.dismiss()
                        }.create()
                    dialog.show()
                }
            }

            private fun deleteItem(videoData: Post, postRef: DatabaseReference) {
                val context = this@ListActivity
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
                                        Toast.makeText(context,
                                            "Failed " + e.message,
                                            Toast.LENGTH_SHORT).show()
                                        Log.e("TAG", "Failed : " + e.message)
                                    }
                            }
                        }
                        if (videoRef != null && videoRef.toString().contains("/")) {
                            val videoSegments = videoRef.toString().split("/")
                            if (videoSegments.size == 2) {
                                val videoFolderName = videoSegments[0]
                                val videoFileName = videoSegments[1]
                                storageReference.child(videoFolderName).child(videoFileName)
                                    .delete().addOnFailureListener { e  ->
                                        if (progressDialog.isShowing) {
                                            progressDialog.dismiss()
                                        }
                                        Log.e(TAG, "deleteItem: $e", )
                                        Toast.makeText(context,
                                            "Failed " + e.message,
                                            Toast.LENGTH_SHORT).show()
                                        Log.e("TAG", "Failed : " + e.message)
                                    }
                            }
                        }
                        postRef.removeValue().addOnSuccessListener {
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Log.e(TAG, "deleteItem: $postRef", )
                            Log.e(TAG, "deleteItem: $it", )
                            Toast.makeText(context, "Something went wrong removing post reference", Toast.LENGTH_SHORT)
                                .show()
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

            override fun onLoadingStateChanged(state: LoadingState) {
                when (state) {
                    LoadingState.LOADING_INITIAL, LoadingState.LOADING_MORE ->                         // Do your loading animation
                        binding.mSwipeRefreshLayout.isRefreshing = true
                    LoadingState.LOADED ->                         // Stop Animation
                        binding.mSwipeRefreshLayout.isRefreshing = false
                    LoadingState.FINISHED ->                         //Reached end of Data set
                        binding.mSwipeRefreshLayout.isRefreshing = false
                    LoadingState.ERROR -> retry()
                }
            }

            override fun onError(@NonNull databaseError: DatabaseError) {
                super.onError(databaseError)
                binding.mSwipeRefreshLayout.isRefreshing = false
                databaseError.toException().printStackTrace()
            }
        }

        //Set Adapter to RecyclerView
        binding.mRecyclerView.adapter = mAdapter

        //Set listener to SwipeRefreshLayout for refresh action
        binding.mSwipeRefreshLayout.setOnRefreshListener { mAdapter.refresh() }

        setContentView(binding.root)
    }
}
