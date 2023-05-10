package vfunny.shortvideovfunnyapp.Login.PostsUtils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import vfunny.shortvideovfunnyapp.Login.data.Const
import vfunny.shortvideovfunnyapp.Login.data.Language
import vfunny.shortvideovfunnyapp.Login.data.Post
import vfunny.shortvideovfunnyapp.Login.data.User
import java.util.concurrent.CountDownLatch

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
    fun getPosts(context: Context, languageList: List<Language>): List<Post> {
        val latch = CountDownLatch(languageList.size)
        val wwList = ArrayList<Post>()
        val enList = ArrayList<Post>()
        val hiList = ArrayList<Post>()
        val otList = ArrayList<Post>()
        if (languageList.contains(Language.WORLDWIDE)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostWorldwideKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(wwList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostWorldwideKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostWorldwideKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostWorldwideKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostWorldwideKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        }
        if (languageList.contains(Language.ENGLISH)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostEnglishKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(enList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostEnglishKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostEnglishKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostEnglishKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostEnglishKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        }
        if (languageList.contains(Language.HINDI)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostHindiKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(hiList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostHindiKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostHindiKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostHindiKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostHindiKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        }
        if (languageList.contains(Language.OTHERS)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostOtherKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(otList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostOtherKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostOtherKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostOtherKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostOtherKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        }
        // Wait for all queries to complete before continuing
        latch.await()

        // Return the final result list
        return alternateLists(wwList, enList, hiList, otList)
    }

    fun getAdminPosts(context: Context, languageList: List<Language>): List<Post> {
        val latch = CountDownLatch(languageList.size)
        val wwList = ArrayList<Post>()
        val enList = ArrayList<Post>()
        val hiList = ArrayList<Post>()
        val otList = ArrayList<Post>()
        if (languageList.contains(Language.WORLDWIDE)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostWorldwideKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(wwList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostWorldwideKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostWorldwideKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostWorldwideKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostWorldwideKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        }
        if (languageList.equals(Language.ENGLISH)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostEnglishKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(enList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostEnglishKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostEnglishKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostEnglishKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostEnglishKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        } else {
            latch.countDown()
        }
        if (languageList.equals(Language.HINDI)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostHindiKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(hiList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostHindiKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostHindiKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostHindiKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostHindiKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        } else {
            latch.countDown()
        }
        if (languageList.equals(Language.OTHERS)) {
            FirebaseFirestore.getInstance().collection(Const.kDataPostOtherKey)
                .whereNotIn(Const.kWatchedBytKey, listOf(User.currentKey()))
                .orderBy(Const.kWatchedBytKey)
                .orderBy(Const.kCreatedOnKey, Query.Direction.DESCENDING)
                .limit((100 / languageList.size).toLong()).get().addOnSuccessListener { snapshot ->
                    addToList(otList, snapshot)
                    latch.countDown()
                }.addOnFailureListener {
                    Log.e(TAG,
                        "getPosts: Error getting ${Const.kDataPostOtherKey}  language contents $it")
                    Toast.makeText(context,
                        "Error getting ${Const.kDataPostOtherKey}  language contents $it",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }.addOnCanceledListener {
                    Log.e(TAG,
                        "getPosts: User Cancelled Getting ${Const.kDataPostOtherKey} contents ")
                    Toast.makeText(context,
                        "User Cancelled Getting ${Const.kDataPostOtherKey} contents",
                        Toast.LENGTH_SHORT).show()
                    latch.countDown()
                }
        } else {
            latch.countDown()
        }
        // Wait for all queries to complete before continuing
        latch.await()

        // Return the final result list
        return alternateLists(wwList, enList, hiList, otList)
    }

    private fun addToList(langList: ArrayList<Post>, querySnapshot: QuerySnapshot?) {
        langList.clear()
        querySnapshot?.documents?.forEach { postSnapshot ->
            // Convert the post data to a Post object
            val post: Post? = postSnapshot.toObject(Post::class.java)
            if (post != null) {
                post.key = postSnapshot.id
                // Add the key to the Post object
                langList.add(post)
            }
        }
    }

    private fun alternateLists(
        wwList: ArrayList<Post>,
        enList: ArrayList<Post>,
        hiList: ArrayList<Post>,
        otList: ArrayList<Post>,
    ): ArrayList<Post> {
        val resultList = ArrayList<Post>()
        var wwIndex = 0
        var enIndex = 0
        var hiIndex = 0
        var otIndex = 0
        while (wwIndex < wwList.size || enIndex < enList.size || hiIndex < hiList.size || otIndex < otList.size) {
            if (wwIndex < wwList.size) {
                resultList.add(wwList[wwIndex])
                wwIndex++
            }
            if (enIndex < enList.size) {
                resultList.add(enList[enIndex])
                enIndex++
            }
            if (hiIndex < hiList.size) {
                resultList.add(hiList[hiIndex])
                hiIndex++
            }
            if (otIndex < otList.size) {
                resultList.add(otList[otIndex])
                otIndex++
            }
        }
        return resultList
    }
}