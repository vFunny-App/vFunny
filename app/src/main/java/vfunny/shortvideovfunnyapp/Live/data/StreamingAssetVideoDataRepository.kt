package vfunny.shortvideovfunnyapp.Live.data

import android.util.Log
import com.player.models.VideoData
import com.videopager.data.VideoDataRepository
import com.videopager.models.PostCollection
import com.videopager.models.VideoDataPaged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import vfunny.shortvideovfunnyapp.Live.ui.BaseActivity
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.PostUtils.PostsManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// get a stream of data, kinda like pagination from firebase database
class StreamingAssetVideoDataRepository() : VideoDataRepository {
    private val videoItemList = ArrayList<VideoData>()

    companion object {
        private const val TAG = "VideoDataRepo"
    }

    override fun videoData(): Flow<List<VideoData>> {
        return flow {
            val isAdsEnabled : Boolean =  fetchAdsEnabled()
            val userSelectedLanguageList :  List<VideoDataPaged> =  fetchLanguageList()
            Log.i(TAG, "Final Data : isAdsEnabled $isAdsEnabled")
            Log.i(TAG, "Final Data : userSelectedLanguageList size ${userSelectedLanguageList.size}")
            var getPostIterationCounter = 0
            var count = 0
            while (count < 1000) {
                val unwatchedPostCollection : PostCollection  = PostsManager.instance.getPosts(
                    userSelectedLanguageList
                )
                var videoCount = 0
                unwatchedPostCollection.alternateList?.forEach {
                    count++
                    videoCount++
                    videoItemList.add(
                        VideoData(
                            count.toString(),
                            it.video ?: "",
                            it.image ?: "",
                            key = it.key,
                            language = it.language,
                            timestamp = it.timestamp
                        )
                    )
                    val totalItemCount = BaseActivity.ITEM_COUNT_THRESHOLD
                    if (isAdsEnabled && videoCount % BaseActivity.ADS_FREQUENCY == 0 && videoCount != totalItemCount) {
                        count++
                        videoItemList.add(
                            VideoData(
                                count.toString(),
                                "",
                                "",
                                type = BaseActivity.ADS_TYPE
                            )
                        )
                    }
                }
                getPostIterationCounter ++
                for ((lastIndex, language) in userSelectedLanguageList) {
                    Log.i(TAG, "Final Output emit videoItemList lastIndex $lastIndex : language $language")
                }
                Log.i(TAG, "Final Output emit videoItemList size ${videoItemList.size}")
                Log.i(TAG, "Final Output Post iteration Counter $getPostIterationCounter")
                emit(videoItemList)
            }
        }
    }

    private suspend fun fetchAdsEnabled(): Boolean {
        return suspendCoroutine { continuation ->
            User.isAdsEnabled { isEnabled ->
                continuation.resume(isEnabled)
            }
        }
    }

    private suspend fun fetchLanguageList(): List<VideoDataPaged> {
        return suspendCoroutine { continuation ->
            User().getCurrentUser { user ->
                val list = mutableListOf<VideoDataPaged>()
                if (user != null) {
                    user.language.forEach {
                        list.add(VideoDataPaged(1,it))
                    }
                    continuation.resume(list)
                } else {
                    continuation.resume(emptyList()) // Provide a default value here
                }
            }
        }
    }
}
