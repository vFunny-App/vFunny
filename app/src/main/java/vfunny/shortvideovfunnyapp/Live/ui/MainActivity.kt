package vfunny.shortvideovfunnyapp.Live.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.animation.Animator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.onesignal.OneSignal
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import vfunny.shortvideovfunnyapp.BuildConfig.APPLICATION_ID
import vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE
import vfunny.shortvideovfunnyapp.Live.di.MainModule
import vfunny.shortvideovfunnyapp.Login.Loginutils.AuthManager
import vfunny.shortvideovfunnyapp.Login.PostsUtils.PostsManager
import vfunny.shortvideovfunnyapp.Login.data.Const
import vfunny.shortvideovfunnyapp.Login.data.Language
import vfunny.shortvideovfunnyapp.Login.data.User
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.databinding.MainActivityBinding
import java.io.IOException

class MainActivity : AppCompatActivity(), AuthManager.AuthListener {
    private val TAG: String = "MainActivity"
    private var mUser: User? = null
    private var context: Context? = null
    private var activity: MainActivity? = null
    private var notificationVideoUrl: String? = null
    private var notificationThumbnailUrl: String? = null
    private var isAdsEnabled: Boolean = false
    val videoItemList = ArrayList<VideoData>()

    companion object {
        const val POST_NOTIFICATIONS_REQ_CODE = 103
        const val ONESIGNAL_APP_ID = "0695d934-66e2-43f6-9853-dbedd55b86ca"
        const val REST_API_KEY = "MzBhMWIzODMtY2U3OC00OTlhLTkwMDEtM2UxZWExYjU5Nzg5"
        const val ADS_TYPE = "ads"
        const val ITEM_COUNT_THRESHOLD = 5
        const val ADS_FREQUENCY = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manual dependency injection
        context = this.applicationContext
        activity = this
        super.onCreate(savedInstanceState)
        val binding = MainActivityBinding.inflate(layoutInflater)
        binding.animationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator) {
            }

