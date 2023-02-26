package com.exo.players

import android.content.Context
import com.MyApp
import com.exo.data.DiffingVideoDataUpdater
import com.google.android.exoplayer2.ExoPlayer
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

        val exoPlayer = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
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
