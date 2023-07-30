package vfunny.shortvideovfunnyapp.Post.PostUtils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import vfunny.shortvideovfunnyapp.BuildConfig.BUILD_TYPE
import vfunny.shortvideovfunnyapp.Login.model.User
import vfunny.shortvideovfunnyapp.Post.model.Const
import vfunny.shortvideovfunnyapp.Post.model.Language
import com.videopager.models.Post

/**
 * Created by shresthasaurabh86@gmail.com 09/05/2023.
 */
class PostsManager {

    companion object {
        private const val TAG: String = "PostsManager"

        @JvmStatic
        val instance: PostsManager by lazy { PostsManager() }
    }

    /**
     * Retrieves posts from Firebase RTDB for the specified languages
     * @param languageList list of languages to fetch posts for
     * @return list of posts
     */
    suspend fun getPosts(languageList: List<Language>): List<Post> = withContext(Dispatchers.IO) {
        // Determine the number of posts to fetch for each language based on the total number of languages to fetch
        val langPostsCount = (1000 / languageList.size)
        // Create an empty list of post lists
        val postsList = mutableListOf<List<Post?>>()
        // Create a deferred task for each language in the languageList
        val deferredList = languageList.map { language -> fetchPosts(langPostsCount, language) }
        // Wait for all deferred tasks to complete and add each list of posts to the post list
        deferredList.awaitAll().forEach { postsList.add(it.filterNotNull()) }
        // Combine all lists of posts into a single list and remove any null elements
        return@withContext alternateLists(postsList)
    }

    /**
     * Combines multiple lists of posts into a single list, removing any null elements
     * @param lists list of post lists to combine
     * @return combined list of posts
     */
    private fun alternateLists(lists: List<List<Post?>>): List<Post> {
        // Create an empty list to store the combined posts
        val combinedList = mutableListOf<Post>()
        // Determine the maximum size of the post lists and use it to iterate through each index
        val size = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until size) {
            // Iterate through each post list and add the post at the current index to the combined list
            for (list in lists) {
                if (list.size > i && list[i] != null) {
                    combinedList.add(list[i]!!)
                }
            }
        }
        return combinedList
    }

    /**
     * Fetches a list of posts for a specific language from the Realtime Database.
     * @param langPostsCount The number of posts to fetch for the specified language.
     * @param language The language for which to fetch posts.
     * @return A list of Post objects fetched from the Realtime Database for the specified language.
     */
    private suspend fun fetchPosts(
        langPostsCount: Int,
        language: Language,
    ): CompletableDeferred<List<Post?>> = withContext(Dispatchers.IO) {
        // Get a reference to the posts node in the Realtime Database for the specified language
        // If the build type is "admin" or "adminDebug", fetch all posts for the specified language
        val deferred = CompletableDeferred<List<Post?>>()
        val query =
            if (BUILD_TYPE == "admin" || BUILD_TYPE == "adminDebug") FirebaseDatabase.getInstance()
                .getReference("posts_${language.code}").limitToLast(langPostsCount)
            else
            // Otherwise, fetch only posts that haven't been watched by the current user
                FirebaseDatabase.getInstance().getReference("posts_${language.code}")
                    .orderByChild("${Const.kWatchedBytKey}/${User.currentKey()}").equalTo(null)
                    .limitToLast(langPostsCount)
// Retrieve the data from the Realtime Database and map it to a list of Post objects
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.reversed().mapNotNull { dataSnapshot ->
                    Post.deserialize(dataSnapshot, language)
                }
                deferred.complete(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                // If an error occurs, log the error and return an empty list
                Log.e(TAG, "Error fetching posts for language $language", error.toException())
                deferred.complete(emptyList())
            }
        })
        return@withContext deferred
    }

    /**
     * Uploads a new post to the Realtime Database for the specified language.
     * @param videoUrl the URL of the video to be uploaded
     * @param thumbnail the URL of the thumbnail image for the video
     * @param language the language of the post
     */
    fun uploadPost(videoUrl: String, thumbnail: String, language: Language) {
        // Create a new Post object with the provided video URL, thumbnail URL, and current timestamp
        val newPost =
            Post(image = thumbnail, video = videoUrl, timestamp = System.currentTimeMillis())
        // Get a reference to the posts node in the Realtime Database for the specified language
        FirebaseDatabase.getInstance().getReference("posts_${language.code}")
            // Push the new post to the Realtime Database
            .push().setValue(newPost)
    }

}
