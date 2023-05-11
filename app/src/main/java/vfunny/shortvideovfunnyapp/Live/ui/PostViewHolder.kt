package vfunny.shortvideovfunnyapp.Live.ui

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.size.Precision
import coil.size.Scale
import vfunny.shortvideovfunnyapp.Data.model.Post
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