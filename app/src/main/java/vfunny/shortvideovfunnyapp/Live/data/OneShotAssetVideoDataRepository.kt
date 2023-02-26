package vfunny.shortvideovfunnyapp.Live.data

import android.util.Log
import com.player.models.VideoData
import com.videopager.data.VideoDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.ArrayList

// Here for demo purposes I've used assets local to the app. This could be any other implementation,
// however. e.g. remotely fetched videos via Retrofit, Firebase, etc.
class OneShotAssetVideoDataRepository(var videoItemList: ArrayList<VideoData>) : VideoDataRepository {
    override fun videoData(): Flow<List<VideoData>> {
            Log.e("TAG", "videoDataFetch: SUCCESS!")
             return flowOf(videoItemList)
    }
}
