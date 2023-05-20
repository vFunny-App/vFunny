package vfunny.shortvideovfunnyapp.Lang.ui.ui.main

import android.R
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.imageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import vfunny.shortvideovfunnyapp.Lang.ui.LangListAdapter
import vfunny.shortvideovfunnyapp.Post.model.Language
import com.videopager.models.Post
import vfunny.shortvideovfunnyapp.databinding.FragmentLangListBinding

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentLangListBinding? = null

    private lateinit var mAdapter: LangListAdapter
    val posts: ArrayList<Post> = ArrayList()
    private val displayMetrics = DisplayMetrics()
    private var FETCH_COUNT = 0
    private var TOTAL_FETCH_COUNT = 0
    private var languageIndex = 1


    companion object {
        private const val TAG = "PlaceholderFragment"
        private const val MAX_POSTS = 50
        private const val MAX_RETRIES = 20

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this)[PageViewModel::class.java].apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
        languageIndex = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLangListBinding.inflate(inflater, container, false)
        val root = binding.root

        val textView: TextView = binding.sectionLabel

        pageViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        addFirstPosts(MAX_POSTS, null)

        return root
    }


    private fun setAdapterToRecycler() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Material_Dialog_Alert)
            .setTitle("${Language.getAllLanguages()[languageIndex].name}")
            .setMessage("Showing ${posts.size} Videos \n " +
                    "Total Retries $TOTAL_FETCH_COUNT ")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val itemWidth = screenWidth / 4
        if (context != null) {
            mAdapter = LangListAdapter(requireContext(),
                requireActivity().imageLoader,
                itemWidth,
                posts)
            binding.recyclerView.setHasFixedSize(true)
            binding.recyclerView.layoutManager = StaggeredGridLayoutManager(4, RecyclerView.VERTICAL)
            binding.recyclerView.adapter = mAdapter
        }
    }


    fun addFirstPosts(mPosts: Int, lastKey: String?) {
        if (context != null) {
            if (FETCH_COUNT >= MAX_RETRIES) {
                MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Material_Dialog_Alert)
                    .setTitle("WARNING!(${Language.getAllLanguages()[languageIndex].name})")
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
                var ref =
                    FirebaseDatabase.getInstance().reference.child("posts_${Language.getAllLanguages()[languageIndex].code}")
                        .limitToLast(mPosts + 1)
                if (!lastKey.isNullOrEmpty()) {
                    ref = FirebaseDatabase.getInstance().reference.child("posts_${Language.getAllLanguages()[languageIndex].code}").orderByKey()
                        .endAt(lastKey).limitToLast(mPosts + 1)
                }
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    var nextKey = ""
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for ((index, it) in snapshot.children.withIndex()) {
                            val post = it.getValue(Post::class.java)
                            if (post != null) {
                                if (index == 0) {
                                    it.key?.let { nextSnapshotKey ->
                                        nextKey = nextSnapshotKey
                                    }
                                    continue
                                }
                                if (post.migrated == true) {
                                    Log.e(TAG, "post skipped :  $post")
                                } else {
                                    post.key = it.key
                                    post.language = Language.getAllLanguages()[languageIndex]
                                    Log.e(TAG, "post added :  ${post.key}")
                                    posts.add(post)
                                }
                            }
                        }
                        if (TOTAL_FETCH_COUNT == 1) {
                            Log.e(TAG, "Reversing first posts")
                            posts.reverse()
                        }
                        if (posts.isEmpty()) {
                            addFirstPosts(MAX_POSTS, nextKey)
                            return
                        } else if (posts.size < MAX_POSTS) {
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
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}