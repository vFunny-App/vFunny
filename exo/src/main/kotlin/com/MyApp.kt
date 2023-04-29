package com

import android.app.Application
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.onesignal.OneSignal


class MyApp : Application() {

    private val ONESIGNAL_APP_ID = "0695d934-66e2-43f6-9853-dbedd55b86ca"

    companion object {
        lateinit var simpleCache: SimpleCache
        const val exoPlayerCacheSize: Long = 90 * 1024 * 1024
        lateinit var exoDatabaseProvider: ExoDatabaseProvider
        lateinit var leastRecentlyUsedCacheEvictor: LeastRecentlyUsedCacheEvictor
    }

    override fun onCreate() {
        super.onCreate()
        exoDatabaseProvider = ExoDatabaseProvider(this)
        leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)
        simpleCache = SimpleCache(cacheDir, leastRecentlyUsedCacheEvictor, exoDatabaseProvider)

        //TODO  Logging set to help debug issues, remove before releasing your app.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        // OneSignal Initialization
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
    }
}