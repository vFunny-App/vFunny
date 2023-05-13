package vfunny.shortvideovfunnyapp.Post.model

import com.google.firebase.database.DataSnapshot


/**
 * Created on 02/05/2019.
 * Copyright by shresthasaurabh86@gmail.com
 */
data class Post(
    var image: String? = null,
    var video: String? = null,
    var key: String? = null,
    var migrated: Boolean? = null,
    var timestamp: Any? = null,
    var language: Language? = null,
    var watchedBy: List<String>? = null,
) {

    companion object {
        fun deserialize(data: DataSnapshot, language: Language): Post {
            val image = data.child("image").getValue(String::class.java)
            val video = data.child("video").getValue(String::class.java)
            val key = data.key
            val migrated = data.child("migrated").getValue(Boolean::class.java)
            val timestamp = data.child("timestamp").getValue(Long::class.java)
            return Post(image, video, key, migrated, timestamp, language)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Post) {
            return image == other.image && video == other.video && timestamp == other.timestamp
        }
        return false
    }

    override fun toString(): String {
        return "Post(image=$image, video=$video, key=$key, migrated=$migrated, timestamp=$timestamp, language=$language)"
    }
}