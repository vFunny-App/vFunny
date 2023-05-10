package androidTest

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.database.FirebaseDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MyFirebaseTest {
    private lateinit var database: FirebaseDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
    }

    @After
    fun tearDown() {
        database.reference.removeValue()
        database.goOffline()
    }

    @Test
    fun writeAndReadFromFirebase() {
        val reference = database.reference.child("test")
        reference.setValue("Hello, world!")
            .addOnSuccessListener {
                reference.get().addOnSuccessListener { snapshot ->
                    assertEquals("Hello, world!", snapshot.value)
                }
            }
    }
}