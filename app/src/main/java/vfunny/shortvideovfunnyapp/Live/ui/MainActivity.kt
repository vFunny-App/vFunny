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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.google.common.reflect.TypeToken
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.onesignal.OneSignal
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import kotlinx.coroutines.Dispatchers
import vfunny.shortvideovfunnyapp.BuildConfig.APPLICATION_ID
import vfunny.shortvideovfunnyapp.Live.di.MainModule
import vfunny.shortvideovfunnyapp.Login.Loginutils.AuthManager
import vfunny.shortvideovfunnyapp.Login.data.Const
import vfunny.shortvideovfunnyapp.Login.data.User
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.databinding.MainActivityBinding
import java.lang.Exception
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity(), AuthManager.AuthListener {
    private val TAG: String = "MainActivity"
    private var seenList = arrayListOf<String>()
    private var mUser: User? = null
    private var context: Context? = null
    private var activity: MainActivity? = null
    private var notificationVideoUrl: String? = null
    private var notificationThumbnailUrl: String? = null
    private var isAdsEnabled: Boolean = false

    companion object {
        private val postsDbRef =
            FirebaseDatabase.getInstance().getReference("posts").limitToLast(100)
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
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            }
        }
        User.currentKey()?.let { currentKey ->
            User.collection(currentKey).run {
                addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val mUser = dataSnapshot.getValue(User::class.java)
                        mUser?.seen?.let { seen ->
                            Log.e(TAG, "seenList: $seen")
                            val gson = Gson()
                            val listType = object : TypeToken<ArrayList<String>>() {}.type
                            try {
                                seenList = gson.fromJson(seen, listType)
                                val videoItemList = ArrayList<VideoData>()

                                lifecycleScope.launch {
                                    val snapshot = withContext(Dispatchers.IO) {
                                        postsDbRef.get().await()
                                    }

                                    var count = 0
                                    var videoCount = 0

                                    if (notificationVideoUrl != null) {
                                        count++
                                        videoItemList.add(
                                            VideoData(
                                                count.toString(),
                                                notificationVideoUrl.toString(),
                                                notificationThumbnailUrl.toString(),
                                                key = ""
                                            )
                                        )
                                    }

                                    snapshot.children.reversed().forEach {
                                        val video: String = it.child("video").value.toString()
                                        val image: String = it.child("image").value.toString()
                                        for (key in seenList) {
                                            if (it.key == key) {
                                                return@forEach
                                            }
                                        }
                                        count++
                                        videoCount++
                                        videoItemList.add(
                                            VideoData(
                                                count.toString(),
                                                video,
                                                image,
                                                key = it.key
                                            )
                                        )
                                        val totalItemCount = ITEM_COUNT_THRESHOLD + seenList.size
                                        if (isAdsEnabled && videoCount % ADS_FREQUENCY == 0 && videoCount != totalItemCount) {
                                            count++
                                            videoItemList.add(
                                                VideoData(
                                                    count.toString(),
                                                    "",
                                                    "",
                                                    type = ADS_TYPE,
                                                    key = null
                                                )
                                            )
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
                            } catch (e: Exception) {
                                Log.e(TAG, "seenList: Error : $e")
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            context,
                            "Error Fetching Data! Check Network Connection!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        } ?: AuthManager.getInstance().showLogin(this) // Show login screen if user key is null

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
    }

    private fun getAdsStatus() {
        val adsEnabled = User.Ads()
        adsEnabled.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isAdsEnabled = dataSnapshot.getValue(Boolean::class.java) == true
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
        if (User.current() != null && mUser?.name == null) {
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
    }

}
