package vfunny.shortvideovfunnyapp.Live.ui.migrate

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import vfunny.shortvideovfunnyapp.Post.model.Post
import vfunny.shortvideovfunnyapp.databinding.MigrateListActivityBinding


open class MigrateListActivity : AppCompatActivity() {
    private val TAG: String = "MIGRATE"
    val posts: ArrayList<Post> = ArrayList()
    private lateinit var mAdapter: MigratePostAdapter
    private val displayMetrics = DisplayMetrics()
    private lateinit var binding : MigrateListActivityBinding
    private var FETCH_COUNT = 0
    private var TOTAL_FETCH_COUNT = 0

    companion object {
        private const val MAX_POSTS = 100
        private const val MAX_RETRIES = 20
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MigrateListActivityBinding.inflate(layoutInflater)
        //Initialize Adapter
        binding.mRecyclerView.setHasFixedSize(true)
        binding.mRecyclerView.layoutManager = StaggeredGridLayoutManager(4, RecyclerView.VERTICAL)
        addFirstPosts(MAX_POSTS, null)
        setContentView(binding.root)
    }

    open fun addFirstPosts(mPosts: Int, lastKey: String?) {
        if(FETCH_COUNT >= MAX_RETRIES ) {
            MaterialAlertDialogBuilder(this@MigrateListActivity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("WARNING!")
                .setMessage("Tried $TOTAL_FETCH_COUNT times to fetch $MAX_POSTS posts" +
                        "\n Found ${posts.size} so far." +
                        "\n Do you want to continue searching for more posts?")
                .setNegativeButton("No") { dialog, _ ->
                    setAdapterToRecycler()
                    dialog.dismiss()
                }
                .setPositiveButton("Yes, $FETCH_COUNT more times") { dialog, _ ->
                    FETCH_COUNT = 0
                    addFirstPosts(mPosts, lastKey)
                    dialog.dismiss()
                }
                .show()
        } else {
            FETCH_COUNT++
            TOTAL_FETCH_COUNT++
            var ref = FirebaseDatabase.getInstance().reference.child("posts").limitToLast(mPosts + 1)
            if(!lastKey.isNullOrEmpty()) {
                ref = FirebaseDatabase.getInstance().reference.child("posts").orderByKey().endAt(lastKey).limitToLast(mPosts + 1)
            }
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                var nextKey = ""
                override fun onDataChange(snapshot: DataSnapshot) {
                    for ((index, it) in snapshot.children.withIndex()) {
                        val post = it.getValue(Post::class.java)
                        if (post != null) {
                            if(index == 0) {
                                it.key?.let { nextSnapshotKey ->
                                    nextKey = nextSnapshotKey
                                }
                                continue
                            }
                            if (post.migrated == true) {
                                Log.e(TAG, "post skipped :  $post")
                            } else {
                                post.key = it.key
                                Log.e(TAG, "post added :  ${post.key}")
                                posts.add(post)
                            }
                        }
                    }
                    if (TOTAL_FETCH_COUNT == 1) {
                        Log.e(TAG, "Reversing first posts")
                        posts.reverse()
                    }
                    if(posts.isEmpty()){
                        addFirstPosts(MAX_POSTS, nextKey)
                        return
                    } else if (posts.size < MAX_POSTS){
                        addFirstPosts(MAX_POSTS - posts.size, nextKey)
                        return
                    }
                    setAdapterToRecycler()
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }
    }

    private fun setAdapterToRecycler() {
        MaterialAlertDialogBuilder(this@MigrateListActivity, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Showing ${posts.size } Videos" )
            .setMessage("Total Retries $TOTAL_FETCH_COUNT " +
                    "\n" + " ")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val itemWidth = screenWidth / 4
        mAdapter = MigratePostAdapter(this@MigrateListActivity, this@MigrateListActivity.imageLoader, itemWidth, posts)
        binding.mRecyclerView.adapter = mAdapter
    }

}
