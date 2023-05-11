package vfunny.shortvideovfunnyapp.Data.PostUtils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE
import vfunny.shortvideovfunnyapp.Data.model.Const
import vfunny.shortvideovfunnyapp.Data.model.Language
import vfunny.shortvideovfunnyapp.Data.model.Post
import vfunny.shortvideovfunnyapp.Login.model.User

/**
 * Created by shresthasaurabh86@gmail.com 09/05/2023.
 */
class PostsManager {
    private val TAG: String = "PostsManager"

    // The list of languages we have
    companion object {
        @JvmStatic
        val instance: PostsManager by lazy { PostsManager() }
    }

    // This function gets the posts from Firebase Firestore
    suspend fun getPosts(context: Context, languageList: List<Language>): List<Post> = withContext(Dispatchers.IO) {
        val langPostsCount = (100 / languageList.size).toLong()
        val postsList = mutableListOf<List<Post>>()

        if (languageList.contains(Language.WORLDWIDE)) {
            val wwDeferred = async { fetchPosts(langPostsCount, Const.kDataPostWorldwideKey, context) }
            postsList.add(wwDeferred.await())
        }
        if (languageList.contains(Language.ENGLISH)) {
            val enDeferred = async { fetchPosts(langPostsCount, Const.kDataPostEnglishKey, context) }
            postsList.add(enDeferred.await())
        }
        if (languageList.contains(Language.HINDI)) {
            val hiDeferred = async { fetchPosts(langPostsCount, Const.kDataPostHindiKey, context) }
            postsList.add(hiDeferred.await())
        }
        if (languageList.contains(Language.OTHERS)) {
            val otDeferred = async { fetchPosts(langPostsCount, Const.kDataPostOtherKey, context) }
            postsList.add(otDeferred.await())
        }
        return@withContext alternateLists(postsList)
    }

    // This function fetches posts for a specific language
    private suspend fun fetchPosts(
        langPostsCount: Long,
        langKey: String,
        context: Context,
    ): List<Post> = withContext(Dispatchers.IO) {

        if (BUILD_TYPE == "admin" || BUILD_TYPE == "adminDebug") {
            Log.d(TAG, "fetchPosts: FETCHING ADMIN POSTS")
            return@withContext try {
                FirebaseFirestore.getInstance().collection(langKey).orderBy(Const.kTimestampKey, Query.Direction.DESCENDING)
                    .limit(langPostsCount).get().await().toObjects(Post::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ADMIN posts for language $langKey", e)
                emptyList()
            }
        } else return@withContext try {
            FirebaseFirestore.getInstance().collection(langKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey())).orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kTimestampKey, Query.Direction.DESCENDING).limit(langPostsCount).get().await()
                .toObjects(Post::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching USER posts for language $langKey", e)
            Toast.makeText(context, "Error fetching posts for language $langKey", Toast.LENGTH_SHORT).show()
            emptyList()
        }
    }


    private fun alternateLists(postsList: MutableList<List<Post>>): List<Post> {
        val resultList = mutableListOf<Post>()
        val indices = mutableListOf<Int>()
        postsList.forEach { _ -> indices.add(0) }
        var listIndex = 0
        var done = false
        while (!done) {
            val currentList = postsList[listIndex]
            val currentIndex = indices[listIndex]
            if (currentIndex < currentList.size) {
                resultList.add(currentList[currentIndex])
                indices[listIndex]++
            }
            listIndex++
            if (listIndex == postsList.size) {
                listIndex = 0
                done = true
                indices.forEachIndexed { index, value ->
                    if (value < postsList[index].size) {
                        done = false
                    }
                }
            }
        }
        return resultList
    }
}