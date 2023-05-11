package vfunny.shortvideovfunnyapp.Live.ui

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import com.google.firebase.database.*
import vfunny.shortvideovfunnyapp.Data.model.Post
import vfunny.shortvideovfunnyapp.databinding.ListActivityBinding


open class ListActivity : AppCompatActivity() {
    private val TAG: String = "LISTING"
    val posts: ArrayList<Post> = ArrayList()
    private lateinit var mAdapter: PostAdapter
    private val displayMetrics = DisplayMetrics()
    val data: MutableList<Post> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ListActivityBinding.inflate(layoutInflater)
        //Initialize Adapter
        binding.mRecyclerView.setHasFixedSize(true)
        binding.mRecyclerView.layoutManager = StaggeredGridLayoutManager(4, RecyclerView.VERTICAL)
        addFirstPosts(1000, binding)
        setContentView(binding.root)
    }

    open fun addFirstPosts(mPosts: Int, binding: ListActivityBinding) {
        val ref = FirebaseDatabase.getInstance().reference.child("posts").limitToLast(mPosts)
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                Log.e(TAG, "onChildAdded: ITEM ADDED")
                val post = dataSnapshot.getValue(Post::class.java)
                if (post != null) {
                    post.key = dataSnapshot.key
                    posts.add(post)
                }
                posts.reverse()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val itemWidth = screenWidth / 4
                mAdapter = PostAdapter(this@ListActivity, this@ListActivity.imageLoader, itemWidth, posts)
                binding.mRecyclerView.adapter = mAdapter
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Toast.makeText(this@ListActivity, "ITEM CHANGED", Toast.LENGTH_SHORT).show()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.e(TAG, "onChildRemoved: CHILD REMOVED ${snapshot.key}")
                Toast.makeText(this@ListActivity, "ITEM CHANGED", Toast.LENGTH_SHORT).show()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                Log.e(TAG, "onChildRemoved: CHILD MOVED ${snapshot.key}")
                Toast.makeText(this@ListActivity, "ITEM MOVED", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onChildRemoved: CHILD MOVED $error")
                Toast.makeText(this@ListActivity, "ERROR $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