            override fun onAnimationEnd(p0: Animator) {
                binding.progressCircular.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(p0: Animator) {
                Log.e("TAG", "onAnimationCancel: $p0")
            }

            override fun onAnimationRepeat(p0: Animator) {
                Log.e("TAG", "onAnimationRepeat: $p0")
            }

        })
        binding.animationView.playAnimation()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        FirebaseApp.initializeApp(this.applicationContext)
        OneSignal.setNotificationOpenedHandler { result ->
            val data = result.notification.additionalData
            Log.e(TAG, "NOTIFICATION additionalData :  ${result.notification.additionalData}")
            if (data.has("video_url") && data.has("thumbnail_url")) {
                notificationVideoUrl = data.getString("video_url").toString()
                notificationThumbnailUrl = data.getString("thumbnail_url").toString()
            } else if (data.has("app_update_notification")) {
                val appPackageName = APPLICATION_ID
                try {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
        User.currentKey()?.let { currentKey ->
            User.collection(currentKey).run {
                addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (User.currentKey() == null) {
                            return
                        }
                        var languageList = Language.getAllLanguages()
                        if(!dataSnapshot.exists()) {
                            Log.e(TAG, "user not in database", )
                            AuthManager.getInstance().completeAuth(this@MainActivity)
                        } else {
                            val user : User? = dataSnapshot.getValue(User::class.java)
                            languageList  = user?.language ?: Language.getAllLanguages()
                            if(user?.language != null && !languageList.contains(Language.WORLDWIDE)) {
                                languageList = Language.addWorldWideLangToDb(this@MainActivity, user.language)
                            }
                        }
                        var count = 0
                        var videoCount = 0
                        val unwatchedPosts = if (BUILD_TYPE == "admin" || BUILD_TYPE == "adminDebug") {
                            Log.e(TAG, "Getting ADMIN POSTS")
                            PostsManager.instance.getAdminPosts(this@MainActivity, Language.getAllLanguages())
                        } else {
                            Log.e(TAG, "Getting USER POSTS")
                            PostsManager.instance.getPosts(this@MainActivity, languageList)
                        }

                        Log.e(TAG, "onDataChange: unwatchedPosts.size ${unwatchedPosts.size}", )
                        // Pass the list of unwatched posts to your application
                        unwatchedPosts.forEach {
                            val video: String = it?.video ?: ""
                            val image: String = it?.image ?: ""
                            val key: String? = it?.key
                            count++
                            videoCount++
                            videoItemList.add(VideoData(count.toString(),
                                video,
                                image,
                                key = key))
                            val totalItemCount = ITEM_COUNT_THRESHOLD
                            if (isAdsEnabled && videoCount % ADS_FREQUENCY == 0 && videoCount != totalItemCount) {
                                count++
                                videoItemList.add(VideoData(count.toString(),
                                    "",
                                    "",
                                    type = ADS_TYPE))
                            }
                        }
                        Log.e(TAG, "onDataChange: FINAL videoItemList.size ${videoItemList.size}", )
                        val module = MainModule(activity!!, videoItemList)
                        supportFragmentManager.fragmentFactory = module.fragmentFactory
                        if (savedInstanceState == null) {
                            supportFragmentManager.commit {
                                replace<VideoPagerFragment>(binding.fragmentContainer.id)
                            }
                        }
                        binding.animationView.clearAnimation()
                        binding.fragmentContainer.visibility = View.VISIBLE
                        binding.loadingLyt.removeAllViewsInLayout()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Something went wrong : $error", Toast.LENGTH_LONG)
                            .show()
                    }
                })
            }
        } ?: AuthManager.getInstance().showLogin(this) // Show login screen if user key is null
        setContentView(binding.root)

        if (vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE == "admin" || com.videopager.BuildConfig.BUILD_TYPE == "adminDebug") {
            Toast.makeText(this@MainActivity, "Running ADMIN build", Toast.LENGTH_SHORT).show()
            binding.addBtn.setOnClickListener { addClick() }
            binding.listBtn.setOnClickListener {
                val intent = Intent(this, ListActivity::class.java)
                startActivity(intent)
            }
            binding.updateNotification.setOnClickListener { showUpdateNotificationConfirmationDialog() }
            getAdsStatus(binding.adsSwitch)
        } else if (vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE == "debug") {
            Toast.makeText(this@MainActivity, "Running DEBUG build", Toast.LENGTH_SHORT).show()
            hideAdminUI(binding)
        } else if (vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE == "release") {
            hideAdminUI(binding)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this@MainActivity, POST_NOTIFICATIONS) -> {}
                else -> {
                    requestPermissions(arrayOf(POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQ_CODE)
                }
            }
        }
    }

    private fun hideAdminUI(binding: MainActivityBinding) {
        binding.listBtn.visibility = View.GONE
        binding.addBtn.visibility = View.GONE
        binding.updateNotification.visibility = View.GONE
        binding.addCard.visibility = View.GONE
    }

    private fun showUpdateNotificationConfirmationDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Send Update Notification?")
            .setMessage("Send Update notification to old version users?")
            .setPositiveButton("SEND") { dialog: DialogInterface?, which: Int ->
                sendUpdateNotification()
            }
            .setNegativeButton("CANCEL") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
            .show()
    }

    private fun sendUpdateNotification() {
        Thread(Runnable {
            val deviceState = OneSignal.getDeviceState()
            val userId = deviceState?.userId
            val isSubscribed = deviceState != null && deviceState.isSubscribed
            if (!isSubscribed) return@Runnable
            try {
                val notificationContent =
                    JSONObject("{'included_segments': ['toBeUpdatedUsers'],'app_id': '$ONESIGNAL_APP_ID','headings': {'en': 'update Available'},'contents': {'en': 'A new version of the app is available. Tap here to update.'},'data': {'app_update_notification': 'true'}}")

                val client = OkHttpClient()
                val json = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = notificationContent.toString().toRequestBody(json)

                val request = Request.Builder().url("https://onesignal.com/api/v1/notifications")
                    .addHeader("Authorization", "Basic $REST_API_KEY").post(body).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "onFailure: $e")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.e(TAG, "onResponse: $response")
                    }
                })
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun showConfirmationDialog(message: String, value: Boolean, adsSwitch: SwitchCompat) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage(message).setPositiveButton("Yes") { dialog, id ->
            // User clicked Yes button
            FirebaseDatabase.getInstance().getReference(Const.kAdsKey).setValue(value)
                .addOnSuccessListener {
                    isAdsEnabled = value
                }.addOnFailureListener {
                    adsSwitch.isChecked = !value
                    Toast.makeText(activity,
                        "Something went wrong while changing ads Status",
                        Toast.LENGTH_LONG).show()
                    Log.e(TAG, "showConfirmationDialog: Error : ", it)
                }
        }.setNegativeButton("No") { dialog, id ->
            // User cancelled the dialog
            adsSwitch.isChecked = !value
        }
        builder.create().show()
    }

    private fun getAdsStatus(adsSwitch: SwitchCompat) {
        val adsEnabled = User.adsDatabaseReference()
        adsEnabled.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isAdsEnabled = dataSnapshot.getValue(Boolean::class.java) == true
                adsSwitch.isChecked = isAdsEnabled
                adsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        showConfirmationDialog("Enable Ads?", true, adsSwitch)
                    } else {
                        showConfirmationDialog("Disable Ads?", false, adsSwitch)
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

    override fun onAuthSuccess(user: User?) {
        if (User.current() != null && mUser?.name == null) {
            mUser!!.name = getString(R.string.name_placeholder)
            User.currentKey()?.let {
                FirebaseDatabase.getInstance().getReference(Const.kUsersKey).child(it).child("name")
                    .setValue(getString(R.string.name_placeholder))
            }
        }
    }

    override fun onAuthFailed() {
        AuthManager.getInstance().showLogin(this)
    }

    private fun addClick() {
        Log.e(TAG, "addClick: clicked")
        if (MediaUtils.storagePermissionGrant(activity)) {
            Log.e(TAG, "addClick: true")
            MediaUtils.openVideoLibrary(activity, MediaUtils.REQUEST_VIDEO_PICK)
        } else {
            Log.e(TAG, "addClick: false")
        }
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
        } else if (requestCode == MediaUtils.REQUEST_VIDEO_PICK) {
            super.onActivityResult(requestCode, resultCode, data)
            val storage = FirebaseStorage.getInstance()
            val storageReference = storage.reference
            if (data != null) {
                if (data.clipData != null) {
                    val uriList = ArrayList<Uri>()
                    for (i in 0 until data.clipData!!.itemCount) {
                        val filePath = data.clipData?.getItemAt(i)?.uri
                        if (filePath != null) {
                            uriList.add(filePath)
                        }
                    }
                    if (uriList.isNotEmpty()) {
                        MediaUtils.uploadMultiplePhoto(storageReference,
                            uriList,
                            this@MainActivity,
                            MediaUtils.REQUEST_VIDEO_PICK)
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                    val storage = FirebaseStorage.getInstance()
                    val storageReference = storage.reference
                    val filePath = data.data
                    Log.e(TAG, "onActivityResult: describeContents ${filePath?.scheme}")
                    data.data?.run {
                        if (resultCode == RESULT_OK) {
                            MediaUtils.uploadPhoto(storageReference,
                                this,
                                this@MainActivity,
                                MediaUtils.REQUEST_VIDEO_PICK)
                        }
                    }
                    Log.e(TAG, "onActivityResult: $filePath")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            POST_NOTIFICATIONS_REQ_CODE,
            -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }
}
