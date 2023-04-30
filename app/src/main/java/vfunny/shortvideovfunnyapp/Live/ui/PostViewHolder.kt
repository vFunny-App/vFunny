package vfunny.shortvideovfunnyapp.Live.ui

import android.app.ProgressDialog
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.size.Precision
import coil.size.Scale
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import vfunny.shortvideovfunnyapp.Login.data.Post
import vfunny.shortvideovfunnyapp.R

class PostViewHolder(itemView: View, imageLoader: ImageLoader) : RecyclerView.ViewHolder(itemView) {
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