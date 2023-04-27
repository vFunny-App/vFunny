package vfunny.shortvideovfunnyapp.Live.data

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.player.models.VideoData
import com.videopager.data.VideoDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import vfunny.shortvideovfunnyapp.Live.ui.MainActivity
import vfunny.shortvideovfunnyapp.Login.data.Post

class ProgressiveAssetVideoDataRepository(private val query: Query, private val isAdsEnable: Boolean, ) : VideoDataRepository {
    private val TAG: String = "PrgesvAstVdoDtaRepo"
    private var numItemsLoaded = 0
    private var count = 0
    private val videoDataFlow = MutableStateFlow<List<VideoData>>(emptyList())

    companion object {
        const val ADS_TYPE = "ads"
        const val ITEM_COUNT_THRESHOLD = 5
        const val ADS_FREQUENCY = 5
    }

    private val childEventListener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            numItemsLoaded++
            val post = snapshot.getValue(Post::class.java)
            post?.let {
                val video: String = it.video ?: ""
                val image: String = it.image ?: ""
                val key: String? = snapshot.key
                Log.e(TAG, "onChildAdded: key =  $key", )
                val item = VideoData(
                    numItemsLoaded.toString(),
                    video,
                    image,
                    key = key
                )
                videoDataFlow.value = videoDataFlow.value + listOf(item)
            }
            Log.e(TAG, "onChildAdded: $numItemsLoaded", )
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "Error fetching video data: $error")
        }
    }

    override fun videoData(): Flow<List<VideoData>> {
            query.addChildEventListener(childEventListener)
        return videoDataFlow
    }
}
