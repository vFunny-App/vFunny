package com.exo.players

import android.content.Context
import com.MyApp
import com.exo.data.DiffingVideoDataUpdater
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.player.players.AppPlayer
import kotlinx.coroutines.Dispatchers


class ExoAppPlayerFactory(context: Context) : AppPlayer.Factory {
    // Use application context to avoid leaking Activity.
    private val appContext = context.applicationContext

    override fun create(config: AppPlayer.Factory.Config): AppPlayer {
        val simpleCache: SimpleCache = MyApp.simpleCache
        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1000, 5000, 1000, 1000)
            .createDefaultLoadControl()
        val exoPlayer = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                if (config.loopVideos) {
                    loopVideos()
                }
            }

        val updater = DiffingVideoDataUpdater(Dispatchers.Default)
        return ExoAppPlayer(appContext, exoPlayer, updater)
    }
}
