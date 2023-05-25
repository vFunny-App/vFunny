package com.videopager.vm

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.player.players.AppPlayer
import com.videopager.R
import com.videopager.data.ProgressListener
import com.videopager.data.VideoDataRepository
import com.videopager.models.*
import com.videopager.ui.extensions.ViewState
import com.videopager.watermark.AddWatermarkVideoBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Owns a stateful [ViewState.appPlayer] instance that will get created and torn down in parallel
 * with Activity lifecycle state changes.
 */
internal class VideoPagerViewModel(
    private val repository: VideoDataRepository,
    private val appPlayerFactory: AppPlayer.Factory,
    private val handle: PlayerSavedStateHandle,
    initialState: ViewState,
) : MviViewModel<ViewEvent, ViewResult, ViewState, ViewEffect>(initialState) {

    override fun onStart() {
        processEvent(LoadVideoDataEvent)
    }

    override fun Flow<ViewEvent>.toResults(): Flow<ViewResult> {
        // MVI boilerplate
        return merge(
            filterIsInstance<LoadVideoDataEvent>().toLoadVideoDataResults(),
            filterIsInstance<PlayerLifecycleEvent>().toPlayerLifecycleResults(),
            filterIsInstance<TappedPlayerEvent>().toTappedPlayerResults(),
            filterIsInstance<TappedWhatsappEvent>().toTappedWhatsappResults(),
            filterIsInstance<TappedShareEvent>().toTappedShareResults(),
            filterIsInstance<TappedDownloadEvent>().toTappedDownloadResults(),
            filterIsInstance<OnPageSettledEvent>().toPageSettledResults(),
            filterIsInstance<PauseVideoEvent>().toPauseVideoResults()
        )
    }

    private fun Flow<LoadVideoDataEvent>.toLoadVideoDataResults(): Flow<ViewResult> {
        Log.e("TAG", "toPageSettledResults: This Ran 1")
        return flatMapLatest { repository.videoData() }
            .map { videoData ->
                val appPlayer = states.value.appPlayer
                // If the player exists, it should be updated with the latest video data that came in
                appPlayer?.setUpWith(videoData, handle.get())
                // Capture any updated index so UI page state can stay in sync. For example, a video
                // may have been added to the page before the currently active one. That means the
                // the current video/page index will have changed
                val index = appPlayer?.currentPlayerState?.currentMediaItemIndex ?: 0
                LoadVideoDataResult(videoData, index)
            }
    }

    /**
     * This is a single flow instead of two distinct ones (e.g. one for starting, one for stopping)
     * so that when the PlayerLifecycleEvent type changes from upstream, the flow initiated by the
     * previous type gets unsubscribed from (see: [flatMapLatest]). This is necessary to cancel flows
     * tied to the AppPlayer instance, e.g. [AppPlayer.onPlayerRendering], when the player is being
     * torn down.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<PlayerLifecycleEvent>.toPlayerLifecycleResults(): Flow<ViewResult> {
        val managePlayerInstance = filterNot { event ->
            // Don't need to create a player when one already exists. This can happen
            // after a configuration change
            states.value.appPlayer != null && event is PlayerLifecycleEvent.Start
                    // Don't tear down the player across configuration changes
                    || event is PlayerLifecycleEvent.Stop && event.isChangingConfigurations
        }.flatMapLatest { event ->
            when (event) {
                is PlayerLifecycleEvent.Start -> createPlayer()
                is PlayerLifecycleEvent.Stop -> tearDownPlayer()
            }
        }

        return merge(
            mapLatest { event -> AttachPlayerToViewResult(doAttach = event is PlayerLifecycleEvent.Start) },
            managePlayerInstance
        )
    }

    private suspend fun createPlayer(): Flow<ViewResult> {
        Log.e("TAG", "createPlayer:  This Ran 3")
        check(states.value.appPlayer == null) { "Tried to create a player when one already exists" }
        val config = AppPlayer.Factory.Config(loopVideos = true)
        val appPlayer = appPlayerFactory.create(config)
        // If video data already exists then the player should have that video data set on it. This
        // can happen because the player has a lifecycle tied to Activity starting/stopping.
        states.value.videoData?.let { videoData -> appPlayer.setUpWith(videoData, handle.get()) }
        return merge(
            flowOf(CreatePlayerResult(appPlayer)),
            appPlayer.onPlayerRendering().map { OnPlayerRenderingResult },
            appPlayer.errors().map(::PlayerErrorResult)
        )
    }

    private fun tearDownPlayer(): Flow<ViewResult> {
        val appPlayer = requireNotNull(states.value.appPlayer)
        // Keep track of player state so that it can be restored across player recreations.
        handle.set(appPlayer.currentPlayerState)
        // Videos are a heavy resource, so tear player down when the app is not in the foreground.
        appPlayer.release()
        return flowOf(TearDownPlayerResult)
    }

    private fun Flow<TappedPlayerEvent>.toTappedPlayerResults(): Flow<ViewResult> {
        return mapLatest {
            val appPlayer = requireNotNull(states.value.appPlayer)
            val drawable = if (appPlayer.currentPlayerState.isPlaying) {
                appPlayer.pause()
                R.drawable.pause
            } else {
                appPlayer.play()
                R.drawable.play
            }
            TappedPlayerResult(drawable)
        }
    }

    private fun Flow<TappedWhatsappEvent>.toTappedWhatsappResults(): Flow<ViewResult> {
        return mapLatest {
            val videoData = requireNotNull(states.value.videoData)
            val page = requireNotNull(states.value.page)
            TappedWhatsappResult(videoData[page].mediaUri)
        }
    }

    private fun Flow<TappedShareEvent>.toTappedShareResults(): Flow<ViewResult> {
        return mapLatest {
            val videoData = requireNotNull(states.value.videoData)
            val page = requireNotNull(states.value.page)
            TappedShareResult(videoData[page].mediaUri)
        }
    }

    private fun Flow<TappedDownloadEvent>.toTappedDownloadResults(): Flow<ViewResult> {
        return flatMapLatest {
            flow {
                val videoData = requireNotNull(states.value.videoData)
                val page = requireNotNull(states.value.page)
                val mediaUri = videoData[page].mediaUri
                val downloadList = requireNotNull(states.value.downloadList)
                if (mediaUri.isNotEmpty()) {
                    val progressChannel = Channel<Int>() // Channel to receive progress updates
                    val progressJob = viewModelScope.launch(Dispatchers.IO) {
                        AddWatermarkVideoBuilder(mediaUri).progressListener(object :
                            ProgressListener {
                            override fun onProgress(page: Int, progress: Int) {
                                for (pageMap in  requireNotNull(states.value.downloadList)) {
                                    if (pageMap.containsKey(page)) { // TODO need to handle multi click on download button
                                        // Update the progress of the page
                                        pageMap[progress] = progress
                                        break
                                    } else {
                                        Log.e("@DOWNLOAD", "onProgress: Added $page to $progress", )
                                        downloadList.add( hashMapOf(page to progress))
                                    }
                                }
                                launch { progressChannel.send(progress) }
                            }
                        }).build()
                    }

                    // Emit progress updates from the channel as ViewResult
                    for (progress in progressChannel) {
                        emit(DownloadVideoDataResult(downloadList))
                    }

                    // Cancel the progress job when the flow completes or is cancelled
                    progressJob.cancel()
                }
            }
        }
    }

    private fun Flow<OnPageSettledEvent>.toPageSettledResults(): Flow<ViewResult> {
        return mapLatest { event ->
            Log.e("TAG", "toPageSettledResults: This Ran 2")
            val appPlayer = requireNotNull(states.value.appPlayer)
            appPlayer.playMediaAt(event.page)
            OnNewPageSettledResult(page = event.page)
        }
    }

    private fun Flow<PauseVideoEvent>.toPauseVideoResults(): Flow<ViewResult> {
        return mapLatest {
            val appPlayer = requireNotNull(states.value.appPlayer)
            appPlayer.pause()
            NoOpResult
        }
    }

    override fun ViewResult.reduce(state: ViewState): ViewState {
        // MVI reducer boilerplate
        return when (this) {
            is LoadVideoDataResult -> state.copy(
                videoData = videoData,
                page = currentMediaItemIndex
            )

            is DownloadVideoDataResult -> state.copy(downloadList = downloadList)
            is CreatePlayerResult -> state.copy(appPlayer = appPlayer)
            is TearDownPlayerResult -> state.copy(appPlayer = null)
            is OnNewPageSettledResult -> state.copy(page = page, showPlayer = false)
            is OnPlayerRenderingResult -> state.copy(showPlayer = true)
            is AttachPlayerToViewResult -> state.copy(attachPlayer = doAttach)
            else -> state
        }
    }

    override fun Flow<ViewResult>.toEffects(): Flow<ViewEffect> {
        return merge(
            filterIsInstance<TappedPlayerResult>().toTappedPlayerEffects(),
            filterIsInstance<TappedWhatsappResult>().toSendWhatsappEffects(),
            filterIsInstance<TappedShareResult>().toTappedShareEffects(),
            filterIsInstance<OnNewPageSettledResult>().toNewPageSettledEffects(),
            filterIsInstance<PlayerErrorResult>().toPlayerErrorEffects()
        )
    }

    private fun Flow<TappedPlayerResult>.toTappedPlayerEffects(): Flow<ViewEffect> {
        return mapLatest { result -> AnimationEffect(result.drawable) }
    }

    private fun Flow<TappedWhatsappResult>.toSendWhatsappEffects(): Flow<ViewEffect> {
        return mapLatest { result -> ShareWhatsappEffect(result.mediaUri) }
    }

    private fun Flow<TappedShareResult>.toTappedShareEffects(): Flow<ViewEffect> {
        return mapLatest { result -> TappedShareEffect(result.mediaUri) }
    }

    private fun Flow<OnNewPageSettledResult>.toNewPageSettledEffects(): Flow<ViewEffect> {
        return mapLatest { ResetAnimationsEffect }
    }

    private fun Flow<PlayerErrorResult>.toPlayerErrorEffects(): Flow<ViewEffect> {
        return mapLatest { result -> PlayerErrorEffect(result.throwable) }
    }
}
