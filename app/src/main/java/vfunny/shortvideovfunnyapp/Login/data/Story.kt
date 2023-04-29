package vfunny.shortvideovfunnyapp.Login.data

import com.google.firebase.database.FirebaseDatabase
import vfunny.shortvideovfunnyapp.Login.data.User.Companion.current

/**
 * Created on 28/10/2021.
 * Copyright by Shresthasaurabh86@gmail.com
 */
class Story {
    private var user: String? = null
    private var image: String? = null
    private var video: String? = null

    companion object {
        @JvmStatic
        fun uploadVideoStory(video: String, thumbnail: String) {
            val newStory = Story()
            newStory.user = current()!!.key
            newStory.image = thumbnail
            newStory.video = video
            uploadStory(newStory)
        }

        private fun uploadStory(story: Story) {
            FirebaseDatabase.getInstance().getReference(Const.kDataPostKey).push()
                .setValue(story)
        }
    }
}