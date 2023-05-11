package androidTest

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vfunny.shortvideovfunnyapp.Login.PostsUtils.PostsManager
import vfunny.shortvideovfunnyapp.Login.data.Language

class MyFirebaseTest {
    private lateinit var database: FirebaseDatabase
    private lateinit var context: Context
    private val TAG = "LanguageTest"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
    }

    @Test
    fun getPostsTest() {
        // Code to test your method
        println("$TAG, getPostsTest")
    }

}