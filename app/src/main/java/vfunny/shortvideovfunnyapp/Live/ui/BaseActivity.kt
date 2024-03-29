package vfunny.shortvideovfunnyapp.Live.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.onesignal.OneSignal
import com.player.models.VideoData
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import vfunny.shortvideovfunnyapp.BuildConfig.APPLICATION_ID
import vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE
import vfunny.shortvideovfunnyapp.LangUtils.LangManager
import vfunny.shortvideovfunnyapp.Login.Loginutils.AuthManager
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.PostUtils.PostsManager
import vfunny.shortvideovfunnyapp.Post.model.Language
import java.io.IOException

abstract class BaseActivity : AppCompatActivity() {
    var isAdsEnabled: Boolean = false
    private var notificationVideoUrl: String? = null
    private var notificationThumbnailUrl: String? = null

    companion object {
        private const val TAG: String = "BaseActivity"
        const val POST_NOTIFICATIONS_REQ_CODE = 103
        const val ONESIGNAL_APP_ID = "0695d934-66e2-43f6-9853-dbedd55b86ca"
        const val REST_API_KEY = "MzBhMWIzODMtY2U3OC00OTlhLTkwMDEtM2UxZWExYjU5Nzg5"
        const val ADS_TYPE = "ads"
        const val ITEM_COUNT_THRESHOLD = 5
        const val ADS_FREQUENCY = 9
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this@BaseActivity, POST_NOTIFICATIONS) -> {}
                else -> {
                    requestPermissions(arrayOf(POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQ_CODE)
                }
            }
        }
    }

    protected fun showUpdateNotificationConfirmationDialog() {
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

    fun setUiFromBuildType() {
        when (BUILD_TYPE) {
            "admin", "adminDebug" -> {
                Toast.makeText(this@BaseActivity, "Running $BUILD_TYPE build", Toast.LENGTH_SHORT)
                    .show()
                addAdminButtons()
                getAdsStatus()
            }
            "debug", "release" -> {
                hideAdminUI()
            }
        }
    }

    fun fetchPosts() {
        val currentKey = User.currentKey()
        User.collection(currentKey).run {
            addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (User.currentKey() == null) {
                        return
                    }
                    Log.e(TAG, "User.currentKey() : ${User.currentKey()}")
                    var languageList = Language.getAllLanguages()
                    if (!dataSnapshot.exists()) {
                        Log.e(TAG, "user not in database")
                        AuthManager.getInstance().completeAuth()
                    } else {
                        val user: User? = dataSnapshot.getValue(User::class.java)
                        user?.id = User.currentKey()
                        languageList = LangManager.instance.getUserLanguages(user)
                        if (!languageList.contains(Language.WORLDWIDE)) {
                            Log.e(TAG, "user doesn't have ww language")
                            if (user != null) {
                                languageList = LangManager.instance.addWorldWideLangToDb(user)
                            }
                        }
                    }
                    Log.e(TAG, "user languageList size : ${languageList.size}")
                    Log.e(TAG, "Getting POSTS")
                    showVideos()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BaseActivity,
                        "Something went wrong : $error",
                        Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    abstract fun hideAdminUI()

    abstract fun getAdsStatus()

    abstract fun addAdminButtons()

    abstract fun showVideos(
    )
    abstract fun showEmptyVideos()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            POST_NOTIFICATIONS_REQ_CODE,
            -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.REQUEST_AUTH_CODE) {
            if (resultCode == RESULT_OK) {
                AuthManager.getInstance().completeAuth()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("showLanguage", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                // error
                finish() // close the app
            }
        } else if (requestCode == MediaUtils.REQUEST_VIDEO_PICK && data != null && resultCode == RESULT_OK) {
            val langList = Language.getAllLanguages()
            val listItems = langList.map { it.name }.toTypedArray()
            val mBuilder = AlertDialog.Builder(this@BaseActivity)
            mBuilder.setTitle("Choose a language")
            mBuilder.setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
                showUploadVideoConfirmationDialog(data, dialogInterface, langList[i])
            }
            mBuilder.setNeutralButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            val mDialog = mBuilder.create()
            mDialog.show()
        }
    }

    private fun showUploadVideoConfirmationDialog(
        data: Intent,
        dialogInterface: DialogInterface,
        language: Language,
    ) {
        MaterialAlertDialogBuilder(this@BaseActivity, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("WARNING!")
            .setMessage("Are you sure you want to upload ${data.clipData?.itemCount ?: 1} file(s) as ${language.name}?")
            .setPositiveButton("Yes") { dialog, _ ->
                if (data.clipData != null) {
                    val uriList = ArrayList<Uri>()
                    for (i in 0 until data.clipData!!.itemCount) {
                        val filePath = data.clipData?.getItemAt(i)?.uri
                        if (filePath != null) {
                            uriList.add(filePath)
                        }
                    }
                    if (uriList.isNotEmpty()) {
                        MediaUtils.uploadMultiplePhoto(uriList, language, this@BaseActivity) {
                            // All items have been processed
                            // Perform any necessary post-processing or UI updates
                            Toast.makeText(this@BaseActivity, "Do not close for a few seconds..", Toast.LENGTH_SHORT).show()
                        }

                    }
                } else {
                    val filePath = data.data
                    Log.e(TAG, "onActivityResult: describeContents ${filePath?.scheme}")
                    data.data?.run {
                        MediaUtils.uploadPhoto(this, language, this@BaseActivity)
                    }
                    Log.e(TAG, "onActivityResult: $filePath")
                }
                dialog.dismiss()
                dialogInterface.dismiss()
            }.setNegativeButton("BACK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

}