package com.videopager.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import androidx.viewpager2.widget.ViewPager2
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import com.player.ui.AppPlayerView
import com.videopager.DownloadWatermarkManager
import com.videopager.R
import com.videopager.databinding.VideoPagerFragmentBinding
import com.videopager.models.*
import com.videopager.ui.extensions.*
import com.videopager.vm.VideoPagerViewModel
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoPagerFragment(
    private val viewModelFactory: (SavedStateRegistryOwner) -> ViewModelProvider.Factory,
    private val appPlayerViewFactory: AppPlayerView.Factory,
    private val imageLoader: ImageLoader,
) : Fragment(R.layout.video_pager_fragment) {
    private val viewModel: VideoPagerViewModel by viewModels { viewModelFactory(this) }
    private lateinit var adapter: PagerAdapter
    private var input_video_uri: File? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var alertDialog: AlertDialog

    private val saveVideoLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) {
            it?.let { uri ->
                val out = requireActivity().contentResolver.openOutputStream(uri)
                if (input_video_uri != null) {
                    val ip: InputStream = FileInputStream(input_video_uri)
                    out?.let {
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (ip.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                        }
                        ip.close()
                        out.flush()
                        out.close()
                    }
                    if (input_video_uri!!.isDirectory) {
                        input_video_uri!!.listFiles()?.forEach { file ->
                            if (!file.isDirectory) {
                                file.delete()
                            }
                        }
                    } else {
                        input_video_uri!!.delete()
                    }
                    Toast.makeText(context, "Download Completed", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = VideoPagerFragmentBinding.bind(view)
        // This single player view instance gets attached to the ViewHolder of the active ViewPager page
        val appPlayerView = appPlayerViewFactory.create(view.context)
        adapter = PagerAdapter(imageLoader)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1// Preload neighbouring page image previews
// Create the ProgressBar
        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyle)
        progressBar.isIndeterminate = true

// Create the AlertDialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Processing Video...")
        builder.setMessage("Please Wait")
        builder.setView(progressBar)
        builder.setCancelable(false)
        alertDialog = builder.create()

        val states = viewModel.states.onEach { state ->
            // Await the list submission so that the adapter list is in sync with state.videoData
            adapter.awaitList(state.videoData)
            // Attach the player to the View whenever it's ready. Note that attachPlayer can
            // be false while appPlayer is non-null during configuration changes and, conversely,
            // attachPlayer can be true while appPlayer is null when the appPlayer hasn't been
            // set up but the view is ready for it. That is why both are checked here.
            if (state.attachPlayer && state.appPlayer != null) {
                appPlayerView.attach(state.appPlayer)
            } else {
                appPlayerView.detachPlayer()
            }
            if (state.downloadDialogState.isShowing) {
                Log.e("@DOWNLOAD", "downloadList: Updated")
                Log.e(
                    "@DOWNLOAD",
                    "downloadList: progress  : ${state.downloadDialogState.progress}"
                )
                alertDialog.setTitle("Downloading Video...")
                alertDialog.setMessage("Please Wait")
                if (!alertDialog.isShowing) {
                    alertDialog.show()
                }
            } else {
                if (alertDialog.isShowing) {
                    alertDialog.dismiss()
                }
            }
            // Restore any saved page state from process recreation and configuration changes.
            // Guarded by an isIdle check so that state emissions mid-swipe or during page change
            // animations are ignored. There would have a jarring page-change effect without that.
            if (binding.viewPager.isIdle) {
                binding.viewPager.setCurrentItem(state.page, false)
            }
            // Can't query any ViewHolders if the adapter has no pages
            if (adapter.currentList.isNotEmpty()) {
                // Set the player view on the active page. Note that ExoPlayer won't render
                // any frames until the output view (here, appPlayerView) is on-screen
                adapter.attachPlayerView(appPlayerView, state.page)

                // If the player media is rendering frames, then show the player
                if (state.showPlayer) {
                    adapter.showPlayerFor(state.page)
                }
            }
        }

        val effects = viewModel.effects.onEach { effect ->
            when (effect) {
                is PageEffect -> adapter.renderEffect(binding.viewPager.currentItem, effect)
                is PlayerErrorEffect -> Snackbar.make(
                    binding.root,
                    "Image/${effect.throwable.message}",
                    Snackbar.LENGTH_LONG
                ).show()

                is ShareWhatsappEffect -> shareToWhatsapp(effect.mediaUri)
                is SaveVideoDataEffect -> {
                    Log.e(DownloadWatermarkManager.TAG, "cancel: Cancelled 2")
                    if (effect.outputFilePath.exists() && effect.outputFilePath.totalSpace != 0L) {
                        val currentTimeMillis = System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val dateTimeString = dateFormat.format(Date(currentTimeMillis))
                        input_video_uri = effect.outputFilePath
                        saveVideoLauncher.launch("Vfunny-$dateTimeString")
                    }
                }

                is TappedShareEffect -> shareClick(effect.mediaUri)
            }
        }

        val events = merge(
            viewLifecycleOwner.lifecycle.viewEvents(),
            binding.viewPager.viewEvents(),
            adapter.viewEvents(),
        ).onEach(viewModel::processEvent)

        merge(states, effects, events).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun Lifecycle.viewEvents(): Flow<ViewEvent> {
        return events().filter { event -> event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP }
            .map { event ->
                // Fragment starting or stopping is a signal to create or tear down the player, respectively.
                // The player should not be torn down across config changes, however.
                when (event) {
                    Lifecycle.Event.ON_START -> PlayerLifecycleEvent.Start
                    Lifecycle.Event.ON_STOP -> PlayerLifecycleEvent.Stop(requireActivity().isChangingConfigurations)
                    else -> error("Unhandled event: $event")
                }
            }
    }

    private fun ViewPager2.viewEvents(): Flow<ViewEvent> {
        return merge(
            // Idling on a page after a scroll is a signal to try and change player playlist positions
            pageIdlings().map { OnPageSettledEvent(currentItem) },
            // A page change (which can happen before a page is idled upon) is a signal to pause media. This
            // is useful for when a user is quickly swiping thru pages and the idle state isn't getting reached.
            // It doesn't make sense for a video on a previous page to continue playing while the user is
            // swiping quickly thru pages.
            pageChanges().map { PauseVideoEvent })
    }

    private fun PagerAdapter.viewEvents(): Flow<ViewEvent> {
        return clicks().map { event ->
            when (event) {
                is TappedPlayerEvent -> event
                is TappedWhatsappEvent -> event
                is TappedShareEvent -> event
                is TappedDownloadEvent -> event
                else -> throw IllegalArgumentException("Unknown event type: $event")
            }
        }
    }


    private fun shareToWhatsapp(mediaUri: String) {
        val shareMessage =
            "Download vFunny App: https://play.google.com/store/apps/details?id=vfunny.shortvideovfunnyapp\nWatch this Video $mediaUri"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sharing Video")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            setPackage("com.whatsapp")
        }
        requireContext().startActivity(Intent.createChooser(intent, "Share Video"))
    }

    private fun shareClick(mediaUri: String) {
        val shareMessage =
            "Download vFunny App: https://play.google.com/store/apps/details?id=vfunny.shortvideovfunnyapp\nWatch this Video $mediaUri"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sharing Video")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        requireContext().startActivity(Intent.createChooser(intent, "Share Video"))
    }

}
