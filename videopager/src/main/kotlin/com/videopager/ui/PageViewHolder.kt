package com.videopager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.onesignal.OneSignal
import com.player.models.VideoData
import com.player.ui.AppPlayerView
import com.squareup.okhttp.*
import com.videopager.BuildConfig.BUILD_TYPE
import com.videopager.DownloadWatermarkManager
import com.videopager.PostManager
import com.videopager.R
import com.videopager.databinding.PageItemBinding
import com.videopager.models.*
import com.videopager.models.AnimationEffect
import com.videopager.models.PageEffect
import com.videopager.models.ResetAnimationsEffect
import com.videopager.models.ViewEvent
import com.videopager.ui.extensions.findParentById
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

val ONESIGNAL_APP_ID = "0695d934-66e2-43f6-9853-dbedd55b86ca"
val REST_API_KEY = "MzBhMWIzODMtY2U3OC00OTlhLTkwMDEtM2UxZWExYjU5Nzg5"

internal class PageViewHolder(
    private val binding: PageItemBinding,
    private val imageLoader: ImageLoader,
    private val click: (event: Any) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var videoData: VideoData
    private val TAG: String = "PageViewHolder"
    private val animationEffect = FadeInThenOutAnimationEffect(binding.playPause)

    companion object {
        const val WATERMARK_END =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end.mp4?alt=media&token=b39bcdc4-be50-49e0-8deb-024abb32ec8f"
        const val WATERMARK_END_SOUND =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio.mp4?alt=media&token=7ff3bd8e-12af-4828-9e59-cc5b50c353a5"
        const val WATERMARK_END_SOUND_2 =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio_2.mp4?alt=media&token=22e76999-c048-4429-bbe0-193b02fef79a"
        const val WATERMARK_END_SOUND_3 =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_end_audio_3.mp4?alt=media&token=c8516689-d112-418b-96af-e8748ba6bf12"
        const val WATERMARK_LOGO_LINK =
            "https://firebasestorage.googleapis.com/v0/b/vfunnyapp-71911.appspot.com/o/watermark_logo.png?alt=media&token=b6d7f2d9-d242-42b2-ae97-a85c82d19647"
    }

    init {
        binding.root.setOnClickListener { click(TappedPlayerEvent) }
        binding.waShare.setOnClickListener { click(TappedWhatsappEvent) }
    }

    var currentNativeAd: NativeAd? = null

    private fun shareClick(url: String) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
        var shareMessage =
            "Download App\n https://play.google.com/store/apps/details?id=vfunny.shortvideovfunnyapp \nWatch this Video\n\n"
        shareMessage += url
        i.putExtra(Intent.EXTRA_TEXT, shareMessage)
        val context = itemView.context
        context.startActivity(Intent.createChooser(i, "Share URL"))
    }

    /**
     * Deletes a video item from Firebase Storage and RealTime Db
     * @param videoData the video data of the item to be deleted
     */
    private fun deleteItem(videoData: VideoData) {
        // Get necessary instances and references
        val context = itemView.context
        val storage = FirebaseStorage.getInstance()
        var thumbnailRef: StorageReference? = null
        var videoRef: StorageReference? = null
        // If the key or language is missing, show a toast and return early
        if (videoData.key == null || videoData.language == null) {
            Toast.makeText(context, "Can't find key or language", Toast.LENGTH_SHORT).show()
            return
        }
        if (videoData.previewImageUri.isNotEmpty()) {
            thumbnailRef = storage.getReferenceFromUrl(videoData.previewImageUri)
        }
        if (videoData.mediaUri.isNotEmpty()) {
            videoRef = storage.getReferenceFromUrl(videoData.mediaUri)
        }

        val postManager = PostManager.instance

        MaterialAlertDialogBuilder(context, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("WARNING!")
            .setMessage("Do you want to delete this item?")
            .setPositiveButton("Yes") { dialog, _ ->
                // User clicked Yes button
                postManager.showProgressDialog(context, "Deleting...")
                postManager.deleteFileFromStorage(context, thumbnailRef)
                postManager.deleteFileFromStorage(context, videoRef)
                postManager.deletePostReferenceFromDatabase(context, videoData)
                postManager.dismissProgressDialog(dialog)
            }
            .setNegativeButton("No") { dialog, _ ->
                // User cancelled the dialog
                postManager.dismissProgressDialog(dialog)
                postManager.showToast(context, "Cancelled")
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Sends a OneSignal push notification with video thumbnail and video url to subscribed users
     * @param thumbnailRef The URL of the video thumbnail
     * @param videoRef The URL of the video
     */
    private fun sendVideoNotification(thumbnailRef: String?, videoRef: String?) {
        Thread {
            // Get device state
            val deviceState = OneSignal.getDeviceState()
            val isSubscribed = deviceState != null && deviceState.isSubscribed
            // Check if user is subscribed
            if (!isSubscribed) return@Thread

            try {
                // Create JSON object with notification content
                val notificationContent =
                    JSONObject("{'included_segments': ['Subscribed Users'], " + "'app_id': '$ONESIGNAL_APP_ID', " + "'headings': {'en': 'Check out this funny video! \uD83E\uDD23\uD83D\uDE06\uD83D\uDE02\uD83D\uDE05'}, " + "'contents': {'en': 'Look at this funny video \uD83D\uDE02. Fresh new memes available!'}, " + "'large_icon': '$thumbnailRef', " + "'big_picture': '$thumbnailRef', " + "'data': {'video_url': '$videoRef', 'thumbnail_url': '$thumbnailRef'}}")
                val client = OkHttpClient()
                val json = MediaType.parse("application/json; charset=utf-8")
                val body = RequestBody.create(json, notificationContent.toString())

                // Create POST request and send it using OkHttp library
                val request = Request.Builder().url("https://onesignal.com/api/v1/notifications")
                    .addHeader("Authorization", "Basic $REST_API_KEY").post(body).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(request: Request?, e: IOException?) {
                        Log.e(TAG, "onFailure: $e")
                    }

                    override fun onResponse(response: Response?) {
                        Log.e(TAG, "onResponse: $response")
                    }
                })
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Displays a popup menu on clicking the "More options" button.
     * The popup menu contains three options:
     * Open the app's page on Google Play Store.
     * Share the URL of the video.
     * Open the app's homepage.
     * @param url The URL of the video to be shared.
     * @param view The view on which the popup menu should be displayed.
     */
    private fun moreOptionClick(url: String, view: View) {
        // Create a new instance of PopupMenu with the given view as an anchor
        val popup = PopupMenu(itemView.context, view)
        // Inflate the menu resource into the PopupMenu's Menu
        popup.inflate(R.menu.menu_opt)
        // Set a listener to be notified of menu item clicks
        popup.setOnMenuItemClickListener { item ->
            // Perform an action based on the selected item
            when (item.itemId) {
                // If the "Open on Google Play Store" item is selected, open the app's page on Google Play Store
                R.id.menu2 -> itemView.context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${itemView.context.packageName}")))
                // If the "Share URL" item is selected, share the URL of the video using an Intent
                R.id.menu3 -> try {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
                    var shareMessage =
                        "Download VFunny App : https://play.google.com/store/apps/details?id=vfunny.shortvideovfunnyapp \n\nWatch this Video "
                    shareMessage += url
                    i.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    val context: Context = itemView.context
                    context.startActivity(Intent.createChooser(i, "Share URL"))
                } catch (e: java.lang.Exception) {
                    // TODO: Handle the exception
                }
                // If the "Open app homepage" item is selected, open the app's homepage
                R.id.menu4 -> itemView.context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://sites.google.com/view/vfunny/home")))
            }
            // Return false to allow normal menu processing to proceed, true to consume it here.
            false
        }
        // Show the PopupMenu
        popup.show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun bind(videoData: VideoData) {
        this.videoData = videoData
        if (videoData.type?.equals("video") == true) {
            binding.previewImage.visibility = View.VISIBLE
            binding.waShare.visibility = View.VISIBLE
            binding.share.visibility = View.VISIBLE
            binding.moreOptions.visibility = View.VISIBLE
            binding.playerContainer.visibility = View.VISIBLE
            binding.playPause.visibility = View.VISIBLE
            binding.adContainer.visibility = View.GONE
            binding.previewImage.load(videoData.previewImageUri, imageLoader)
            binding.share.setOnClickListener { shareClick(videoData.mediaUri) }
            binding.downloadButton.setOnClickListener {
                runBlocking {
                    //Run converting process in a new thread as rendering is intense process for main thread
                    withContext(newSingleThreadContext("Converting"))
                    {
                        //Check again for external storage permission if android API >= 30
                        DownloadWatermarkManager.instance.addWatermarkVideo(videoData.mediaUri,
                            WATERMARK_LOGO_LINK,
                            WATERMARK_END_SOUND_3,
                            itemView.context)
                    }
                }
//                DownloadWatermarkManager.instance.addWatermarkToVideo(videoData.mediaUri,
//                    WATERMARK_END_SOUND,
//                    itemView.context)
//                DownloadWatermarkManager.instance.addWatermarkToVideo(videoData.mediaUri,
//                    WATERMARK_END,
//                    itemView.context)
            }
            binding.moreOptions.setOnClickListener {
                moreOptionClick(videoData.mediaUri, binding.moreOptions)
            }

            if (BUILD_TYPE == "admin" || BUILD_TYPE == "adminDebug") {
                binding.deleteItem.setOnClickListener {
                    deleteItem(videoData)
                }
                binding.sendNoti.setOnClickListener {
                    val context = itemView.context
                    val builder = AlertDialog.Builder(context)
                    val storage = FirebaseStorage.getInstance()
                    val thumbnailRef = storage.getReferenceFromUrl(videoData.previewImageUri)
                    val videoRef = storage.getReferenceFromUrl(videoData.mediaUri)
                    Log.e(TAG, "sendNotification: previewImageUri :  ${videoData.previewImageUri}")
                    Log.e(TAG, "sendNotification: mediaUri : ${videoData.mediaUri}")
                    Log.e(TAG, "thumbnailRef: $thumbnailRef")
                    Log.e(TAG, "videoRef: $videoRef")
                    builder.setCancelable(false)
                    builder.setMessage("Do you want to send notification for this item?")
                        .setPositiveButton("Yes") { dialog, id ->
                            // Launch the coroutine to perform the network operation
                            sendVideoNotification(videoData.previewImageUri, videoData.mediaUri)
                        }.setNegativeButton("No") { dialog, id ->
                            // User cancelled the dialog
                            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                        }
                    builder.create().show()
                }
                binding.infoAdmin.text =
                    "${videoData.key} | ${videoData.language} | ${videoData.getDateString()}"
                binding.infoAdmin.visibility = View.VISIBLE
            } else {
                binding.deleteItem.visibility = View.GONE
                binding.sendNoti.visibility = View.GONE
            }
            ConstraintSet().apply {
                clone(binding.root)
                val ratio = videoData.aspectRatio?.let { "$it:1" }
                setDimensionRatio(binding.playerContainer.id, ratio)
                setDimensionRatio(binding.previewImage.id, ratio)
                applyTo(binding.root)
            }
            currentNativeAd?.destroy()
        } else {
            binding.previewImage.visibility = View.GONE
            binding.waShare.visibility = View.GONE
            binding.share.visibility = View.GONE
            binding.moreOptions.visibility = View.GONE
            binding.playerContainer.visibility = View.GONE
            binding.playPause.visibility = View.GONE
            binding.adContainer.visibility = View.VISIBLE
            refreshAd(binding.adContainer)
        }
    }

    /**
     * Sets the video as seen for the current user.
     * @param videoData the video to set as seen.
     */
    private fun setSeen(videoData: VideoData) {
        if (videoData.key.isNullOrEmpty()) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        videoData.language ?: return

        val videoRef = FirebaseDatabase.getInstance()
            .getReference("posts_${videoData.language!!.code}").child(videoData.key!!)
        val watchedByRef = videoRef.child("watchedBy").child(userId)
        watchedByRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Add the user ID to the "watchedBy" list for the video
                    val watchedByUpdate = hashMapOf(userId to true)
                    videoRef.child("watchedBy").updateChildren(watchedByUpdate as Map<String, Any>)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: $error")
                // Handle errors
            }
        })
    }


    private fun refreshAd(adContainer: FrameLayout) {
        val context = itemView.context
        val builder = AdLoader.Builder(context, context.getString(R.string.NATIVE_ADD_ID))
        builder.forNativeAd { nativeAd ->
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val inflater: LayoutInflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val adView = inflater.inflate(R.layout.ad_unified, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            adContainer.removeAllViews()
            adContainer.addView(adView)
        }
        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()
        builder.withNativeAdOptions(adOptions)
        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    "domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}"
                Snackbar.make(adContainer,
                    "Failed to load native ad with error $error",
                    Snackbar.LENGTH_SHORT).show()
                Log.e("ADS", "onAdFailedToLoad: $error")
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        if (nativeAd.mediaContent != null) {
            adView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
        }
        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }
        if (nativeAd.price == null) {
            adView.priceView?.visibility = View.INVISIBLE
        } else {
            adView.priceView?.visibility = View.VISIBLE
            (adView.priceView as TextView).text = nativeAd.price
        }
        if (nativeAd.store == null) {
            adView.storeView?.visibility = View.INVISIBLE
        } else {
            adView.storeView?.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }
        if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }
        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val vc = nativeAd.mediaContent?.videoController
        // Updates the UI to say whether or not this ad has a video asset.
        if (vc != null) {
            if (vc.hasVideoContent()) {
                Log.e("TAG",
                    String.format(Locale.getDefault(),
                        "Video status: Ad contains a %.2f:1 video asset.",
                        nativeAd.mediaContent?.aspectRatio))
                // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
                // VideoController will call methods on this object when events occur in the video
                // lifecycle.
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        // Publishers should allow native ads to complete video playback before
                        // refreshing or replacing them with another ad in the same UI location.
                        Log.e("TAG", "onVideoEnd: Video status: Video playback has ended.")
                        super.onVideoEnd()
                    }
                }
            } else {
                Log.e("TAG", "Video status: Ad does not contain a video asset.")
            }
        }
    }

    fun attach(appPlayerView: AppPlayerView) {
        if (binding.playerContainer == appPlayerView.view.parent) {
            return
        }
        /**
         * Since effectively only one [AppPlayerView] instance is used in the app, it might currently
         * be attached to a View from a previous page. In that case, remove it from that parent
         * before adding it to this ViewHolder's View, and cleanup state from the previous ViewHolder.
         */
        appPlayerView.view.findParentById(binding.root.id)?.let(PageItemBinding::bind)?.apply {
            playerContainer.removeView(appPlayerView.view)
            previewImage.isVisible = true
        }
        binding.playerContainer.addView(appPlayerView.view)
        /**
        add this user to current video watchedBy
         */
        Log.e(TAG, "video ID : : ${videoData.id}")
        Log.e(TAG, ": Updating Seen List")
        itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            setSeen(videoData)
        }
    }

    fun renderEffect(effect: PageEffect) {
        when (effect) {
            is ResetAnimationsEffect -> animationEffect.reset()
            is AnimationEffect -> {
                binding.playPause.setImageResource(effect.drawable)
                animationEffect.go()
            }
        }
    }

    fun hidePreviewImage() {
        binding.previewImage.isVisible = false
    }
}
