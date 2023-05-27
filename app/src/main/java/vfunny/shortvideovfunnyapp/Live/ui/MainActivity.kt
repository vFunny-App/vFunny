package vfunny.shortvideovfunnyapp.Live.ui

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.*
import com.player.models.VideoData
import com.videopager.ui.VideoPagerFragment
import vfunny.shortvideovfunnyapp.Lang.ui.LangListActivity
import vfunny.shortvideovfunnyapp.Live.di.MainModule
import vfunny.shortvideovfunnyapp.Live.ui.list.ListActivity
import vfunny.shortvideovfunnyapp.Live.ui.migrate.MigrateListActivity
import vfunny.shortvideovfunnyapp.Login.Loginutils.AuthManager
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Const
import vfunny.shortvideovfunnyapp.R
import vfunny.shortvideovfunnyapp.databinding.MainActivityBinding
import vfunny.shortvideovfunnyapp.vm.MainActivityViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import vfunny.shortvideovfunnyapp.Live.ui.extensions.events
import vfunny.shortvideovfunnyapp.models.*

class MainActivity : BaseActivity(), AuthManager.AuthListener {
    private val TAG: String = "MainActivity"
    private var mUser: User? = null
    private var context: Context? = null
    private var showLanguage = false
    private lateinit var binding: MainActivityBinding

    private val viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

    // Extra buffer capacity so that emissions can be sent outside a coroutine
    private val clicks = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    private fun clicks() = clicks.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manual dependency injection
        context = this.applicationContext
        super.onCreate(savedInstanceState)
        showLanguage = intent.getBooleanExtra("showLanguage", false)
        binding = MainActivityBinding.inflate(layoutInflater)
        initWelcomeAnimation()

        val states = viewModel.states.onEach { state ->

        }

        val effects = viewModel.effects.onEach { effect ->
            when (effect) {
                is PageEffect -> Log.e(TAG, "onCreate: ", )
                is AnimationEffect -> Log.e(TAG, "onCreate: ", )
                is ResetAnimationsEffect -> Log.e(TAG, "onCreate: ", )
                is TappedLanguageEffect -> Log.e(TAG, "onCreate: ", )
                is TappedLanguageListEffect -> Log.e(TAG, "onCreate: ", )
                is TappedUpdatesNotifyEffect -> Log.e(TAG, "onCreate: ", )
                is PlayerErrorEffect -> Log.e(TAG, "onCreate: ", )
            }
        }

        val events = merge(
            lifecycle.viewEvents(),
            viewEvents(),
        ).onEach(viewModel::processEvent)

        merge(states, effects, events).launchIn(lifecycleScope)

        if (User.currentKey() == null) {
            AuthManager.getInstance().showLogin(this) // Show login screen if user key is null
        } else {
            fetchPosts()
        }
        setContentView(binding.root)
        setUiFromBuildType()
        binding.setLanguageBtn.setOnClickListener {
            showLanguageDialog()
        }
        if (showLanguage) {
            showLanguageDialog()
        }
    }

    private fun viewEvents(): Flow<ViewEvent> {
        return clicks().map { event ->
            when (event) {
                is TappedAddPostsEvent -> event
                is ToggleAdsEvent -> event
                is TappedUpdatesNotifyEvent -> event
                is TappedLanguageEvent -> event
                is TappedLanguageListEvent -> event
                else -> throw IllegalArgumentException("Unknown event type: $event")
            }
        }
    }


    private fun showLanguageDialog() {
        showLanguage = false
        val dialog = LanguageSelectionDialog(this)
        dialog.setLanguageSelectionCallback(object : LanguageSelectionCallback {
            override fun onLanguageSelected() {
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        })
        dialog.show()
    }

    private fun initWelcomeAnimation() {
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
    }

    private fun showConfirmationDialog(message: String, value: Boolean) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setMessage(message).setPositiveButton("Yes") { dialog, id ->
            // User clicked Yes button
            FirebaseDatabase.getInstance().getReference(Const.kAdsKey).setValue(value)
                .addOnSuccessListener {
                    isAdsEnabled = value
                }.addOnFailureListener {
                    binding.adsSwitch.isChecked = !value
                    Toast.makeText(
                        this@MainActivity,
                        "Something went wrong while changing ads Status",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e(TAG, "showConfirmationDialog: Error : ", it)
                }
        }.setNegativeButton("No") { dialog, id ->
            // User cancelled the dialog
            binding.adsSwitch.isChecked = !value
        }
        builder.create().show()
    }

    override fun hideAdminUI() {
        binding.LangListBtn.visibility = View.GONE
        binding.migratelistBtn.visibility = View.GONE
        binding.listBtn.visibility = View.GONE
        binding.addBtn.visibility = View.GONE
        binding.updateNotification.visibility = View.GONE
        binding.addCard.visibility = View.GONE
    }

    override fun getAdsStatus() {
        val adsEnabled = User.adsDatabaseReference()
        adsEnabled.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isAdsEnabled = dataSnapshot.getValue(Boolean::class.java) == true
                binding.adsSwitch.isChecked = isAdsEnabled
                binding.adsSwitch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        showConfirmationDialog("Enable Ads?", true)
                    } else {
                        showConfirmationDialog("Disable Ads?", false)
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

    override fun addAdminButtons() {
        binding.addBtn.setOnClickListener {
            if (MediaUtils.storagePermissionGrant(this@MainActivity)) {
                MediaUtils.openVideoLibrary(this@MainActivity, MediaUtils.REQUEST_VIDEO_PICK)
            } else {
                //TODO open settings to change permissions
            }
        }
        binding.listBtn.setOnClickListener {
            val intent = Intent(this, ListActivity::class.java)
            startActivity(intent)
        }
        binding.migratelistBtn.setOnClickListener {
            val intent = Intent(this, MigrateListActivity::class.java)
            startActivity(intent)
        }
        binding.LangListBtn.setOnClickListener {
            val intent = Intent(this, LangListActivity::class.java)
            startActivity(intent)
        }
        binding.updateNotification.setOnClickListener { showUpdateNotificationConfirmationDialog() }
    }

    override fun showVideos(videoItemList: ArrayList<VideoData>) {
        Log.d(TAG, "onDataChange: FINAL videoItemList.size ${videoItemList.size}")
        val module = MainModule(this, videoItemList)
        supportFragmentManager.fragmentFactory = module.fragmentFactory
        supportFragmentManager.commit {
            replace<VideoPagerFragment>(binding.fragmentContainer.id)
        }
        binding.animationView.clearAnimation()
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.loadingLyt.removeAllViewsInLayout()
    }

    override fun showEmptyVideos() {
        binding.animationView.clearAnimation()
        binding.fragmentContainer.removeAllViewsInLayout()
        binding.fragmentContainer.visibility = View.GONE
        binding.loadingLyt.removeAllViewsInLayout()
        binding.emptyViewsLyt.visibility = View.VISIBLE
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


    private fun Lifecycle.viewEvents(): Flow<ViewEvent> {
        return events().filter { event -> event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP }
            .map { event ->
                // Fragment starting or stopping is a signal to create or tear down the player, respectively.
                // The player should not be torn down across config changes, however.
                when (event) {
                    Lifecycle.Event.ON_START -> HandleIntentsEvent.Start
                    Lifecycle.Event.ON_STOP -> HandleIntentsEvent.Stop(isChangingConfigurations)
                    else -> error("Unhandled event: $event")
                }
            }
    }

}
