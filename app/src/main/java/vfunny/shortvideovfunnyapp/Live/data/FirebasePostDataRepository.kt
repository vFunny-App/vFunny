package vfunny.shortvideovfunnyapp.Live.data

import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.player.models.VideoData
import com.videopager.data.VideoDataRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebasePostDataRepository : VideoDataRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("posts")
    private val currentUser: String? = FirebaseAuth.getInstance().uid

    override fun videoData(): Flow<List<VideoData>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val videoData = snapshot.children.mapNotNull { childSnapshot ->
                    val postId = childSnapshot.key.orEmpty()
                    val mediaUri = childSnapshot.child("mediaUri").getValue(String::class.java).orEmpty()
                    val previewImageUri = childSnapshot.child("previewImageUri").getValue(String::class.java).orEmpty()
                    val width = childSnapshot.child("width").getValue(Int::class.java)
                    val height = childSnapshot.child("height").getValue(Int::class.java)
                    val aspectRatio = calculateAspectRatio(width, height)

                    if (postId.isNotBlank() && mediaUri.isNotBlank() && previewImageUri.isNotBlank()) {
                        VideoData(
                            id = postId,
                            mediaUri = mediaUri,
                            previewImageUri = previewImageUri,
                            aspectRatio = aspectRatio
                        )
                    } else {
                        null
                    }
                }

                trySend(videoData).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.addValueEventListener(listener)

        awaitClose {
            database.removeEventListener(listener)
        }
    }

    private fun calculateAspectRatio(width: Int?, height: Int?): Float? {
        return if (width != null && height != null) {
            width.toFloat() / height.toFloat()
        } else {
            null
        }
    }
}