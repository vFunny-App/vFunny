package vfunny.shortvideovfunnyapp.Live.data

import android.util.Log
import com.player.models.VideoData
import com.videopager.data.VideoDataRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import vfunny.shortvideovfunnyapp.Live.ui.BaseActivity
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.PostUtils.PostsManager
import vfunny.shortvideovfunnyapp.Post.model.Language
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
            val isAdsEnabledDeferred =  fetchAdsEnabled()
            val languageListDeferred =  fetchLanguageList()

            Log.i(TAG, "Final Data : isAdsEnabled $isAdsEnabledDeferred")
            Log.i(TAG, "Final Data : languageList ${languageListDeferred.size}")

            val unwatchedPosts = PostsManager.instance.getPosts(languageListDeferred)
            var count = 0
            var videoCount = 0
            unwatchedPosts.forEach {
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
                if (isAdsEnabledDeferred && videoCount % BaseActivity.ADS_FREQUENCY == 0 && videoCount != totalItemCount) {
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
            emit(videoItemList)
        }
    }

    private suspend fun fetchAdsEnabled(): Boolean {
        return suspendCoroutine { continuation ->
            User.isAdsEnabled { isEnabled ->
                continuation.resume(isEnabled)
            }
        }
    }

    private suspend fun fetchLanguageList(): List<Language> {
        return suspendCoroutine { continuation ->
            User().getCurrentUser { user ->
                if (user != null) {
                    continuation.resume(user.language)
                } else {
                    continuation.resume(Language.getAllLanguages()) // Provide a default value here
                }
            }
        }
    }
}
