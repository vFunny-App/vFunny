package vfunny.shortvideovfunnyapp.Live.ui

import android.animation.Animator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.common.reflect.TypeToken
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.onesignal.OneSignal
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import vfunny.shortvideovfunnyapp.BuildConfig.APPLICATION_ID
import vfunny.shortvideovfunnyapp.Live.di.MainModule
import vfunny.shortvideovfunnyapp.Login.Loginutils.AuthManager
import vfunny.shortvideovfunnyapp.Login.data.Const
import vfunny.shortvideovfunnyapp.Login.data.User
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.databinding.MainActivityBinding

class MainActivity : AppCompatActivity(), AuthManager.AuthListener {
    private val TAG: String = "MainActivity"
    var seenList: ArrayList<String> = ArrayList()
    private var mUser: User? = null
    private var context: Context? = null
    private var activity: MainActivity? = null
    private var notificationVideoUrl: String? =  null
    private var notificationThumbnailUrl: String? =  null
    private var isAdsEnabled: Boolean =  false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manual dependency injection
        context = this.applicationContext
        activity = this
        super.onCreate(savedInstanceState)
        val binding = MainActivityBinding.inflate(layoutInflater)
        FirebaseApp.initializeApp(this.applicationContext)
        OneSignal.setNotificationOpenedHandler { result ->
            val data = result.notification.additionalData
            Log.e(TAG, "NOTIFICATION additionalData :  ${result.notification.additionalData }")
            if(data.has("video_url") && data.has("thumbnail_url")){
                notificationVideoUrl = data.getString("video_url").toString()
                notificationThumbnailUrl = data.getString("thumbnail_url").toString()
            } else if (data.has("app_update_notification")) {
                val appPackageName = APPLICATION_ID
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
        if (User.currentKey() != null) {
            getAdsStatus(binding.adsSwitch)
            Log.e(TAG, "onCreate:  currentKey : " + User.currentKey())
            User.collection(User.currentKey()).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.e(TAG, "onCreate:  dataSnapshot : " + dataSnapshot)
                        mUser = dataSnapshot.getValue(User::class.java)
                        val videoItemList = ArrayList<VideoData>()
                        val firebaseDatabase = FirebaseDatabase.getInstance()
                        val databaseReference = firebaseDatabase.getReference("posts").limitToLast(100)
                        databaseReference.addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            var count = 0
                            var videoCount = 0
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if(notificationVideoUrl!=null ){
                                    count ++
                                    videoItemList.add(VideoData(count.toString(), notificationVideoUrl.toString(), notificationThumbnailUrl.toString(), key = ""))
                                }
                                snapshot.children.reversed().forEach {
                                    val video: String = it.child("video").value.toString()
                                    val image: String = it.child("image").value.toString()
                                    var i = 0
                                    if (seenList.isNotEmpty()) {
                                        while (i < seenList.size) {
                                            if (it.key == seenList[i])
                                                return@forEach
                                            i++
                                        }
                                    } else {
                                        Log.e(TAG, "Empty seenList")
                                    }
                                    count++                                                                                                                                                   // Increment the id for next video  // count  =  1
                                    videoCount++                                                                                                                                            // Increment the count of video(s) added
                                    videoItemList.add(VideoData(count.toString(), video, image, key = it.key))                                    // Add the video to list
                                    if(isAdsEnabled) {
                                        if (videoCount % 5 == 0 && videoCount != 100 + seenList.size) {                                                       //check the  count if current item was  position #5 (but not the last item #100 + seenList size)
                                            count++                                                                                                                                                   // count  = 1
                                            videoItemList.add(
                                                VideoData(
                                                    count.toString(),
                                                    "",
                                                    "",
                                                    type = "ads",
                                                    key = null
                                                )
                                            )      // Increment the id for next video  // id=6 //enter  ad in list with count=6
                                        }
                                    }
                                }
                                val module = MainModule(activity!!, videoItemList)
                                supportFragmentManager.fragmentFactory = module.fragmentFactory
                                if (savedInstanceState == null) {
                                    supportFragmentManager.commit {
                                        replace<VideoPagerFragment>(binding.fragmentContainer.id)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context,
                                    "Error Fetching Data! Check Network Connection!",
                                    Toast.LENGTH_SHORT).show()
                            }
                        })
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(context,
                            "Error Fetching Data! Check Network Connection!",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        }
        else {
            AuthManager.getInstance().showLogin(this)
        }
        setContentView(binding.root)
//        showBannerAds(binding)
        binding.animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
                Log.e("TAG", "onAnimationStart: $p0")
            }

