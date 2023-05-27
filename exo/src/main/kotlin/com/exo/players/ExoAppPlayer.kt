package com.exo.players

import android.content.Context
import android.content.Intent
import com.exo.data.VideoDataUpdater
import com.exo.service.VideoPreLoadingService
import com.exo.ui.MAX_CACHED_VIDEOS
import com.exo.ui.VIDEO_LIST
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.player.models.PlayerState
import com.player.models.VideoData
import com.player.players.AppPlayer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class ExoAppPlayer(
    private var appContext: Context,
    internal val player: Player,
    private val updater: VideoDataUpdater,
) : AppPlayer {
    private lateinit var vData: List<VideoData>
    override val currentPlayerState: PlayerState get() = player.toPlayerState()
    private var isPlayerSetUp = false

    override suspend fun setUpWith(videoData: List<VideoData>, playerState: PlayerState?) {
        vData = videoData
        /** Delegate video insertion, removing, moving, etc. to this [updater] */
        updater.update(player = player, incoming = videoData)
        // Player should only have saved state restored to it one time per instance of this class.
        if (!isPlayerSetUp) {
            setUpPlayerState(playerState)
            isPlayerSetUp = true
            initPreLoading(0)
        }
        player.prepare()
    }

    private fun setUpPlayerState(playerState: PlayerState?) {
        val currentMediaItems = player.currentMediaItems
         // When restoring saved state, the saved media item might be not be in the player's current
         // collection of media items. In that case, the saved media item cannot be restored.
        val canRestoreSavedPlayerState = playerState != null && currentMediaItems.any { mediaItem -> mediaItem.mediaId == playerState.currentMediaItemId }

        val reconciledPlayerState = if (canRestoreSavedPlayerState) {
            requireNotNull(playerState)
        } else {
            PlayerState.INITIAL
        }

        val windowIndex = currentMediaItems.indexOfFirst { mediaItem ->
            mediaItem.mediaId == reconciledPlayerState.currentMediaItemId
        }
        if (windowIndex != -1) {
            player.seekTo(windowIndex, reconciledPlayerState.seekPositionMillis)
        }
        player.playWhenReady = reconciledPlayerState.isPlaying
    }

    // A signal that video content is immediately ready to play; any preview images
    // on top of the video can be hidden to reveal actual video playback underneath.
    override fun onPlayerRendering(): Flow<Unit> = callbackFlow {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                trySend(Unit)
            }
        }
        player.addListener(listener)
        awaitClose { player.removeListener(listener) }
    }

    override fun errors(): Flow<Throwable> = callbackFlow {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                trySend(error)
            }
        }

        player.addListener(listener)

        awaitClose { player.removeListener(listener) }
    }

    private fun Player.toPlayerState(): PlayerState {
        return PlayerState(
            currentMediaItemId = currentMediaItem?.mediaId,
            currentMediaItemIndex = currentMediaItemIndex,
            seekPositionMillis = currentPosition,
            isPlaying = playWhenReady
        )
    }

    override fun playMediaAt(position: Int) {
        // Already playing media at this position; nothing to do
        if (player.currentMediaItemIndex == position && player.isPlaying) return
        player.seekToDefaultPosition(position)
        player.playWhenReady = true
        player.prepare() // Recover from any errors that may have happened at previous media positions
        if (position % 3 == 0) initPreLoading(position)
    }

    private fun initPreLoading(position: Int) {
        val videoList = ArrayList<String>()
        var limit : Int = vData.size - position
        var count = 0
        while(limit > 0 && count <= MAX_CACHED_VIDEOS) {
            videoList.add(vData[position + count].mediaUri)
            limit -= 1
            count += 1
        }
        if(videoList.isNotEmpty())startPreLoadingService(videoList)
    }

    private fun startPreLoadingService(videoList : ArrayList<String>) {
        val preloadingServiceIntent = Intent(appContext, VideoPreLoadingService::class.java)
        preloadingServiceIntent.putStringArrayListExtra(VIDEO_LIST, videoList)
        appContext.startService(preloadingServiceIntent)
    }

    override fun play() {
        player.prepare() // Recover from any errors
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun release() {
        player.release()
    }
}
