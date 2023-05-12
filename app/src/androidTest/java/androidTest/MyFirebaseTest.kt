package androidTest

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import org.junit.Before
import org.junit.Test
import vfunny.shortvideovfunnyapp.Post.model.Post
import java.text.SimpleDateFormat
import java.util.*

class MyFirebaseTest {
    private lateinit var database: FirebaseDatabase
    private lateinit var context: Context

    companion object {
        private val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
        private const val TAG: String = "LanguageTest"
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FirebaseDatabase.getInstance()
    }

    @Test
    fun getPostsTest() {
        // Code to test your method

        val ref = database.reference.child("posts").limitToLast(10)
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                val post = dataSnapshot.getValue(Post::class.java)
                Log.e(TAG, "onChildAdded: ${dataSnapshot.key}")
                post?.timestamp?.let {
                        try {
                            if(it is Long) {
                                Log.e(TAG, "Time : ${simpleDateFormat.format(0 - it)}")
                            } else {
                                Log.e(TAG, "$it is not long")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting timestamp $e")
                        }
                    }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}