            override fun onAnimationEnd(p0: Animator) {
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.animationView.pauseAnimation()
            }

            override fun onAnimationCancel(p0: Animator) {
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.animationView.pauseAnimation()
            }

            override fun onAnimationRepeat(p0: Animator) {
                TODO("Not yet implemented")
            }

        })
        binding.animationView.playAnimation()
        binding.addBtn.setOnClickListener { addClick() }
        binding.updateNotification.setOnClickListener() {
                showUpdateNotificationConfirmationDialog()
        }
    }

    private fun showUpdateNotificationConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Send Update notification to users?")
            .setPositiveButton("Yes") { dialog, id ->
                // TODO send notification to old users
                // User cancelled the dialog
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, id ->
                // User cancelled the dialog
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showConfirmationDialog(message: String, value: Boolean, adsSwitch: SwitchCompat) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage(message)
            .setPositiveButton("Yes") { dialog, id ->
                // User clicked Yes button
                FirebaseDatabase.getInstance().getReference(Const.kAdsKey).setValue(value).addOnSuccessListener {
                    isAdsEnabled = value
                }.addOnFailureListener {
                    adsSwitch.isChecked = !value
                    Toast.makeText(activity, "Something went wrong while changing ads Status", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "showConfirmationDialog: Error : ", it)
                }
            }
            .setNegativeButton("No") { dialog, id ->
                // User cancelled the dialog
                adsSwitch.isChecked = !value
            }
        builder.create().show()
    }

    private fun addClick() {
        Log.e(TAG, "addClick: clicked")
        if(MediaUtils.storagePermissionGrant(activity)) {
            Log.e(TAG, "addClick: true")
            MediaUtils.openVideoLibrary(activity, MediaUtils.REQUEST_VIDEO_PICK)
        } else {
            Log.e(TAG, "addClick: false")
        }
    }

    private fun getAdsStatus(adsSwitch: SwitchCompat) {
        val adsEnabled = User.Ads()
        adsEnabled.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isAdsEnabled = dataSnapshot.getValue(Boolean::class.java) == true
                adsSwitch.isChecked = isAdsEnabled
                adsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        showConfirmationDialog("Enable Ads?", true, adsSwitch)
                    } else {
                        showConfirmationDialog("Disable Ads?", false,  adsSwitch)
                    }
                }
                // do something with isEnabled
                // you can use the isEnabled value here in your loop
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "onCancelled: $databaseError")
                // handle error
            }
        })
    }

    private fun showBannerAds(binding: MainActivityBinding) {
        //Admob banner
//        MobileAds.initialize(this)
//        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
//            .setTestDeviceIds(listOf("9AD5A1773917848899AF34A92ACCF1BC")).build())
//        val adRequest = AdRequest.Builder().build()
//        binding.adView.loadAd(adRequest)
    }

    override fun onAuthSuccess(user: User?) {
        if (User.current() != null && mUser?.name  == null) {
            mUser!!.name = getString(R.string.name_placeholder)
            FirebaseDatabase.getInstance().getReference(Const.kUsersKey)
                .child(User.currentKey())
                .child("name")
                .setValue(getString(R.string.name_placeholder))
        }
    }

    override fun onAuthFailed() {
        AuthManager.getInstance().showLogin(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.REQUEST_AUTH_CODE) {
            if (resultCode == RESULT_OK) {
                AuthManager.getInstance().completeAuth(this)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // error
                finish() // close the app
            }
        }
        else if (requestCode == MediaUtils.REQUEST_VIDEO_PICK) {
            super.onActivityResult(requestCode, resultCode, data)
            val storage = FirebaseStorage.getInstance()
            val storageReference = storage.reference
            if (data != null) {
                val filePath = data.data
                if (resultCode == RESULT_OK) {
                    MediaUtils.uploadPhoto(
                        storageReference,
                        filePath,
                        this,
                        MediaUtils.REQUEST_VIDEO_PICK
                    )
                }
            }
        }
        else if (requestCode == MediaUtils.REQUEST_VIDEO_CAPTURE) {
            super.onActivityResult(requestCode, resultCode, data)
            val storage = FirebaseStorage.getInstance()
            val storageReference = storage.reference
            if (data != null) {
                val filePath = data.data
                if (resultCode == RESULT_OK) {
                    MediaUtils.uploadPhoto(
                        storageReference,
                        filePath,
                        this,
                        MediaUtils.REQUEST_VIDEO_CAPTURE
                    )
                }
            }
        }
    }

}
